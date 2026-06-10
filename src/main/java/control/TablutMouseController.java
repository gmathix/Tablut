package control;


import boardifier.control.ActionPlayer;
import boardifier.control.Controller;
import boardifier.control.ControllerMouse;
import boardifier.model.Coord2D;
import boardifier.model.ElementTypes;
import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import boardifier.view.GridLook;
import boardifier.view.View;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import model.Move;
import model.Pawn;
import model.TablutBoard;
import model.TablutStageModel;


import java.util.ArrayList;
import java.util.List;

public class TablutMouseController extends ControllerMouse implements EventHandler<MouseEvent> {

    private static final double DRAG_THRESHOLD = 5.0;

    private Pawn draggedPawn = null;
    private double pressX, pressY;
    private double dragOffsetX, dragOffsetY;

    public TablutMouseController(Model model, View view, Controller control) {
        super(model, view, control);
        view.getRootPane().addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onDrag);
        view.getRootPane().addEventFilter(MouseEvent.MOUSE_RELEASED, this::onRelease);
    }


    private boolean isGuardOk() {
        TablutStageModel stageModel = (TablutStageModel) model.getGameStage();
        TablutController tablutControl = (TablutController) control;

        if (stageModel == null) return false;
        if (stageModel.getMode() == TablutStageModel.MODE_VIEW_GAME) return false;
        if (stageModel.getMode() == TablutStageModel.MODE_PLAY &&
                tablutControl.getMoveHistoryIterator().hasNext()) return false;
        if (!model.isCaptureMouseEvent()) return false;

        return true;
    }

    private void executeMove(Pawn pawn, int[] dest, boolean animated) {
        TablutStageModel stageModel = (TablutStageModel) model.getGameStage();
        TablutController tablutControl = (TablutController) control;
        TablutBoard board = stageModel.getBoard();

        if (dest != null && board.canReachCell(dest[0], dest[1])) {
            ActionList actions = tablutControl.genMoveAnimationWithCapture(model, pawn, board, dest[0], dest[1], animated);
            ActionPlayer play = new ActionPlayer(model, control, actions);
            play.start();

            tablutControl.getMoveHistoryIterator().add(new Move(board, pawn.getBoardX(), pawn.getBoardY(), dest[1], dest[0]));

            if (pawn.getColor() == Pawn.PAWN_KING) {
                board.setKingX(dest[1]);
                board.setKingY(dest[0]);
            }
            pawn.setBoardX(dest[1]);
            pawn.setBoardY(dest[0]);
        }
    }

    private boolean isOwnPawn(Pawn pawn) {
        return (pawn.getColor() == Pawn.PAWN_MOSCOVITE && model.getIdPlayer() == 1)
                || (pawn.getColor() != Pawn.PAWN_MOSCOVITE && model.getIdPlayer() == 0);
    }


    public void handle(MouseEvent event) {
        if (!isGuardOk()) return;

        TablutStageModel stageModel = (TablutStageModel) model.getGameStage();

        pressX = event.getSceneX();
        pressY = event.getSceneY();

        Coord2D pos = new Coord2D(pressX, pressY);
        List<GameElement> currSelected = new ArrayList<>(control.elementsAt(pos));
        List<GameElement> prevSelected = new ArrayList<>(model.getSelected());



        if (stageModel.getState() == TablutStageModel.STATE_SELECTPAWN) {
            for (GameElement element : currSelected) {
                if (element.getType() == ElementTypes.getType("pawn")) {
                    Pawn pawn = (Pawn) element;
                    if (isOwnPawn(pawn)) {
                        element.toggleSelected();
                        stageModel.setState(TablutStageModel.STATE_SELECTDEST);

                        // initialize drag
                        draggedPawn = pawn;
                        dragOffsetX = event.getSceneX() - pawn.getX();
                        dragOffsetY = event.getSceneY() - pawn.getY();
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
                    draggedPawn = null;
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
                draggedPawn = null;
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
            Pawn pawn = (Pawn) model.getSelected().getFirst();
            GridLook lookBoard = (GridLook) control.getElementLook(board);
            int[] dest = lookBoard.getCellFromSceneLocation(pos);
            executeMove(pawn, dest, false);
            draggedPawn = null;
        }
    }

    /*
     * Mouse dragged
     */

    private void onDrag(MouseEvent event) {
        if (draggedPawn == null) return;
        if (!isGuardOk()) return;

        Group g = control.getElementLook(draggedPawn).getGroup();
        g.setTranslateX(event.getSceneX() - dragOffsetX);
        g.setTranslateY(event.getSceneY() - dragOffsetY);
    }

    /*
    Mouse released
     */

    private void onRelease(MouseEvent event) {
        if (draggedPawn == null) return;
        if (!isGuardOk()) {
            resetDrag();
            return;
        }

        double dx = event.getSceneX() - pressX;
        double dy = event.getSceneY() - pressY;
        boolean wasDrag = Math.sqrt(dx * dx + dy * dy) > DRAG_THRESHOLD;

        if (wasDrag) {
            TablutStageModel stageModel = (TablutStageModel) model.getGameStage();
            TablutBoard board = stageModel.getBoard();
            GridLook lookBoard = (GridLook) control.getElementLook(board);
            Coord2D pos = new Coord2D(event.getSceneX(), event.getSceneY());
            int[] dest = lookBoard.getCellFromSceneLocation(pos);

            // revert visual position before executeMove animates it properly
            resetDragVisual();

            if (dest != null && board.canReachCell(dest[0], dest[1])) {
                executeMove(draggedPawn, dest, true);
            } else {
                // invalid drop - snap back and stay in SELECTDEST for a second-chance click
                resetDragVisual();
            }

            draggedPawn = null;
        }
        // if not a drag: two-click flow already handled everything in handle()
    }


    private void resetDragVisual() {
        if (draggedPawn == null) return;
        Group g = control.getElementLook(draggedPawn).getGroup();
        g.setTranslateX(draggedPawn.getX());
        g.setTranslateY(draggedPawn.getY());
    }

    private void resetDrag() {
        resetDragVisual();
        if (draggedPawn != null && draggedPawn.isSelected()) {
            draggedPawn.toggleSelected();
        }
        TablutStageModel stageModel = (TablutStageModel) model.getGameStage();
        if (stageModel != null) stageModel.setState(TablutStageModel.STATE_SELECTPAWN);
        draggedPawn = null;
    }
}