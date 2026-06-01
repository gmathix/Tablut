package view;

import boardifier.model.GameElement;
import boardifier.view.ElementLook;
import javafx.geometry.Bounds;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import model.Pawn;

public class PawnLook extends ElementLook {
    private static final Color MOSCOVITE_EDGE = Color.web("#735a18");
    private static final Color MOSCOVITE_LIGHT = Color.web("#f2df8a");
    private static final Color MOSCOVITE_DARK = Color.web("#b88f1f");

    private static final Color SOLDIER_EDGE = Color.web("#1f4f2f");
    private static final Color SOLDIER_LIGHT = Color.web("#bfe2c8");
    private static final Color SOLDIER_DARK = Color.web("#2f7a47");

    private static final Color KING_EDGE = Color.web("#7b6320");
    private static final Color KING_LIGHT = Color.web("#bfe2c8");
    private static final Color KING_DARK = Color.web("#2f7a47");


    private Circle circle;
    private Circle innerCircle; // only for the king
    private Circle highlight;
    private final int radius;

    public PawnLook(int radius, GameElement element) {
        // Pawn look is constituted of a single character, so shape size = 1x1
        super(element);
        this.radius = radius;
        render();
    }

    @Override
    public void onSelectionChange() {
        Pawn pawn = (Pawn) getElement();
        if (pawn.isSelected()) {
            circle.setStrokeWidth(4);
            circle.setStrokeType(StrokeType.CENTERED);
            circle.setStroke(Color.web("#f7f0c0"));
            getGroup().setEffect(new DropShadow(18, Color.color(0, 0, 0, 0.55)));
        } else {
            circle.setStrokeWidth(1.5);
            circle.setStrokeType(StrokeType.CENTERED);
            circle.setStroke(getEdgeColor(pawn.getColor()));
            getGroup().setEffect(new DropShadow(10, Color.color(0, 0, 0, 0.35)));
        }
    }

    @Override
    public void onFaceChange() {

    }

    public static Color getEdgeColor(int pawnType) {
        return switch (pawnType) {
            case Pawn.PAWN_MOSCOVITE -> MOSCOVITE_EDGE;
            case Pawn.PAWN_SOLDIER -> SOLDIER_EDGE;
            case Pawn.PAWN_KING -> KING_EDGE;
            default -> Color.BLACK;
        };
    }

    public static RadialGradient getGradient(int pawnType) {
        return switch (pawnType) {
            case Pawn.PAWN_MOSCOVITE -> new RadialGradient(
                    0, .1, -.28, .32, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, MOSCOVITE_LIGHT),
                    new Stop(.55, Color.web("#d9b956")),
                    new Stop(1, MOSCOVITE_DARK)
            );
            case Pawn.PAWN_SOLDIER-> new RadialGradient(
                    0, .1, -.28, .32, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, SOLDIER_LIGHT),
                    new Stop(.55, Color.web("#6cbf84")),
                    new Stop(1, SOLDIER_DARK)
            );
            case Pawn.PAWN_KING -> new RadialGradient(
                    0, .1, -.28, .32, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, KING_LIGHT),
                    new Stop(.55, Color.web("#6cbf84")),
                    new Stop(1, KING_DARK)
            );
            default -> new RadialGradient(
                    0, 0.1, -0.28, -0.32, 1.0, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.LIGHTGRAY),
                    new Stop(1.0, Color.GRAY)
            );
        };
    }

    protected void render() {
        Pawn pawn = (Pawn)element;

        circle = new Circle(radius);
        circle.setFill(getGradient(pawn.getColor()));
        circle.setStroke(getEdgeColor(pawn.getColor()));
        circle.setStrokeWidth(1.5);

        innerCircle = new Circle(radius * 0.58);
        if (pawn.getColor() == Pawn.PAWN_KING) {
            innerCircle.setFill(Color.web("#2a2417"));
            innerCircle.setStroke(Color.web("#f7dd77"));
            innerCircle.setStrokeWidth(2);
        } else if (pawn.getColor() == Pawn.PAWN_MOSCOVITE) {
            innerCircle.setFill(Color.web("#f9e8a7", 0.22));
            innerCircle.setStroke(Color.web("#fff8df", 0.45));
            innerCircle.setStrokeWidth(1);
        } else {
            innerCircle.setFill(Color.web("#e1f3e6", 0.18));
            innerCircle.setStroke(Color.web("#f3fff5", 0.42));
            innerCircle.setStrokeWidth(1);
        }

        highlight = new Circle(radius * 0.24);
        highlight.setTranslateX(-radius * 0.28);
        highlight.setTranslateY(-radius * 0.34);
        highlight.setFill(Color.web("#ffffff", 0.28));
        highlight.setStroke(Color.TRANSPARENT);


        addShape(circle);
        addShape(innerCircle);
        addShape(highlight);

        onSelectionChange();
    }
}
