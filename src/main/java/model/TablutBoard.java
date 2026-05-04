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
    }

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
         /*
        TO FULFILL:
            - compute the list of cells that are valid to play taking the pawn value (i.e. number) into account.
            each Point in this list consists in couple x,y, where x is a column and y a row in the board.
         */
        return lst;
    }
}
