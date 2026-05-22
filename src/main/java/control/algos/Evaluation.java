package control.algos;

import javax.swing.plaf.ViewportUI;

public class Evaluation {

    // virtual inf boundary that doesn't break floating point math of exp functions (unlike damn Double.NEGATIVE_INFINITY)
    public static final double VIRTUAL_INF = 10000;

    // weights for each evaluation criteria
    public static final double ESCAPE_PATH_WEIGHT  = 40;
    public static final double ENCERCLEMENT_WEIGHT = 25;
    public static final double MATERIAL_WEIGHT     = 15;
    public static final double POSITION_WEIGHT     = 10;


    
    /**
     * Main evaluation method used for the negamax
     * There current 3 evaluation criteria are :
     *   1. Immediate win conditions :
     *      - Green king reached an edge -> green wins
     *      - Yellow has surrounded the king -> yellow wins
     *   2. Delayed win conditions :
     *      - either it is green to play and it can reach one or more edges -> green wins
     *      - or it is yellow to play and the king can reach two or more edges (Tuichi) -> green wins
     *      - or it is yellow to play and they can surround the king -> yellow wins
     *   3. King encerclement (relative to green) :
     *      - Vertically or horizontally, surrounding pieces receive a +5 score
     *      - Diagonally, they receive a +2.5 score
     *      - Moscovites (yellow pieces) receive a x3 multiplier
     *      - Total encerclement is then negative
     *   4. Material difference :
     *      - Calculate the proportion difference between the remaining pawns :
     *         - Green pawns are 8 at the start
     *         - Yellow pawns are 16 at the start
     *
     * @param turn 0 for green, 1 for yellow
     * @param currDepth the depth of the current node (should be 0)
     * @param baseDepth the total depth of the whole tree
     * @return
     */
    public static double evaluate(Board board, int turn, int currDepth, int baseDepth) {
        int depthDiff = baseDepth - currDepth;

        double score = 0;

        // 1. check win conditions immediately
        double win = board.checkWin();
        if (win == (double) Integer.MAX_VALUE) {
            /** green won : if we are green this is good otherwise it's terrible
             * we substract/add depthDiff so that less deep wins will have better score
             */
            return (turn == 0) ? (VIRTUAL_INF - depthDiff) : (-VIRTUAL_INF + depthDiff);
        }
        if (win == Integer.MIN_VALUE) {
            // yellow won : if we are yellow this is good otherwise it's terrible
            return (turn == 1) ? (VIRTUAL_INF - depthDiff) : (-VIRTUAL_INF + depthDiff);
        }


        // 2. check delayed wins
        double delayedWin = checkDelayedWinAndPaths(board, turn, depthDiff);
        if (Math.abs(delayedWin) >= VIRTUAL_INF - 100)
            return delayedWin;





        // 3. positional evaluation
        double encerclement = countKingEncerclement(board);

        // 4. count material difference
        double materialDiff = countMaterialDiff(board);

        double boardControl = evaluateBoardControl(board);


        double greenScore = (delayedWin * ESCAPE_PATH_WEIGHT) +
                            (encerclement * ENCERCLEMENT_WEIGHT) +
                            (materialDiff * MATERIAL_WEIGHT) +
                            (boardControl * POSITION_WEIGHT);


        return (turn == 0) ? greenScore : -greenScore;
    }

    public static double checkDelayedWinAndPaths(Board board, int turn, int depthDiff) {


        /**
         * check king escape and encerclement simultaneously.
         * we start from the king's position, loop in all 4 direction until :
         *    - an edge is reached without anything blocking the king -> green wins
         *    - a moscovite is reached with nothing between it and the king, and there are already 3
         *      moscovites surrounding the king (or the king is next to the center throne, so there just
         *      needs to be 3 surrounding moscovites)
         */

        int nbEdgesReachable = 0;
        int surroundingMoscovites = 0;
        boolean kingSurrounded = false;
        int kingObstructions = 0;


        boolean kingOnThrone = (board.kingX == 4 && board.kingY == 4);

        for (int i = 0; i < 4; i++) {
            int dy = Board.DY_VALS[i];
            int dx = Board.DX_VALS[i];
            int x = board.kingX;
            int y = board.kingY;
            boolean edgeReachable = true;

            for (int j = 0; j < 9; j++) {
                x += dx;
                y += dy;
                if (x < 0 || x > 8 || y < 0 || y > 8) break;

                int piece = board.board[y][x];

                if (j == 0) {
                    if (board.isMoscovite(piece) || (x == 4 && y == 4)) {
                        surroundingMoscovites++;
                    }
                }

                if (!board.isEmpty(piece)) {
                    edgeReachable = false;
                    if (board.isMoscovite(piece) && j > 0) {
                        kingObstructions++;
                    } else if (board.isSoldier(piece)) {
                        kingObstructions++;
                    }
                    break;
                }
            }

            if (edgeReachable) {
                nbEdgesReachable++;
            }
        }


        if (kingOnThrone && surroundingMoscovites == 4) {
            kingSurrounded = true;
        } else if (!kingOnThrone && (board.kingX == 4 && (board.kingY == 3 || board.kingY == 5))
                || (board.kingY == 4 && (board.kingX == 3 || board.kingX == 5))) {
            if (surroundingMoscovites >= 3) kingSurrounded = true;
        } else {
            if (surroundingMoscovites >= 4) kingSurrounded = true;
        }


        if ((turn == 0 && nbEdgesReachable >= 1) || (turn == 1 && nbEdgesReachable >= 2)) {
            return turn == 0 ? (VIRTUAL_INF - depthDiff) : (-VIRTUAL_INF + depthDiff);
        }
        if (kingSurrounded) {
            return turn == 1 ? (VIRTUAL_INF - depthDiff) : (-VIRTUAL_INF + depthDiff);
        }


        // return relative to green
        return (nbEdgesReachable * 4.0) - (kingObstructions * 1.5) + (surroundingMoscovites * -5.0);
    }

    /**
     * Evaluate the king encerclement relative to Green (always negative) :
     *    - Vertically or horizontally, surrounding pieces receive a +1 score
     *    - Diagonally, they receive a +0.5 score
     *    - Moscovites (yellow pieces) receive a x3 multiplier
     *    - Soldiers (green pieces) receive a -1 multiplier because they protect the king
     */
    public static double countKingEncerclement(Board board) {
        double encerclement = 0;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int y = board.kingY + dy;
                int x = board.kingX + dx;
                if (dy == 0 && dx == 0) continue;
                if (y < 0 || y > 8 || x < 0 || x > 8) continue;

                int piece = board.board[y][x];
                if (!board.isEmpty(piece)) {
                    double score;
                    if (Math.abs(dy - dx) == 1) score = 1; // vertical or horizontal
                    else                        score = 0.5; // diagonal

                    if (board.isMoscovite(piece)) {
                        score *= 3;
                    } else {
                        score *= -1;
                    }

                    encerclement += score;
                }
            }
        }

        return -encerclement;
    }


    /**
     * Returns the material difference (king excluded) relative to Green
     */
    public static double countMaterialDiff(Board board) {
        double nbYellowPawns = 0;
        double nbGreenPawns = 0;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (board.board[i][j] == Board.MOSCOVITE) {
                    nbYellowPawns++;
                } else if (board.board[i][j] == Board.SOLDIER) {
                    nbGreenPawns++;
                }
            }
        }

        // normalize between -10 and 10
        return (nbGreenPawns / 8 - nbYellowPawns / 16) * 10;
    }


    public static double evaluateBoardControl(Board board) {
        double score = 0;
        for (int i = 2; i <= 6; i++) {
            for (int j = 2; j <= 6; j++) {
                if (i == 4 && j == 4) continue;
                int piece = board.board[i][j];
                if (board.isSoldier(piece)) {
                    score += 0.2;
                } else if (board.isMoscovite(piece)) {
                    score -= 0.2;
                }
            }
        }
        return score;
    }
}
