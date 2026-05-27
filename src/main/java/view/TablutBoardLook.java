package view;

import boardifier.model.ContainerElement;
import boardifier.view.ClassicBoardLook;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;


public class TablutBoardLook extends ClassicBoardLook {
    public static final Color SQUARE_COLOR = Color.BEIGE;
    public static final Color LEGAL_MOVE_SQUARE_COLOR = Color.RED;

    public TablutBoardLook(int size, ContainerElement element) {
        super(size/9, element, -1, SQUARE_COLOR, SQUARE_COLOR, 10, Color.BLACK, 20, Color.BLACK, true);
    }

    // override this otherwise all square borders disappears except the legal moves squares' when clicking a piece
    @Override
    public void onFaceChange() {
        ContainerElement board = (ContainerElement)element;
        boolean[][] reach = board.getReachableCells();
        for(int i=0;i<nbRows;i++) {
            for(int j=0;j<nbCols;j++) {
                cells[i][j].setStrokeWidth(3);
                cells[i][j].setStrokeMiterLimit(10);
                cells[i][j].setStrokeType(StrokeType.CENTERED);
                cells[i][j].setStroke(Color.valueOf("0x333333"));
                if (reach[i][j]) {
                    cells[i][j].setFill(LEGAL_MOVE_SQUARE_COLOR);
                } else {
                    cells[i][j].setFill(SQUARE_COLOR);
                }
            }
        }
    }
}
