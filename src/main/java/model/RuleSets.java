package model;


import java.util.List;

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




    public static final int RULESET_ASHTON_RULES                = 1 << 4;




    public record RuleOption(int bit, String description) {}

    public static List<RuleOption> ruleOptions = List.of(
            new RuleOption(RuleSets.RULESET_ASHTON_RULES,             "Ashton rules (official, tournament)"),
            new RuleOption(RuleSets.RULESET_CONSTRAINED_KING_SQUARES, "King cannot land on starting Moscovite squares"),
            new RuleOption(RuleSets.RULESET_CONSTRAINED_KING_MOVES,   "King cannot move more than 4 squares"),
            new RuleOption(RuleSets.RULESET_CORNER_KING_ESCAPES,      "King must reach a corner to win")
    );



    public static final List<Integer> constrainedKingSquares = List.of(
            3, 4, 5, // D1, E1, F1
            27, 36, 45, // A4, A5, A6
            35, 44, 53, // I4, I5, I6
            75, 76, 77  // D9, E9, F9
    );

    // list of corner squares (flat order index)
    public static final List<Integer> cornerSquares = List.of(
            0, // A1
            8, // I1
            72, // A9,
            80 //I9
    );


    public static final List<Integer> campsSquares = List.of(
            3, 4, 5, 13, // D1, E1, F1
            27, 36, 45, 37, // A4, A5, A6
            35, 44, 53, 43, // I4, I5, I6
            75, 76, 77, 84  // D9, E9, F9
    );



    public static boolean isNormal(int ruleSet) {
        return (ruleSet & RULESET_NORMAL) > 0;
    }
    public static boolean isConstrainedKingSquares(int ruleSet) {
        return (ruleSet & RULESET_CONSTRAINED_KING_SQUARES) > 0;
    }
    public static boolean isConstrainedKingMoves(int ruleSet) {
        return (ruleSet & RULESET_CONSTRAINED_KING_MOVES) > 0;
    }
    public static boolean isCornerKingEscapes(int ruleSet) {
        return (ruleSet & RULESET_CORNER_KING_ESCAPES) > 0;
    }
    public static boolean isAshtonRules(int ruleSet) { return (ruleSet & RULESET_ASHTON_RULES) > 0;}
}
