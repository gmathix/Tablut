package model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import boardifier.model.Model;

public class TablutStageFactoryUnitTest {

    private TablutStageModel mockStageModel;
    private TablutStageFactory factory;
    private Model mockModel;

    @BeforeEach
    public void setUp() {
        mockModel = Mockito.mock(Model.class);
        mockStageModel = Mockito.mock(TablutStageModel.class);
        factory = new TablutStageFactory(mockStageModel);
    }

    // --- Tests sur la construction de la factory ---

    @Test
    void testFactoryInstantiationNotNull() {
        // Define / When
        TablutStageFactory f = new TablutStageFactory(mockStageModel);
        // Then
        Assertions.assertNotNull(f);
    }

    // --- Test que setup() appelle bien setBoard sur le stageModel ---

    @Test
    void testSetupCallsSetBoard() {
        // Define
        // On a besoin d'un vrai stageModel pour que setup() s'exécute sans null pointer
        TablutStageModel realStageModel = new TablutStageModel("test", mockModel);
        TablutStageFactory realFactory = new TablutStageFactory(realStageModel);
        // When
        realFactory.setup();
        // Then : le board doit avoir été affecté
        Assertions.assertNotNull(realStageModel.getBoard());
    }

    @Test
    void testSetupCreatesMoscovitePawns() {
        // Define
        TablutStageModel realStageModel = new TablutStageModel("test", mockModel);
        TablutStageFactory realFactory = new TablutStageFactory(realStageModel);
        // When
        realFactory.setup();
        // Then : 16 moscovites
        Assertions.assertEquals(16, realStageModel.getMoscovitePawns().length);
    }

    @Test
    void testSetupCreatesSoldierPawns() {
        // Define
        TablutStageModel realStageModel = new TablutStageModel("test", mockModel);
        TablutStageFactory realFactory = new TablutStageFactory(realStageModel);
        // When
        realFactory.setup();
        // Then : 8 soldats
        Assertions.assertEquals(8, realStageModel.getSoldierPawns().length);
    }

    @Test
    void testSetupCreatesKingPawn() {
        // Define
        TablutStageModel realStageModel = new TablutStageModel("test", mockModel);
        TablutStageFactory realFactory = new TablutStageFactory(realStageModel);
        // When
        realFactory.setup();
        // Then : 1 roi
        Assertions.assertEquals(1, realStageModel.getKingPawns().length);
    }

    @Test
    void testSetupKingPawnColorIsKing() {
        // Define
        TablutStageModel realStageModel = new TablutStageModel("test", mockModel);
        TablutStageFactory realFactory = new TablutStageFactory(realStageModel);
        // When
        realFactory.setup();
        // Then
        Assertions.assertEquals(Pawn.PAWN_KING, realStageModel.getKingPawns()[0].getColor());
    }
}
