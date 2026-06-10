package control;

import boardifier.model.GameElement;
import boardifier.model.Model;
import boardifier.model.action.GameAction;
import view.sounds.Sounds;

public class PlayMoveSoundAction extends GameAction {

    protected boolean isCapture;

    public PlayMoveSoundAction(Model model, GameElement element, int rowDest, int colDest, boolean isCapture) {
        super(model, element, "playMoveSound");

        this.isCapture = isCapture;
    }

    public boolean isCapture() { return isCapture; }

    public void execute() {
        if (isCapture) {
            Sounds.playSound("src/main/java/view/sounds/capture.wav");
        } else {
            Sounds.playSound("src/main/java/view/sounds/move-self.wav");
        }
    }

    protected void createAnimation() {
        // no animation
    }
}
