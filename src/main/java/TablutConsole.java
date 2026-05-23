import boardifier.control.Decider;
import boardifier.control.StageFactory;
import boardifier.model.GameException;
import boardifier.model.Model;
import boardifier.view.View;
import control.TablutController;
import control.TablutController.BotSelection;
import model.RuleSets;


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
                if ((mode < 0) || (mode > 2)) mode = 0;
            } catch (NumberFormatException e) {
                mode = 0;
            }
        } else {
            System.out.printf("-----------------------------------\n");
            System.out.printf("|         MODE SELECTION          |\n");
            System.out.printf("-----------------------------------\n");
            System.out.printf("-----\n");
            System.out.printf("| 0 | Human vs Human\n");
            System.out.printf("-----\n");
            System.out.printf("| 1 | Human vs Bot\n");
            System.out.printf("-----\n");
            System.out.printf("| 2 | Bot vs Bot\n");
            System.out.printf("-----\n");

            do {
                System.out.printf("\n* Enter your selection (0, 1 or 2) --> ");
                try {
                    mode = Integer.parseInt(sc.next());
                } catch (NumberFormatException e) {
                    mode = -1;
                }
                if (mode < 0 || mode > 2) {
                    System.out.printf(" Invalid input.\n");
                }
            } while (mode < 0 || mode > 2);
            System.out.printf("\n");
        }
        if (args.length >= 2) {
            inputFile = args[1];
        }


        int playerSide = -1;

        Model model = new Model();
        if (mode == 0) {
            model.addHumanPlayer("player1");
            model.addHumanPlayer("player2");
        } else if (mode == 1) {
            System.out.printf("-----------------------------------\n");
            System.out.printf("|         SIDE SELECTION          |\n");
            System.out.printf("-----------------------------------\n");
            System.out.printf("-----\n");
            System.out.printf("| 1 | Green  (Swedish / Defenders)\n");
            System.out.printf("-----\n");
            System.out.printf("| 2 | Yellow (Moscovites / Attackers)\n");
            System.out.printf("-----\n");

            do {
                System.out.printf("\n* Pick your side (1 or 2) --> ");
                try {
                    playerSide = Integer.parseInt(sc.next());
                } catch (NumberFormatException e) {
                    playerSide = -1;
                }
                if (playerSide != 1 && playerSide != 2) {
                    System.out.printf(" Invalid input.\n");
                }
            } while (playerSide != 1 && playerSide != 2);
            System.out.printf("\n");

            if (playerSide == 1) {
                model.addHumanPlayer("player");
                model.addComputerPlayer("computer");
            } else {
                model.addComputerPlayer("computer");
                model.addHumanPlayer("player");
            }

        } else if (mode == 2) {
            model.addComputerPlayer("computer1");
            model.addComputerPlayer("computer2");
        }



        // ---- RULESET SELECTION ----
        record RuleOption(int bit, String description) {}

        List<RuleOption> ruleOptions = List.of(
                new RuleOption(RuleSets.RULESET_CONSTRAINED_KING_SQUARES, "King cannot land on starting Moscovite squares"),
                new RuleOption(RuleSets.RULESET_CONSTRAINED_KING_MOVES,   "King cannot move more than 4 squares"),
                new RuleOption(RuleSets.RULESET_CORNER_KING_ESCAPES,      "King must reach a corner to win")
        );

        System.out.printf("-----------------------------------\n");
        System.out.printf("|         RULESET SELECTION       |\n");
        System.out.printf("-----------------------------------\n");
        System.out.printf("| Base rules are always active.   |\n");
        System.out.printf("| Toggle optional rules below.    |\n");
        System.out.printf("-----------------------------------\n");
        for (int i = 0; i < ruleOptions.size(); i++) {
            System.out.printf("-----\n");
            System.out.printf("| %d | %s\n", i + 1, ruleOptions.get(i).description());
            System.out.printf("-----\n");
        }
        System.out.printf("-----\n");
        System.out.printf("| 0 | Done - start with current ruleset\n");
        System.out.printf("-----\n");

        int ruleSelection;
        do {
            // print active ruleset state
            System.out.printf("\nActive rules : [ NORMAL");
            for (int i = 0; i < ruleOptions.size(); i++) {
                if ((RuleSets.currentRuleset & ruleOptions.get(i).bit()) > 0) {
                    System.out.printf(", %d", i + 1);
                }
            }
            System.out.printf(" ]\n");

            System.out.printf("* Toggle a rule (1-%d) or 0 to start --> ", ruleOptions.size());
            try {
                ruleSelection = Integer.parseInt(sc.next());
            } catch (NumberFormatException e) {
                ruleSelection = -1;
            }

            if (ruleSelection >= 1 && ruleSelection <= ruleOptions.size()) {
                RuleSets.currentRuleset ^= ruleOptions.get(ruleSelection - 1).bit(); // toggle
            } else if (ruleSelection != 0) {
                System.out.printf(" Invalid input.\n");
            }
        } while (ruleSelection != 0);
        System.out.printf("\n");





        StageFactory.registerModelAndView("tablut", "model.TablutStageModel", "view.TablutStageView");
        View holeView = new View(model);
        TablutController control = new TablutController(model,holeView,mode,inputFile);
        control.setFirstStageName("tablut");


        // ---- BOT SELECTION ----
        if (mode > 0) {
            for (int i = 0; i <= 1; i++) {
                if (mode == 1 && i == playerSide - 1) continue;
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