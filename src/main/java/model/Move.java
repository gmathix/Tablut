package model;

public record Move(int srcX, int srcY, int dstX, int dstY) {
    private static final String[] rows = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final String[] cols = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I"};

    public String toString() {
        return cols[srcX] + rows[srcY] + cols[dstX] + rows[dstY];
    }
}