package control.algos;

public class Evaluation {


    // weights for each evaluation criteria
    public static final double ENCERCLEMENT_WEIGHT = 0.7;
    public static final double MATERIAL_WEIGHT     = 0.3;

    
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
            return (turn == 0) ? (Double.MAX_VALUE - depthDiff) : (Double.MIN_VALUE + depthDiff);
        }
        if (win == Integer.MIN_VALUE) {
            // yellow won : if we are yellow this is good otherwise it's terrible
            return (turn == 1) ? (Double.MAX_VALUE - depthDiff) : (Double.MIN_VALUE + depthDiff);
        }


        // 2. check delayed wins
        double delayedWin = checkDelayedWin(board, turn, depthDiff);
        if (delayedWin != 0) return delayedWin;





        // 3. check king encerclement
        double encerclement = countKingEncerclement(board);


        // 4. count material difference
        double materialDiff = countMaterialDiff(board);


        double greenScore = ENCERCLEMENT_WEIGHT * encerclement +
                            MATERIAL_WEIGHT * materialDiff;


        return (turn == 0) ? greenScore : -greenScore;
    }

    public static double checkDelayedWin(Board board, int turn, int depthDiff) {


        /**
         * check king escape and encerclement simultaneously.
         * we start from the king's position, loop in all 4 direction until :
         *    - an edge is reached without anything blocking the king -> green wins
         *    - a moscovite is reached with nothing between it and the king, and there are already 3
         *      moscovites surrounding the king (or the king is next to the center throne, so there just
         *      needs to be 3 surrounding moscovites)
         */

        int nbEdgesReachable = 0;
        int nbSurrounding = 0;
        boolean kingSurrounded = false;
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

                // count surrounding moscovites
                if (j == 0 && (board.isMoscovite(board.board[i][j]) || (x==4 && y==4))) {
                    nbSurrounding++;
                    if (nbSurrounding == 4) {
                        kingSurrounded = true;
                    }
                }

                // check if a moscovite can move next to the king and surround it
                if (j > 0 && nbSurrounding == 3 && board.isMoscovite(board.board[i][j])) {
                    kingSurrounded = true;
                }

                // check if there is something else in the way
                if (!board.isEmpty(board.board[y][x])) {
                    edgeReachable = false;
                    break;
                }
            }

            if (edgeReachable) {
                nbEdgesReachable++;
            }
        }

        /**
         * green wins if :
         * - it is green to play and the king can reach at least one edge
         * - or it is yellow to play and the king can reach at least two edges
         */
        if ((turn == 0 && nbEdgesReachable >= 1) ||
                (turn == 1 && nbEdgesReachable >= 2)) {

            // closer to the root (=higher depth value) means a faster win
            return (turn == 0) ? (Double.MAX_VALUE - depthDiff) : (Double.MIN_VALUE + depthDiff);
        }

        /**
         * yellow wins if the king can be surrounded
         */
        if (turn == 1 && kingSurrounded) {
            return Double.MAX_VALUE - depthDiff;
        }




        return 0;
    }


    /**
     * Evaluate the king encerclement relative to Green (always negative) :
     *    - Vertically or horizontally, surrounding pieces receive a +5 score
     *    - Diagonally, they receive a +2.5 score
     *    - Moscovites (yellow pieces) receive a x3 multiplier
     */
    public static double countKingEncerclement(Board board) {
        double encerclement = 0;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int y = board.kingY + dy;
                int x = board.kingX + dx;
                if (dy == 0 && dx == 0) continue;
                if (y < 0 || y > 8 || x < 0 || x > 8) continue;

                if (!board.isEmpty(board.board[y][x])) {
                    double score;
                    if (Math.abs(dy - dx) == 1) score = 5; // vertical or horizontal
                    else                        score = 2.5; // diagonal

                    if (board.isMoscovite(board.board[y][x])) {
                        score *= 3;
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

        // normalize between -5 and 5
        return (nbGreenPawns / 8 - nbYellowPawns / 16) * 500;
    }
}
