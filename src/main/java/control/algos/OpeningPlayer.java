package control.algos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OpeningPlayer {
    private static final String openingBook =
            """
                    B5B3 D5D6 H5H7 E6F6 E2G2 C5C8 A4C4 E7A7
                    B5B7 D5D4 H5H3 E4F4 E8G8 C5C2 A6C6 E3A3
                    E2C2 E4F4 E8G8 F5F6 B5B7 E3H3 D1D3 G5G1
                    E2G2 E4D4 E8C8 D5D6 H5H7 E3B3 F1F3 C5C1
                    E8C8 E6F6 E2G2 F5F4 B5B3 E7H7 D9D7 G5G9
                    E8G8 E6D6 E2C2 D5D4 H5H3 E7B7 F9F7 C5C9
                    H5H3 F5F6 B5B7 E6D6 E2C2 G5G8 I4G4 E7I7
                    H5H7 F5F4 B5B3 E4D4 E8C8 G5G2 I6G6 E3I3
                    B5B3 E6H6 A6G6 E5E6 B3B6 E7G7
                    B5B7 E4H4 A4G4 E5E4 B7B4 E3G3
                    E2C2 F5F8 F1F7 E5F5 C2F2 G5G7
                    E2G2 D5D8 D1D7 E5D5 G2D2 C5C7
                    E8C8 F5F2 F9F3 E5F5 C8F8 G5G3
                    E8G8 D5D2 D9D3 E5D5 G8D8 C5C3
                    H5H3 E6B6 I6C6 E5E6 H3H6 E7C7
                    H5H7 E4B4 I4C4 E5E4 H7H4 E3C3
                    A4B4
                    A6B6
                    D1D2
                    D9D8
                    F1F2
                    F9F8
                    I4H4
                    I6H6
                    A4A3
                    A6A7
                    D1C1
                    D9C9
                    F1G1
                    F9G9
                    I4I3
                    I6I7
                    """;


    private static int openingMoveToInt(String move) {

        String src = move.substring(0, 2);
        String dst = move.substring(2, 4);

        int rowSrc = src.charAt(1) - '1';
        int colSrc = src.charAt(0) - 'A';
        int rowDst = dst.charAt(1) - '1';
        int colDst = dst.charAt(0) - 'A';

        return(rowSrc*9 + colSrc) | ((rowDst*9 + colDst) << 7);
    }

    /**
     * returns -1 if no continuation with current opening was found
     */
    public static int makeOpeningMove(String moveSequence) {
        String[] openingBookContent = openingBook.split("\n");

        List<String> candidates = new ArrayList<>();

        for (String line : openingBookContent) {
            if (moveSequence.isEmpty()) {
                candidates.add(line.split(" ")[0].trim());
            } else if (line.startsWith(moveSequence)) {
                String[] words = line.split(moveSequence);
                if (words.length <= 1) continue;
                candidates.add(words[1].trim());
            }
        }

        if (candidates.isEmpty()) return -1;

        boolean noContinuation = false;
        for (String candidate : candidates) {
            if (candidate.isEmpty()) {
                noContinuation = true;
                break;
            }
        }
        if (noContinuation) return -1;

        int index = (int) (Math.random() * (candidates.size()-1));
        String move = candidates.get(index).split(" ")[0];

        return openingMoveToInt(move);
    }
}
