package control.algos;

import model.Capture;
import model.Pawn;
import model.TablutBoard;

import java.util.ArrayList;
import java.util.List;

public class RecurMove {
    private static final String[] rows = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final String[] cols = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I"};


    private RecurBoard board;
    private int srcX, srcY, dstX, dstY;
    private int piece;

//    private List<Capture> captures;

    private short captures[];

    public RecurMove(RecurBoard board, int srcX, int srcY, int dstX, int dstY) {
        this.board = board;
        this.srcX = srcX;
        this.srcY = srcY;
        this.dstX = dstX;
        this.dstY = dstY;

        piece = board.getBoard()[srcY][srcX];

        captures = new short[3];

//        captures = new ArrayList<>();
//        List<Integer> caps = board.checkCaptures(this);
//        for (int cap : caps) {
//            captures.add(new Capture(cap % 9, cap / 9, board.getBoard()[cap/9][cap%9]));
//        }
    }

    /**
     * this previously was a Record, so we keep the names like this for compatibility
     */
    public int srcX() { return srcX; }
    public int srcY() { return srcY; }
    public int dstX() { return dstX; }
    public int dstY() { return dstY; }
    public short[] getCaptures() { return captures; }




    public static RecurMove fromString(String move) {
        move = move.toUpperCase();
        return new RecurMove(
                null,
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