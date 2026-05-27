package view;

import boardifier.model.ContainerElement;
import boardifier.view.ClassicBoardLook;
import javafx.scene.paint.Color;


public class TablutBoardLook extends ClassicBoardLook {
    public static final Color SQUARE_COLOR = Color.BEIGE;
    public TablutBoardLook(int size, ContainerElement element) {
        super(size/9, element, -1, SQUARE_COLOR, SQUARE_COLOR, 10, Color.BLACK, 20, Color.BLACK, true);
    }
}
