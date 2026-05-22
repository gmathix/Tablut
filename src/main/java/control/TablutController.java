package control;

import boardifier.control.ActionFactory;
import boardifier.control.ActionPlayer;
import boardifier.control.Controller;
import boardifier.control.Decider;
import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.Player;
import boardifier.model.action.ActionList;
import boardifier.view.View;
import com.sun.java.accessibility.util.SwingEventMonitor;
import model.Pawn;
import model.TablutStageModel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TablutController extends Controller {

    public static final int NEGAMAX_PLAYER = 0;
    public static final int MONTECARLO_PLAYER = 1;
    public static final int NEGAMONTECARLO_PLAYER = 2;

    public static record BotSelection(int type, String name, Supplier<Decider> supplier) {}


    public static final Map<Integer, BotSelection> availableBots[] = new Map[2];



    int gameMode;
    int botPlayers[];

    BufferedReader consoleIn;
    String inputFile;

    public TablutController(Model model, View view, int gameMode, String inputFile,
                            int greenBotPlayer, int yellowBotPlayer, int botLevels[]) {
        super(model, view);
        this.gameMode = gameMode;
        this.inputFile = inputFile;
        this.botPlayers = new int[]{
            NEGAMAX_PLAYER,
            NEGAMAX_PLAYER,
        };

        availableBots[0] = new HashMap<>();
        availableBots[1] = new HashMap<>();

        setBotLevel(0, botLevels[0]);
        setBotLevel(1, botLevels[1]);

    }

    public TablutController(Model model, View view, int gameMode, String inputFile) {
        this(model, view, gameMode, inputFile, NEGAMAX_PLAYER, NEGAMAX_PLAYER, new int[]{5, 5});
    }


    // 0 for green, 0 for yellow
    public void setBotLevel(int color, int level) {
        availableBots[color].clear();
        availableBots[color].put(NEGAMAX_PLAYER, new BotSelection(NEGAMAX_PLAYER, "Negamax",
                () -> new NegamaxDecider(model, this, level)));
        availableBots[color].put(MONTECARLO_PLAYER, new BotSelection(MONTECARLO_PLAYER, "Monte-Carlo",
                () -> new MonteCarloDecider(model, this, level)));
        availableBots[color].put(NEGAMONTECARLO_PLAYER, new BotSelection(NEGAMONTECARLO_PLAYER, "Nega-Monte-Carlo",
                () -> new NegaMonteCarloDecider(model, this, level)));
    }




    public void setBotPlayer(int color, int botPlayer) {
        if (botPlayer < 0 || botPlayer > availableBots[color].size()) return;
        this.botPlayers[color] = botPlayer;
    }

    public Map<Integer, BotSelection>[] getAvailableBots() {
        return availableBots;
    }




    /**
     *Implementing a file reader to read the entry file
     * Defines what to do within the single stage of the single party
     * It is pretty straight forward to write :
     */
    public void stageLoop() {
        if (!inputFile.isEmpty() && gameMode == 0) {
            try {
                consoleIn = new BufferedReader(new FileReader(inputFile));
                System.out.println("game scenario based on the entry file  : " + inputFile);
            } catch (IOException e) {
                System.out.println("Error: \"" + inputFile + " not found. fallback to player vs player\". ");
                consoleIn = new BufferedReader(new InputStreamReader(System.in));

            }

        } else {
            consoleIn = new BufferedReader(new InputStreamReader(System.in));
        }
        update();
        while (!model.isEndStage()) {
            playTurn();
            endOfTurn();
            update();
        }
        endGame();
    }



    private void playTurn() {
        // get the new player
        Player p = model.getCurrentPlayer();
        if (p.getType() == Player.COMPUTER) {
            int turn = model.getIdPlayer();
            BotSelection selection = availableBots[turn].get(botPlayers[turn]);
            Decider decider = selection.supplier.get();
            ActionPlayer play = new ActionPlayer(model, this, decider, null);

            play.start();
        }
        else {
            boolean ok = false;
            while (!ok) {
                System.out.print(p.getName()+ " > ");
                try {
                    String line = consoleIn.readLine();
                    if (line.length() == 4) {
                        ok = analyseAndPlay(line);
                    }
                    if (!ok) {
                        System.out.println("incorrect instruction. retry !");
                    }
                }
                catch(IOException e) {}
            }
        }
    }

    public void endOfTurn() {

        model.setNextPlayer();
        // get the new player to display its name
        Player p = model.getCurrentPlayer();
        TablutStageModel stageModel = (TablutStageModel) model.getGameStage();
        stageModel.getPlayerName().setText(p.getName());
    }
    private boolean analyseAndPlay(String line) {
        TablutStageModel gameStage = (TablutStageModel) model.getGameStage();

        line = line.toUpperCase();
        boolean isYellow = model.getIdPlayer() == 1;


        if (line.contains("STOP")) {
            stopStage();
        }


        int colSrc = line.charAt(0) - 'A';
        int rowSrc = line.charAt(1) - '1';
        int colDest = line.charAt(2) - 'A';
        int rowDest = line.charAt(3) - '1';

        if (colSrc<0 || rowSrc<0 || colDest>8 || rowDest>8) return false;


        GameElement elementSrc = gameStage.getBoard().getElement(rowSrc, colSrc);
        Pawn currPawn;
        // check that the selected square contains a pawn
        if (!(elementSrc instanceof Pawn)) {
            return false;
        } else {
            currPawn = (Pawn) elementSrc;
        }

        // check that the destination square is empty
        GameElement elementDst = gameStage.getBoard().getElement(rowDest, colDest);
        if (elementDst instanceof Pawn p) {
            return false;
        }


        // check if selected pawn does not belong to the current player
        if (model.getIdPlayer() == 0 && currPawn.getColor() == Pawn.PAWN_MOSCOVITE) {
            return false;
        }
        else if (model.getIdPlayer() == 1 && currPawn.getColor() != Pawn.PAWN_MOSCOVITE) {
            return false;
        }

        // check that this is a legal move
        gameStage.getBoard().setValidCells(currPawn.getNumber());
        if (!gameStage.getBoard().canReachCell(rowDest, colDest)) return false;



        // update the board's king coordinates if we just moved the king
        if (currPawn.getColor() == Pawn.PAWN_KING) {
            gameStage.getBoard().setKingX(colDest);
            gameStage.getBoard().setKingY(rowDest);
        }




        // check capture
        gameStage.checkCapture(isYellow, colSrc, colDest, rowSrc, rowDest);




        // make move
        ActionList actions = ActionFactory.generateMoveWithinContainer(model, elementSrc, rowDest, colDest);
        actions.setDoEndOfTurn(true);
        ActionPlayer play = new ActionPlayer(model, this, actions);
        play.start();




        return true;
    }
}