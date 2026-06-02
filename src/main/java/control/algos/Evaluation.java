package control.algos;

import model.RuleSets;


public class Evaluation {

    // virtual inf boundary that doesn't break floating point math of exp functions (unlike damn Double.NEGATIVE_INFINITY)
    public static final double VIRTUAL_INF = 10000;

    // weights for each evaluation criteria
    public static final double ESCAPE_PATH_WEIGHT  = 15;
    public static final double ENCERCLEMENT_WEIGHT = 20;
    public static final double MATERIAL_WEIGHT     = 30;
    public static final double POSITION_WEIGHT     = 5;


    
    /**
     * Main evaluation method used for the negamax
     * There current 3 evaluation criteria are :
     *   1. Immediate win conditions :
     *      - Swedish king reached an edge -> swedish wins
     *      - Moscovite has surrounded the king -> moscovite wins
     *   2. Delayed win conditions :
     *      - either it is swedish to play and it can reach one or more edges -> swedish wins
     *      - or it is moscovite to play and the king can reach two or more edges (Tuichi) -> swedish wins
     *      - or it is moscovite to play and they can surround the king -> moscovite wins
     *   3. King encerclement (relative to swedish) :
     *      - Vertically or horizontally, surrounding pieces receive a +5 score
     *      - Diagonally, they receive a +2.5 score
     *      - Moscovites (moscovite pieces) receive a x3 multiplier
     *      - Total encerclement is then negative
     *   4. Material difference :
     *      - Calculate the proportion difference between the remaining pawns :
     *         - Swedish pawns are 8 at the start
     *         - Moscovite pawns are 16 at the start
     *
     * @param turn 0 for swedish, 1 for moscovite
     * @param currDepth the depth of the current node (should be 0)
     * @param baseDepth the total depth of the whole tree
     * @return
     */
    public static double evaluate(RecurBoard recurBoard, int turn, int currDepth, int baseDepth) {
        int depthDiff = baseDepth - currDepth;

        double score = 0;

        // 1. check win conditions immediately
        double win = recurBoard.checkWin();
        if (win == (double) Integer.MAX_VALUE) {
            /** swedish won : if we are swedish this is good otherwise it's terrible
             * we substract/add depthDiff so that less deep wins will have better score
             */
            return (turn == 0) ? (VIRTUAL_INF - depthDiff) : (-VIRTUAL_INF + depthDiff);
        }
        if (win == Integer.MIN_VALUE) {
            // moscovite won : if we are moscovite this is good otherwise it's terrible
            return (turn == 1) ? (VIRTUAL_INF - depthDiff) : (-VIRTUAL_INF + depthDiff);
        }


        // 2. check delayed wins
        double delayedWin = checkDelayedWinAndPaths(recurBoard, turn, depthDiff);
        if (Math.abs(delayedWin) >= VIRTUAL_INF - 100)
            return delayedWin;





        // 3. positional evaluation
        double encerclement = countKingEncerclement(recurBoard);

        // 4. count material difference
        double materialDiff = countMaterialDiff(recurBoard);

        double boardControl = evaluateBoardControl(recurBoard);


        double swedishScore = (delayedWin * ESCAPE_PATH_WEIGHT) +
                            (encerclement * ENCERCLEMENT_WEIGHT) +
                            (materialDiff * MATERIAL_WEIGHT) +
                            (boardControl * POSITION_WEIGHT);


        return (turn == 0) ? swedishScore : -swedishScore;
    }

    public static double checkDelayedWinAndPaths(RecurBoard recurBoard, int turn, int depthDiff) {

        int kingY = recurBoard.getKingY();
        int kingX = recurBoard.getKingX();

        /**
         * check king escape and encerclement simultaneously.
         * we start from the king's position, loop in all 4 direction until :
         *    - an edge is reached without anything blocking the king -> swedish wins
         *    - a moscovite is reached with nothing between it and the king, and there are already 3
         *      moscovites surrounding the king (or the king is next to the center throne, so there just
         *      needs to be 3 surrounding moscovites)
         */

        int nbEdgesReachable = 0;
        int surroundingMoscovites = 0;
        boolean kingSurrounded = false;
        int kingObstructions = 0;


        boolean kingOnThrone = (kingX == 4 && kingY == 4);

        int maxDistance = 8;
        if (RuleSets.isConstrainedKingMoves(recurBoard.getStageModel().getRuleSet())) {
            maxDistance = 4;
        }

        for (int i = 0; i < 4; i++) {
            int dy = RecurBoard.DY_VALS[i];
            int dx = RecurBoard.DX_VALS[i];
            int x = kingX;
            int y = kingY;
            boolean edgeReachable = true;

            for (int j = 1; j <= maxDistance; j++) {
                x += dx;
                y += dy;
                if (x < 0 || x > 8 || y < 0 || y > 8) break;

                int piece = recurBoard.getBoard()[y][x];

                if (j == 1) {
                    if (recurBoard.isMoscovite(piece) || (x == 4 && y == 4)) {
                        surroundingMoscovites++;
                    }
                }

                if (!recurBoard.isEmpty(piece)) {
                    edgeReachable = false;
                    if (recurBoard.isMoscovite(piece) && j > 0) {
                        kingObstructions++;
                    } else if (recurBoard.isSoldier(piece)) {
                        kingObstructions++;
                    }
                    break;
                }

                if (x == 0 || x == 8 || y == 0 || y == 8) { // king on edge
                    if (RuleSets.isConstrainedKingSquares(recurBoard.getStageModel().getRuleSet()) && RecurBoard.constrainedKingSquares.contains(y * 9 + x)) {
                        edgeReachable = false;
                    } else if (RuleSets.isCornerKingEscapes(recurBoard.getStageModel().getRuleSet()) && !RecurBoard.cornerSquares.contains(y * 9 + x)) {
                        edgeReachable = false;
                    }
                }

            }

            if (edgeReachable) {
                nbEdgesReachable++;
            }
        }


        if (kingOnThrone && surroundingMoscovites == 4) {
            kingSurrounded = true;
        } else if (!kingOnThrone && (kingX == 4 && (kingY == 3 || kingY == 5))
                || (kingY == 4 && (kingX == 3 || kingX == 5))) {
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


        // return relative to swedish
        return (nbEdgesReachable * 4.0) - (kingObstructions * 1.5);
    }

    /**
     * Evaluate the king encerclement relative to Swedish (always negative) :
     *    - Vertically or horizontally, surrounding pieces receive a +1 score
     *    - Diagonally, they receive a +0.5 score
     *    - Moscovites (moscovite pieces) receive a x3 multiplier
     *    - Soldiers (swedish pieces) receive a -1 multiplier because they protect the king
     */
    public static double countKingEncerclement(RecurBoard recurBoard) {
        double encerclement = 0;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int y = recurBoard.getKingY() + dy;
                int x = recurBoard.getKingX() + dx;
                if (dy == 0 && dx == 0) continue;
                if (y < 0 || y > 8 || x < 0 || x > 8) continue;

                int piece = recurBoard.getBoard()[y][x];
                if (!recurBoard.isEmpty(piece)) {
                    double score;
                    if (Math.abs(dy - dx) == 1) score = 1; // vertical or horizontal
                    else                        score = 0.5; // diagonal

                    if (recurBoard.isMoscovite(piece)) {
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
     * Returns the material difference (king excluded) relative to Swedish
     */
    public static double countMaterialDiff(RecurBoard recurBoard) {
        double nbMoscovitePawns = 0;
        double nbSwedishPawns = 0;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (recurBoard.getBoard()[i][j] == RecurBoard.MOSCOVITE) {
                    nbMoscovitePawns++;
                } else if (recurBoard.getBoard()[i][j] == RecurBoard.SWEDISH) {
                    nbSwedishPawns++;
                }
            }
        }

        // normalize between -30 and 30
        return (nbSwedishPawns / 8 - nbMoscovitePawns / 16) * 30;
    }


    public static double evaluateBoardControl(RecurBoard recurBoard) {
        double score = 0;
        for (int i = 2; i <= 6; i++) {
            for (int j = 2; j <= 6; j++) {
                if (i == 4 && j == 4) continue;
                int piece = recurBoard.getBoard()[i][j];
                if (recurBoard.isSoldier(piece)) {
                    score += 0.2;
                } else if (recurBoard.isMoscovite(piece)) {
                    score -= 0.2;
                }
            }
        }
        return score;
    }


    /**
     * Counts the number of moscovites present in the same 5x5 outer region as the king
     */
    public static int countMoscovitesIn5x5Region(RecurBoard recurBoard) {
        int regionStartX = (recurBoard.getKingX() <= 4) ? 0 : 4;
        int regionStartY = (recurBoard.getKingY() <= 4) ? 0 : 4;

        int nbMoscovites = 0;
        for (int y = regionStartY; y < regionStartY + 5; y++) {
            for (int x = regionStartX; x < regionStartX + 5; x++) {
                if (recurBoard.isMoscovite(recurBoard.getBoard()[y][x])) {
                    nbMoscovites++;
                }
            }
        }

        return nbMoscovites;
    }
}
