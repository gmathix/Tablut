package model;

public record Move(int srcX, int srcY, int dstX, int dstY) {
    private static final String[] rows = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final String[] cols = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I"};

    public static Move fromString(String move) {
        move = move.toUpperCase();
        return new Move(
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