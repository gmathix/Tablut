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

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TablutController extends Controller {

    BufferedReader consoleIn;
    boolean firstPlayer;

    public TablutController(Model model, View view) {
        super(model, view);
        firstPlayer = true;
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
            TablutNegamaxDecider decider = new TablutNegamaxDecider(model, this, 10);
//            TablutDecider decider = new TablutDecider(model,this);
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



        gameStage.checkCapture(isYellow, colSrc, colDest, rowSrc, rowDest);

//        // check capture
//        int horizontalDirection = 0;
//        int verticalDirection = 0;
//
//        if (colSrc - colDest != 0)
//            horizontalDirection = colDest - colSrc > 0 ? 1 : -1; // 1 for right, -1 for left
//        if (rowSrc - rowDest != 0)
//            verticalDirection = rowDest - rowSrc > 0 ? 1 : -1;   // 1 for down, -1 for up
//
//        int[] dy_vals = {-1, 0, 1, 0};
//        int[] dx_vals = {0, -1, 0, 1};
//
//        for (int i = 0; i < 4; i++) {
//            int dy = dy_vals[i];
//            int dx = dx_vals[i];
//
//            // do not check the squares on the path the pawn came from
//            if (dx == -horizontalDirection && horizontalDirection != 0) continue;
//            if (dy == -verticalDirection && verticalDirection != 0) continue;
//
//            // check bounds for pawn 2 squares away
//            if (rowDest + 2*dy < 0 || rowDest + 2*dy >= 9) continue;
//            if (colDest + 2*dx < 0 || colDest + 2*dx >= 9) continue;
//
//
//            GameElement sideEl = gameStage.getBoard().getElement(rowDest + dy, colDest + dx);
//            GameElement sideEl2 = gameStage.getBoard().getElement(rowDest + 2*dy, colDest + 2*dx);
//
//            if ((sideEl instanceof Pawn sideP) && (sideEl2 instanceof Pawn sideP2)) {
//                if (isYellow) {
//                    if (sideP.getColor() == Pawn.PAWN_SOLDIER && sideP2.getColor() == Pawn.PAWN_MOSCOVITE) {
//                        gameStage.getBoard().removeElement(sideEl);
//                        gameStage.removeElement(sideEl);
//                    }
//                } else {
//                    if (sideP.getColor() == Pawn.PAWN_MOSCOVITE && sideP2.getColor() != Pawn.PAWN_MOSCOVITE) {
//                        gameStage.getBoard().removeElement(sideEl);
//                        gameStage.removeElement(sideEl);
//                    }
//                }
//            }
//        }




        // make move
        ActionList actions = ActionFactory.generateMoveWithinContainer(model, elementSrc, rowDest, colDest);
        actions.setDoEndOfTurn(true);
        ActionPlayer play = new ActionPlayer(model, this, actions);
        play.start();






        return true;
    }
}
