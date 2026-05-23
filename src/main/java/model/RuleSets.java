package model;


/**
 * List of modular game variants/rules, which can be added on top of each other
 */
public class RuleSets {
    /**
     * Normal rules :
     *    - all pieces move like chess rooks in whatever direction, can go to every square except
     *      occupied squares or the center square
     *    - pieces can capture an enemy piece by surrounding on 2 opposite sides with another ally piece
     *    - the king must reach an edge in order to win
     *    - the moscovites must surround the king with 4 pieces in order to win
     *       - they can win by surrounding it with 3 pieces if the king is next to the center square
     */
    public static final int RULESET_NORMAL                      = 1 << 0;


    /**
     * The king cannnot move to squares that are starting moscovite positions
     */
    public static final int RULESET_CONSTRAINED_KING_SQUARES = 1 << 1;


    /**
     * The king cannot move more than 4 squares in a single move
     */
    public static final int RULESET_CONSTRAINED_KING_MOVES      = 1 << 2;


    /**
     * The king has to reach a corner (A1, I1, A9, I9) in order to win
     */
    public static final int RULESET_CORNER_KING_ESCAPES         = 1 << 3;




    // generally not good to have a global variable since it's harder to debug
    // but here, we know that it will only be modified at the start of the program, then won't be reused
    // until the program is relaunched
    public static int currentRuleset = RULESET_NORMAL;




    public static boolean isNormal() {
        return (currentRuleset & RULESET_NORMAL) > 0;
    }
    public static boolean isConstrainedKingSquares() {
        return (currentRuleset & RULESET_CONSTRAINED_KING_SQUARES) > 0;
    }
    public static boolean isConstrainedKingMoves() {
        return (currentRuleset & RULESET_CONSTRAINED_KING_MOVES) > 0;
    }
    public static boolean isCornerKingEscapes() {
        return (currentRuleset & RULESET_CORNER_KING_ESCAPES) > 0;
    }
}
