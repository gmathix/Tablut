package view;

import boardifier.model.GameElement;
import boardifier.view.ConsoleColor;
import boardifier.view.ElementLook;
import model.Pawn;

public class PawnLook extends ElementLook {

    public PawnLook(GameElement element) {
        // Pawn look is constituted of a single character, so shape size = 1x1
        super(element, 2, 1);
    }



    protected void render() {
        Pawn pawn = (Pawn)element;

        if (pawn.getColor() == Pawn.PAWN_MOSCOVITE) {
            shape[0][0] = ConsoleColor.BLACK_BACKGROUND + ConsoleColor.YELLOW_BACKGROUND + 0 +
                    ConsoleColor.RESET;
        } else if (pawn.getColor() == Pawn.PAWN_SOLDIER) {
            shape[0][0] = ConsoleColor.BLACK_BACKGROUND + ConsoleColor.GREEN_BACKGROUND + 0 +
                    ConsoleColor.RESET;
        } else if (pawn.getColor() == Pawn.PAWN_KING) {
            shape[0][0] = ConsoleColor.BLACK_BACKGROUND + ConsoleColor.BLUE_BACKGROUND_BRIGHT + 0 +
                    ConsoleColor.RESET;
        }
        /*
        TO FULFILL:
            - put in shape[0][0] the pawn's value, char color = white on black, or black on red depending on the pawn color.
         */
    }

}
