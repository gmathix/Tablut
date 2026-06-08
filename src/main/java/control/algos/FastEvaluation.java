package control.algos;

import model.Pawn;
import model.RuleSets;


public class FastEvaluation {

    // virtual inf boundary that doesn't break floating point math of exp functions (unlike damn Double.NEGATIVE_INFINITY)
    public static final float VIRTUAL_INF = 10000f;

    // weights for each evaluation criteria
    public static final float BLOCKING_MOSCOVITES_WEIGHT      = 25;
    public static final float ENCERCLEMENT_WEIGHT             = 60;
    public static final float EARLY_KING_SECURITY_WEIGHT      = 50;
    public static final float MIDDLEGAME_KING_SECURITY_WEIGHT = 50;
    public static final float MATERIAL_WEIGHT                 = 40;
    public static final float POSITION_WEIGHT                 = 20;

    public static final byte EMPTY     = 0;
    public static final byte MOSCOVITE = Pawn.PAWN_MOSCOVITE;
    public static final byte SOLDIER   = Pawn.PAWN_SOLDIER;
    public static final byte KING      = Pawn.PAWN_KING;



    /**
     * Main evaluation method used for the negamax
     * There current 3 evaluation criteria are :
     *
     *   1. Delayed win conditions :
     *      - either it is swedish to play and it can reach one or more edges -> swedish wins
     *      - or it is moscovite to play and the king can reach two or more edges (Tuichi) -> swedish wins
     *      - or it is moscovite to play and they can surround the king -> moscovite wins
     *
     *  2. King encerclement (relative to swedish) :
     *      - Vertically or horizontally, surrounding pieces receive a +1 score
     *      - Diagonally, they receive a +0.5 score
     *      - Total encerclement is then negative
     *
     *   3. Material difference :
     *      - Calculate the proportion difference between the remaining pawns :
     *         - Swedish pawns are 8 at the start
     *         - Moscovite pawns are 16 at the start
     *
     *   4. Early king security :
     *      - In early game (number of moscovites >= 12), the king must keep its 4 inner soldiers next to him
     *
     *   5. Middlegame king security :
     *      - In middlegame (8 <= number of moscovites < 12), the king should keep at least 2 orthogonally placed soldiers
     *
     *
     * @param turn 0 for swedish, 1 for moscovite
     * @param currDepth the depth of the current node (should be 0)
     * @param baseDepth the total depth of the whole tree
     * @return
     */
    public static float evaluate(byte[] board, int turn, int ply, int currDepth, int baseDepth,
                                 byte[] soldierCountStack, byte[] moscoviteCountStack,
                                 byte[] kingPosStack, int ruleSet) {


        // 1. check delayed wins
        float delayedWin = checkDelayedWinAndPaths(board, turn, ply, kingPosStack, ruleSet);
        if (delayedWin > VIRTUAL_INF - 100 || delayedWin < -VIRTUAL_INF + 100) {
            return delayedWin;
        }


        // 2. king encerclement
        float encerclement = countKingEncerclement(board, ply, kingPosStack);


        // 3. material difference (range is [-16;16] so we divide by 1.6f to normalize in [-10;10])
        byte materialDiff = (byte) ((soldierCountStack[ply]*2 - moscoviteCountStack[ply]) / 1.6f);


        // 4. early-game king security
        float earlyKingSecurity = evaluateEarlyKingSecurity(board, ply, kingPosStack, moscoviteCountStack, soldierCountStack);
        if (turn == 1) earlyKingSecurity = 0;


        // 5. middlegame king security
        float middlegameKingSecurity = evaluateMiddlegameKingSecurity(board, ply, kingPosStack, moscoviteCountStack, soldierCountStack);
        if (turn == 1) middlegameKingSecurity = 0;
        // only use this criteria when analyzing for swedish, otherwise moscovites might be tempted to throw material away,
        // just for the sake of triggering this evaluation and getting a better score


        // 6. board control (more centered pieces = better score)
        float boardControl = evaluateBoardControl(board);


        float swedishScore =
                (delayedWin              * BLOCKING_MOSCOVITES_WEIGHT) +
                (encerclement            * ENCERCLEMENT_WEIGHT) +
                (materialDiff            * MATERIAL_WEIGHT) +
                (earlyKingSecurity       * EARLY_KING_SECURITY_WEIGHT) +
                (middlegameKingSecurity  * MIDDLEGAME_KING_SECURITY_WEIGHT) +
                (boardControl            * POSITION_WEIGHT);


        return (turn == 0) ? swedishScore : -swedishScore;
    }

    public static float checkDelayedWinAndPaths(byte[] board, int turn, int ply, byte[] kingPosStack, int ruleSet) {

        int kingPos = kingPosStack[ply];
        int kingY = kingPos / 9;
        int kingX = kingPos % 9;

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
        int surroundMask = 0;
        boolean kingSurrounded = false;
        int kingObstructions = 0;



        boolean kingOnThrone = (kingX == 4 && kingY == 4);

        int maxDistance = 8;
        if (RuleSets.isConstrainedKingMoves(ruleSet)) {
            maxDistance = 4;
        }


        for (int i = 0; i < 4; i++) {
            int dy = FastBoard.DY_VALS[i];
            int dx = FastBoard.DX_VALS[i];
            int x = kingX;
            int y = kingY;
            boolean edgeReachable = true;

            for (int j = 1; j <= maxDistance; j++) {
                x += dx;
                y += dy;
                if (x < 0 || x > 8 || y < 0 || y > 8) break;

                int piece = board[y*9+x];

                if (edgeReachable && piece == MOSCOVITE ||  // that moscovite can come and bite the king
                        (j == 1 && RuleSets.isAshtonRules(ruleSet) && RuleSets.isCampSquare(y*9+x))) { // or it's a direct neighbor camp square

                    surroundingMoscovites++;
                    surroundMask |= 1 << i;
                }


                if (piece != EMPTY || (y*9+x == 40) || RuleSets.isAshtonRules(ruleSet) && RuleSets.isCampSquare(y*9+x)) {
                    edgeReachable = false;
                    kingObstructions++;
                    if (piece == MOSCOVITE) kingObstructions++; // moscovites that block count double
                }

                if (x == 0 || x == 8 || y == 0 || y == 8) { // king on edge
                    if (RuleSets.isConstrainedKingSquares(ruleSet) && RuleSets.constrainedKingSquares.contains(y * 9 + x)) {
                        edgeReachable = false;
                    } else if (RuleSets.isCornerKingEscapes(ruleSet) && !RuleSets.cornerSquares.contains(y * 9 + x)) {
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
            if (RuleSets.isAshtonRules(ruleSet)) {
                if (surroundingMoscovites >= 2 && ((surroundMask & 0b1010) == 0b1010 || (surroundMask & 0b0101) == 0b0101))
                    kingSurrounded = true;
            } else if (surroundingMoscovites >= 4) kingSurrounded = true;
        }


        if ((turn == 0 && nbEdgesReachable >= 1) || (turn == 1 && nbEdgesReachable >= 2)) {
            return VIRTUAL_INF - ply;
        }
        if (kingSurrounded) {
            return -VIRTUAL_INF + ply;
        }


        if (kingObstructions > 10) kingObstructions = 10;
        return -kingObstructions;
    }



    /**
     * Evaluate the king encerclement relative to Swedish (always negative) :
     *    - Vertically or horizontally, surrounding pieces receive a +1 score
     *    - Diagonally, they receive a +0.5 score
     */
    public static float countKingEncerclement(byte[] board, int ply, byte[] kingPosStack) {
        float encerclement = 0;

        int kingPos = kingPosStack[ply];
        int kingY = kingPos / 9;
        int kingX = kingPos % 9;


        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int y = kingY + dy;
                int x = kingX + dx;
                if (dy == 0 && dx == 0) continue;
                if (y < 0 || y > 8 || x < 0 || x > 8) continue;

                int piece = board[y*9+x];
                if (piece == MOSCOVITE) {
                    float score;
                    if (Math.abs(dy - dx) == 1) score = 1; // vertical or horizontal
                    else                        score = 0.5f; // diagonal

                    encerclement += score;
                } else if (y == 4 && x == 4) { // center
                    if (Math.abs(dy - dx) == 1) encerclement += 1;
                }
            }
        }

        if (encerclement == 0) return 10;
        if (encerclement <= 1) return 8;
        return -(encerclement * ((float) 10 / 6));
    }


    /**
     * Swedish early game strategy : until 3-4 enemy pieces get taken, the inner four pawns should stay locked in place to protect the king
     * And the king should stay in the center
     * We only penalize swedish if they have missing inner pawns during that stage, no bonus for extra security
     */
    public static float evaluateEarlyKingSecurity(byte[] board, int ply, byte[] kingPosStack, byte[] moscoviteCountStack, byte[] soldierCountStack) {
        if (moscoviteCountStack[ply] < 12) return 0;

        float score = 0f;
        int kingPos = kingPosStack[ply];
        int kingY = kingPos / 9;
        int kingX = kingPos % 9;

        if (kingPos != 40) score -= 4f;
        for (int d = 0; d < 4; d++) {
            int y = kingY + FastBoard.DY_VALS[d];
            int x = kingX + FastBoard.DX_VALS[d];
            if (board[y*9+x] != SOLDIER) score -= 1.5f;
        }

        return score;
    }


    /**
     * Swedish middlegame strategy 1 : always ensure that 2 orthogonal sides of the king are occupied by soldiers
     * And the king should stay in the center
     * this ensures that the king cannot be taken and is a nice safe strategy as we work towards clearing a way to a board edge
     *
     * Only penalizes swedish when the king is not protected enough
     */
    public static float evaluateMiddlegameKingSecurity(byte[] board, int ply, byte[] kingPosStack, byte[] moscoviteCountStack, byte[] soldierCountStack) {
        if (moscoviteCountStack[ply] >= 12 || moscoviteCountStack[ply] < 8) return 0;

        float score = 0f;

        int kingPos = kingPosStack[ply];
        int kingY = kingPos / 9;
        int kingX = kingPos % 9;

        int surroundMask = 0;
        for (int d = 0; d < 4; d++) {
            int y = kingY + FastBoard.DY_VALS[d];
            int x = kingX + FastBoard.DX_VALS[d];
            if (board[y*9+x] == SOLDIER) surroundMask |= 1 << d;
        }

        // we use a surround mask to ensure that surrounding soldiers are placed orthogonally
        if ( ! ((surroundMask & 0b1010) == 0b1010) || ((surroundMask & 0b0101) == 0b0101)) {
            score = -5f;
        }
        if (kingPos != 40) score -= 5f;

        return score;
    }


    public static byte countPieces(byte[] board, int pieceType) {
        byte nb = 0;
        for (int i = 0; i < 81; i++) if (board[i] == pieceType) nb++;
        return nb;
    }

    public static float evaluateBoardControl(byte[] board) {
        float score = 0;
        for (int i = 2; i <= 6; i++) {
            for (int j = 2; j <= 6; j++) {
                if (i == 4 && j == 4) continue;
                int piece = board[i*9+j];
                if (piece == SOLDIER) {
                    score += 2;
                } else if (piece == MOSCOVITE) {
                    score -= 1;
                }
            }
        }
        return score / 1.6f;
    }


    /**
     * Counts the number of moscovites present in the same 5x5 outer region as the king
     */
    public static int countMoscovitesIn5x5Region(byte[] board, int ply, byte[] kingPosStack) {
        int kingPos = kingPosStack[ply];
        int kingY = kingPos / 9;
        int kingX = kingPos % 9;

        int regionStartX = (kingX <= 4) ? 0 : 4;
        int regionStartY = (kingY <= 4) ? 0 : 4;

        int nbMoscovites = 0;
        for (int y = regionStartY; y < regionStartY + 5; y++) {
            for (int x = regionStartX; x < regionStartX + 5; x++) {
                if (board[y*9+x] == MOSCOVITE) {
                    nbMoscovites++;
                }
            }
        }

        return nbMoscovites;
    }


    public static boolean kingCanEscapeIn1(byte[] board, int kingPos, int ruleSet) {
        for (int d = 0; d < 4; d++) {
            int dy = FastBoard.DY_VALS[d];
            int dx = FastBoard.DX_VALS[d];
            int y = kingPos / 9 + dy;
            int x = kingPos % 9 + dx;

            boolean isOpenWay = true;
            for (int i = 0; i < 9; i++) {
                if (y < 0 || y > 8 || x < 0 || x > 8) break;
                if (board[y*9+x] != EMPTY ||
                        (RuleSets.isAshtonRules(ruleSet) && RuleSets.isCampSquare(y*9+x))) {
                    isOpenWay = false;
                    break;
                }
            }
            if (isOpenWay) return true;
        }
        return false;
    }
}
