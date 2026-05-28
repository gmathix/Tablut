package model;

import boardifier.model.GameStageModel;
import boardifier.model.StageElementsFactory;
import boardifier.model.TextElement;
import view.PawnLook;

import static model.TablutBoard.startingBoard;

/**
 * TablutStageFactory must create the game elements that are defined in TablutStageModel
 * WARNING: it just creates the game element and NOT their look, which is done in TablutStageView.
 *
 * If there must be a precise position in the display for the look of a game element, then this element must be created
 * with that position in the virtual space and MUST NOT be placed in a container element. Indeed, for such
 * elements, the position in their virtual space will match the position on the display. For example, in the following,
 * the black pot is placed in 18,0. When displayed on screen, the top-left character of the black pot will be effectively
 * placed at column 18 and row 0.
 *
 * Otherwise, game elements must be put in a container and it will be the look of the container that will manage
 * the position of element looks on the display. For example, pawns are put in a ContainerElement. Thus, their virtual space is
 * in fact the virtual space of the container and their location in that space in managed by boardifier, depending of the
 * look of the container.
 *
 */
public class TablutStageFactory extends StageElementsFactory {
    private static final int BOARD_BACKDROP_X = 20;
    private static final int BOARD_BACKDROP_Y = 60;
    private static final int PANEL_BACKDROP_X = 760;
    private static final int PANEL_BACKDROP_Y = 40;


    private TablutStageModel stageModel;

    public TablutStageFactory(GameStageModel gameStageModel) {
        super(gameStageModel);
        stageModel = (TablutStageModel) gameStageModel;
    }

    @Override
    public void setup() {
        TextElement fullBackdrop = new TextElement("", stageModel);

        fullBackdrop.setClickable(false);
        fullBackdrop.setLocation(0, 0);
        stageModel.setFullBackdrop(fullBackdrop);

        TextElement boardBackdrop = new TextElement("", stageModel);
        boardBackdrop.setClickable(false);
        boardBackdrop.setLocation(BOARD_BACKDROP_X, BOARD_BACKDROP_Y);
        stageModel.setBoardBackdrop(boardBackdrop);

        TextElement panelBackdrop = new TextElement("", stageModel);
        panelBackdrop.setClickable(false);
        panelBackdrop.setLocation(PANEL_BACKDROP_X, PANEL_BACKDROP_Y);
        stageModel.setPanelBackdrop(panelBackdrop);

        TextElement title = new TextElement("TABLUT", stageModel);
        title.setClickable(false);
        title.setLocation(PANEL_BACKDROP_X + 35, PANEL_BACKDROP_Y + 40);
        stageModel.setTitleText(title);

        TextElement subtitle = new TextElement("Escape the king, or trap him.", stageModel);
        subtitle.setClickable(false);
        subtitle.setLocation(PANEL_BACKDROP_X + 35, PANEL_BACKDROP_Y + 90);
        stageModel.setSubtitleText(subtitle);

        TextElement text = new TextElement(stageModel.getCurrentPlayerName(), stageModel);
        text.setLocation(PANEL_BACKDROP_X + 35, PANEL_BACKDROP_Y + 165);
        stageModel.setPlayerName(text);

        TextElement help = new TextElement("Click a pawn, then a highlighted square.", stageModel);
        help.setClickable(false);
        help.setLocation(PANEL_BACKDROP_X + 35, PANEL_BACKDROP_Y + 250);
        stageModel.setHelpText(help);

        TextElement legend = new TextElement("Green = defenders  |  Gold = attackers", stageModel);
        legend.setClickable(false);
        legend.setLocation(PANEL_BACKDROP_X + 35, PANEL_BACKDROP_Y + 300);
        stageModel.setLegendText(legend);

        TablutBoard board = new TablutBoard(40, 80, stageModel);
        stageModel.setBoard(board);


        Pawn[] moscovitePawns = new Pawn[16];
        Pawn[] soldierPawns   = new Pawn[8];
        Pawn[] kingPawns      = new Pawn[1];

        for (int i = 0; i < 16; i++) {
            moscovitePawns[i] = new Pawn(i + 1, Pawn.PAWN_MOSCOVITE, stageModel);
        }
        for (int i = 0; i < 8; i++) {
            soldierPawns[i] = new Pawn(16 + i + 1, Pawn.PAWN_SOLDIER, stageModel);
        }
        kingPawns[0] = new Pawn(25, Pawn.PAWN_KING, stageModel);

        stageModel.setMoscovitePawns(moscovitePawns);
        stageModel.setSoldierPawns(soldierPawns);
        stageModel.setKingPawns(kingPawns);

        int moscoviteIdx = 0;
        int soldierIdx   = 0;
        for (int i = 0; i < TablutBoard.BOARD_SIZE; i++) {
            for (int j = 0; j < TablutBoard.BOARD_SIZE; j++) {
                int type = TablutBoard.startingBoard[i][j];
                if (type != 0) {
                    if (type == Pawn.PAWN_MOSCOVITE) {
                        board.addElement(moscovitePawns[moscoviteIdx], i, j);
                        moscovitePawns[moscoviteIdx].setBoardX(j);
                        moscovitePawns[moscoviteIdx].setBoardY(i);
                        moscoviteIdx++;
                    } else if (type == Pawn.PAWN_SOLDIER) {
                        board.addElement(soldierPawns[soldierIdx], i, j);
                        soldierPawns[soldierIdx].setBoardX(j);
                        soldierPawns[soldierIdx].setBoardY(i);
                        soldierIdx++;
                    } else if (type == Pawn.PAWN_KING) {
                        board.addElement(kingPawns[0], i, j);
                        kingPawns[0].setBoardX(j);
                        kingPawns[0].setBoardY(i);
                    }
                }
            }
        }
    }
}
