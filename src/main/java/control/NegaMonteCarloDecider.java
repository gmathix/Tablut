package control;

import boardifier.control.Controller;
import boardifier.control.Decider;
import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import control.algos.*;
import model.Move;
import model.TablutBoard;
import model.TablutStageModel;

public class NegaMonteCarloDecider extends Decider {
    private int level;


    public NegaMonteCarloDecider(Model model, Controller control, int level) {
        super(model, control);
        this.level = level;
    }

    public NegaMonteCarloDecider(Model model, Controller controller) {
        this(model, controller, 5);
    }


    public int getLevel() { return level; }




    @Override
    public ActionList decide() {
        // do a cast get a variable of the real type to get access to the attributes of HoleStageModel
        TablutStageModel stage = (TablutStageModel)model.getGameStage();
        TablutController tablutControl = (TablutController) control;
        TablutBoard tablutBoard = stage.getBoard(); // get the board
        control.algos.RecurBoard recurBoard = new control.algos.RecurBoard(tablutBoard, model.getGameStage());
        GameElement pawn = null; // the pawn that is moved

        int turn = model.getIdPlayer();



        NegaMonteCarlo.resetBuffers();
        NegaMonteCarlo.configure(level, tablutBoard);
        int bestMoveInt = NegaMonteCarlo.findBestMove(tablutBoard, turn, ((TablutController) control).isBoardRepeated());

        int src = bestMoveInt & 0x7F;
        int dst = (bestMoveInt >> 7) & 0x7F;
        RecurMove bestMove = new RecurMove(recurBoard, src % 9, src / 9, dst % 9, dst / 9);


        pawn = tablutBoard.getElement(bestMove.srcY(), bestMove.srcX());

        if (recurBoard.isKing(recurBoard.getBoard()[bestMove.srcY()][bestMove.srcX()])) {
            tablutBoard.setKingY(bestMove.dstY());
            tablutBoard.setKingX(bestMove.dstX());
        }

        tablutControl.getMoveHistoryIterator().add(new Move(tablutBoard, bestMove.srcX(), bestMove.srcY(), bestMove.dstX(), bestMove.dstY()));


        return tablutControl.genMoveAnimationWithCapture(model, pawn, tablutBoard, bestMove.dstY(), bestMove.dstX());
    }
}
