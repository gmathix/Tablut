package view;

import boardifier.model.ContainerElement;
import boardifier.view.ClassicBoardLook;
import control.algos.RecurBoard;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import model.TablutStageModel;

import java.util.ArrayList;
import java.util.List;


public class TablutBoardLook extends ClassicBoardLook {
    private static final Color LIGHT_SQUARE = Color.web("#d8c39a");
    private static final Color DARK_SQUARE = Color.web("#bf9f6f");
    private static final Color BORDER_COLOR = Color.web("#5b4630");
    private static final Color FRAME_COLOR = Color.web("#24180f");
    private static final Color SPECIAL_SQUARE = Color.web("#7a5528");
    private static final Color THRONE_COLOR = Color.web("7a5528");

    private final List<Circle> legalMoveMarkers = new ArrayList<>();

    public TablutBoardLook(int size, ContainerElement element) {
        super(size/9, element, -1, LIGHT_SQUARE, DARK_SQUARE, 2, DARK_SQUARE, 22, FRAME_COLOR, true);
    }

    // override this otherwise all square borders disappears except the legal moves squares' when clicking a piece
    @Override
    public void onFaceChange() {
        for (Circle marker : legalMoveMarkers) {
            getGroup().getChildren().remove(marker);
            getShapes().remove(marker);
        }
        legalMoveMarkers.clear();

        ContainerElement board = (ContainerElement)element;
        boolean[][] reach = board.getReachableCells();
        for(int i=0;i<nbRows;i++) {
            for(int j=0;j<nbCols;j++) {
                cells[i][j].setStrokeWidth(2);
                cells[i][j].setStrokeMiterLimit(10);
                cells[i][j].setStrokeType(StrokeType.CENTERED);
                cells[i][j].setStroke(BORDER_COLOR);

                if (RecurBoard.constrainedKingSquares.contains(i*9 + j)) { // darker color for moscovite starting squares
                    cells[i][j].setFill(SPECIAL_SQUARE);
                } else if (i*9+j == 40) {
                    cells[i][j].setFill(THRONE_COLOR);
                } else {
                    cells[i][j].setFill(((i + j) % 2 == 0) ? LIGHT_SQUARE : DARK_SQUARE);
                }

                if (reach[i][j]) {
                    double cellWidth = cells[i][j].getWidth();
                    double cellHeight = cells[i][j].getHeight();

                    Circle marker = new Circle(cellWidth/2, cellHeight/2, cellWidth*0.16);
                    marker.setFill(Color.web("#efe2c5", 0.88));

                    DropShadow glow = new DropShadow();
                    glow.setRadius(12);
                    glow.setSpread(.2);
                    glow.setColor(Color.web("#fff6de", .3));

                    marker.setEffect(glow);
                    marker.setMouseTransparent(true);
                    marker.setCenterX(cells[i][j].getX() + cellWidth/2);
                    marker.setCenterY(cells[i][j].getY() + cellHeight/2);

                    addShape(marker);
                    legalMoveMarkers.add(marker);
                }
            }
        }
    }
}
