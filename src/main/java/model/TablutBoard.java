package model;

import boardifier.control.Controller;
import boardifier.model.GameElement;
import boardifier.model.GameStageModel;
import boardifier.model.ContainerElement;
import boardifier.view.GameStageView;
import control.TablutController;
import control.algos.FastBoard;
import view.Constants;
import view.PawnLook;

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
    private TablutStageModel stageModel;


    public static final int BOARD_SIZE = 9;

    // current coordinates of the king
    private int kingX;
    private int kingY;

    /* 2 = moscovite (moscovite)
     * 3 = soldier (swedish)
     * 4 = king (swedish)
     */
    public static int[][] startingBoard = {
            {0, 0, 0, 1, 1, 1, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 2, 0, 0, 0, 0},
            {1, 0, 0, 0, 2, 0, 0, 0, 1},
            {1, 1, 2, 2, 3, 2, 2, 1, 1},
            {1, 0, 0, 0, 2, 0, 0, 0, 1},
            {0, 0, 0, 0, 2, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 0, 0, 1, 1, 1, 0, 0, 0},
    };
    public TablutBoard(int x, int y, GameStageModel gameStageModel) {
        // call the super-constructor to create a 9x9 grid, named "holeboard", and in x,y in space
        super("tablutboard", x, y, BOARD_SIZE , BOARD_SIZE, gameStageModel);
        resetReachableCells(false);
        this.kingY = 4;
        this.kingX = 4;

        this.stageModel = (TablutStageModel) gameStageModel;
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
        if (pawn.getColor() == Pawn.PAWN_KING && RuleSets.isConstrainedKingMoves(stageModel.getRuleSet())) {
            distanceCount = 4;
        }

        for (int d = 0; d < 4; d++) {
            int dy = FastBoard.DY_VALS[d];
            int dx = FastBoard.DX_VALS[d];

            int y = pawnY + dy;
            int x = pawnX + dx;

            for (int i = 0; i < distanceCount; i++) {
                if (y < 0 || y > 8 || x < 0 || x > 8) break;
                if (y == 4 && x == 4) break;

                if (!(getElement(y, x) instanceof Pawn p)) {
                    if (RuleSets.isAshtonRules(stageModel.getRuleSet()) && RuleSets.isCampSquare(y*9 + x)) {
                        if (pawn.getColor() == Pawn.PAWN_MOSCOVITE && RuleSets.isCampSquare(pawnY*9 + pawnX)) {
                            lst.add(new Point(x, y));
                        } else {
                            break;
                        }
                    } else {
                        lst.add(new Point(x, y));
                    }
                } else break;

                y += dy;
                x += dx;
            }
        }

        return lst;
    }


    public void applyMove(Move move) {
        Pawn movedPawn = (Pawn) getElement(move.srcY(), move.srcX());
        moveElement(movedPawn, move.dstY(), move.dstX());
        movedPawn.setBoardX(move.dstX());
        movedPawn.setBoardY(move.dstY());

        if (movedPawn.getColor() == Pawn.PAWN_KING) {
            kingX = move.dstX();
            kingY = move.dstY();
        }

        List<Capture> captures = move.getCaptures();
        List<Pawn> capturedPawns = move.getCapturedPawns();
        for (int i = 0; i < captures.size(); i++) {
            Capture cap = captures.get(i);
            Pawn pawn = i < capturedPawns.size() ? capturedPawns.get(i) : null;
            if (pawn == null) {
                pawn = (Pawn) getElement(cap.y(), cap.x());
            }
            if (pawn != null) {
                removeElement(pawn);
            }
        }
    }

    public void undoMove(Controller control, GameStageView view, GameStageModel model, Move move) {
        // assume the move was valid

        TablutController tablutControl = (TablutController) control;
        TablutStageModel stageModel = (TablutStageModel) model;

        Pawn movedPawn = (Pawn) getElement(move.dstY(), move.dstX());
        if (movedPawn != null) {
            moveElement(movedPawn, move.srcY(), move.srcX());
            movedPawn.setBoardX(move.srcX());
            movedPawn.setBoardY(move.srcY());

            if (movedPawn.getColor() == Pawn.PAWN_KING) {
                kingX = move.srcX();
                kingY = move.srcY();
            }
        }

        List<Capture> captures = move.getCaptures();
        List<Pawn> capturedPawns = move.getCapturedPawns();

        for (int i = 0; i < captures.size(); i++) {
            Capture cap = captures.get(i);

            Pawn pawn = (i < capturedPawns.size()) ? capturedPawns.get(i) : null;

            if (pawn == null) {
                // fallback only when replay data is incomplete otherwise we create a clone of the pawn (which would be cheating)
                pawn = new Pawn(0, cap.piece(), stageModel);
                PawnLook look = new PawnLook((int) Constants.PAWN_SIZE, pawn);
                look.setDepth(5);
                view.addLook(look);
                tablutControl.getMapElementLook().put(pawn, look);
            }

            pawn.setBoardX(cap.x());
            pawn.setBoardY(cap.y());

            if (pawn.getColor() == Pawn.PAWN_KING) {
                kingX = cap.x();
                kingY = cap.y();
            }
            pawn.setVisible(true);

            addElement(pawn, cap.y(), cap.x());
        }
    }

    /**

     * Returns a list of the pawns that get captured with the current move
     * Returned values are pawn indexes in raster order (y * 9 + x)
     */
    public List<Integer> checkCaptures(boolean isMoscovite, int colSrc, int colDest, int rowSrc, int rowDest) {
        List<Integer> captures = new ArrayList<>();


        // check capture
        int horizontalDirection = 0;
        int verticalDirection = 0;

        if (colSrc - colDest != 0)
            horizontalDirection = colDest - colSrc > 0 ? 1 : -1; // 1 for right, -1 for left
        if (rowSrc - rowDest != 0)
            verticalDirection = rowDest - rowSrc > 0 ? 1 : -1;   // 1 for down, -1 for up

        int[] dy_vals = {-1, 0, 1, 0};
        int[] dx_vals = {0, -1, 0, 1};

        for (int i = 0; i < 4; i++) {
            int dy = dy_vals[i];
            int dx = dx_vals[i];

            // do not check the squares on the path the pawn came from
            if (dx == -horizontalDirection && horizontalDirection != 0) continue;
            if (dy == -verticalDirection && verticalDirection != 0) continue;


            int dstY1 = rowDest + dy;
            int dstX1 = colDest + dx;

            int dstY2 = rowDest + 2*dy;
            int dstX2 = colDest + 2*dx;

            // check bounds for pawn 2 squares away
            if (dstY2 < 0 || dstY2 >= 9) continue;
            if (dstX2 < 0 || dstX2 >= 9) continue;


            GameElement sideEl = getElement(dstY1, dstX1);
            GameElement sideEl2 = getElement(dstY2, dstX2);


            boolean isN2Ally = (RuleSets.isAshtonRules(stageModel.getRuleSet()) &&
                                (RuleSets.isCampSquare(dstY2 * 9 + dstX2) || (dstY2 == 4 && dstX2 == 4)));

            if ((sideEl instanceof Pawn sideP)) {
                boolean isNEnemy = (isMoscovite && sideP.getColor() == Pawn.PAWN_SOLDIER) ||
                                    (!isMoscovite && sideP.getColor() == Pawn.PAWN_MOSCOVITE);
                if ((sideEl2 instanceof Pawn sideP2)) {
                    isN2Ally = isN2Ally || (isMoscovite && sideP2.getColor() == Pawn.PAWN_MOSCOVITE) ||
                            (!isMoscovite && sideP2.getColor() != Pawn.PAWN_MOSCOVITE);
                }


                if (isNEnemy && isN2Ally) {
                    captures.add(dstY1 * 9 + dstX1);
                }
            }
        }

        return captures;
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
