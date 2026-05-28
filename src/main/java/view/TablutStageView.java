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

        BackgroundLook fullBg = new BackgroundLook(1180, 820, BackgroundLook.BACKGROUND_COLOR, "0x1d251d", model.getFullBackdrop());
        fullBg.setDepth(-200);
        addLook(fullBg);

        BackgroundLook boardCard = new BackgroundLook(700, 700, BackgroundLook.BACKGROUND_COLOR, "0x2a2116", model.getBoardBackdrop());
        boardCard.setDepth(-180);
        addLook(boardCard);

        BackgroundLook panelCard = new BackgroundLook(360, 720, BackgroundLook.BACKGROUND_COLOR, "0x233026", model.getPanelBackdrop());
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
