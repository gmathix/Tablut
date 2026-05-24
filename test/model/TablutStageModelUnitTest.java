package model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import boardifier.model.Model;
import boardifier.model.TextElement;

public class TablutStageModelUnitTest {

    private TablutStageModel stageModel;
    private Model mockModel;

    @BeforeEach
    public void setUp() {
        mockModel = Mockito.mock(Model.class);
        stageModel = new TablutStageModel("test", mockModel);
    }

    // --- Tests sur setBoard / getBoard ---

    @Test
    void testGetBoardAfterSet() {
        // Define
        TablutBoard mockBoard = Mockito.mock(TablutBoard.class);
        // When
        stageModel.setBoard(mockBoard);
        // Then
        Assertions.assertEquals(mockBoard, stageModel.getBoard());
    }

    // --- Tests sur setMoscovitePawns / getMoscovitePawns ---

    @Test
    void testGetMoscovitePawnsAfterSet() {
        // Define
        Pawn mockPawn1 = Mockito.mock(Pawn.class);
        Pawn mockPawn2 = Mockito.mock(Pawn.class);
        Pawn[] pawns = {mockPawn1, mockPawn2};
        // When
        stageModel.setMoscovitePawns(pawns);
        // Then
        Assertions.assertEquals(2, stageModel.getMoscovitePawns().length);
    }

    @Test
    void testMoscovitePawnsContainExpectedPawn() {
        // Define
        Pawn mockPawn = Mockito.mock(Pawn.class);
        Pawn[] pawns = {mockPawn};
        // When
        stageModel.setMoscovitePawns(pawns);
        // Then
        Assertions.assertEquals(mockPawn, stageModel.getMoscovitePawns()[0]);
    }

    // --- Tests sur setSoldierPawns / getSoldierPawns ---

    @Test
    void testGetSoldierPawnsAfterSet() {
        // Define
        Pawn mockSoldier = Mockito.mock(Pawn.class);
        Pawn[] soldiers = {mockSoldier};
        // When
        stageModel.setSoldierPawns(soldiers);
        // Then
        Assertions.assertEquals(mockSoldier, stageModel.getSoldierPawns()[0]);
    }

    // --- Tests sur setKingPawns / getKingPawns ---

    @Test
    void testGetKingPawnsAfterSet() {
        // Define
        Pawn mockKing = Mockito.mock(Pawn.class);
        Pawn[] kings = {mockKing};
        // When
        stageModel.setKingPawns(kings);
        // Then
        Assertions.assertEquals(mockKing, stageModel.getKingPawns()[0]);
    }

    // --- Tests sur setPlayerName / getPlayerName ---

    @Test
    void testGetPlayerNameAfterSet() {
        // Define
        TextElement mockText = Mockito.mock(TextElement.class);
        // When
        stageModel.setPlayerName(mockText);
        // Then
        Assertions.assertEquals(mockText, stageModel.getPlayerName());
    }

    // --- Test checkCapture : s'assure que la direction est bien calculée ---

    @Test
    void testCheckCaptureNoExceptionHorizontalMove() {
        // Define
        TablutBoard mockBoard = Mockito.mock(TablutBoard.class);
        stageModel.setBoard(mockBoard);
        // On configure le board mock pour retourner null sur getElement (aucune pièce)
        Mockito.when(mockBoard.getElement(Mockito.anyInt(), Mockito.anyInt())).thenReturn(null);
        // When / Then : aucune exception ne doit être levée
        Assertions.assertDoesNotThrow(() ->
            stageModel.checkCapture(false, 0, 2, 4, 4)
        );
    }
}
