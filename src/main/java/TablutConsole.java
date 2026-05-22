import boardifier.control.StageFactory;
import boardifier.model.GameException;
import boardifier.model.Model;
import boardifier.view.View;
import control.TablutController;

public class TablutConsole {

    public static void main(String[] args) {

        int mode = 0;
        String inputFile = "";
        if (args.length == 1) {
            try {
                mode = Integer.parseInt(args[0]);
                if ((mode <0) || (mode>2)) mode = 0;
            }
            catch(NumberFormatException e) {
                mode = 0;
            }
        }
        if (args.length >= 2 && mode == 0) {
            inputFile = args[1];
        }


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
        TablutController control = new TablutController(model,holeView,inputFile);
        control.setFirstStageName("tablut");
        try {
            control.startGame();

            int first = Math.random() > 0.5 ? 1 : 0;
            model.setIdPlayer(first);
            System.out.printf("%s makes the first move\n", first == 0 ? "Green" : "Yellow");

            control.stageLoop();
        }
        catch(GameException e) {
            System.out.println("error while starting the game");
        }
    }
}
