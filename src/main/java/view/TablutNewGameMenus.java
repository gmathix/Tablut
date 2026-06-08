package view;

import boardifier.model.Model;
import control.TablutController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import model.RuleSets;
import model.TablutStageModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TablutNewGameMenus {
    private static final int MIN_BOT_LEVEL = 0;
    private static final int MAX_BOT_LEVEL = 10;
    private static final double IMAGE_SLOT_WIDTH = 96;
    private static final double IMAGE_SLOT_HEIGHT = 70;

    private TablutNewGameMenus() {
    }

    private record NewGameSelection(
            int mode,
            int rulesetMask,
            Integer humanSide,
            int swedishBotType,
            int swedishBotLevel,
            int moscoviteBotType,
            int moscoviteBotLevel
    ) {
    }

    public static boolean configureNewGame(javafx.stage.Stage owner, Model model, TablutController controller) {
        Optional<NewGameSelection> selection = showDialog(owner, controller);
        if (selection.isEmpty()) {
            return false;
        }

        applySelection(model, controller, selection.get());
        return true;
    }

    private static Optional<NewGameSelection> showDialog(javafx.stage.Stage owner, TablutController controller) {
        Dialog<NewGameSelection> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("New game setup");
        dialog.setHeaderText("Pick the game mode, rules, side and bot settings.");
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Start game", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );
        dialog.getDialogPane().setPrefSize(920, 760);
        dialog.getDialogPane().setStyle("-fx-background-color: #1a2318;");

        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton pvp = createModeButton("PvP", "Human vs human", 0, modeGroup);
        RadioButton pvb = createModeButton("PvB", "Human vs bot", 1, modeGroup);
        RadioButton bvb = createModeButton("BvB", "Bot vs bot", 2, modeGroup);

        int defaultMode = controller.getGameMode();
        switch (defaultMode) {
            case 1 -> pvb.setSelected(true);
            case 2 -> bvb.setSelected(true);
            default -> pvp.setSelected(true);
        }

        VBox modeSection = new VBox(8,
                sectionTitle("Game mode"),
                pvp,
                pvb,
                bvb
        );

        List<CheckBox> ruleBoxes = new ArrayList<>();
        VBox ruleBoxColumn = new VBox(8);
        ruleBoxColumn.getChildren().add(sectionTitle("Rulesets"));
        ruleBoxColumn.getChildren().add(createInfoLabel("Normal rules are always active. Toggle the extras below."));
        for (RuleSets.RuleOption option : RuleSets.ruleOptions) {
            CheckBox checkBox = new CheckBox(option.description());
            checkBox.setUserData(option.bit());
            checkBox.setFont(Font.font(14));
            checkBox.setStyle("-fx-text-fill: #dde8d8;");
            ruleBoxes.add(checkBox);
            ruleBoxColumn.getChildren().add(checkBox);
        }

        ToggleGroup sideGroup = new ToggleGroup();
        RadioButton swedishSide = createSideButton("Swedish / defenders", "The king and his allies", 0, sideGroup);
        RadioButton moscoviteSide = createSideButton("Moscovite / attackers", "Moscovites and siege", 1, sideGroup);
        swedishSide.setSelected(true);

        VBox sideSection = new VBox(10,
                sectionTitle("Side selection"),
                createSideRow(swedishSide),
                createSideRow(moscoviteSide)
        );

        BotSection swedishBotSection = createBotSection(controller, 0);
        BotSection moscoviteBotSection = createBotSection(controller, 1);

        VBox botSection = new VBox(16,
                sectionTitle("Bot implementation"),
                swedishBotSection.root,
                moscoviteBotSection.root
        );

        VBox content = new VBox(18);
        content.setPadding(new Insets(18));
        content.setFillWidth(true);
        content.setStyle("-fx-background-color: #1a2318;");

        content.getChildren().addAll(
                modeSection,
                new Separator(),
                ruleBoxColumn,
                new Separator(),
                sideSection,
                new Separator(),
                botSection
        );

        ScrollPane scroller = new ScrollPane(content);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setPannable(true);
        scroller.setStyle("-fx-background: #1a2318; -fx-background-color: #1a2318;");
        dialog.getDialogPane().setContent(scroller);

        Runnable refreshVisibility = () -> {
            Integer mode = selectedInt(modeGroup);
            boolean isPvB = mode != null && mode == 1;
            boolean isBvB = mode != null && mode == 2;
            boolean showBots = isPvB || isBvB;

            setManagedAndVisible(sideSection, isPvB);
            setManagedAndVisible(botSection, showBots);
            setManagedAndVisible(swedishBotSection.root, isBvB || (isPvB && selectedInt(sideGroup) != null && selectedInt(sideGroup) == 1));
            setManagedAndVisible(moscoviteBotSection.root, isBvB || (isPvB && selectedInt(sideGroup) != null && selectedInt(sideGroup) == 0));
        };

        modeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> refreshVisibility.run());
        sideGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> refreshVisibility.run());

        refreshVisibility.run();

        dialog.setResultConverter(buttonType -> {
            if (buttonType == null || buttonType.getButtonData() != ButtonBar.ButtonData.OK_DONE) {
                return null;
            }

            int mode = selectedInt(modeGroup) != null ? selectedInt(modeGroup) : 0;
            int rulesetMask = RuleSets.RULESET_NORMAL;
            for (CheckBox box : ruleBoxes) {
                if (box.isSelected() && box.getUserData() instanceof Integer bit) {
                    rulesetMask |= bit;
                }
            }

            Integer humanSide = mode == 1 ? selectedInt(sideGroup) : null;

            int swedishBotType = swedishBotSection.selectedBotType();
            int swedishBotLevel = swedishBotSection.levelSpinner.getValue();
            int moscoviteBotType = moscoviteBotSection.selectedBotType();
            int moscoviteBotLevel = moscoviteBotSection.levelSpinner.getValue();

            return new NewGameSelection(
                    mode,
                    rulesetMask,
                    humanSide,
                    swedishBotType,
                    swedishBotLevel,
                    moscoviteBotType,
                    moscoviteBotLevel
            );
        });

        return dialog.showAndWait();
    }

    private static void applySelection(Model model, TablutController controller, NewGameSelection selection) {
        controller.setGameMode(selection.mode());
        controller.setInputFile("");
        controller.setConfigRuleSet(selection.rulesetMask());

        model.getPlayers().clear();

        if (selection.mode() == 0) {
            model.addHumanPlayer("Player 1");
            model.addHumanPlayer("Player 2");
        } else if (selection.mode() == 1 && Integer.valueOf(0).equals(selection.humanSide())) {
            model.addHumanPlayer("Player 1");
        }

        if (selection.mode() > 0) {
            if (selection.mode() == 2 || Integer.valueOf(1).equals(selection.humanSide())) {
                controller.setBotPlayer(0, selection.swedishBotType());
                controller.setBotLevel(0, selection.swedishBotLevel());
                model.addComputerPlayer(controller.getAvailableBots()[0].get(selection.swedishBotType()).name());
            }

            if (selection.mode() == 2 || Integer.valueOf(0).equals(selection.humanSide())) {
                controller.setBotPlayer(1, selection.moscoviteBotType());
                controller.setBotLevel(1, selection.moscoviteBotLevel());
                model.addComputerPlayer(controller.getAvailableBots()[1].get(selection.moscoviteBotType()).name());
            }
        }

        if (selection.mode() == 1 && Integer.valueOf(1).equals(selection.humanSide())) {
            model.addHumanPlayer("Player 2");
        }

        controller.setStartingPlayerId(Math.random() > 0.5 ? 1 : 0);
    }

    private static RadioButton createModeButton(String shortLabel, String description, int value, ToggleGroup group) {
        RadioButton button = new RadioButton(shortLabel + "  -  " + description);
        button.setUserData(value);
        button.setToggleGroup(group);
        button.setFont(Font.font(14));
        button.setWrapText(true);
        button.setStyle("-fx-text-fill: #dde8d8;");
        return button;
    }

    private static RadioButton createSideButton(String title, String description, int value, ToggleGroup group) {
        RadioButton button = new RadioButton(title + "\n" + description);
        button.setUserData(value);
        button.setToggleGroup(group);
        button.setFont(Font.font(14));
        button.setWrapText(true);
        button.setStyle("-fx-text-fill: #dde8d8;");
        return button;
    }

    private static HBox createSideRow(RadioButton button) {
        Region imageSlot = new Region();
        imageSlot.setPrefSize(IMAGE_SLOT_WIDTH, IMAGE_SLOT_HEIGHT);
        imageSlot.setMinSize(IMAGE_SLOT_WIDTH, IMAGE_SLOT_HEIGHT);
        imageSlot.setMaxSize(IMAGE_SLOT_WIDTH, IMAGE_SLOT_HEIGHT);
        imageSlot.setStyle("""
                -fx-border-color: rgba(255,255,255,0.18);
                -fx-border-radius: 12;
                -fx-border-style: dashed;
                -fx-background-color: rgba(255,255,255,0.03);
                -fx-background-radius: 12;
                """);

        Label placeholder = new Label("Image slot");
        placeholder.setStyle("-fx-text-fill: rgba(255,255,255,0.35); -fx-font-size: 11px;");

        StackPane slot = new StackPane(imageSlot, placeholder);
        slot.setAlignment(Pos.CENTER);

        HBox row = new HBox(14, button, slot);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(button, Priority.ALWAYS);
        button.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private static BotSection createBotSection(TablutController controller, int color) {
        String sideName = color == 0 ? "Swedish bot" : "Moscovite bot";

        ToggleGroup botGroup = new ToggleGroup();
        VBox radios = new VBox(8);

        Integer defaultBot = controller.getBotPlayer(color);
        if (controller.getAvailableBots()[color].isEmpty()) {
            Label warning = new Label("No bot implementation is available.");
            warning.setStyle("-fx-text-fill: #ff9d9d;");
            radios.getChildren().add(warning);
        } else {
            for (var entry : controller.getAvailableBots()[color].entrySet()) {
                RadioButton button = new RadioButton(entry.getValue().name());
                button.setUserData(entry.getKey());
                button.setToggleGroup(botGroup);
                button.setFont(Font.font(14));
                button.setStyle("-fx-text-fill: #dde8d8;");
                radios.getChildren().add(button);
                if (entry.getKey().equals(defaultBot)) {
                    button.setSelected(true);
                }
            }
        }

        if (botGroup.getSelectedToggle() == null && !botGroup.getToggles().isEmpty()) {
            botGroup.selectToggle(botGroup.getToggles().get(0));
        }

        Spinner<Integer> levelSpinner = new Spinner<>();
        levelSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                MIN_BOT_LEVEL,
                MAX_BOT_LEVEL,
                clamp(controller.getBotLevel(color), MIN_BOT_LEVEL, MAX_BOT_LEVEL)
        ));
        levelSpinner.setEditable(false);
        levelSpinner.setPrefWidth(110);

        Label levelLabel = new Label("Bot level");
        levelLabel.setFont(Font.font(13));
        levelLabel.setStyle("-fx-text-fill: #dde8d8;");

        VBox box = new VBox(10);
        box.getChildren().addAll(
                sectionTitle(sideName),
                radios,
                new HBox(10, levelLabel, levelSpinner)
        );
        box.setStyle("""
                -fx-padding: 12;
                -fx-background-color: rgba(255,255,255,0.03);
                -fx-background-radius: 12;
                -fx-border-color: rgba(255,255,255,0.08);
                -fx-border-radius: 12;
                """);

        return new BotSection(box, botGroup, levelSpinner);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(18));
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #c8a76a;");
        return label;
    }

    private static Label createInfoLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #dde8d8;");
        return label;
    }

    private static Integer selectedInt(ToggleGroup group) {
        Toggle toggle = group.getSelectedToggle();
        if (toggle == null || !(toggle.getUserData() instanceof Integer value)) {
            return null;
        }
        return value;
    }

    private static void setManagedAndVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private static void showError(javafx.stage.Stage owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record BotSection(VBox root, ToggleGroup toggleGroup, Spinner<Integer> levelSpinner) {
        private Integer selectedBotType() {
            Toggle selected = toggleGroup.getSelectedToggle();
            if (selected == null || !(selected.getUserData() instanceof Integer value)) {
                return null;
            }
            return value;
        }
    }
}