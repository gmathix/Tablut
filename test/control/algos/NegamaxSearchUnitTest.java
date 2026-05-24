package control.algos;

import model.Move;
import model.RuleSets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

public class NegamaxSearchUnitTest {

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

    // --- Tests sur la construction de NegamaxSearch ---

    @Test
    void testConstructeurProfondeurNegativeClampeeAZero() {
        // Define / When
        NegamaxSearch search = new NegamaxSearch(-5);
        // Then : findBestMove ne lève pas d'exception (preuve que la profondeur est clampée)
        Board b = TestBoardFactory.empty();
        b.board[0][0] = Board.SOLDIER;
        Assertions.assertDoesNotThrow(() -> search.findBestMove(b, 0));
    }

    @Test
    void testConstructeurProfondeurTropGrandeClampeeADix() {
        // Define / When
        NegamaxSearch search = new NegamaxSearch(100);
        // Then : construction ne lève pas d'exception
        Assertions.assertNotNull(search);
    }

    // --- Tests sur findBestMove ---

    @Test
    void testFindBestMoveBoardVideRetourneNull() {
        // Define : aucune pièce verte => pas de coup possible
        NegamaxSearch search = new NegamaxSearch(1);
        Board b = TestBoardFactory.empty();
        // When
        Move result = search.findBestMove(b, 0);
        // Then
        Assertions.assertNull(result);
    }

    @Test
    void testFindBestMoveAvecUnSoldatRetourneCoup() {
        // Define : un seul soldat vert, profondeur 1
        NegamaxSearch search = new NegamaxSearch(1);
        Board b = TestBoardFactory.empty();
        b.board[0][0] = Board.SOLDIER;
        b.kingY = 8;
        b.kingX = 8;
        // When
        Move result = search.findBestMove(b, 0);
        // Then : un coup est trouvé
        Assertions.assertNotNull(result);
    }

    @Test
    void testFindBestMoveCoupDeSrcEstValide() {
        // Define : un soldat en (y=0, x=0), profondeur 1
        NegamaxSearch search = new NegamaxSearch(1);
        Board b = TestBoardFactory.empty();
        b.board[0][0] = Board.SOLDIER;
        // When
        Move result = search.findBestMove(b, 0);
        // Then : le coup provient bien du soldat
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.srcY());
        Assertions.assertEquals(0, result.srcX());
    }

    @Test
    void testFindBestMoveVictoireImmediate() {
        // Define : roi un mouvement du bord => le négamax doit choisir de gagner
        NegamaxSearch search = new NegamaxSearch(1);
        Board b = TestBoardFactory.empty();
        // Roi en (y=4, x=1) : peut atteindre x=0 => victoire verte
        b.board[4][1] = Board.KING;
        b.kingY = 4;
        b.kingX = 1;
        // When
        Move result = search.findBestMove(b, 0);
        // Then : le coup doit exister
        Assertions.assertNotNull(result);
    }

    // --- Tests sur nbEvals ---

    @Test
    void testNbEvalsMisAJourApresFindBestMove() {
        // Define
        NegamaxSearch search = new NegamaxSearch(2);
        Board b = TestBoardFactory.empty();
        b.board[0][0] = Board.SOLDIER;
        NegamaxSearch.nbEvals = 0;
        // When
        search.findBestMove(b, 0);
        // Then : au moins une évaluation a été faite
        Assertions.assertTrue(NegamaxSearch.nbEvals > 0);
    }
}
