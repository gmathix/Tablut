package control;

import boardifier.control.*;
import boardifier.model.Coord2D;
import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.Player;
import boardifier.model.action.ActionList;
import boardifier.model.action.MoveWithinContainerAction;
import boardifier.model.action.RemoveFromContainerAction;
import boardifier.model.animation.AnimationTypes;
import boardifier.view.*;

import javafx.scene.text.Font;
import model.Pawn;
import model.RuleSets;
import model.TablutBoard;
import model.TablutStageModel;
import view.Constants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Supplier;

public class TablutController extends Controller {

    public static final int NEGAMAX_PLAYER = 0;
    public static final int MONTECARLO_PLAYER = 1;
    public static final int NEGAMONTECARLO_PLAYER = 2;

    public static final int NB_BOARDS_IN_MEMORY = 15;

    public static record BotSelection(int type, String name, Supplier<Decider> supplier) {}


    public static final Map<Integer, BotSelection> availableBots[] = new Map[2];



    int gameMode;
    int botPlayers[];
    int botLevels[];

    BufferedReader consoleIn;
    String inputFile;


    private String lastBoardsRepresentations[];
    private int currentBoardRepIndex;
    private boolean boardRepeated;


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


        this.botLevels = botLevels;

        setBotLevel(0, botLevels[0]);
        setBotLevel(1, botLevels[1]);



        lastBoardsRepresentations = new String[NB_BOARDS_IN_MEMORY];
        for (int i = 0; i < NB_BOARDS_IN_MEMORY; i++) {
            lastBoardsRepresentations[i] = "";
        }
        currentBoardRepIndex = 0;
        boardRepeated = false;


        setControlKey(new TablutKeyController(model, view, this));
        setControlMouse(new TablutMouseController(model, view, this));
        setControlAction(new TablutActionController(model, view, this));

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
        this.botLevels[color] = level;
    }




    public void setBotPlayer(int color, int botPlayer) {
        if (botPlayer < 0 || botPlayer > availableBots[color].size()) return;
        this.botPlayers[color] = botPlayer;
    }

    public Map<Integer, BotSelection>[] getAvailableBots() {
        return availableBots;
    }


    public boolean isBoardRepeated() { return boardRepeated; }



    private void processBoardRepetition() {
        currentBoardRepIndex = (currentBoardRepIndex + 1) % NB_BOARDS_IN_MEMORY;

        String currBoardRep = ((TablutStageModel) model.getGameStage()).getBoard().getStringRepresentation();

        lastBoardsRepresentations[currentBoardRepIndex] = currBoardRep;

        int nbFound = 0;
        for (int i = 0; i < NB_BOARDS_IN_MEMORY; i++) {
            if (i == currentBoardRepIndex) continue;
            if (lastBoardsRepresentations[i].equals(currBoardRep)) {
                nbFound++;
            }
        }

        if (nbFound >= 3) {
            boardRepeated = true;
        } else {
            boardRepeated = false;
        }
    }


    public ActionList genMoveAnimationWithCapture(Model model, GameElement element, TablutBoard board, int dstY, int dstX) {
        ActionList actions = new ActionList();

        ElementLook elementLook = getElementLook(element);
        ContainerLook containerLook = (ContainerLook) getElementLook(board);
        Coord2D center = containerLook.getContainerLocationForLookFromCell(elementLook, dstY, dstX);
        actions.addSingleAction(new MoveWithinContainerAction(
                model, element, dstY, dstX, AnimationTypes.MOVE_LINEARPROP, center.getX(), center.getY(), 10
        ));

        TablutStageModel stageModel = (TablutStageModel) model.getGameStage();
        Pawn pawn = (Pawn) element;
        int capture = stageModel.checkCapture(
                model.getIdPlayer() == 1, pawn.getBoardX(), dstX, pawn.getBoardY(), dstY
        );
        if (capture != -1) {
            GameElement capturedElement = board.getElement(capture / 9, capture % 9);
            actions.addSingleAction(new RemoveFromContainerAction(model, capturedElement));
        }

        actions.setDoEndOfTurn(true);
        stageModel.unselectAll();
        stageModel.setState(TablutStageModel.STATE_SELECTPAWN);

        return actions;
    }



    public void endOfTurn() {

        model.setNextPlayer();
        // get the new player to display its name
        Player p = model.getCurrentPlayer();
        TablutStageModel stageModel = (TablutStageModel) model.getGameStage();
        stageModel.getPlayerName().setText(p.getName());

        GameStageView stageView = view.getGameStageView();

        String player1Text, player2Text;
        Player player1 = model.getPlayers().get(0);
        Player player2 = model.getPlayers().get(1);

        String[] chosenBotSentences;
        int turn = model.getIdPlayer();
        if (botLevels[turn] <= 5) chosenBotSentences = BotSentences.SENTENCES_LOSING;
        else if (botLevels[turn] <= 8) chosenBotSentences = BotSentences.SENTENCES_WINNING;
        else chosenBotSentences = BotSentences.SENTENCES_EXTREMELY_ARROGANT;


        if (model.getIdPlayer() == 0) {
            if (player1.getType() == Player.HUMAN) {
                player1Text = player1.getName() + " : Your Move";
            } else {
                int index = (int) (Math.random() * chosenBotSentences.length);
                player1Text = player1.getName() + " " + chosenBotSentences[index];
            }
            player2Text = player2.getName();
        } else {
            if (player2.getType() == Player.HUMAN) {
                player2Text = player2.getName() + " : Your Move";
            } else {
                int index = (int) (Math.random() * chosenBotSentences.length);
                player2Text = player2.getName() + " " + chosenBotSentences[index];
            }
            player1Text = player1.getName();
        }

        stageModel.getSwedishPlayerText().setText(player1Text);
        stageModel.getMoscovitePlayerText().setText(player2Text);

        if (p.getType() == Player.COMPUTER) {
            turn = model.getIdPlayer();
            BotSelection selection = availableBots[turn].get(botPlayers[turn]);
            Decider decider = selection.supplier.get();
            ActionPlayer play = new ActionPlayer(model, this, decider, null);
            play.start();
        }
    }





    /** maybe someone can clean those up **/

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


                // read starting side from entry file
                String startingSide = consoleIn.readLine().toLowerCase();
                if (startingSide.equals("yellow")) {
                    model.setIdPlayer(1);
                } else if (startingSide.equals("green")) {
                    model.setIdPlayer(0);
                } else {
                    System.out.printf("Invalid starting side in entry file : got %s, expected 'yellow' or 'green'\n");
                }

                // read rulesets from entry file
                int ruleset;
                do {
                    ruleset = Integer.parseInt(consoleIn.readLine());
                    if (ruleset < 0 || ruleset > RuleSets.ruleOptions.size()) {
                        System.out.printf("Invalid ruleset %d, expected 0 <= ruleset <= %d\n",
                                ruleset, RuleSets.ruleOptions.size());
                    } else if (ruleset != 0) {
                        RuleSets.currentRuleset |= RuleSets.ruleOptions.get(ruleset-1).bit();
                    }
                } while (ruleset != 0);

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

            processBoardRepetition();
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


            int botLevel;
            if (decider instanceof NegamaxDecider d) {
                botLevel = d.getLevel();
            } else if (decider instanceof MonteCarloDecider d) {
                botLevel = d.getLevel();
            } else if (decider instanceof NegaMonteCarloDecider d) {
                botLevel = d.getLevel();
            } else {
                botLevel = 5;
            }
            String[] sentenceArray;
            if (botLevel <= 4) {
                sentenceArray = BotSentences.SENTENCES_LOSING;
            } else if (botLevel <= 8) {
                sentenceArray = BotSentences.SENTENCES_WINNING;
            } else {
                sentenceArray = BotSentences.SENTENCES_EXTREMELY_ARROGANT;
            }

            int sentenceIndex = (int) (Math.random() * sentenceArray.length);
            String sentence = sentenceArray[sentenceIndex];

            System.out.printf("%s %s\n", selection.name, sentence);

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
//        ActionList actions = ActionFactory.generateMoveWithinContainer(model, elementSrc, rowDest, colDest);
//        actions.setDoEndOfTurn(true);
//        ActionPlayer play = new ActionPlayer(model, this, actions);
//        play.start();



        return true;
    }
}