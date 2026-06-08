package control.algos;

import model.Pawn;
import model.RuleSets;
import model.TablutBoard;

import java.util.List;

public class FastBoard {
    public static final float VIRTUAL_INF = FastEvaluation.VIRTUAL_INF;


    public static final byte EMPTY     = 0;
    public static final byte MOSCOVITE = Pawn.PAWN_MOSCOVITE;
    public static final byte SWEDISH   = Pawn.PAWN_SOLDIER;
    public static final byte KING      = Pawn.PAWN_KING;

    public static int[] pieceTypes = new int[]{EMPTY, MOSCOVITE, SWEDISH, KING};



    public static final int[] DY_VALS = new int[]{-1, 0, 1, 0};
    public static final int[] DX_VALS = new int[]{0, 1, 0, -1};

    public static int[][] killerMovesGenStack = new int[Negamax.MAX_DEPTH][2];
    public static int[][] capturesGenStack    = new int[Negamax.MAX_DEPTH][Negamax.NB_POSSIBLE_MOVES];
    public static int[][] kingMovesGenStack   = new int[Negamax.MAX_DEPTH][Negamax.NB_POSSIBLE_MOVES];
    public static int[][] otherMovesGenStack  = new int[Negamax.MAX_DEPTH][Negamax.NB_POSSIBLE_MOVES];


    public static final List<Integer> constrainedKingSquares = List.of(
            3, 4, 5, // D1, E1, F1
            27, 36, 45, // A4, A5, A6
            35, 44, 53, // I4, I5, I6
            75, 76, 77  // D9, E9, F9
    );

    // list of corner squares (flat order index)
    public static final List<Integer> cornerSquares = List.of(
            0, // A1
            8, // I1
            72, // A9,
            80 //I9
    );



    public static byte[] fromTablutBoard(TablutBoard tablutBoard) {
        byte[] board = new byte[81];

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (tablutBoard.getElement(i, j) instanceof Pawn p) {
                    board[i*9 + j] = (byte) p.getColor();
                } else {
                    board[i*9 + j] = EMPTY;
                }
            }
        }

        return board;
    }

    public static byte getKingPos(byte[] board) {
        byte pos = -1;
        for (byte i = 0; i < 81; i++) {
            if (board[i] == KING) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    private static boolean isSwedish(byte piece) { return piece == SWEDISH || piece == KING; }
    private static boolean isMoscovite(byte piece) { return piece == MOSCOVITE; }
    private static boolean isKing(byte piece) { return piece == KING; }
    private static boolean isEmpty(byte piece) { return piece == EMPTY; }



    public static void generateMoves(byte[] board, int turn, int ply, int[] moveCountStack, int[][] movesStack,
                                     int[][] killerMovesStack, int bestTTMove, boolean hasTTBestMove, int ruleSet) {
        int[] moveList = movesStack[ply];
        moveCountStack[ply] = 0;

        int nbMoves = 0;

        int nbTTBestMove  = hasTTBestMove ? 1 : 0;
        int nbKillerMoves = 0;
        int nbCaptures    = 0;
        int nbKingMoves   = 0;
        int nbOtherMoves  = 0;

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (((turn == 0 && isSwedish(board[i*9+j])) || (turn == 1 && isMoscovite(board[i*9+j])))) {
                    int maxDistance = 8;
                    if (isKing(board[i*9+j]) && RuleSets.isConstrainedKingMoves(ruleSet)) {
                        maxDistance = 4;
                    }
                    for (int d = 0; d < 4; d++) {
                        int currY = i;
                        int currX = j;
                        for (int n = 0; n <= maxDistance; n++) {
                            currY += DY_VALS[d];
                            currX += DX_VALS[d];

                            if (currY < 0 || currY > 8 || currX < 0 || currX > 8) break;
                            if (!isEmpty(board[currY*9+currX])) break; // stop when path is occupied
                            if (currY == 4 && currX == 4) break; // throne blocks

                            if (RuleSets.isAshtonRules(ruleSet)) {
                                if (RuleSets.isCampSquare(currY*9+currX)) {
                                    if (!(board[i*9+j] == MOSCOVITE && RuleSets.isCampSquare(i*9+j))) {
                                        break;
                                    }
                                }
                            }


                            int move = (i*9 + j) | ((currY*9 + currX) << 7);

                            nbMoves++;

                            if (move == bestTTMove) continue; // will be added after, avoid duplication in otherMoves
                            if (move == killerMovesStack[ply][0]) {
                                killerMovesGenStack[ply][nbKillerMoves] = move;
                                nbKillerMoves++;
                            } else if (move == killerMovesStack[ply][1]) {
                                killerMovesGenStack[ply][nbKillerMoves] = move;
                                nbKillerMoves++;
                            } else if (isCapture(board, move, ply, ruleSet)) {
                                capturesGenStack[ply][nbCaptures] = move;
                                nbCaptures++;
                            } else if (isKing(board[i * 9 + j])) {
                                kingMovesGenStack[ply][nbKingMoves] = move;
                                nbKingMoves++;
                            } else {
                                otherMovesGenStack[ply][nbOtherMoves] = move;
                                nbOtherMoves++;
                            }
                        }
                    }
                }
            }
        }

//        System.arraycopy(killerMoves, 0, moveList, 0, nbKillerMoves);
//        System.arraycopy(captures, 0, moveList, nbKillerMoves, nbCaptures);
//        System.arraycopy(kingMoves, 0, moveList, nbKillerMoves + nbCaptures, nbKingMoves);
//        System.arraycopy(otherMoves, 0, moveList, nbKillerMoves + nbCaptures + nbKingMoves, nbOtherMoves);
        if (hasTTBestMove) {
            movesStack[ply][0] = bestTTMove;
        }
        for (int i = 0; i < nbKillerMoves; i++) {
            movesStack[ply][i + nbTTBestMove] = killerMovesGenStack[ply][i];
        }
        for (int i = 0; i < nbCaptures; i++) {
            movesStack[ply][i + nbTTBestMove + nbKillerMoves] = capturesGenStack[ply][i];
        }
        for (int i = 0; i < nbKingMoves; i++) {
            movesStack[ply][i + nbTTBestMove + nbKillerMoves + nbCaptures] = kingMovesGenStack[ply][i];
        }
        for (int i = 0; i < nbOtherMoves; i++) {
            movesStack[ply][i + nbTTBestMove + nbKillerMoves + nbCaptures + nbKingMoves] = otherMovesGenStack[ply][i];
        }

        moveCountStack[ply] = nbMoves;
    }

    public static boolean isCapture(byte[] board, int move, int ply, int ruleSet) {
        int src = move & 0x7F;
        int dst = (move >> 7) & 0x7F;

        int dstY = dst / 9;
        int dstX = dst % 9;

        byte selPiece = board[src];

        int capIndex = 0;
        for (int d = 0; d < 4; d++) {
            int dy = DY_VALS[d];
            int dx = DX_VALS[d];

            if (dstY + 2*dy < 0 || dstY + 2*dy > 8) continue;
            if (dstX + 2*dx < 0 || dstX + 2*dx > 8) continue;

            int nCoord = (dstY + dy) * 9 + (dstX + dx);
            int n2Coord = (dstY + 2*dy) * 9 + (dstX + 2*dx);

            byte n = board[nCoord];
            byte n2 = board[n2Coord];

            boolean isNEnemy = (isMoscovite(selPiece) && isSwedish(n) && !isKing(n)) ||
                                (isSwedish(selPiece) && isMoscovite(n));
            boolean isN2Ally = (isMoscovite(selPiece) && isMoscovite(n2)) ||
                                (isSwedish(selPiece) && isSwedish(n2)) ||
                                (n2Coord == 40) ||
                                (RuleSets.isAshtonRules(ruleSet) && RuleSets.isCampSquare(n2Coord));

            if (isNEnemy && isN2Ally) {
                return true;
            }
        }

        return false;
    }

    public static void checkCaptures(byte[] board, int move, int ply, byte[] captureCountStack, short[][] captureStack, int ruleSet) {
        byte nbCaptures = 0;
        captureCountStack[ply] = 0;

        int src = move & 0x7F;
        int dst = (move >> 7) & 0x7F;

        int dstY = dst / 9;
        int dstX = dst % 9;

        byte selPiece = board[src];

        int capIndex = 0;
        for (int d = 0; d < 4; d++) {
            int dy = DY_VALS[d];
            int dx = DX_VALS[d];

            if (dstY + 2*dy < 0 || dstY + 2*dy > 8) continue;
            if (dstX + 2*dx < 0 || dstX + 2*dx > 8) continue;

            int nCoord = (dstY + dy) * 9 + (dstX + dx);
            int n2Coord = (dstY + 2*dy) * 9 + (dstX + 2*dx);

            byte n = board[nCoord];
            byte n2 = board[n2Coord];

            boolean isNEnemey = (isMoscovite(selPiece) && isSwedish(n) && !isKing(n)) ||
                    (isSwedish(selPiece) && isMoscovite(n));
            boolean isN2Ally = (isMoscovite(selPiece) && isMoscovite(n2)) ||
                    (isSwedish(selPiece) && isSwedish(n2)) ||
                    (n2Coord == 40) ||
                    (RuleSets.isAshtonRules(ruleSet) && RuleSets.isCampSquare(n2Coord));

            if (isNEnemey && isN2Ally) {
                captureStack[ply][nbCaptures] = (short) ((nCoord << 3) | n);
                nbCaptures++;
            }
        }

        captureCountStack[ply] = nbCaptures;
    }


    public static void makeMove(byte[] board, int move, int ply, byte[] captureCountStack, short[][] captureStack,
                                byte[] soldierCountStack, byte[] moscoviteCountStack,
                                byte[] kingPosStack, long[][] zobrist, long[] zobristKey,
                                long sideToMove) {
        // update kingPosStack[ply+1], not kingPosStack[ply]

        int src = move & 0x7F;
        int dst = (move >> 7) & 0x7F;

        if (isKing(board[src])) {
            kingPosStack[ply+1] = (byte) dst;
        }
        board[dst] = board[src];
        board[src] = EMPTY;

        zobristKey[0] ^= zobrist[board[dst]][src];
        zobristKey[0] ^= zobrist[board[dst]][dst];

        for (int i = 0; i < captureCountStack[ply]; i++) {
            int capCoord = captureStack[ply][i] >> 3;

            if (isMoscovite(board[capCoord])) moscoviteCountStack[ply+1]--;
            else soldierCountStack[ply+1]--;

            zobristKey[0] ^= zobrist[board[capCoord]][capCoord];
            board[capCoord] = EMPTY;
        }

        zobristKey[0] ^= sideToMove;
    }

    public static void undoMove(byte[] board, int move, int ply, byte[] captureCountStack, short[][] captureStack,
                                byte[] kingPosStack, long[][] zobrist, long[] zobristKey,
                                long sideToMove) {
        int src = move & 0x7F;
        int dst = (move >> 7) & 0x7F;

        // no need to reset king pos here as it will already be reset in negamax

        board[src] = board[dst];
        board[dst] = EMPTY;

        zobristKey[0] ^= zobrist[board[src]][dst];
        zobristKey[0] ^= zobrist[board[src]][src];

        for (int i = 0; i < captureCountStack[ply]; i++) {
            int capCoord = captureStack[ply][i] >> 3;
            int capPiece = captureStack[ply][i] & 0x03;

            // no need to reset material diff too, done in negamax

            board[capCoord] = (byte) capPiece;

            zobristKey[0] ^= zobrist[board[capCoord]][capCoord];
        }

        zobristKey[0] ^= sideToMove;

        kingPosStack[ply+1]         = kingPosStack[ply];
    }

    public static float checkWin(byte[] board, int ply, byte[] kingPosStack, int ruleSet) {
        int kingPos = kingPosStack[ply];

        int kingY = kingPos / 9;
        int kingX = kingPos % 9;

        if (kingY == 0 || kingY == 8 || kingX == 0 || kingX == 8) {
            if (RuleSets.isCornerKingEscapes(ruleSet) || RuleSets.isConstrainedKingSquares(ruleSet)) {
                if (RuleSets.isCornerKingEscapes(ruleSet)) {
                    if (cornerSquares.contains(kingPos)) return VIRTUAL_INF;
                } else if (RuleSets.isConstrainedKingSquares(ruleSet)) {
                    if (!constrainedKingSquares.contains(kingPos)) return VIRTUAL_INF;
                }
            } else {
                return VIRTUAL_INF;
            }
        }

        int nbSurrounding = 0;
        int surroundMask = 0;

        boolean kingInCenter = kingY == 4 && kingX == 4;
        boolean hasCenterNeighbor = false;

        for (int d = 0; d < 4; d++ ) {
            int y = kingY + DY_VALS[d];
            int x = kingX + DX_VALS[d];
            if (y < 0 || y > 8 || x < 0 || x > 8) continue;

            if (y == 4 && x == 4) {
                hasCenterNeighbor = true;
                nbSurrounding++;
                surroundMask |= 1 << d;
            } else if (isMoscovite(board[y*9+x]) ||
                (RuleSets.isAshtonRules(ruleSet) && RuleSets.isCampSquare(y*9+x))) {

                nbSurrounding++;
                surroundMask |= 1 << d;
            }
        }

        if (RuleSets.isAshtonRules(ruleSet)) {
            if ((kingInCenter && nbSurrounding == 4) ||
                (hasCenterNeighbor && nbSurrounding == 4) ||
                (!kingInCenter && !hasCenterNeighbor && nbSurrounding >= 2 && ((surroundMask & 0b1010) == 0b1010 || (surroundMask & 0b0101) == 0b0101))) {

                return -VIRTUAL_INF;
            }
        } else if ((hasCenterNeighbor && nbSurrounding == 3) || (nbSurrounding == 4)) {
            return -VIRTUAL_INF;
        }

        return 0;
    }
}
