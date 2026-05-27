package control;

import boardifier.control.Controller;
import boardifier.control.ControllerKey;
import boardifier.model.Model;
import boardifier.view.View;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;

public class TablutKeyController extends ControllerKey implements EventHandler<KeyEvent> {
    public TablutKeyController(Model model, View view, Controller control) {
        super(model, view, control);
    }

    public void handle(KeyEvent event) {
        if (!model.isCaptureKeyEvent()) return;
    }
}
