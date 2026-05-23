package control.algos;

import model.RuleSets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

public class EvaluationUnitTest {

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

    // --- Tests countMaterialDiff ---

    @Test
    void testCountMaterialDiffBoardVide() {
        // Define : aucune pièce => 0
        Board b = TestBoardFactory.empty();
        // When
        double result = Evaluation.countMaterialDiff(b);
        // Then
        Assertions.assertEquals(0.0, result);
    }

    @Test
    void testCountMaterialDiffAvantageVertAvecHuitSoldats() {
        // Define : 8 soldats verts, 0 moscovites => avantage vert maximal
        Board b = TestBoardFactory.empty();
        b.board[0][0] = Board.SOLDIER;
        b.board[0][1] = Board.SOLDIER;
        b.board[0][2] = Board.SOLDIER;
        b.board[0][3] = Board.SOLDIER;
        b.board[1][0] = Board.SOLDIER;
        b.board[1][1] = Board.SOLDIER;
        b.board[1][2] = Board.SOLDIER;
        b.board[1][3] = Board.SOLDIER;
        // When
        double result = Evaluation.countMaterialDiff(b);
        // Then : (8/8 - 0/16) * 10 = 10.0
        Assertions.assertEquals(10.0, result, 0.001);
    }

    @Test
    void testCountMaterialDiffAvantageJauneAvecSeizeMoscovites() {
        // Define : 16 moscovites, 0 soldats => avantage jaune maximal
        Board b = TestBoardFactory.empty();
        int count = 0;
        for (int i = 0; i < 9 && count < 16; i++) {
            for (int j = 0; j < 9 && count < 16; j++) {
                b.board[i][j] = Board.MOSCOVITE;
                count++;
            }
        }
        // When
        double result = Evaluation.countMaterialDiff(b);
        // Then : (0/8 - 16/16) * 10 = -10.0
        Assertions.assertEquals(-10.0, result, 0.001);
    }

    // --- Tests countKingEncerclement ---

    @Test
    void testCountKingEncerelementBoardVide() {
        // Define : roi seul, aucun entourage
        Board b = TestBoardFactory.empty();
        b.board[4][4] = Board.KING;
        b.kingY = 4;
        b.kingX = 4;
        // When
        double result = Evaluation.countKingEncerclement(b);
        // Then
        Assertions.assertEquals(0.0, result);
    }

    @Test
    void testCountKingEncerelementUnMoscoviteHorizontal() {
        // Define : roi en (4,4), moscovite en (4,5) => encerclement négatif pour vert
        Board b = TestBoardFactory.empty();
        b.board[4][4] = Board.KING;
        b.board[4][5] = Board.MOSCOVITE;
        b.kingY = 4;
        b.kingX = 4;
        // When
        double result = Evaluation.countKingEncerclement(b);
        // Then : un moscovite horizontal = score 1 * 3 = 3, négatif = -3
        Assertions.assertEquals(-3.0, result, 0.001);
    }

    @Test
    void testCountKingEncerelementUnSoldatHorizontalProtege() {
        // Define : roi en (4,4), soldat en (4,3) => protège le roi (score positif pour vert)
        Board b = TestBoardFactory.empty();
        b.board[4][4] = Board.KING;
        b.board[4][3] = Board.SOLDIER;
        b.kingY = 4;
        b.kingX = 4;
        // When
        double result = Evaluation.countKingEncerclement(b);
        // Then : un soldat horizontal = score 1 * -1 = -1, négatif de -1 = +1
        Assertions.assertEquals(1.0, result, 0.001);
    }

    // --- Tests evaluate (cas de victoire immédiate) ---

    @Test
    void testEvaluateVictoireVertePourVertRetournePositif() {
        // Define : roi sur le bord => victoire verte
        Board b = TestBoardFactory.empty();
        b.board[0][4] = Board.KING;
        b.kingY = 0;
        b.kingX = 4;
        // When : tour vert (0), profondeur base = 3, profondeur courante = 3
        double result = Evaluation.evaluate(b, 0, 3, 3);
        // Then : résultat positif car victoire verte et on est vert
        Assertions.assertTrue(result > 0);
    }

    @Test
    void testEvaluateVictoireVertePourJauneRetourneNegatif() {
        // Define : roi sur le bord => victoire verte
        Board b = TestBoardFactory.empty();
        b.board[0][4] = Board.KING;
        b.kingY = 0;
        b.kingX = 4;
        // When : tour jaune (1)
        double result = Evaluation.evaluate(b, 1, 3, 3);
        // Then : résultat négatif car victoire verte mais on est jaune
        Assertions.assertTrue(result < 0);
    }

    @Test
    void testEvaluateBoardNeutre() {
        // Define : board sans situation gagnante immédiate
        Board b = TestBoardFactory.empty();
        b.board[4][4] = Board.KING;
        b.kingY = 4;
        b.kingX = 4;
        // When
        double result = Evaluation.evaluate(b, 0, 3, 3);
        // Then : pas de valeur infinie
        Assertions.assertTrue(Math.abs(result) < Evaluation.VIRTUAL_INF);
    }
}
