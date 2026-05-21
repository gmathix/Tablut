package control.algos;

import model.Move;
import model.Pawn;
import model.TablutBoard;

import java.util.ArrayList;
import java.util.List;


/**
 * Convenience board wrapper with minimal information in memory compared to TablutBoard
 */
public class Board {
    public static final int EMPTY     = 0;
    public static final int MOSCOVITE = 1;
    public static final int SOLDIER   = 2;
    public static final int KING      = 3;


    // utils for clockwise rotation
    public static final int[] DY_VALS = {-1, 0, 1, 0};
    public static final int[] DX_VALS = {0, 1, 0, -1};


    public int[][] board;
    public int kingX;
    public int kingY;


    public Board(TablutBoard tablutBoard) {
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

    public Board(Board board) {
        this.board = new int[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                this.board[i][j] = board.board[i][j];
            }
        }
        this.kingY = board.kingY;
        this.kingX = board.kingX;
    }

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

        int[] dy_vals = {-1, 0, 1, 0};
        int[] dx_vals = {0, 1, 0, -1};

        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (((turn == 0 && isGreen(board[y][x])) || (turn == 1 && isMoscovite(board[y][x])))) {
                    for (int d = 0; d < 4; d++) {
                        int currY = y;
                        int currX = x;
                        for (int i = 0; i < 9; i++) {
                            currY += dy_vals[d];
                            currX += dx_vals[d];
                            if (currY < 0 || currY > 8 || currX < 0 || currX > 8) break;
                            if (currY == 4 && currX == 4 && !isKing(board[y][x])) break;
                            if (!isEmpty(board[currY][currX])) break;
                            legalMoves.add(new Move(x, y, currX, currY));
                        }
                    }
                }
            }
        }

        return legalMoves;
    }


    public void makeMove(Move move) {
        // assume the move is valid

        int dstY = move.dstY();
        int dstX = move.dstX();
        int srcY = move.srcY();
        int srcX = move.srcX();

        int selPiece = board[srcY][srcX];
        board[dstY][dstX] = selPiece;
        board[srcY][srcX] = EMPTY;

        
        if (isKing(board[dstY][dstX])) {
            kingY = dstY;
            kingX = dstX;
        }


        // check capture
        int horizontalDirection = 0;
        int verticalDirection = 0;

        if (srcX - dstX != 0)
            horizontalDirection = dstX - srcX > 0 ? 1 : -1; // 1 for right, -1 for left
        if (srcY - dstY != 0)
            verticalDirection = dstY - srcY > 0 ? 1 : -1;   // 1 for down, -1 for up

        int[] dy_vals = {-1, 0, 1, 0};
        int[] dx_vals = {0, -1, 0, 1};

        for (int i = 0; i < 4; i++) {
            int dy = dy_vals[i];
            int dx = dx_vals[i];

            // do not check the squares on the path the pawn came from
            if (dx == -horizontalDirection && horizontalDirection != 0) continue;
            if (dy == -verticalDirection && verticalDirection != 0) continue;

            // check bounds for pawn 2 squares away
            if (dstY + 2*dy < 0 || dstY + 2*dy >= 9) continue;
            if (dstX + 2*dx < 0 || dstX + 2*dx >= 9) continue;

            int n = board[dstY + dy][dstX + dx];
            int n2 = board[dstY + 2*dy][dstX + 2*dx];

            boolean centerCapturing = dstY + 2 * dy == 4 && dstX + 2 * dx == 4;
            if (isMoscovite(selPiece)) {
                if ((isSoldier(n) && (isMoscovite(n2) || centerCapturing))) {
                    board[dstY + dy][dstX + dx] = EMPTY;
                }
            } else {
                // either the piece 2 squares apart is a green one or it is the center (which is hostile too for captures)
                if (isMoscovite(n) && (isGreen(n2) || centerCapturing)) {
                    board[dstY + dy][dstX + dx] = EMPTY;
                }
            }
        }
        
        
    }


    /**
     * @return MAX_VALUE for green win, MIN_VALUE for yellow win
     */
    public double checkWin() {


        // check king position on edges
        if (kingX == 0 || kingX == 8 || kingY == 0 || kingY == 8) {
                return (double) Integer.MAX_VALUE;
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
