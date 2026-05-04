import boardifier.control.StageFactory;
import boardifier.model.GameException;
import boardifier.model.Model;
import boardifier.view.View;
import control.TablutController;

public class TablutConsole {

    public static void main(String[] args) {

        int mode = 0;
        if (args.length == 1) {
            try {
                mode = Integer.parseInt(args[0]);
                if ((mode <0) || (mode>2)) mode = 0;
            }
            catch(NumberFormatException e) {
                mode = 0;
            }
        }
        /*
        TO FULFILL:
            - add both players to model taking mode value into account
            - register the model and view class names (i.e model.HoleStageModel & view.HoleStageView
            - create the controller
            - set the name of the first stage to use when starting the game
            - start the game
            - start the stage loop.
         */

        Model model = new Model();
        if (mode == 0) {
            model.addHumanPlayer("player1");
            model.addHumanPlayer("player2");
        } else if (mode == 1) {
            model.addHumanPlayer("player");
            model.addComputerPlayer("computer");
        } else if (mode == 2) {
            model.addComputerPlayer("computer1");
            model.addComputerPlayer("computer2");
        }

        StageFactory.registerModelAndView("tablut", "model.TablutStageModel", "view.TablutStageView");
        View holeView = new View(model);
        TablutController control = new TablutController(model,holeView);
        control.setFirstStageName("tablut");
        try {
            control.startGame();
            control.stageLoop();
        }
        catch(GameException e) {
            System.out.println("error while starting the game");
        }
    }
}
