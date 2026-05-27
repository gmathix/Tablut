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
    private TablutStageModel stageModel;

    public TablutStageFactory(GameStageModel gameStageModel) {
        super(gameStageModel);
        stageModel = (TablutStageModel) gameStageModel;
    }

    @Override
    public void setup() {

        TextElement text = new TextElement(stageModel.getCurrentPlayerName(), stageModel);
        text.setLocation(10,20);
        stageModel.setPlayerName(text);

        TablutBoard board = new TablutBoard(0, 1, stageModel);
        stageModel.setBoard(board);

        Pawn[] moscovitePawns = new Pawn[16];
        Pawn[] soldierPawns   = new Pawn[8];
        Pawn[] kingPawns      = new Pawn[1];

        for (int i = 0; i < 16; i++) {
            moscovitePawns[i] = new Pawn(i+1, Pawn.PAWN_MOSCOVITE, stageModel);
        }
        for (int i = 0; i < 8; i++) {
            soldierPawns[i] = new Pawn(16 + i+1, Pawn.PAWN_SOLDIER, stageModel);
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
