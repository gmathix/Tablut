package control;

import boardifier.model.Model;
import boardifier.control.Controller;
import model.*;
import control.algos.TestBoardFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

/**
 * Tests unitaires de TablutDecider en isolation.
 *
 * TablutDecider dépend de Model et de TablutStageModel.
 * On mock ces dépendances avec Mockito pour tester le décideur sans
 * interférence des autres composants.
 */
public class TablutDeciderUnitTest {

    private Model mockModel;
    private Controller mockController;
    private TablutStageModel mockStageModel;
    private TablutBoard mockBoard;

    @BeforeEach
    public void setUp() {
        mockModel      = Mockito.mock(Model.class);
        mockController = Mockito.mock(Controller.class);
        mockStageModel = Mockito.mock(TablutStageModel.class);
        mockBoard      = Mockito.mock(TablutBoard.class);

        // Configuration du mock Model pour retourner le stageModel
        Mockito.when(mockModel.getGameStage()).thenReturn(mockStageModel);
        Mockito.when(mockStageModel.getBoard()).thenReturn(mockBoard);
    }

    // --- Test : TablutDecider s'instancie sans exception ---

    @Test
    void testDeciderInstanciationSansException() {
        // Define / When / Then
        Assertions.assertDoesNotThrow(() -> new TablutDecider(mockModel, mockController));
    }

    // --- Test : getGameStage() est bien appelé sur le model lors de decide() ---

    @Test
    void testDecideAppelleGetGameStage() {
        // Define
        TablutDecider decider = new TablutDecider(mockModel, mockController);
        TablutPawnPot mockPot = Mockito.mock(TablutPawnPot.class);
        Mockito.when(mockModel.getIdPlayer()).thenReturn(Pawn.PAWN_MOSCOVITE);
        Mockito.when(mockStageModel.getBlackPot()).thenReturn(mockPot);
        // On retourne null pour les éléments du pot => pas de mouvement
        Mockito.when(mockPot.getElement(Mockito.anyInt(), Mockito.anyInt())).thenReturn(null);
        // When
        try { decider.decide(); } catch (Exception e) { /* attendu si aucun coup valide */ }
        // Then : getGameStage a bien été invoqué
        Mockito.verify(mockModel).getGameStage();
    }

    // --- Test : le bon pot est sélectionné selon le joueur ---

    @Test
    void testDecideUtiliseBlackPotPourJoueur2() {
        // Define
        TablutDecider decider = new TablutDecider(mockModel, mockController);
        TablutPawnPot mockPot = Mockito.mock(TablutPawnPot.class);
        Mockito.when(mockModel.getIdPlayer()).thenReturn(Pawn.PAWN_MOSCOVITE);
        Mockito.when(mockStageModel.getBlackPot()).thenReturn(mockPot);
        Mockito.when(mockPot.getElement(Mockito.anyInt(), Mockito.anyInt())).thenReturn(null);
        // When
        try { decider.decide(); } catch (Exception e) { /* pas de coup */ }
        // Then : le blackPot a été consulté
        Mockito.verify(mockStageModel).getBlackPot();
    }

    @Test
    void testDecideUtiliseRedPotPourJoueur0() {
        // Define
        TablutDecider decider = new TablutDecider(mockModel, mockController);
        TablutPawnPot mockPot = Mockito.mock(TablutPawnPot.class);
        Mockito.when(mockModel.getIdPlayer()).thenReturn(0);
        Mockito.when(mockStageModel.getRedPot()).thenReturn(mockPot);
        Mockito.when(mockPot.getElement(Mockito.anyInt(), Mockito.anyInt())).thenReturn(null);
        // When
        try { decider.decide(); } catch (Exception e) { /* pas de coup */ }
        // Then : le redPot a été consulté
        Mockito.verify(mockStageModel).getRedPot();
    }
}
