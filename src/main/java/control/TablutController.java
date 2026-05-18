package control;

import boardifier.control.ActionFactory;
import boardifier.control.ActionPlayer;
import boardifier.control.Controller;
import boardifier.model.GameElement;
import boardifier.model.ContainerElement;
import boardifier.model.Model;
import boardifier.model.Player;
import boardifier.model.action.ActionList;
import boardifier.view.View;
import model.Pawn;
import model.TablutStageModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TablutController extends Controller {

    BufferedReader consoleIn;
    boolean firstPlayer;
    String firstPlayerColor;
    String secondPlayerColor;

    public TablutController(Model model, View view) {
        super(model, view);
        firstPlayer = true;
        firstPlayerColor = "Green";
        secondPlayerColor = "Yellow";
    }

    public void setFirstPlayerColor(String firstPlayerColor) {
        this.firstPlayerColor = firstPlayerColor;
        if (firstPlayerColor.equals("Green")) {
            secondPlayerColor = "Yellow";
        } else {
            secondPlayerColor = "Green";
        }
    }

    /**
     * Defines what to do within the single stage of the single party
     * It is pretty straight forward to write :
     */
    public void stageLoop() {
        consoleIn = new BufferedReader(new InputStreamReader(System.in));
        update();
        while(! model.isEndStage()) {
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
            System.out.println("COMPUTER PLAYS");
            TablutDecider decider = new TablutDecider(model,this);
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

        int colSrc = line.charAt(0) - 'A';
        int rowSrc = line.charAt(1) - '1';
        int colDest = line.charAt(2) - 'A';
        int rowDest = line.charAt(3) - '1';

        if (colSrc<0 || rowSrc<0 || colDest>8 || rowDest>8) return false;


        GameElement elementSrc = gameStage.getBoard().getElement(rowSrc, colSrc);
        Pawn pawn;
        if (!(elementSrc instanceof Pawn)) {
            return false;
        } else {
            pawn = (Pawn) elementSrc;
        }

        GameElement elementDst = gameStage.getBoard().getElement(rowDest, colDest);
        if (elementDst instanceof Pawn p) {
            return false;
        }


        String playerColor = model.getIdPlayer() == 0 ? firstPlayerColor : secondPlayerColor;

        // check if selected pawn does not belong to the current player
        if (playerColor.equals("Green") && pawn.getColor() == Pawn.PAWN_MOSCOVITE) {
            return false;
        }
        else if (playerColor.equals("Yellow") && pawn.getColor() != Pawn.PAWN_MOSCOVITE) {
            return false;
        }
        gameStage.getBoard().setValidCells(pawn.getNumber());
        if (!gameStage.getBoard().canReachCell(rowDest, colDest)) return false;


        ActionList actions = ActionFactory.generateMoveWithinContainer(model, elementSrc, rowDest, colDest);
        actions.setDoEndOfTurn(true);
        ActionPlayer play = new ActionPlayer(model, this, actions);
        play.start();





//        // get the pawn value from the first char
//        int pawnIndex = (int) (line.charAt(0) - '1');
//        if ((pawnIndex<0)||(pawnIndex>3)) return false;
//        // get the ccords in the board
//        int col = (int) (line.charAt(1) - 'A');
//        int row = (int) (line.charAt(2) - '1');
//        // check coords validity
//        if ((row<0)||(row>8)) return false;
//        if ((col<0)||(col>8)) return false;
//        // check if the pawn is still in its pot
//        ContainerElement pot = null;
//        if (model.getIdPlayer() == 0) {
//            pot = gameStage.getBlackPot();
//        }
//        else {
//            pot = gameStage.getRedPot();
//        }
//        if (pot.isEmptyAt(pawnIndex,0)) return false;
//        GameElement pawn = pot.getElement(pawnIndex,0);
//        // compute valid cells for the chosen pawn
//        gameStage.getBoard().setValidCells(pawnIndex+1);
//        if (!gameStage.getBoard().canReachCell(row,col)) return false;
//
//        ActionList actions = ActionFactory.generatePutInContainer(model, pawn, "tablutboard", row, col);
//        actions.setDoEndOfTurn(true); // after playing this action list, it will be the end of turn for current player.
//        ActionPlayer play = new ActionPlayer(model, this, actions);
//        play.start();
        return true;
    }
}
