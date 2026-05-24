package control.algos;

import model.Move;
import model.Pawn;
import model.RuleSets;
import model.TablutBoard;

import java.util.ArrayList;
import java.util.List;


/**
 * Convenience board wrapper with minimal information in memory compared to TablutBoard
 */
public class RecurBoard {
    public static final int EMPTY     = 0;
    public static final int MOSCOVITE = 1;
    public static final int SOLDIER   = 2;
    public static final int KING      = 3;


    // utils for clockwise rotation
    public static final int[] DY_VALS = {-1, 0, 1, 0};
    public static final int[] DX_VALS = {0, 1, 0, -1};

    // list of square unreachable by the king in the RULESET_CONSTRAINED_KING_SQUARES rule (flat order index)
    public static final List<Integer> constrainedKingSquares = List.of(
            3, 4, 5, 13, // D1, E1, F1, E2
            27, 36, 45, 37, // A4, A5, A6, B5
            35, 44, 53, 43, // I4, I5, I6, H5
            75, 76, 77, 68  // D9, E9, F9, E8
    );

    // list of corner squares (flat order index)
    public static final List<Integer> cornerSquares = List.of(
            0, // A1
            8, // I1
            72, // A9,
            80 //I9
    );


    private int[][] board;
    private int kingX;
    private int kingY;


    // those values will have to be collected by the negamax search after every makeMove()
    // in order to pass them to undoMove() right after
    private int lastCaptureX;
    private int lastCaptureY;
    private int lastCapturePiece;


    public RecurBoard(TablutBoard tablutBoard) {
        board = new int[9][9];

        // make a smaller board (in memory) from the huge TablutBoard class
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (tablutBoard.getElement(i, j) instanceof Pawn p) {
                    board[i][j] = switch (p.getColor()) {
                        case 0 -> EMPTY;
                        case Pawn.PAWN_MOSCOVITE -> MOSCOVITE;
                        case Pawn.PAWN_SOLDIER -> SOLDIER;
                        case Pawn.PAWN_KING -> KING;
                        default -> EMPTY;
                    };
                    if (isKing(board[i][j])) {
                        kingY = i;
                        kingX = j;
                    }
                }
            }
        }
    }

    public RecurBoard(RecurBoard board) {
        this.board = new int[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                this.board[i][j] = board.board[i][j];
            }
        }
        this.kingY = board.kingY;
        this.kingX = board.kingX;
    }


    public int getKingX() { return kingX; }
    public int getKingY() { return kingY; }
    public int[][] getBoard() { return board; }
    public int getLastCaptureX() { return lastCaptureX; }
    public int getLastCaptureY() { return lastCaptureY; }
    public int getLastCapturePiece() { return lastCapturePiece; }


    public boolean isMoscovite(int piece) {
        return piece == MOSCOVITE;
    }
    public boolean isGreen(int piece) {
        return piece == SOLDIER || piece == KING;
    }
    public boolean isKing(int piece) {
        return piece == KING;
    }
    public boolean isSoldier(int piece) {
        return piece == SOLDIER;
    }
    public boolean isEmpty(int piece) {
        return piece == EMPTY;
    }

    public void print() {
        for (int i = 0; i < 9; i++) {
            for (int b = 0; b < 9; b++) {
                System.out.printf("--");
            }
            System.out.printf("-\n");

            for (int j = 0; j < 9; j++) {
                System.out.printf("|");
                System.out.printf(
                        switch (board[i][j]) {
                            case EMPTY -> " ";
                            case MOSCOVITE -> "M";
                            case SOLDIER -> "S";
                            case KING -> "K";
                            default -> " ";
                        }
                );
            }
            System.out.printf("|\n");
        }
        for (int b = 0; b < 9; b++) {
            System.out.printf("--");
        }
        System.out.printf("-\n");
    }


    public List<Move> getLegalMoves(int turn) {
        List<Move> legalMoves = new ArrayList<>();

        List<Move> captures = new ArrayList<>();
        List<Move> kingMoves = new ArrayList<>();
        List<Move> otherMoves = new ArrayList<>();

        Move currMove;

        int[] dy_vals = {-1, 0, 1, 0};
        int[] dx_vals = {0, 1, 0, -1};

        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (((turn == 0 && isGreen(board[y][x])) || (turn == 1 && isMoscovite(board[y][x])))) {
                    int maxDistance = 8;
                    if (isKing(board[y][x]) && RuleSets.isConstrainedKingMoves()) {
                        maxDistance = 4;
                    }
                    for (int d = 0; d < 4; d++) {
                        int currY = y;
                        int currX = x;
                        for (int i = 0; i <= maxDistance; i++) {
                            currY += dy_vals[d];
                            currX += dx_vals[d];
                            currMove = new Move(x, y, currX, currY);

                            if (currY < 0 || currY > 8 || currX < 0 || currX > 8) break; // prevent out of bounds
                            if (!isEmpty(board[currY][currX])) break; // stop when path is obstructed
                            if (currY == 4 && currX == 4) continue; // skip center square
                            if (RuleSets.isConstrainedKingSquares()) {
                                if (isKing(board[y][x]) && constrainedKingSquares.contains(currY * 9 + currX))
                                    continue; // skip constrained king squares
                            }

                            if (checkCapture(currMove) != -1) {
                                captures.add(currMove);
                            } else if (isKing(board[y][x])) {
                                kingMoves.add(currMove);
                            } else {
                                otherMoves.add(currMove);
                            }
                        }
                    }
                }
            }
        }

        // move ordering : captures first, then king moves, then other moves0
        // in order to cut the branching factor in negamax
        legalMoves.addAll(captures);
        legalMoves.addAll(kingMoves);
        legalMoves.addAll(otherMoves);

        return legalMoves;
    }

    // return the piece index in flat array
    public int checkCapture(Move move) {
        int dstY = move.dstY();
        int dstX = move.dstX();
        int srcY = move.srcY();
        int srcX = move.srcX();

        int selPiece = board[srcY][srcX];

        // check capture
        for (int i = 0; i < 4; i++) {
            int dy = DY_VALS[i];
            int dx = DX_VALS[i];

            // check bounds for pawn 2 squares away
            if (dstY + 2*dy < 0 || dstY + 2*dy >= 9) continue;
            if (dstX + 2*dx < 0 || dstX + 2*dx >= 9) continue;

            int n = board[dstY + dy][dstX + dx];
            int n2 = board[dstY + 2*dy][dstX + 2*dx];

            boolean centerCapturing = dstY + 2 * dy == 4 && dstX + 2 * dx == 4;
            if (isMoscovite(selPiece)) {
                if ((isSoldier(n) && (isMoscovite(n2) || centerCapturing))) {
                    return (dstY + dy) * 9 + (dstX + dx);
                }
            } else {
                if (isMoscovite(n) && (isGreen(n2) || centerCapturing)) {
                    return (dstY + dy) * 9 + (dstX + dx);
                }
            }
        }

        return -1;
    }

    public void makeMove(Move move) {
        // assume the move is valid

        int dstY = move.dstY();
        int dstX = move.dstX();
        int srcY = move.srcY();
        int srcX = move.srcX();

        int selPiece = board[srcY][srcX];


        int capture = checkCapture(move);
        if (capture != -1) {
            lastCapturePiece = board[capture/9][capture%9];
            lastCaptureY = capture / 9;
            lastCaptureX = capture % 9;

            board[capture/9][capture%9] = EMPTY;
        } else {
            lastCaptureY = -1;
            lastCaptureX = -1;
            lastCapturePiece = EMPTY;
        }

        board[dstY][dstX] = selPiece;
        board[srcY][srcX] = EMPTY;

        if (isKing(board[dstY][dstX])) {
            kingY = dstY;
            kingX = dstX;
        }
    }

    public void undoMove(Move move, int lastCaptureX, int lastCaptureY, int lastCapturePiece, int prevKingX, int prevKingY) {
        // assumes that this move corresponds to the last move played on this board

        board[move.srcY()][move.srcX()] = board[move.dstY()][move.dstX()];
        board[move.dstY()][move.dstX()] = EMPTY;

        this.kingX = prevKingX;
        this.kingY = prevKingY;

        if (lastCaptureX != -1 && lastCaptureY != -1){
            board[lastCaptureY][lastCaptureX] = lastCapturePiece;
        }
    }


    /**
     * @return MAX_VALUE for green win, MIN_VALUE for yellow win
     */
    public double checkWin() {


        // check king position on edges
        if (kingX == 0 || kingX == 8 || kingY == 0 || kingY == 8) {
            if (RuleSets.isCornerKingEscapes() || RuleSets.isConstrainedKingSquares()) {
                if (RuleSets.isCornerKingEscapes()) {
                    if (cornerSquares.contains(kingY * 9 + kingX)) {
                        return Integer.MAX_VALUE;
                    }
                } else if (RuleSets.isConstrainedKingSquares()) {
                    if (!constrainedKingSquares.contains(kingY * 9 + kingX)) {
                        return Integer.MAX_VALUE;
                    }
                }
            } else {
                return (double) Integer.MAX_VALUE;
            }
        }


        // check king emprisonment
        int nbSurrounding = 0;
        boolean hasCenterNeighbor = false;
        for (int i = 0; i < 4; i++) {
            int y = kingY + DY_VALS[i];
            int x = kingX + DX_VALS[i];
            if (y < 0 || y > 8 || x < 0 || x > 8) continue;
            if (y == 4 && x == 4) hasCenterNeighbor = true;
            if (isMoscovite(board[y][x])) {
                nbSurrounding++;
            }
        }
        if ((hasCenterNeighbor && nbSurrounding == 3) || nbSurrounding == 4) {
            return (double) Integer.MIN_VALUE;
        }


        return 0;
    }
}
