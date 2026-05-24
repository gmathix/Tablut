package control;

import boardifier.control.ActionFactory;
import boardifier.control.ActionPlayer;
import boardifier.control.Controller;
import boardifier.control.Decider;
import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import control.algos.RecurBoard;
import control.algos.NegamaxSearch;
import model.*;

public class NegamaxDecider extends Decider {
    private int depth;


    public NegamaxDecider(Model model, Controller control, int level) {
        super(model, control);
        this.depth = level;
        if (depth < 0) depth = 0;
        if (depth > 10) depth = 10;
    }

    public NegamaxDecider(Model model, Controller controller) {
        this(model, controller, 5);
    }

    public void setLevel(int level) {
        this.depth = level;
        if (depth < 0) depth = 0;
        if (depth > 10) depth = 10;
    }




    @Override
    public ActionList decide() {
        // do a cast get a variable of the real type to get access to the attributes of HoleStageModel
        TablutStageModel stage = (TablutStageModel)model.getGameStage();
        TablutBoard tablutBoard = stage.getBoard(); // get the board
        RecurBoard recurBoard = new RecurBoard(tablutBoard);
        GameElement pawn = null; // the pawn that is moved

        int turn = model.getIdPlayer();

        NegamaxSearch negamaxSearch = new NegamaxSearch(depth);



        Move bestMove = negamaxSearch.findBestMove(recurBoard, turn, ((TablutController) control).isBoardRepeated());
        if (bestMove == null) return null;



        pawn = tablutBoard.getElement(bestMove.srcY(), bestMove.srcX());

        stage.checkCapture(turn == 1, bestMove.srcX(), bestMove.dstX(), bestMove.srcY(), bestMove.dstY());
        if (recurBoard.isKing(recurBoard.getBoard()[bestMove.srcY()][bestMove.srcX()])) {
            tablutBoard.setKingY(bestMove.dstY());
            tablutBoard.setKingX(bestMove.dstX());
        }



        ActionList actions = ActionFactory.generateMoveWithinContainer(model, pawn, bestMove.dstY(), bestMove.dstX());
        actions.setDoEndOfTurn(true);
        ActionPlayer play = new ActionPlayer(model, control, actions);
        play.start();

        return actions;
    }
}
