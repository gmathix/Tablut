package model;

import java.util.ArrayList;
import java.util.List;

public class Move {
    private static final String[] rows = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final String[] cols = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I"};


    private TablutBoard board;
    private int srcX, srcY, dstX, dstY;
    private int piece;
    private List<Capture> captures;
    private List<Pawn> capturedPawns;

    public Move(TablutBoard board, int srcX, int srcY, int dstX, int dstY) {
        this.board = board;
        this.srcX = srcX;
        this.srcY = srcY;
        this.dstX = dstX;
        this.dstY = dstY;


        piece = ((Pawn)board.getElement(srcY, srcX)).getColor();

        captures = new ArrayList<>();
        capturedPawns = new ArrayList<>();
        // checkCaptures expects (colSrc, colDest, rowSrc, rowDest)
        List<Integer> caps = board.checkCaptures(piece == Pawn.PAWN_MOSCOVITE, srcX, dstX, srcY, dstY);
        for (int cap : caps) {
            Pawn capturedPawn = (Pawn) board.getElement(cap / 9, cap % 9);
            captures.add(new Capture(cap % 9, cap / 9, capturedPawn.getColor()));
            capturedPawns.add(capturedPawn);
        }
    }

    /**
     * this previously was a Record, so we keep the names like this for compatibility
     */
    public int srcX() { return srcX; }
    public int srcY() { return srcY; }
    public int dstX() { return dstX; }
    public int dstY() { return dstY; }
    public List<Capture> getCaptures() { return List.copyOf(captures); }
    public List<Pawn> getCapturedPawns() { return List.copyOf(capturedPawns); }




    public static Move fromString(TablutBoard board, String move) {
        move = move.toUpperCase();
        return new Move(
                board,
                move.charAt(0) - 'A',
                move.charAt(1) - '1',
                move.charAt(2) - 'A',
                move.charAt(3) - '1'
        );
    }

    public String toString() {
        return cols[srcX] + rows[srcY] + cols[dstX] + rows[dstY];
    }
}