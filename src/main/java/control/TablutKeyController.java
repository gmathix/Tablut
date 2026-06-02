package control;

import boardifier.control.ActionPlayer;
import boardifier.control.Controller;
import boardifier.control.ControllerKey;
import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import boardifier.view.View;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import model.*;

public class TablutKeyController extends ControllerKey implements EventHandler<KeyEvent> {
    private boolean leftPressed = false;


    public TablutKeyController(Model model, View view, Controller control) {
        super(model, view, control);
    }

    public void handle(KeyEvent event) {
        if (!model.isCaptureKeyEvent()) return;

        if (event.getEventType() == KeyEvent.KEY_PRESSED) {

            // play or rewind through the move list in MoveHistory, using the arrow keys

            TablutController tablutControl = (TablutController) control;
            TablutStageModel stageModel = (TablutStageModel) model.getGameStage();


            if (event.getCode() == KeyCode.RIGHT && tablutControl.getMoveHistoryIterator().hasNext()) {

                Move nextMove = tablutControl.getMoveHistoryIterator().next();

                GameElement element = stageModel.getBoard().getElement(nextMove.srcY(), nextMove.srcX());
                Pawn pawn = (Pawn) element;

                TablutBoard board = stageModel.getBoard();
                if (pawn.getColor() == Pawn.PAWN_KING) {
                    board.setKingX(nextMove.dstX());
                    board.setKingY(nextMove.dstY());
                }
                pawn.setBoardX(nextMove.dstX());
                pawn.setBoardY(nextMove.dstY());


                ActionList actions = tablutControl.genMoveAnimationWithCapture(model, element, board,
                        nextMove.dstY(), nextMove.dstX());
                actions.setDoEndOfTurn(false);
                ActionPlayer play = new ActionPlayer(model, control, actions);
                play.start();


            } else if (event.getCode() == KeyCode.LEFT && tablutControl.getMoveHistoryIterator().hasPrevious()) {
                if (leftPressed) return;
                leftPressed = true;

                // no animation for rewind

                Move prevMove = tablutControl.getMoveHistoryIterator().previous();

                stageModel.getBoard().undoMove(control, view.getGameStageView(), model.getGameStage(), prevMove);
            }
        } else if (event.getEventType() == KeyEvent.KEY_RELEASED) {
            if (event.getCode() == KeyCode.LEFT) {
                leftPressed = false;
            }
        }
    }
}
