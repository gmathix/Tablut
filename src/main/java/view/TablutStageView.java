package view;

import boardifier.model.GameStageModel;
import boardifier.view.BackgroundLook;
import boardifier.view.GameStageView;
import boardifier.view.TextLook;
import model.TablutStageModel;

public class TablutStageView extends GameStageView {
    public TablutStageView(String name, GameStageModel gameStageModel) {
        super(name, gameStageModel);
        width = 1180;
        height = 820;
    }

    @Override
    public void createLooks() {
        TablutStageModel model = (TablutStageModel) gameStageModel;

        BackgroundLook fullBg = new BackgroundLook(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT, BackgroundLook.BACKGROUND_COLOR, "0x" + Constants.BACKGROUND_COLOR, model.getFullBackdrop());
        fullBg.setDepth(-200);
        addLook(fullBg);

        BackgroundLook moscovitePanelCard = new BackgroundLook(
                Constants.PLAYER_PANEL_WIDTH, Constants.PLAYER_PANEL_HEIGHT, BackgroundLook.BACKGROUND_COLOR,
                "0x" + Constants.MOSCOVITE_PANEL_COLOR, model.getMoscovitePanelBackdrop());
        moscovitePanelCard.setDepth(-180);
        addLook(moscovitePanelCard);

        BackgroundLook soldierPanelCard = new BackgroundLook(
                Constants.PLAYER_PANEL_WIDTH, Constants.PLAYER_PANEL_HEIGHT, BackgroundLook.BACKGROUND_COLOR,
                "0x" + Constants.SWEDISH_PANEL_COLOR, model.getSoldierPanelBackdrop());
        soldierPanelCard.setDepth(-180);
        addLook(soldierPanelCard);

        BackgroundLook boardCard = new BackgroundLook(
                Constants.BOARD_WIDTH, Constants.BOARD_HEIGHT, BackgroundLook.BACKGROUND_COLOR,
                "0x" + Constants.BOARD_COLOR, model.getBoardBackdrop());
        boardCard.setDepth(-180);
        addLook(boardCard);

        BackgroundLook panelCard = new BackgroundLook(
                Constants.SIDE_PANEL_WIDTH, Constants.SIDE_PANEL_HEIGHT, BackgroundLook.BACKGROUND_COLOR,
                "0x" + Constants.SIDE_PANEL_COLOR, model.getPanelBackdrop());
        panelCard.setDepth(-170);
        addLook(panelCard);

        TablutBoardLook boardLook = new TablutBoardLook(600, model.getBoard());
        boardLook.setDepth(0);
        addLook(boardLook);

        for (int i = 0; i < 16; i++) {
            PawnLook look = new PawnLook(25, model.getMoscovitePawns()[i]);
            look.setDepth(5);
            addLook(look);
        }
        for (int i = 0; i < 8; i++) {
            PawnLook look = new PawnLook(25, model.getSoldierPawns()[i]);
            look.setDepth(5);
            addLook(look);
        }
        PawnLook kingLook = new PawnLook(25, model.getKingPawns()[0]);
        kingLook.setDepth(6);
        addLook(kingLook);

        TextLook moscovitePlayerText = new TextLook((int) Constants.PLAYER_FONT.getSize(), "0x" + Constants.PLAYER_TEXT_COLOR, model.getMoscovitePlayerText());
        moscovitePlayerText.setDepth(20);
        addLook(moscovitePlayerText);

        TextLook swedishPlayerText = new TextLook((int) Constants.PLAYER_FONT.getSize(), "0x" + Constants.PLAYER_TEXT_COLOR, model.getSwedishPlayerText());
        swedishPlayerText.setDepth(20);
        addLook(swedishPlayerText);

        TextLook title = new TextLook(40, "0xF5E7B8", model.getTitleText());
        title.setDepth(20);
        addLook(title);

        TextLook subtitle = new TextLook(18, "0xD9CFAF", model.getSubtitleText());
        subtitle.setDepth(20);
        addLook(subtitle);

        TextLook currentPlayer = new TextLook(24, "0xFFFFFF", model.getPlayerName());
        currentPlayer.setDepth(20);
        addLook(currentPlayer);

        TextLook help = new TextLook(16, "0xE1E8DC", model.getHelpText());
        help.setDepth(20);
        addLook(help);

        TextLook legend = new TextLook(15, "0xC8D2C3", model.getLegendText());
        legend.setDepth(20);
        addLook(legend);
    }
}
