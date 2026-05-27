package view;

import boardifier.model.GameStageModel;
import boardifier.view.ClassicBoardLook;
import boardifier.view.GameStageView;

import boardifier.view.TextLook;
import model.Pawn;
import model.TablutBoard;
import model.TablutStageModel;

import java.util.Arrays;


public class TablutStageView extends GameStageView {
    public TablutStageView(String name, GameStageModel gameStageModel) {
        super(name, gameStageModel);
    }

    @Override
    public void createLooks() {
        TablutStageModel model = (TablutStageModel)gameStageModel;

        addLook(new TablutBoardLook(600, model.getBoard()));

        for (int i = 0; i < 16; i++) {
            addLook(new PawnLook(25, model.getMoscovitePawns()[i]));
        }
        for (int i = 0; i < 8; i++) {
            addLook(new PawnLook(25, model.getSoldierPawns()[i]));
        }
        addLook(new PawnLook(25, model.getKingPawns()[0]));

        addLook(new TextLook(24, "0x000000", model.getPlayerName()));
    }
}
