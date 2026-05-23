package control.algos;

import model.Move;
import model.RuleSets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

public class NegaMonteCarloUnitTest {

    private int savedRuleset;

    @BeforeEach
    public void setUp() {
        savedRuleset = RuleSets.currentRuleset;
        RuleSets.currentRuleset = RuleSets.RULESET_NORMAL;
    }

    @AfterEach
    public void tearDown() {
        RuleSets.currentRuleset = savedRuleset;
    }

    // --- Tests findBestMove ---

    @Test
    void testFindBestMoveBoardVideRetourneNull() {
        // Define : aucune pièce verte
        NegaMonteCarlo nmc = new NegaMonteCarlo(0);
        Board b = TestBoardFactory.empty();
        // When
        Move result = nmc.findBestMove(b, 0);
        // Then
        Assertions.assertNull(result);
    }

    @Test
    void testFindBestMoveAvecUnSoldatRetourneCoup() {
        // Define : un soldat, niveau 0
        NegaMonteCarlo nmc = new NegaMonteCarlo(0);
        Board b = TestBoardFactory.empty();
        b.board[0][0] = Board.SOLDIER;
        b.kingY = 8;
        b.kingX = 8;
        // When
        Move result = nmc.findBestMove(b, 0);
        // Then
        Assertions.assertNotNull(result);
    }

    @Test
    void testFindBestMoveMoscoviteRetourneCoup() {
        // Define : une moscovite, tour jaune
        NegaMonteCarlo nmc = new NegaMonteCarlo(0);
        Board b = TestBoardFactory.empty();
        b.board[3][3] = Board.MOSCOVITE;
        b.kingY = 8;
        b.kingX = 8;
        // When
        Move result = nmc.findBestMove(b, 1);
        // Then
        Assertions.assertNotNull(result);
    }

    // --- Test constante C ---

    @Test
    void testConstanteCEstPositive() {
        Assertions.assertTrue(NegaMonteCarlo.C > 0);
    }
}
