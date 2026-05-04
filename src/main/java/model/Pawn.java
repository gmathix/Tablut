package model;

import boardifier.model.ElementTypes;
import boardifier.model.GameElement;
import boardifier.model.GameStageModel;

/**
 * A basic pawn element, with only 2 fixed parameters : number and color
 * There are no setters because the state of a Tablut pawn is fixed.
 */
public class Pawn extends GameElement {

    private int number;
    private int color;


    public static int PAWN_MOSCOVITE = 2;
    public static int PAWN_SOLDIER   = 3;
    public static int PAWN_KING      = 4;


    public Pawn(int number, int color, GameStageModel gameStageModel) {
        super(gameStageModel);
        ElementTypes.register("pawn",50);
        type = ElementTypes.getType("pawn");
        this.number = number;
        this.color  = color;
    }

    public int getNumber() {
        return number;
    }
    public int getColor() {
        return color;
    }
}
