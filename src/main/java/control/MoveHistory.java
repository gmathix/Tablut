package control;

import model.Move;

import java.util.ArrayList;
import java.util.List;



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
}
