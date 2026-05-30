package view;

import boardifier.model.Model;
import control.TablutController;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import model.RuleSets;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;




public final class TablutNewGameMenus {
    private TablutNewGameMenus() {}

    public static boolean configureNewGame(Stage owner, Model model, TablutController controller) {
        int mode = chooseMode(owner, controller.getGameMode());

        Integer playerSide = null;
        if (mode == 1) {
            playerSide = chooseSide(owner);
        }

        Integer ruleset = chooseRuleset(owner);
        if (ruleset == null) {
            return false;
        }

        Integer[] botSelections = new Integer[2];
        Integer[] botLevels = new Integer[2];

        if (mode > 0) {
            for (int color = 0; color <= 1; color++) {
                if (mode == 1 && color == playerSide - 1) {
                    continue;
                }

                Integer botSelection = chooseBot(owner, controller, color);
                if (botSelection == null) {
                    return false;
                }
                botSelections[color] = botSelection;

                Integer level = chooseBotLevel(owner, controller.getBotLevel(color));
                if (level == null) {
                    return false;
                }
                botLevels[color] = level;
            }
        }

        controller.setGameMode(mode);
        controller.setInputFile("");
        RuleSets.currentRuleset = ruleset;

        model.getPlayers().clear();

        if (mode == 0) {
            model.addHumanPlayer("Player 1");
            model.addHumanPlayer("Player 2");
        } else if (mode == 1 && playerSide == 1) {
            model.addHumanPlayer("Player 1");
        }

        if (mode > 0) {
            for (int color = 0; color <= 1; color++) {
                if (mode == 1 && color == playerSide - 1) {
                    continue;
                }
                controller.setBotPlayer(color, botSelections[color]);
                controller.setBotLevel(color, botLevels[color]);
                String botName = controller.getAvailableBots()[color].get(botSelections[color]).name();
                model.addComputerPlayer(botName);
            }
        }

        if (mode == 1 && playerSide == 2) {
            model.addHumanPlayer("Player 2");
        }

        controller.setStartingPlayerId(Math.random() > 0.5 ? 1 : 0);
        return true;
    }

    private static Integer chooseMode(Stage owner, int defaultMode) {
        List<Integer> choices = List.of(0, 1, 2);
        String content = """
                0 | Human vs Human
                1 | Human vs Bot
                2 | Bot vs Bot
                """;
        return chooseInt(owner,
                "MODE SELECTION",
                "Pick the game mode.",
                content,
                choices,
                defaultMode >= 0 && defaultMode <= 2 ? defaultMode : 0);
    }

    private static Integer chooseSide(Stage owner) {
        List<Integer> choices = List.of(1, 2);
        String content = """
                1 | Green  (Swedish / Defenders)
                2 | Yellow (Moscovites / Attackers)
                """;
        return chooseInt(owner,
                "SIDE SELECTION",
                "Pick your side.",
                content,
                choices,
                1);
    }

    private static Integer chooseBot(Stage owner, TablutController controller, int color) {
        List<Integer> choices = new ArrayList<>();
        StringBuilder content = new StringBuilder();

        for (var entry : controller.getAvailableBots()[color].entrySet()) {
            choices.add(entry.getKey());
            content.append(entry.getKey())
                    .append(" | ")
                    .append(entry.getValue().name())
                    .append('\n');
        }

        if (choices.isEmpty()) {
            showError(owner, "BOT SELECTION", "No bots are available for this side.");
            return null;
        }

        return chooseInt(owner,
                "BOT SELECTION FOR " + (color == 0 ? "GREEN" : "YELLOW"),
                "Pick the bot implementation.",
                content.toString(),
                choices,
                choices.get(0));
    }

    private static Integer chooseBotLevel(Stage owner, int defaultLevel) {
        while (true) {
            TextInputDialog dialog = new TextInputDialog(Integer.toString(defaultLevel));
            dialog.initOwner(owner);
            dialog.setTitle("BOT LEVEL");
            dialog.setHeaderText("Enter the bot level (between 0 and 10).");
            dialog.setContentText("Level:");

            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return null;
            }

            try {
                int level = Integer.parseInt(result.get().trim());
                if (level < 0 || level > 10) {
                    showError(owner, "BOT LEVEL", "Invalid input. Level must be between 0 and 10.");
                    continue;
                }
                return level;
            } catch (NumberFormatException e) {
                showError(owner, "BOT LEVEL", "Invalid input. Please enter a whole number.");
            }
        }
    }

    private static Integer chooseRuleset(Stage owner) {
        int current = RuleSets.currentRuleset;

        while (true) {
            int selection = chooseInt(
                    owner,
                    "RULESET SELECTION",
                    "Base rules are always active.\nToggle optional rules below.",
                    rulesetContent(current),
                    rulesetChoices(),
                    0
            );

            if (selection == 0) {
                return current;
            }

            if (selection >= 1 && selection <= RuleSets.ruleOptions.size()) {
                current ^= RuleSets.ruleOptions.get(selection - 1).bit();
            }
        }
    }

    private static List<Integer> rulesetChoices() {
        List<Integer> choices = new ArrayList<>();
        for (int i = 1; i <= RuleSets.ruleOptions.size(); i++) {
            choices.add(i);
        }
        choices.add(0);
        return choices;
    }

    private static String rulesetContent(int currentRuleset) {
        StringBuilder sb = new StringBuilder();
        sb.append("Active rules : [ NORMAL");
        for (int i = 0; i < RuleSets.ruleOptions.size(); i++) {
            if ((currentRuleset & RuleSets.ruleOptions.get(i).bit()) > 0) {
                sb.append(", ").append(i + 1);
            }
        }
        sb.append(" ]\n\n");
        for (int i = 0; i < RuleSets.ruleOptions.size(); i++) {
            sb.append(i + 1).append(" | ")
                    .append(RuleSets.ruleOptions.get(i).description())
                    .append('\n');
        }
        sb.append("0 | Done - start with current ruleset");
        return sb.toString();
    }

    private static Integer chooseInt(Stage owner, String title, String header, String content, List<Integer> choices, int defaultChoice) {
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(defaultChoice, FXCollections.observableArrayList(choices));
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        Optional<Integer> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private static void showError(Stage owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
