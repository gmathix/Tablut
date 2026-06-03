package control.algos;

import javafx.css.Rule;
import model.Pawn;
import model.RuleSets;
import model.TablutBoard;

import java.util.Arrays;
import java.util.List;

public class FastBoard {
    public static final float VIRTUAL_INF = Evaluation.VIRTUAL_INF;


    public static final byte EMPTY     = 0;
    public static final byte MOSCOVITE = Pawn.PAWN_MOSCOVITE;
    public static final byte SWEDISH   = Pawn.PAWN_SOLDIER;
    public static final byte KING      = Pawn.PAWN_KING;

    public static int[] pieceTypes = new int[]{EMPTY, MOSCOVITE, SWEDISH, KING};



    public static final int[] DY_VALS = new int[]{-1, 0, 1, 0};
    public static final int[] DX_VALS = new int[]{0, 1, 0, -1};

    public static int[] killerMoves = new int[2];
    public static int[] captures    = new int[NegamaxSearchFast.NB_POSSIBLE_MOVES];
    public static int[] kingMoves   = new int[NegamaxSearchFast.NB_POSSIBLE_MOVES];
    public static int[] otherMoves  = new int[NegamaxSearchFast.NB_POSSIBLE_MOVES];


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
                            if (currY == 4 && currX == 4) continue; // skip throne

                            int move = (i*9 + j) | ((currY*9 + currX) << 7);

                            if (move == killerMovesStack[ply][0]) {
                                killerMoves[nbKillerMoves] = move;
                                nbKillerMoves++;
                            } else if (move == killerMovesStack[ply][1]) {
                                killerMoves[nbKillerMoves] = move;
                                nbKillerMoves++;
                            } else if (isCapture(board, move, ply)) {
                                captures[nbCaptures] = move;
                                nbCaptures++;
                            } else if (isKing(board[i * 9 + j])) {
                                kingMoves[nbKingMoves] = move;
                                nbKingMoves++;
                            } else {
                                otherMoves[nbOtherMoves] = move;
                                nbOtherMoves++;
                            }
                            nbMoves++;
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
            movesStack[ply][i + nbTTBestMove] = killerMoves[i];
        }
        for (int i = 0; i < nbCaptures; i++) {
            movesStack[ply][i + nbTTBestMove + nbKillerMoves] = captures[i];
        }
        for (int i = 0; i < nbKingMoves; i++) {
            movesStack[ply][i + nbTTBestMove + nbKillerMoves + nbCaptures] = kingMoves[i];
        }
        for (int i = 0; i < nbOtherMoves; i++) {
            movesStack[ply][i + nbTTBestMove + nbKillerMoves + nbCaptures + nbKingMoves] = otherMoves[i];
        }

        moveCountStack[ply] = nbMoves;
    }

    public static boolean isCapture(byte[] board, int move, int ply) {
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

            byte n = board[(dstY + dy) * 9 + (dstX + dx)];
            byte n2 = board[(dstY + 2*dy) * 9 + (dstX + 2*dx)];
            if ((isMoscovite(selPiece) && isSwedish(n) && isMoscovite(n2) && !isKing(n)) ||
                    (isSwedish(selPiece) && isMoscovite(n) && isSwedish(n2))) {
                return true;
            }
        }

        return false;
    }

    public static void checkCaptures(byte[] board, int move, int ply, byte[] captureCountStack, short[][] captureStack) {
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

            byte n = board[(dstY + dy) * 9 + (dstX + dx)];
            byte n2 = board[(dstY + 2*dy) * 9 + (dstX + 2*dx)];
            if ((isMoscovite(selPiece) && isSwedish(n) && isMoscovite(n2) && !isKing(n)) ||
                (isSwedish(selPiece) && isMoscovite(n) && isSwedish(n2))) {
                short coord = (short) ((dstY + dy) * 9 + (dstX + dx));
                captureStack[ply][nbCaptures] = (short) ((coord << 3) | n);
                nbCaptures++;
            }
        }

        captureCountStack[ply] = nbCaptures;
    }


    public static void makeMove(byte[] board, int move, int ply, byte[] captureCountStack, short[][] captureStack,
                                byte[] materialDiffStack, byte[] kingPosStack, long[][] zobrist, long[] zobristKey,
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

        materialDiffStack[ply+1] = materialDiffStack[ply];
        for (int i = 0; i < captureCountStack[ply]; i++) {
            int capCoord = captureStack[ply][i] >> 3;

            // relative to swedish side : +2 for moscovite loss, -4 for swedish loss
            if (isMoscovite(board[capCoord])) materialDiffStack[ply+1] += 2;
            else materialDiffStack[ply+1] -= 4;

            zobristKey[0] ^= zobrist[board[capCoord]][capCoord];
            board[capCoord] = EMPTY;
        }

        zobristKey[0] ^= sideToMove;
    }

    public static void undoMove(byte[] board, int move, int ply, byte[] captureCountStack, short[][] captureStack,
                                byte[] materialDiffStack, byte[] kingPosStack, long[][] zobrist, long[] zobristKey,
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
        for (int d = 0; d < 4; d++ ) {
            int y = kingY + DY_VALS[d];
            int x = kingX + DX_VALS[d];
            if (y < 0 || y > 8 || x < 0 || x > 8) continue;
            if (isMoscovite(board[y*9+x]) || (y == 4 && x == 4)) {
                nbSurrounding++;
            }
        }
        if (nbSurrounding == 4) {
            return -VIRTUAL_INF;
        }

        return 0;
    }
}
