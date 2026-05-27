package model;

import boardifier.model.GameStageModel;
import boardifier.model.ContainerElement;

import java.util.ArrayList;
import java.util.List;
import java.awt.*;



/**
 * Tablut main board represent the element where pawns are put when played
 * Thus, a simple ContainerElement with 3 rows and 3 column is needed.
 * Nevertheless, in order to "simplify" the work for the controller part,
 * this class also contains method to determine all the valid cells to put a
 * pawn with a given value.
 */

public class TablutBoard extends ContainerElement {
    static final int BOARD_SIZE = 9;

    // current coordinates of the king
    private int kingX;
    private int kingY;

    /* 2 = moscovite (yellow)
     * 3 = soldier (green)
     * 4 = king (green)
     */
    public static int[][] startingBoard = {
            {0, 0, 0, 2, 2, 2, 0, 0, 0},
            {0, 0, 0, 0, 2, 0, 0, 0, 0},
            {0, 0, 0, 0, 3, 0, 0, 0, 0},
            {2, 0, 0, 0, 3, 0, 0, 0, 2},
            {2, 2, 3, 3, 4, 3, 3, 2, 2},
            {2, 0, 0, 0, 3, 0, 0, 0, 2},
            {0, 0, 0, 0, 3, 0, 0, 0, 0},
            {0, 0, 0, 0, 2, 0, 0, 0, 0},
            {0, 0, 0, 2, 2, 2, 0, 0, 0},
    };
    public TablutBoard(int x, int y, GameStageModel gameStageModel) {
        // call the super-constructor to create a 9x9 grid, named "holeboard", and in x,y in space
        super("tablutboard", x, y, BOARD_SIZE , BOARD_SIZE, gameStageModel);
        resetReachableCells(false);
        this.kingY = 4;
        this.kingX = 4;
    }

    public int getKingX() { return kingX; }
    public int getKingY() { return kingY; }
    public void setKingX(int kingX) { this.kingX = kingX; }
    public void setKingY(int kingY) { this.kingY = kingY; }

    public void setValidCells(int number) {
        resetReachableCells(false);
        List<Point> valid = computeValidCells(number);
        if (valid != null) {
            for(Point p : valid) {
                reachableCells[p.y][p.x] = true;
            }
        }
    }
    public List<Point> computeValidCells(int number) {
        List<Point> lst = new ArrayList<>();


        Pawn pawn = null;
        int pawnY = 0, pawnX = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (getElement(i, j) instanceof Pawn p) {
                    if (p.getNumber() == number) {
                        pawn = p;
                        pawnY = i;
                        pawnX = j;
                    }
                }
            }
        }

        int distanceCount = 8;
        if (pawn.getColor() == Pawn.PAWN_KING && RuleSets.isConstrainedKingMoves()) {
            distanceCount = 4;
        }
        // check horizontal empty squares to the left
        for (int x = pawnX-1; x >= 0; x--) {
            if (distanceCount == 0) break;
            if (!(getElement(pawnY, x) instanceof Pawn)) {
                if (!(x == 4 && pawnY == 4)) {
                    lst.add(new Point(x, pawnY));
                }
            } else {
                // there is a pawn here, can't go further
                break;
            }
            distanceCount--;
        }

        distanceCount = 8;
        if (pawn.getColor() == Pawn.PAWN_KING && RuleSets.isConstrainedKingMoves()) {
            distanceCount = 4;
        }
        // check horizontal empty squares to the right
        for (int x = pawnX+1; x < BOARD_SIZE; x++) {
            if (distanceCount == 0) break;
            if (!(getElement(pawnY, x) instanceof Pawn)) {
                if (!(x == 4 && pawnY == 4)) {
                    lst.add(new Point(x, pawnY));
                }
            } else {
                // there is a pawn here, can't go further
                break;
            }
            distanceCount--;
        }

        distanceCount = 8;
        if (pawn.getColor() == Pawn.PAWN_KING && RuleSets.isConstrainedKingMoves()) {
            distanceCount = 4;
        }
        // check vertical empty squares up
        for (int y = pawnY-1; y >= 0; y--) {
            if (distanceCount == 0) break;
            if (!(getElement(y, pawnX) instanceof Pawn)) {
                if (!(x == 4 && pawnY == 4)) {
                    lst.add(new Point(pawnX, y));
                }
            } else {
                // there is a pawn here, can't go further
                break;
            }
            distanceCount--;
        }

        distanceCount = 8;
        if (pawn.getColor() == Pawn.PAWN_KING && RuleSets.isConstrainedKingMoves()) {
            distanceCount = 4;
        }
        // check vertical empty squares down
        for (int y = pawnY+1; y < BOARD_SIZE; y++) {
            if (distanceCount == 0) break;
            if (!(getElement(y, pawnX) instanceof Pawn)) {
                if (!(x == 4 && pawnY == 4)) {
                    lst.add(new Point(pawnX, y));
                }
            } else {
                // there is a pawn here, can't go further
                break;
            }
            distanceCount--;
        }



        return lst;
    }


    public String getStringRepresentation() {
        String rep = "";

        for (int i = 0 ; i < 9; i++) {
            for (int j =0 ; j< 9; j++) {
                if (getElement(i, j) instanceof Pawn p) {
                    rep += switch (p.getColor()) {
                        case Pawn.PAWN_SOLDIER -> "S";
                        case Pawn.PAWN_MOSCOVITE -> "M";
                        case Pawn.PAWN_KING -> "K";
                        default -> "E";
                    };
                } else {
                    rep += "E";
                }
            }
        }

        return rep;
    }
}
