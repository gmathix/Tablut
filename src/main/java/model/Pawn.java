package model;

import boardifier.model.ElementTypes;
import boardifier.model.GameElement;
import boardifier.model.GameStageModel;
import boardifier.model.animation.Animation;
import boardifier.model.animation.AnimationStep;

/**
 * A basic pawn element, with only 2 fixed parameters : number and color
 * There are no setters because the state of a Tablut pawn is fixed.
 */
public class Pawn extends GameElement {

    private int number;
    private int color;


    public static final int PAWN_MOSCOVITE = 1;
    public static final int PAWN_SOLDIER   = 2;
    public static final int PAWN_KING      = 3;

    // coords inside the board
    private int boardX;
    private int boardY;


    public Pawn(int number, int color, GameStageModel gameStageModel) {
        super(gameStageModel);
        ElementTypes.register("pawn",50);
        type = ElementTypes.getType("pawn");
        this.number = number;
        this.color  = color;
        this.boardY = 0;
        this.boardX = 0;
    }

    public void setBoardX(int boardX) { this.boardX = boardX; }
    public void setBoardY(int boardY) { this.boardY = boardY; }

    public int getNumber() {
        return number;
    }
    public int getColor() {
        return color;
    }
    public int getBoardX() { return boardX; }
    public int getBoardY() { return boardY; }

    public String toString() {
        String type = switch (color) {
            case PAWN_MOSCOVITE -> "moscovite";
            case PAWN_SOLDIER ->   "soldier";
            case PAWN_KING ->      "king";
            default ->             "thing";
        };

        return String.format("Pawn : %s, number %d\n", type, number);
    }

    public void update() {
        if (animation != null) {
            AnimationStep step = animation.next();
            if (step == null) {
                animation = null;
            } else if (step != Animation.NOPStep) {
                setLocation(step.getInt(0), step.getInt(1));
            }
        }
    }
}
