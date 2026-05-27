package control;

import boardifier.control.Controller;
import boardifier.control.ControllerAction;
import boardifier.model.GameException;
import boardifier.model.Model;
import boardifier.view.View;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import view.TablutView;

public class TablutActionController extends ControllerAction implements EventHandler<ActionEvent> {
    private TablutView tablutView;

    public TablutActionController(Model model, View view, Controller control) {
        super(model, view, control);

        tablutView = (TablutView) view;

        setMenuHandlers();
    }

    private void setMenuHandlers() {
        tablutView.getMenuStart().setOnAction(e -> {
            try {
                control.startGame();
            } catch (GameException err) {
                System.err.println(err.getMessage());
                System.exit(1);
            }
        });

        tablutView.getMenuIntro().setOnAction(e -> {
            control.stopGame();
            tablutView.resetView();
        });
        tablutView.getMenuQuit().setOnAction(e -> {
            System.exit(0);
        });
    }

    public void handle(ActionEvent event) {
        if (!model.isCaptureActionEvent()) return;
    }
}
