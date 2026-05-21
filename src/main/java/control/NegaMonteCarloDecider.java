package control;

import boardifier.control.ActionFactory;
import boardifier.control.ActionPlayer;
import boardifier.control.Controller;
import boardifier.control.Decider;
import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import control.algos.Board;
import control.algos.NegaMonteCarlo;
import control.algos.NegamaxSearch;
import model.Move;
import model.TablutBoard;
import model.TablutStageModel;

public class NegaMonteCarloDecider extends Decider {
    public NegaMonteCarloDecider(Model model, Controller control, int level) {
        super(model, control);
    }




    @Override
    public ActionList decide() {
        // do a cast get a variable of the real type to get access to the attributes of HoleStageModel
        TablutStageModel stage = (TablutStageModel)model.getGameStage();
        TablutBoard tablutBoard = stage.getBoard(); // get the board
        Board board = new Board(tablutBoard);
        GameElement pawn = null; // the pawn that is moved

        int turn = model.getIdPlayer();

        NegaMonteCarlo negaMonteCarlo = new NegaMonteCarlo();



        Move bestMove = new Move(0, 0, 0, 0);



        pawn = tablutBoard.getElement(bestMove.srcY(), bestMove.srcX());
        stage.checkCapture(turn == 1, bestMove.srcX(), bestMove.dstX(), bestMove.srcY(), bestMove.dstY());



        ActionList actions = ActionFactory.generateMoveWithinContainer(model, pawn, bestMove.dstY(), bestMove.dstX());
        actions.setDoEndOfTurn(true);
        ActionPlayer play = new ActionPlayer(model, control, actions);
        play.start();

        return actions;
    }
}
