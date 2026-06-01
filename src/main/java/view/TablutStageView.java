package view;

import boardifier.model.GameStageModel;
import boardifier.view.BackgroundLook;
import boardifier.view.GameStageView;
import boardifier.view.TextLook;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.TablutStageModel;

public class TablutStageView extends GameStageView {
    public TablutStageView(String name, GameStageModel gameStageModel) {
        super(name, gameStageModel);
        width = (int) Constants.WINDOW_WIDTH;
        height = (int) Constants.WINDOW_HEIGHT;
    }

    @Override
    public void createLooks() {
        TablutStageModel model = (TablutStageModel) gameStageModel;


        BackgroundLook fullBg = new BackgroundLook(
                (int) Constants.WINDOW_WIDTH,
                (int) Constants.WINDOW_HEIGHT,
                BackgroundLook.BACKGROUND_COLOR,
                Constants.BACKGROUND_HEX.replace("#", "0x"),
                model.getFullBackdrop()
        );
        fullBg.setDepth(-200);
        addLook(fullBg);

        BackgroundLook boardCard = new BackgroundLook(
                (int) Constants.BOARD_SIZE,
                (int) Constants.BOARD_SIZE,
                BackgroundLook.BACKGROUND_COLOR,
                Constants.BOARD_HEX.replace("#", "0x"),
                model.getBoardBackdrop()
        );
        boardCard.setDepth(-180);
        addLook(boardCard);

        BackgroundLook panelCard = new BackgroundLook(
                (int) Constants.PANEL_WIDTH,
                (int) Constants.PANEL_HEIGHT,
                BackgroundLook.BACKGROUND_COLOR,
                Constants.BACKGROUND_HEX.replace("#", "0x"),
                model.getPanelBackdrop()
        );
        panelCard.setDepth(-170);
        addLook(panelCard);

        TablutBoardLook boardLook = new TablutBoardLook(
                gameStageModel,
                (int) Constants.BOARD_RENDER_SIZE,
                model.getBoard()
        );
        boardLook.setDepth(0);
        addLook(boardLook);

        for (int i = 0; i < 16; i++) {
            PawnLook look = new PawnLook(
                    (int) Constants.PAWN_SIZE,
                    model.getMoscovitePawns()[i]
            );
            look.setDepth(5);
            addLook(look);
        }
        for (int i = 0; i < 8; i++) {
            PawnLook look = new PawnLook(
                    (int) Constants.PAWN_SIZE,
                    model.getSoldierPawns()[i]
            );
            look.setDepth(5);
            addLook(look);
        }
        PawnLook look = new PawnLook(
                (int) Constants.PAWN_SIZE,
                model.getKingPawns()[0]
        );
        look.setDepth(6);
        addLook(look);


        TextLook title = new TextLook(
                42,
                Constants.TITLE_HEX.replace("#", "0x"),
                Constants.FONT_FAMILY,
                FontWeight.BOLD,
                model.getTitleText()
        );
        title.setFont(Constants.TITLE_FONT);
        title.setDepth(20);
        addLook(title);

        TextLook subtitle = new TextLook(
                17,
                Constants.SUBTITLE_HEX.replace("#", "0x"),
                Constants.FONT_FAMILY,
                FontWeight.NORMAL,
                model.getSubtitleText()
        );
        subtitle.setFont(Constants.SUBTITLE_FONT);
        subtitle.setDepth(20);
        addLook(subtitle);

        TextLook currentPlayer = new TextLook(
                24,
                Constants.BODY_HEX,
                Constants.FONT_FAMILY,
                FontWeight.BOLD,
                model.getPlayerName()
        );
        currentPlayer.setFont(Constants.BODY_FONT);
        currentPlayer.setDepth(20);
        addLook(currentPlayer);

        TextLook botSentence = new TextLook(
                15,
                Constants.BODY_HEX.replace("#", "0x"),
                Constants.FONT_FAMILY,
                FontWeight.NORMAL,
                model.getBotSentenceText()
        );
        botSentence.setFont(Constants.BODY_FONT);
        botSentence.setWrappingWidth(Constants.CONTENT_WIDTH);
        botSentence.setDepth(20);
        addLook(botSentence);

        TextLook help = new TextLook(
                15,
                Constants.BODY_HEX.replace("#", "0x"),
                Constants.FONT_FAMILY,
                FontWeight.NORMAL,
                model.getHelpText()
        );
        help.setFont(Constants.BODY_FONT);
        help.setWrappingWidth(Constants.CONTENT_WIDTH);
        help.setDepth(20);
        addLook(help);

        TextLook legend = new TextLook(
                14,
                Constants.BODY_HEX.replace("#", "0x"),
                Constants.FONT_FAMILY,
                FontWeight.NORMAL,
                model.getLegendText()
        );
        legend.setFont(Constants.BODY_FONT);
        legend.setWrappingWidth(Constants.CONTENT_WIDTH);
        legend.setDepth(20);
        addLook(legend);

        TextLook material = new TextLook(
                15,
                Constants.BODY_HEX,
                Constants.FONT_FAMILY,
                FontWeight.NORMAL,
                model.getMaterialText()
        );
        material.setFont(Constants.BODY_FONT);
        material.setWrappingWidth(Constants.CONTENT_WIDTH);
        addLook(material);

        TextLook threat = new TextLook(
                15,
                Constants.BODY_HEX.replace("#", "0x"),
                Constants.FONT_FAMILY,
                FontWeight.NORMAL,
                model.getThreatText()
        );
        threat.setFont(Constants.BODY_FONT);
        threat.setWrappingWidth(Constants.CONTENT_WIDTH);
        addLook(threat);
    }
}
