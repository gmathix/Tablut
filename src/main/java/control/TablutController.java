package control;

import boardifier.control.*;
import boardifier.model.*;
import boardifier.model.action.ActionList;
import boardifier.model.action.MoveWithinContainerAction;
import boardifier.model.action.RemoveFromContainerAction;
import boardifier.model.animation.AnimationTypes;
import boardifier.view.*;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.*;
import view.Constants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Supplier;

public class TablutController extends Controller {

    public static final int NEGAMAX_PLAYER = 0;
    public static final int MONTECARLO_PLAYER = 1;
    public static final int NEGAMONTECARLO_PLAYER = 2;

    public static final int NB_BOARDS_IN_MEMORY = 50;
    public static final int NB_BOARD_REPETITION_TRESHOLD = 3;

    public static record BotSelection(int type, String name, Supplier<Decider> supplier) {}


    public static final Map<Integer, BotSelection> availableBots[] = new Map[2];




    int gameMode;
    int botPlayers[];
    int botLevels[];

    BufferedReader consoleIn;
    String inputFile;
    private int startingPlayerId = 0;


    private String lastBoardsRepresentations[];
    private int currentBoardRepIndex;
    private boolean boardRepeated;


    private MoveHistory moveHistory;
    private ListIterator<Move> moveHistoryIterator;

    private VBox gameOverExportPanel;
    private TextArea gameOverPgnArea;
    private boolean gameoverExportPanelShown;


    public TablutController(Model model, View view, int gameMode, String inputFile,
                            int swedishBotPlayer, int moscoviteBotPlayer, int botLevels[]) {
        super(model, view);
        this.gameMode = gameMode;
        this.inputFile = inputFile;
        this.botPlayers = new int[]{
                swedishBotPlayer,
                moscoviteBotPlayer,
        };

        availableBots[0] = new LinkedHashMap<>();
        availableBots[1] = new LinkedHashMap<>();


        this.botLevels = botLevels;

        setBotLevel(0, botLevels[0]);
        setBotLevel(1, botLevels[1]);
        setBotPlayer(0, swedishBotPlayer);
        setBotPlayer(1, moscoviteBotPlayer);

        lastBoardsRepresentations = new String[NB_BOARDS_IN_MEMORY];
        for (int i = 0; i < NB_BOARDS_IN_MEMORY; i++) {
            lastBoardsRepresentations[i] = "";
        }
        currentBoardRepIndex = 0;
        boardRepeated = false;


        setControlKey(new TablutKeyController(model, view, this));
        setControlMouse(new TablutMouseController(model, view, this));
        setControlAction(new TablutActionController(model, view, this));


        gameOverExportPanel = null;
        gameOverPgnArea = null;
        gameoverExportPanelShown = false;
    }

    public TablutController(Model model, View view, int gameMode, String inputFile) {
        this(model, view, gameMode, inputFile, NEGAMAX_PLAYER, NEGAMAX_PLAYER, new int[]{5, 5});
    }

    public int getGameMode() {
        return gameMode;
    }

    public void setGameMode(int gameMode) {
        this.gameMode = gameMode;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile == null ? "" : inputFile;
    }

    public int getStartingPlayerId() {
        return startingPlayerId;
    }

    public void setStartingPlayerId(int startingPlayerId) {
        this.startingPlayerId = startingPlayerId;
    }

    public int getBotLevel(int color) {
        return botLevels[color];
    }

    public int getBotPlayer(int color) {
        return botPlayers[color];
    }

    public MoveHistory getMoveHistory() {
        return moveHistory;
    }

    public ListIterator<Move> getMoveHistoryIterator() { return moveHistoryIterator; }

    public Map<GameElement, ElementLook> getMapElementLook() { return mapElementLook; }

    // 0 for swedish, 0 for moscovite
    public void setBotLevel(int color, int level) {
        availableBots[color].clear();
        availableBots[color].put(NEGAMAX_PLAYER, new BotSelection(NEGAMAX_PLAYER, "Negamax",
                () -> new NegamaxDecider(model, this, level)));
        availableBots[color].put(MONTECARLO_PLAYER, new BotSelection(MONTECARLO_PLAYER, "Monte-Carlo",
                () -> new MonteCarloDecider(model, this, level)));
        availableBots[color].put(NEGAMONTECARLO_PLAYER, new BotSelection(NEGAMONTECARLO_PLAYER, "Nega-Monte-Carlo",
                () -> new NegaMonteCarloDecider(model, this, level)));
        this.botLevels[color] = level;
    }




    public void setBotPlayer(int color, int botPlayer) {
        if (!availableBots[color].containsKey(botPlayer)) return;
        this.botPlayers[color] = botPlayer;
    }

    public Map<Integer, BotSelection>[] getAvailableBots() {
        return availableBots;
    }


    public boolean isBoardRepeated() { return boardRepeated; }



    private void processBoardRepetition() {
        currentBoardRepIndex = (currentBoardRepIndex + 1) % NB_BOARDS_IN_MEMORY;

        String currBoardRep = ((TablutStageModel) model.getGameStage()).getBoard().getStringRepresentation();

        lastBoardsRepresentations[currentBoardRepIndex] = currBoardRep;

        int nbFound = 0;
        for (int i = 0; i < NB_BOARDS_IN_MEMORY; i++) {
            if (i == currentBoardRepIndex) continue;
            if (lastBoardsRepresentations[i].equals(currBoardRep)) {
                nbFound++;
            }
        }

        if (nbFound >= NB_BOARD_REPETITION_TRESHOLD) {
            boardRepeated = true;
        } else {
            boardRepeated = false;
        }
    }


    @Override
    protected void startStage(String stageName) throws GameException {
        if (model.isStageStarted()) stopGame();

        GameStageModel gameStageModel = StageFactory.createStageModel(stageName, model);
        gameStageModel.createElements(gameStageModel.getDefaultElementFactory());

        GameStageView gameStageView = StageFactory.createStageView(stageName, gameStageModel);
        gameStageView.createLooks();

        mapElementLook = new HashMap<>();
        for (GameElement element : gameStageModel.getElements()) {
            ElementLook look = gameStageView.getElementLook(element);
            mapElementLook.put(element, look);
        }

        moveHistory = new MoveHistory(
                this,
                model,
                view,
                model.getPlayers().get(0).getName(),
                model.getPlayers().get(1).getName(),
                startingPlayerId,
                RuleSets.currentRuleset
        );
        moveHistoryIterator = moveHistory.getMoves().listIterator();


        model.startGame(gameStageModel);
        model.setIdPlayer(startingPlayerId);
        ((TablutStageModel)gameStageModel).setMode(TablutStageModel.MODE_PLAY);

        view.setView(gameStageView);
        view.getRootPane().setFocusTraversable(true);
        view.getRootPane().requestFocus();
        controlAnimation.startAnimation();

        // make the bot play immediately if it has to start playing
        Platform.runLater(this::triggerCurrentPlayerTurn);
    }


    public ActionList genMoveAnimationWithCapture(Model model, GameElement element, TablutBoard board, int dstY, int dstX) {
        ActionList actions = new ActionList();

        ElementLook elementLook = getElementLook(element);
        ContainerLook containerLook = (ContainerLook) getElementLook(board);
        Coord2D center = containerLook.getContainerLocationForLookFromCell(elementLook, dstY, dstX);
        actions.addSingleAction(new MoveWithinContainerAction(
                model, element, dstY, dstX, AnimationTypes.MOVE_LINEARPROP, center.getX(), center.getY(), 10
        ));

        TablutStageModel stageModel = (TablutStageModel) model.getGameStage();
        Pawn pawn = (Pawn) element;
        List<Integer> captures = stageModel.getBoard().checkCaptures(
                pawn.getColor() == Pawn.PAWN_MOSCOVITE, pawn.getBoardX(), dstX, pawn.getBoardY(), dstY
        );
        if (!captures.isEmpty()) {
            for (Integer cap : captures) {
                GameElement capturedElement = board.getElement(cap / 9, cap % 9);
                actions.addSingleAction(new RemoveFromContainerAction(model, capturedElement));
            }
        }

        actions.setDoEndOfTurn(true);
        stageModel.unselectAll();
        stageModel.setState(TablutStageModel.STATE_SELECTPAWN);

        return actions;
    }


    private int[] countMaterial(TablutStageModel stageModel) {
        int defenders = 0;
        int attackers = 0;
        TablutBoard board = stageModel.getBoard();
        for (int y = 0; y < TablutBoard.BOARD_SIZE; y++) {
            for (int x = 0; x < TablutBoard.BOARD_SIZE; x++) {
                if (board.getElement(y, x) instanceof Pawn pawn) {
                    if (pawn.getColor() == Pawn.PAWN_MOSCOVITE) {
                        attackers++;
                    } else {
                        defenders++;
                    }
                }
            }
        }
        return new int[]{defenders, attackers};
    }

    private String buildKingThreatMessage(TablutStageModel stageModel) {
        TablutBoard board = stageModel.getBoard();
        int kingX = board.getKingX();
        int kingY = board.getKingY();

        int nbEdgesReachable = 0;
        int[] dy = {-1, 0, 1, 0};
        int[] dx = {0, -1, 0, 1};

        for (int i = 0; i < 4; i++) {
            boolean freeWay = true;
            int y = kingY;
            int x = kingX;
            for (int step = 0; step < 8; step++) {
                y += dy[i];
                x += dx[i];
                if (y < 0 || y > 8 || x < 0 || x > 8) {
                    break;
                }
                if (board.getElement(y, x) instanceof Pawn) {
                    freeWay = false;
                    break;
                }
            }
            if (freeWay) {
                nbEdgesReachable++;
            }
        }

        if (nbEdgesReachable == 1) {
            return "Threat: Raichi - the king has one escape lane.";
        }
        if (nbEdgesReachable >= 2) {
            return "Threat: Tuichi - the king has two escape lanes.";
        }
        return "Threat: no Raichi or Tuichi yet.";
    }

    private void updateStatusPanel() {
        TablutStageModel stageModel = (TablutStageModel) model.getGameStage();
        if (stageModel == null) return;

        int[] material = countMaterial(stageModel);
        stageModel.getMaterialText().setText(String.format("Material: Swedish %d  |  Gold %d", material[0], material[1]));
        stageModel.getThreatText().setText(buildKingThreatMessage(stageModel));

        TextLook nameLook = (TextLook) view.getElementLook(stageModel.getPlayerName());
        TextLook botLook = (TextLook) view.getElementLook(stageModel.getBotSentenceText());

        if (nameLook != null) {
            nameLook.setColor(model.getIdPlayer() == 0 ? "0x8BCB8F" : "0xE3C36A");
        }
        if (botLook != null) {
            botLook.setColor("0xC8D2C3");
        }
        if (nameLook != null && stageModel.getBotSentenceText() != null) {
            double gap = 12.0;
            double x = stageModel.getPlayerName().getX() + nameLook.getTextWidth() + gap;
            double y = stageModel.getPlayerName().getY() + 3.0;
            stageModel.getBotSentenceText().setLocation(x, y);
        }
    }

    private void triggerCurrentPlayerTurn() {
        if (model.isEndStage() || model.isEndGame()) return;

        Player p = model.getCurrentPlayer();
        TablutStageModel stageModel = (TablutStageModel) model.getGameStage();
        if (stageModel == null) return; // could happen at the very beginning, dunno why

        stageModel.getPlayerName().setText(p.getName());

        if (((TablutStageModel)model.getGameStage()).getMode() == TablutStageModel.MODE_VIEW_GAME) return;
        if (((TablutStageModel)model.getGameStage()).getMode() == TablutStageModel.MODE_PLAY &&
            moveHistoryIterator.hasNext()) return;

        if (p.getType() == Player.COMPUTER) {
            int turn = model.getIdPlayer();
            BotSelection selection = availableBots[turn].get(botPlayers[turn]);
            if (selection == null) return;

            Decider decider = selection.supplier.get();

            int botLevel = 5;
            if (decider instanceof NegamaxDecider d) {
                botLevel = d.getLevel();
            } else if (decider instanceof MonteCarloDecider d) {
                botLevel = d.getLevel();
            } else if (decider instanceof NegaMonteCarloDecider d) {
                botLevel = d.getLevel();
            }

            String[] sentenceArray;
            if (botLevel <= 4) {
                sentenceArray = BotSentences.SENTENCES_LOSING;
            } else if (botLevel <= 8) {
                sentenceArray = BotSentences.SENTENCES_WINNING;
            } else {
                sentenceArray = BotSentences.SENTENCES_EXTREMELY_ARROGANT;
            }

            int sentenceIndex = (int) (Math.random() * sentenceArray.length);
            String sentence = sentenceArray[sentenceIndex];
            stageModel.getBotSentenceText().setText(sentence);

            updateStatusPanel();

            ActionPlayer play = new ActionPlayer(model, this, decider, null);
            play.start();


        } else {
            stageModel.getBotSentenceText().setText("Your Move.");
            updateStatusPanel();
        }
    }


    private void removeGamePGN() {
        if (gameOverExportPanel == null) return;
        if (view != null && view.getRootPane() != null) {
            view.getRootPane().getChildren().remove(gameOverExportPanel);
        }
        gameOverExportPanel = null;
        gameOverPgnArea = null;
        gameoverExportPanelShown = false;
    }

    private void showGamePGN(String pgnText) {
        if (gameoverExportPanelShown) {
            if (gameOverPgnArea != null) {
                gameOverPgnArea.setText(pgnText);
            }
            return;
        }

        removeGamePGN();

        gameOverPgnArea = new TextArea(pgnText);
        gameOverPgnArea.setEditable(false);
        gameOverPgnArea.setWrapText(false);
        gameOverPgnArea.setPrefWidth(Constants.CONTENT_WIDTH);
        gameOverPgnArea.setPrefHeight(120);
        gameOverPgnArea.setStyle(
                "-fx-control-inner-background: #152016;" +
                        "-fx-text-fill: #e9f0e6;" +
                        "-fx-highlight-fill: #7f9c7e;" +
                        "-fx-highlight-text-fill: #152016;" +
                        "-fx-font-family: 'Monospaced';" +
                        "-fx-font-size: 12px;"
        );


        VBox panel = new VBox(10, gameOverPgnArea);
        panel.setPadding(new Insets(8, 0, 0, 0));
        panel.setLayoutX(Constants.CONTENT_X);
        panel.setLayoutY(Constants.THREAT_Y + 35);
        panel.setPrefWidth(Constants.CONTENT_WIDTH);
        panel.setStyle(
                "-fx-background-color: rgba(18, 26, 18, 0.55);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(200, 170, 120, 0.55);" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;"
        );

        gameOverExportPanel = panel;
        gameoverExportPanelShown = true;

        if (view != null && view.getRootPane() != null) {
            view.getRootPane().getChildren().add(panel);
            panel.toFront();
        }
    }

    public void exportGame(String pgnText) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Tablut Game");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Tablut PGN (*.tpgn)", "*.tpgn")
        );
        fileChooser.setInitialFileName("tablut_game.tpgn");

        java.io.File chosen = fileChooser.showSaveDialog(view.getStage());
        if (chosen == null) return;

        String filePath = chosen.getAbsolutePath();
        if (!filePath.toLowerCase(Locale.ROOT).endsWith(".tpgn")) {
            filePath += ".tpgn";
        }

        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of(filePath),
                    pgnText,
                    java.nio.charset.StandardCharsets.UTF_8
            );
        } catch (IOException ex) {
            System.err.printf("could not export game file : \n");
            ex.printStackTrace();
        }
    }

    public void importGame() throws GameException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Tablut Game");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tablut PGN (*.tpgn)", "*.tpgn")
        );

        java.io.File chosen = fileChooser.showOpenDialog(view.getStage());
        if (chosen == null) return;

        String filePath = chosen.getAbsolutePath();
        if (!filePath.toLowerCase(Locale.ROOT).endsWith(".tpgn")) {
            filePath += ".tpgn";
        }

        try {
            moveHistory = new MoveHistory(this, model, view);
            moveHistory.parseGameFileHeader(filePath);
            moveHistoryIterator = moveHistory.getMoves().listIterator();
        } catch (IOException e) {
            e.printStackTrace();
        }

        startingPlayerId = moveHistory.getStartingSide();
        model.setIdPlayer(startingPlayerId);
        model.addHumanPlayer(moveHistory.getSwedishPlayer());
        model.addHumanPlayer(moveHistory.getMoscovitePlayer());
        RuleSets.currentRuleset = moveHistory.getRuleSet();

        startStage("tablut");

        // reassign the move history because startStage just reassigned it
        try {
            moveHistory.parseGameFileMoves(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }


        model.setIdWinner(-1);


        ((TablutStageModel)model.getGameStage()).setMode(TablutStageModel.MODE_VIEW_GAME);
    }


    public void endOfTurn() {
        if (model.getIdWinner() == -1) {
            model.setNextPlayer();
            triggerCurrentPlayerTurn();
            processBoardRepetition();
        } else {
            model.stopStage();
            String message = String.format("Game over : %s\n",
                    model.getIdWinner() == 0
                            ? "the king has reached an edge"
                            : "the king has been encircled");
            ((TablutStageModel)model.getGameStage()).getThreatText().setText(message);
            moveHistory.setWinningSide(model.getIdWinner());

            TablutStageModel stageModel = (TablutStageModel) model.getGameStage();
            stageModel.getThreatText().setText(message);
            showGamePGN(moveHistory.buildGameString());
        }
    }



    /** maybe someone can clean those up **/

    /**
     *Implementing a file reader to read the entry file
     * Defines what to do within the single stage of the single party
     * It is pretty straight forward to write :
     */
    public void stageLoop() {
        if (!inputFile.isEmpty() && gameMode == 0) {
            try {
                consoleIn = new BufferedReader(new FileReader(inputFile));
                System.out.println("game scenario based on the entry file  : " + inputFile);


                // read starting side from entry file
                String startingSide = consoleIn.readLine().toLowerCase();
                if (startingSide.equals("moscovite")) {
                    model.setIdPlayer(1);
                } else if (startingSide.equals("swedish")) {
                    model.setIdPlayer(0);
                } else {
                    System.out.printf("Invalid starting side in entry file : got %s, expected 'moscovite' or 'swedish'\n");
                }

                // read rulesets from entry file
                int ruleset;
                do {
                    ruleset = Integer.parseInt(consoleIn.readLine());
                    if (ruleset < 0 || ruleset > RuleSets.ruleOptions.size()) {
                        System.out.printf("Invalid ruleset %d, expected 0 <= ruleset <= %d\n",
                                ruleset, RuleSets.ruleOptions.size());
                    } else if (ruleset != 0) {
                        RuleSets.currentRuleset |= RuleSets.ruleOptions.get(ruleset-1).bit();
                    }
                } while (ruleset != 0);

            } catch (IOException e) {
                System.out.println("Error: \"" + inputFile + " not found. fallback to player vs player\". ");
                consoleIn = new BufferedReader(new InputStreamReader(System.in));

            }

        } else {
            consoleIn = new BufferedReader(new InputStreamReader(System.in));
        }
        update();
        while (!model.isEndStage()) {
//            playTurn();
            endOfTurn();
            update();

            processBoardRepetition();
        }
        endGame();
    }


}