import boardifier.control.Decider;
import boardifier.control.StageFactory;
import boardifier.model.GameException;
import boardifier.model.Model;
import boardifier.view.View;
import control.TablutController;
import control.TablutController.BotSelection;


import java.util.*;
import java.util.Map.*;



public class TablutConsole {

    public static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        int mode = 0;
        String inputFile = "";
        if (args.length >= 1) {
            try {
                mode = Integer.parseInt(args[0]);
                if ((mode <0) || (mode>2)) mode = 0;
            }
            catch(NumberFormatException e) {
                mode = 0;
            }
        }
        if (args.length >= 2) {
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
        TablutController control = new TablutController(model,holeView,mode,inputFile);
        control.setFirstStageName("tablut");


        if (mode > 0) {
            for (int i = 0; i <= 1; i++) {
                if (mode == 1 && i == 0) continue;
                System.out.printf("-----------------------------------\n");
                System.out.printf("|     BOT SELECTION FOR %s    |\n", i == 0 ? "GREEN " : "YELLOW");
                System.out.printf("-----------------------------------\n");

                List<Integer> validValues = new ArrayList<>();
                for (Entry<Integer, BotSelection> entry : control.getAvailableBots()[i].entrySet()) {
                    validValues.add(entry.getKey());
                    System.out.printf("-----\n");
                    System.out.printf("| %d | %s\n", entry.getKey(), entry.getValue().name());
                    System.out.printf("-----\n");
                }

                int selection = -1;
                do {
                    System.out.printf("\n* Enter your selection --> ");
                    selection = Integer.parseInt(sc.next());
                    if (!validValues.contains(selection)) {
                        System.out.printf("\n Invalid input.\n");
                    }
                } while (!validValues.contains(selection));

                control.setBotPlayer(i, selection);


                int level = -1;
                do {
                    System.out.printf("\n* Enter the bot level (between 0 and 10) --> ");
                    level = Integer.parseInt(sc.next());
                    if (level < 0 || level > 10) {
                        System.out.printf("\n Invalid input.\n");
                    }
                } while (level < 0 || level > 10);
                System.out.printf("\n\n");

                control.setBotLevel(i, level);
            }
        }


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