package view;

import boardifier.model.GameElement;
import boardifier.view.ElementLook;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import model.Pawn;

public class PawnLook extends ElementLook {
    private static final Color MOSCOVITE_COLOR = Color.GOLD;
    private static final Color SOLDIER_COLOR = Color.DARKGREEN;
    private static final Color KING_INNER_COLOR = Color.BLACK;

    private Circle circle;
    private Circle innerCircle; // only for the king
    private int radius;

    public PawnLook(int radius, GameElement element) {
        // Pawn look is constituted of a single character, so shape size = 1x1
        super(element);

        this.radius = radius;
        render();
    }

    @Override
    public void onSelectionChange() {
        Pawn pawn = (Pawn)getElement();
        if (pawn.isSelected()) {
            circle.setStrokeWidth(3);
            circle.setStrokeMiterLimit(10);
            circle.setStrokeType(StrokeType.CENTERED);
            circle.setStroke(Color.valueOf("0x333333"));
        }
        else {
            circle.setStrokeWidth(0);
        }
    }

    @Override
    public void onFaceChange() {

    }

    protected void render() {
        Pawn pawn = (Pawn)element;
        circle = new Circle();
        circle.setRadius(radius);

        innerCircle = new Circle();
        innerCircle.setRadius((double) radius / 3);

        if (pawn.getColor() == Pawn.PAWN_SOLDIER || pawn.getColor() == Pawn.PAWN_KING) {
            circle.setFill(SOLDIER_COLOR);
            innerCircle.setFill(SOLDIER_COLOR);
        } else if (pawn.getColor() == Pawn.PAWN_MOSCOVITE) {
            circle.setFill(MOSCOVITE_COLOR);
            innerCircle.setFill(MOSCOVITE_COLOR);
        }

        if (pawn.getColor() == Pawn.PAWN_KING) {
            innerCircle.setFill(KING_INNER_COLOR);
        }


        addShape(circle);
        addShape(innerCircle);
    }
}
