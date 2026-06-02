package control;

import boardifier.control.Controller;
import boardifier.control.Decider;
import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import control.algos.NegamaxSearch;
import control.algos.NegamaxSearchFast;
import control.algos.RecurMove;
import model.*;

public class NegamaxDecider extends Decider {
    private int level;


    public NegamaxDecider(Model model, Controller control, int level) {
        super(model, control);
        this.level = level;

    }

    public NegamaxDecider(Model model, Controller controller) {
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

//        NegamaxSearch negamaxSearch = new NegamaxSearch(level);
        NegamaxSearchFast negamaxSearchFast = new NegamaxSearchFast(tablutBoard, level);

        int bestMoveInt = negamaxSearchFast.findBestMove(turn, ((TablutController) control).isBoardRepeated());

//        RecurMove bestMove = negamaxSearch.findBestMove(recurBoard, turn, ((TablutController) control).isBoardRepeated());

        int src = bestMoveInt & 0x7F;
        int dst = (bestMoveInt >> 7) & 0x7F;
        RecurMove bestMove = new RecurMove(recurBoard, src % 9, src / 9, dst % 9, dst / 9);


        pawn = tablutBoard.getElement(bestMove.srcY(), bestMove.srcX());


        if (recurBoard.isKing(recurBoard.getBoard()[bestMove.srcY()][bestMove.srcX()])) {
            tablutBoard.setKingY(bestMove.dstY());
            tablutBoard.setKingX(bestMove.dstX());
        }
        ((Pawn)pawn).setBoardX(bestMove.dstX());
        ((Pawn)pawn).setBoardY(bestMove.dstY());


        tablutControl.getMoveHistoryIterator().add(new Move(tablutBoard, bestMove.srcX(), bestMove.srcY(), bestMove.dstX(), bestMove.dstY()));


        return tablutControl.genMoveAnimationWithCapture(model, pawn, tablutBoard, bestMove.dstY(), bestMove.dstX());
    }
}
