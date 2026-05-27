package control;

import boardifier.control.ActionFactory;
import boardifier.control.ActionPlayer;
import boardifier.control.Controller;
import boardifier.control.ControllerMouse;
import boardifier.model.Coord2D;
import boardifier.model.ElementTypes;
import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import boardifier.model.animation.AnimationTypes;
import boardifier.view.GridLook;
import boardifier.view.View;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import model.Pawn;
import model.TablutBoard;
import model.TablutStageModel;


import java.util.ArrayList;
import java.util.List;

public class TablutMouseController extends ControllerMouse implements EventHandler<MouseEvent> {
    public TablutMouseController(Model model, View view, Controller control) {
        super(model, view, control);
    }

    public void handle(MouseEvent event) {
        if (!model.isCaptureMouseEvent()) return;

        Coord2D pos = new Coord2D(event.getSceneX(), event.getSceneY());
        List<GameElement> currSelected = new ArrayList<>(control.elementsAt(pos));
        List<GameElement> prevSelected = new ArrayList<>(model.getSelected());

        TablutStageModel stageModel = (TablutStageModel) model.getGameStage();

        if (stageModel.getState() == TablutStageModel.STATE_SELECTPAWN) {
            for (GameElement element : currSelected) {
                if (element.getType() == ElementTypes.getType("pawn")) {
                    Pawn pawn = (Pawn) element;
                    if ((pawn.getColor() == Pawn.PAWN_MOSCOVITE && model.getIdPlayer() == 1) ||
                        (pawn.getColor() != Pawn.PAWN_MOSCOVITE && model.getIdPlayer() == 0)) {

                        element.toggleSelected();
                        stageModel.setState(TablutStageModel.STATE_SELECTDEST);
                        return;
                    }
                }
            }
        } else if (stageModel.getState() == TablutStageModel.STATE_SELECTDEST) {
            boolean pawnSelected = false;
            for (GameElement element : currSelected) {
                if (element.isSelected()) {
                    element.toggleSelected();
                    stageModel.setState(TablutStageModel.STATE_SELECTPAWN);
                    return;
                } else if (element.getType() == ElementTypes.getType("pawn")) {
                    pawnSelected = true;
                }
            }
            if (pawnSelected) {
                /*
                 * Player previously clicked a pawn and now clicked on another
                 * so we set the previous pawn to unselected and re-call handle() but this time with the state
                 * of actually selecting the current pawn
                 */
                for (GameElement el : prevSelected) {
                    el.toggleSelected();
                }
                stageModel.setState(TablutStageModel.STATE_SELECTPAWN);
                handle(event);
                return;
            }


            boolean boardClicked = false;
            for (GameElement element : currSelected) {
                if (element == stageModel.getBoard()) {
                    boardClicked = true;
                    break;
                }
            }
            if (!boardClicked) return;

            TablutBoard board = stageModel.getBoard();
            GameElement element = model.getSelected().getFirst();
            Pawn pawn = (Pawn) element;

            GridLook lookBoard = (GridLook) control.getElementLook(board);
            int[] dest = lookBoard.getCellFromSceneLocation(pos);
            if (board.canReachCell(dest[0], dest[1])) {
                if (pawn.getColor() == Pawn.PAWN_MOSCOVITE) {
                    board.setKingX(dest[0]);
                    board.setKingY(dest[1]);
                }
                ActionList actions = ActionFactory.generateMoveWithinContainer(control, model, element, dest[0], dest[1], AnimationTypes.MOVE_LINEARPROP, 10);
                actions.setDoEndOfTurn(true);
                stageModel.unselectAll();
                stageModel.setState(TablutStageModel.STATE_SELECTPAWN);
                ActionPlayer play = new ActionPlayer(model, control, actions);
                play.start();
            }
        }


    }
}
