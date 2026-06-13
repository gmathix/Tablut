package model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;


public class RuleSetsTest {

    @Test
    void isCampSquare_knownCampSquares_returnsTrue() {
        assertTrue(RuleSets.isCampSquare(3));
        assertTrue(RuleSets.isCampSquare(4));
        assertTrue(RuleSets.isCampSquare(5));
        assertTrue(RuleSets.isCampSquare(13));
    }

    @Test
    void isCampSquare_centerSquare_returnsFalse() {
        assertFalse(RuleSets.isCampSquare(40));
    }

    @Test
    void isAshtonRules_withAshtonBit_returnsTrue() {
        int ruleset = RuleSets.RULESET_ASHTON_RULES;
        assertTrue(RuleSets.isAshtonRules(ruleset));
        assertFalse(RuleSets.isNormal(ruleset));
    }

    @Test
    void isConstrainedKingMoves_combinedRuleset() {
        int ruleset = RuleSets.RULESET_NORMAL | RuleSets.RULESET_CONSTRAINED_KING_MOVES;
        assertTrue(RuleSets.isConstrainedKingMoves(ruleset));
        assertTrue(RuleSets.isNormal(ruleset));
        assertFalse(RuleSets.isCornerKingEscapes(ruleset));
    }

    @Test
    void campSquareList_matchesBitmaks() {
        for (int sq : RuleSets.campSquaresList) {
            assertTrue(RuleSets.isCampSquare(sq));
        }
    }

}
