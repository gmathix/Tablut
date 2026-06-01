package control;

import model.Move;
import model.RuleSets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MoveHistory {


    private String swedishPlayer;
    private String moscovitePlayer;

    private int startingSide;       // 0 for swedish, 1 for moscovite
    private int winningSide;

    private int ruleSet;

    private List<Move> moves;


    public MoveHistory(String swedishPlayer, String moscovitePlayer, int startingSide, int ruleSet) {
        moves = new ArrayList<>();

        this.swedishPlayer   = swedishPlayer;
        this.moscovitePlayer = moscovitePlayer;
        this.startingSide    = startingSide;
        this.ruleSet         = ruleSet;

        this.winningSide = -1;
    }

    public static MoveHistory fromGameFile(String gameFile) throws IOException {
        MoveHistory moveHistory = new MoveHistory(null, null, 0, 0);
        moveHistory.parseGameFile(gameFile);

        return moveHistory;
    }


    // SETTERS
    public void setSwedishPlayer(String swedishPlayer) { this.swedishPlayer = swedishPlayer; }
    public void setMoscovitePlayer(String moscovitePlayer) { this.moscovitePlayer = moscovitePlayer; }
    public void setStartingSide(int startingSide) { this.startingSide = startingSide; }
    public void setWinningSide(int winningSide) { this.winningSide = winningSide; }
    public void setRuleSet(int ruleSet) { this.ruleSet = ruleSet; }
    public void setMoves(List<Move> moves) { this.moves = moves; }


    // GETTERS
    public String getSwedishPlayer() { return swedishPlayer; }
    public String getMoscovitePlayer() { return moscovitePlayer; }
    public int getStartingSide() { return startingSide; }
    public int getWinningSide() { return winningSide; }
    public int getRuleSet() { return ruleSet; }
    public List<Move> getMoves() { return moves; }




    public void addMove(Move move) {
        moves.add(move);
    }
    public void undoLastMove() {
        moves.removeLast();
    }
    public void undoLastMoves(int n) {
        for (int i = 0; i < n; n++) moves.removeLast();
    }



    /**
     * Builds a pgn-style text that represents the game
     */
    public String buildGameString() {
        StringBuilder sb = new StringBuilder();

        sb  .append(String.format("[SwedishPlayer \"%s\"]\n", swedishPlayer))
            .append(String.format("[MoscovitePlayer \"%s\"]\n", moscovitePlayer))
            .append(String.format("[StartingSide \"%d\"]\n", startingSide))
            .append(String.format("[Ruleset \"%d\"]\n", ruleSet))
            .append(String.format("[Result \"%s\"]\n", winningSide == 0 ? "1-0" : "0-1"))
            .append("\n");


        int counter = 1;
        int moveNumber = 1;
        for (Move move : moves) {
            if (counter % 2 == 1) {
                sb.append(String.format("%d. %s ", moveNumber, move.toString()));
            } else {
                sb.append(String.format("%s\n", move.toString()));
                moveNumber++;
            }
            counter++;
        }

        sb.append(String.format("%s\n", winningSide == 0 ? "1-0" : "0-1"));


        return sb.toString();
    }


    public void parseGameFile(String filePath) throws IOException {
        String content = Files.readString(Path.of(filePath));

        String[] lines = content.split("\n");


        // default values in case they are not found in the header
        swedishPlayer = "Player 1";
        moscovitePlayer = "Player 2";
        startingSide = 0;
        ruleSet = 1;
        winningSide = 0;


        for (String line : lines) {
            if (line.startsWith("[") && line.endsWith("]")) { // header
                String[] info = line.substring(1, line.length()-1).split("\"");
                String param = info[0].strip().toLowerCase();
                String value = info[1];
                if (param.equals("swedishplayer")) {
                    swedishPlayer = value;
                } else if (param.equals("moscoviteplayer")) {
                    moscovitePlayer = value;
                } else if (param.equals("startingside")) {
                    startingSide = Integer.parseInt(value);
                } else if (param.equals("ruleset")) {
                    ruleSet = Integer.parseInt(value);
                } else if (param.equals("result")) {
                    winningSide = value.equals("1-0") ? 0 : 1;
                } else {
                    System.err.printf("Warning : unknown param found in the game file header. ignoring.\n");
                }
            } else { // moves or result
                if (line.isBlank()) continue;

                String[] movesLine = line.split(" ");

                if (movesLine.length == 3) {
                    moves.add(Move.fromString(movesLine[1]));
                    moves.add(Move.fromString(movesLine[2]));
                } else if (movesLine.length == 1) {
                    winningSide = movesLine[0].equals("1-0") ? 0 : 1;
                }
            }
        }
    }
}
