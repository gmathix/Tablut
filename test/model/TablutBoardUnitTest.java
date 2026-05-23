package model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import boardifier.model.GameStageModel;

public class TablutBoardUnitTest {

    private TablutBoard board;
    private GameStageModel mockStageModel;

    @BeforeEach
    public void setUp() {
        mockStageModel = Mockito.mock(GameStageModel.class);
        board = new TablutBoard(0, 0, mockStageModel);
    }

    // --- Tests sur getKingX / getKingY (valeurs initiales) ---

    @Test
    void testKingInitialPositionX() {
        // Define
        // (fait dans setUp)
        // When
        int kingX = board.getKingX();
        // Then
        Assertions.assertEquals(4, kingX);
    }

    @Test
    void testKingInitialPositionY() {
        // Define
        // (fait dans setUp)
        // When
        int kingY = board.getKingY();
        // Then
        Assertions.assertEquals(4, kingY);
    }

    // --- Tests sur setKingX / setKingY ---

    @Test
    void testSetKingX() {
        // Define
        int newX = 2;
        // When
        board.setKingX(newX);
        // Then
        Assertions.assertEquals(newX, board.getKingX());
    }

    @Test
    void testSetKingY() {
        // Define
        int newY = 7;
        // When
        board.setKingY(newY);
        // Then
        Assertions.assertEquals(newY, board.getKingY());
    }

    @Test
    void testSetKingXToZero() {
        // Define
        // When
        board.setKingX(0);
        // Then
        Assertions.assertEquals(0, board.getKingX());
    }

    @Test
    void testSetKingYToEdge() {
        // Define
        // When
        board.setKingY(8);
        // Then
        Assertions.assertEquals(8, board.getKingY());
    }

    // --- Tests sur startingBoard ---

    @Test
    void testStartingBoardCenterIsKing() {
        // Define / When
        int centerCell = TablutBoard.startingBoard[4][4];
        // Then
        Assertions.assertEquals(4, centerCell);
    }

    @Test
    void testStartingBoardCornerIsEmpty() {
        // Define / When
        int corner = TablutBoard.startingBoard[0][0];
        // Then
        Assertions.assertEquals(0, corner);
    }

    @Test
    void testStartingBoardTopEdgeMoscovite() {
        // Define / When
        // D1, E1, F1 sont des moscovites (valeur 2)
        int topCenter = TablutBoard.startingBoard[0][4];
        // Then
        Assertions.assertEquals(2, topCenter);
    }

    @Test
    void testStartingBoardSoldierAdjacentToKing() {
        // Define / When
        int soldier = TablutBoard.startingBoard[4][5];
        // Then
        Assertions.assertEquals(3, soldier);
    }
}
