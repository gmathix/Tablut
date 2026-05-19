package view;

import boardifier.model.GameElement;
import boardifier.view.ConsoleColor;
import boardifier.view.ElementLook;
import model.Pawn;

public class PawnLook extends ElementLook {

    public PawnLook(GameElement element) {
        // Pawn look is constituted of a single character, so shape size = 1x1
        super(element, 1, 1);
    }



    protected void render() {
        Pawn pawn = (Pawn)element;

        if (pawn.getColor() == Pawn.PAWN_MOSCOVITE) {
            shape[0][0] = ConsoleColor.BLACK_BACKGROUND + ConsoleColor.YELLOW_BRIGHT + ConsoleColor.YELLOW_BACKGROUND + 'M' +
                    ConsoleColor.RESET;
        } else if (pawn.getColor() == Pawn.PAWN_SOLDIER) {
            shape[0][0] = ConsoleColor.BLACK_BACKGROUND + ConsoleColor.GREEN_BRIGHT + ConsoleColor.GREEN_BACKGROUND + 'S' +
                    ConsoleColor.RESET;
        } else if (pawn.getColor() == Pawn.PAWN_KING) {
            shape[0][0] = ConsoleColor.BLACK_BACKGROUND + ConsoleColor.GREEN_BOLD_BRIGHT +  ConsoleColor.BLUE_BACKGROUND_BRIGHT + 'K' +
                    ConsoleColor.RESET;
        }
    }

}
