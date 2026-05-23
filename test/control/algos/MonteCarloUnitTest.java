package control.algos;

import model.Move;
import model.RuleSets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

public class MonteCarloUnitTest {

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
        // Define : aucune pièce verte => pas de coup
        MonteCarlo mc = new MonteCarlo(0); // niveau 0 = 500ms max
        Board b = TestBoardFactory.empty();
        // When
        Move result = mc.findBestMove(b, 0);
        // Then
        Assertions.assertNull(result);
    }

    @Test
    void testFindBestMoveAvecUnSoldatRetourneCoup() {
        // Define : un soldat vert, niveau 0 (500ms)
        MonteCarlo mc = new MonteCarlo(0);
        Board b = TestBoardFactory.empty();
        b.board[0][0] = Board.SOLDIER;
        b.kingY = 8;
        b.kingX = 8;
        // When
        Move result = mc.findBestMove(b, 0);
        // Then
        Assertions.assertNotNull(result);
    }

    @Test
    void testFindBestMoveCoupPartDuBonEndroit() {
        // Define : un soldat en (y=5, x=5)
        MonteCarlo mc = new MonteCarlo(0);
        Board b = TestBoardFactory.empty();
        b.board[5][5] = Board.SOLDIER;
        b.kingY = 8;
        b.kingX = 8;
        // When
        Move result = mc.findBestMove(b, 0);
        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.srcY());
        Assertions.assertEquals(5, result.srcX());
    }

    @Test
    void testFindBestMoveMoscoviteAvecUnePiece() {
        // Define : une moscovite, tour jaune
        MonteCarlo mc = new MonteCarlo(0);
        Board b = TestBoardFactory.empty();
        b.board[1][1] = Board.MOSCOVITE;
        b.kingY = 8;
        b.kingX = 8;
        // When
        Move result = mc.findBestMove(b, 1);
        // Then
        Assertions.assertNotNull(result);
    }

    // --- Test constante C ---

    @Test
    void testConstanteCEstPositive() {
        Assertions.assertTrue(MonteCarlo.C > 0);
    }
}
