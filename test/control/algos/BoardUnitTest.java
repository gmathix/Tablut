package control.algos;

import model.Move;
import model.RuleSets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

import java.util.List;

/**
 * Tests unitaires de la classe Board.
 *
 * Pour construire des instances de Board avec un état précis sans passer
 * par TablutBoard (couplage fort), on utilise la classe TestBoardFactory
 * qui est dans le même package et expose un constructeur de test.
 */
public class BoardUnitTest {

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

    // --- Tests isMoscovite / isGreen / isKing / isSoldier / isEmpty ---

    @Test
    void testIsMoscoviteReturnsTrue() {
        // Define
        Board b = TestBoardFactory.empty();
        // When / Then
        Assertions.assertTrue(b.isMoscovite(Board.MOSCOVITE));
    }

    @Test
    void testIsMoscoviteReturnsFalseForSoldier() {
        Board b = TestBoardFactory.empty();
        Assertions.assertFalse(b.isMoscovite(Board.SOLDIER));
    }

    @Test
    void testIsKingReturnsTrue() {
        Board b = TestBoardFactory.empty();
        Assertions.assertTrue(b.isKing(Board.KING));
    }

    @Test
    void testIsSoldierReturnsTrue() {
        Board b = TestBoardFactory.empty();
        Assertions.assertTrue(b.isSoldier(Board.SOLDIER));
    }

    @Test
    void testIsEmptyReturnsTrue() {
        Board b = TestBoardFactory.empty();
        Assertions.assertTrue(b.isEmpty(Board.EMPTY));
    }

    @Test
    void testIsGreenForSoldier() {
        Board b = TestBoardFactory.empty();
        Assertions.assertTrue(b.isGreen(Board.SOLDIER));
    }

    @Test
    void testIsGreenForKing() {
        Board b = TestBoardFactory.empty();
        Assertions.assertTrue(b.isGreen(Board.KING));
    }

    // --- Tests checkWin ---

    @Test
    void testCheckWinKingOnLeftEdge() {
        // Define : roi en (y=4, x=0) => bord gauche
        Board b = TestBoardFactory.empty();
        b.board[4][0] = Board.KING;
        b.kingY = 4;
        b.kingX = 0;
        // When
        double result = b.checkWin();
        // Then
        Assertions.assertEquals((double) Integer.MAX_VALUE, result);
    }

    @Test
    void testCheckWinKingOnTopEdge() {
        // Define : roi en (y=0, x=4) => bord haut
        Board b = TestBoardFactory.empty();
        b.board[0][4] = Board.KING;
        b.kingY = 0;
        b.kingX = 4;
        // When
        double result = b.checkWin();
        // Then
        Assertions.assertEquals((double) Integer.MAX_VALUE, result);
    }

    @Test
    void testCheckWinKingInCenterReturnsZero() {
        // Define : roi au centre, pas de situation gagnante
        Board b = TestBoardFactory.empty();
        b.board[4][4] = Board.KING;
        b.kingY = 4;
        b.kingX = 4;
        // When
        double result = b.checkWin();
        // Then
        Assertions.assertEquals(0.0, result);
    }

    @Test
    void testCheckWinKingSurroundedByFourMoscovites() {
        // Define : roi en (2,2) entouré de 4 moscovites => victoire moscovite
        Board b = TestBoardFactory.empty();
        b.board[2][2] = Board.KING;
        b.board[1][2] = Board.MOSCOVITE;
        b.board[3][2] = Board.MOSCOVITE;
        b.board[2][1] = Board.MOSCOVITE;
        b.board[2][3] = Board.MOSCOVITE;
        b.kingY = 2;
        b.kingX = 2;
        // When
        double result = b.checkWin();
        // Then
        Assertions.assertEquals((double) Integer.MIN_VALUE, result);
    }

    // --- Tests makeMove ---

    @Test
    void testMakeMoveDeplaceLaPiece() {
        // Define : soldat en (y=0, x=0) vers (y=3, x=0)
        Board b = TestBoardFactory.empty();
        b.board[0][0] = Board.SOLDIER;
        Move move = new Move(0, 0, 0, 3);
        // When
        b.makeMove(move);
        // Then
        Assertions.assertEquals(Board.SOLDIER, b.board[3][0]);
        Assertions.assertEquals(Board.EMPTY,   b.board[0][0]);
    }

    @Test
    void testMakeMoveSourceDevientVide() {
        // Define : moscovite en (y=0, x=5) vers (y=2, x=5)
        Board b = TestBoardFactory.empty();
        b.board[0][5] = Board.MOSCOVITE;
        Move move = new Move(5, 0, 5, 2);
        // When
        b.makeMove(move);
        // Then
        Assertions.assertEquals(Board.EMPTY, b.board[0][5]);
    }

    @Test
    void testMakeMoveKingMetAJourKingY() {
        // Define : roi en (y=4, x=4) se déplace vers (y=0, x=4)
        Board b = TestBoardFactory.empty();
        b.board[4][4] = Board.KING;
        b.kingY = 4;
        b.kingX = 4;
        Move move = new Move(4, 4, 4, 0);
        // When
        b.makeMove(move);
        // Then
        Assertions.assertEquals(0, b.kingY);
    }

    @Test
    void testMakeMoveKingMetAJourKingX() {
        // Define : roi en (y=4, x=4) se déplace vers (y=4, x=0)
        Board b = TestBoardFactory.empty();
        b.board[4][4] = Board.KING;
        b.kingY = 4;
        b.kingX = 4;
        Move move = new Move(4, 4, 0, 4);
        // When
        b.makeMove(move);
        // Then
        Assertions.assertEquals(0, b.kingX);
    }

    // --- Tests getLegalMoves ---

    @Test
    void testGetLegalMovesAvecUnSoldat() {
        // Define : un soldat en (y=0, x=0), tour vert
        Board b = TestBoardFactory.empty();
        b.board[0][0] = Board.SOLDIER;
        // When
        List<Move> moves = b.getLegalMoves(0);
        // Then : le soldat peut bouger
        Assertions.assertFalse(moves.isEmpty());
    }

    @Test
    void testGetLegalMovesBoardVideRetourneListeVide() {
        // Define : aucune pièce verte
        Board b = TestBoardFactory.empty();
        // When
        List<Move> moves = b.getLegalMoves(0);
        // Then
        Assertions.assertTrue(moves.isEmpty());
    }

    @Test
    void testGetLegalMovesMoscoviteAvecUneMoscovite() {
        // Define : une moscovite, tour jaune
        Board b = TestBoardFactory.empty();
        b.board[1][1] = Board.MOSCOVITE;
        // When
        List<Move> moves = b.getLegalMoves(1);
        // Then
        Assertions.assertFalse(moves.isEmpty());
    }

    // --- Tests checkCapture ---

    @Test
    void testCheckCaptureSansCapture() {
        // Define : un soldat seul
        Board b = TestBoardFactory.empty();
        b.board[0][0] = Board.SOLDIER;
        Move move = new Move(0, 0, 0, 1);
        // When
        int result = b.checkCapture(move);
        // Then
        Assertions.assertEquals(-1, result);
    }

    @Test
    void testCheckCaptureMoscoviteCaptureUnSoldat() {
        // Define : moscovite en (y=0,x=3) vers (y=0,x=1),
        //          soldat en (y=0,x=2), moscovite alliée en (y=0,x=0)
        Board b = TestBoardFactory.empty();
        b.board[0][3] = Board.MOSCOVITE;
        b.board[0][2] = Board.SOLDIER;
        b.board[0][0] = Board.MOSCOVITE;
        Move move = new Move(3, 0, 1, 0);
        // When
        int result = b.checkCapture(move);
        // Then : index plat de (y=0, x=2) = 2
        Assertions.assertEquals(2, result);
    }

    // --- Tests constantes ---

    @Test
    void testConstanteMOSCOVITE() {
        Assertions.assertEquals(1, Board.MOSCOVITE);
    }

    @Test
    void testConstanteSOLDIER() {
        Assertions.assertEquals(2, Board.SOLDIER);
    }

    @Test
    void testConstanteKING() {
        Assertions.assertEquals(3, Board.KING);
    }

    @Test
    void testConstanteEMPTY() {
        Assertions.assertEquals(0, Board.EMPTY);
    }
}
