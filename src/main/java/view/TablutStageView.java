package view;

import boardifier.model.GameStageModel;
import boardifier.view.ClassicBoardLook;
import boardifier.view.GameStageView;

import boardifier.view.TextLook;
import model.Pawn;
import model.TablutBoard;
import model.TablutStageModel;

import java.util.Arrays;

/**
 * HoleStageView has to create all the looks for all game elements created by the HoleStageFactory.
 * The desired UI is the following:
 * player            ╔═╗    ┏━━━┓
 *    A   B   C      ║1║    ┃ 1 ┃
 *  ╔═══╦═══╦═══╗    ╠═╣    ┣━━━┫
 * 1║   ║   ║   ║    ║2║    ┃ 2 ┃
 *  ╠═══╬═══╬═══╣    ╠═╣    ┣━━━┫
 * 2║   ║   ║   ║    ║3║    ┃ 3 ┃
 *  ╠═══╬═══╬═══╣    ╠═╣    ┣━━━┫
 * 3║   ║   ║   ║    ║4║    ┃ 4 ┃
 *  ╚═══╩═══╩═══╝    ╚═╝    ┗━━━┛
 *
 * The UI constraints are :
 *   - the main board has double-segments border, coordinates, and cells of size 2x4
 *   - the black pot has double-segments border, will cells that resize to match what is within (or not)
 *   - the red pot has simple-segment border, and cells have a fixed size of 2x4
 *
 *   main board can be instanciated directly as a ClassicBoardLook.
 *   black pot could be instanciated directly as a TableLook, but in this demo a BlackPotLook subclass is created (in case of we want to modifiy the look in some way)
 *   for red pot, a subclass RedPotLook of GridLook is used, in order to override the method that render the borders.
 */

public class TablutStageView extends GameStageView {
    public TablutStageView(String name, GameStageModel gameStageModel) {
        super(name, gameStageModel);
    }

    @Override
    public void createLooks() {
        TablutStageModel model = (TablutStageModel)gameStageModel;

        addLook(new TextLook(model.getPlayerName()));
        addLook(new ClassicBoardLook(2, 6, model.getBoard(), 1, 1, true));

        System.out.printf(Arrays.toString(model.getMoscovitePawns()));
        for (int i = 0; i < 16; i++) {
            addLook(new PawnLook(model.getMoscovitePawns()[i]));
        }
        for (int i = 0; i < 8; i++) {
            addLook(new PawnLook(model.getSoldierPawns()[i]));
        }
        addLook(new PawnLook(model.getKingPawns()[0]));

        /*
        TO FULFILL:
            using the model of the board, pots and pawns
            - create & add the look of the main board using an instance of ClassicBoardLook, with cells of size 4x2
            - create & add the look of the two pots using instances of PawnPotLook with cells of size 4x2
            - crate & add the look of the 8 pawns
         */
    }
}
