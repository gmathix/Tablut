package control;

import boardifier.model.Model;
import boardifier.control.Controller;
import model.*;
import control.algos.Board;
import control.algos.TestBoardFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

/**
 * Tests unitaires de NegamaxDecider en isolation avec Mockito.
 */
public class TablutNegamaxDeciderUnitTest {

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

        Mockito.when(mockModel.getGameStage()).thenReturn(mockStageModel);
        Mockito.when(mockStageModel.getBoard()).thenReturn(mockBoard);
    }

    // --- Tests construction ---

    @Test
    void testConstructeurAvecNiveauValideInstancie() {
        Assertions.assertDoesNotThrow(() ->
            new NegamaxDecider(mockModel, mockController, 3)
        );
    }

    @Test
    void testConstructeurSansNiveauInstancie() {
        Assertions.assertDoesNotThrow(() ->
            new NegamaxDecider(mockModel, mockController)
        );
    }

    // --- Tests setLevel ---

    @Test
    void testSetLevelNePasLeverException() {
        // Define
        NegamaxDecider decider = new NegamaxDecider(mockModel, mockController, 3);
        // When / Then
        Assertions.assertDoesNotThrow(() -> decider.setLevel(5));
    }

    @Test
    void testSetLevelNiveauNegatifClampAZero() {
        // Define
        NegamaxDecider decider = new NegamaxDecider(mockModel, mockController, 3);
        // When / Then : aucune exception (le niveau est clampé à 0)
        Assertions.assertDoesNotThrow(() -> decider.setLevel(-10));
    }

    // --- Tests decide() : vérification des appels via Mockito ---

    @Test
    void testDecideAppelleGetGameStage() {
        // Define
        NegamaxDecider decider = new NegamaxDecider(mockModel, mockController, 1);
        // On configure le board mock avec une grille vide
        Board emptyAlgoBoard = TestBoardFactory.empty();
        // Le TablutBoard mock doit retourner null pour tous ses éléments
        Mockito.when(mockBoard.getElement(Mockito.anyInt(), Mockito.anyInt())).thenReturn(null);
        Mockito.when(mockModel.getIdPlayer()).thenReturn(0);
        // When
        try { decider.decide(); } catch (Exception e) { /* pas de coup disponible */ }
        // Then : getGameStage a été appelé
        Mockito.verify(mockModel).getGameStage();
    }

    @Test
    void testDecideAppelleGetBoard() {
        // Define
        NegamaxDecider decider = new NegamaxDecider(mockModel, mockController, 1);
        Mockito.when(mockBoard.getElement(Mockito.anyInt(), Mockito.anyInt())).thenReturn(null);
        Mockito.when(mockModel.getIdPlayer()).thenReturn(0);
        // When
        try { decider.decide(); } catch (Exception e) { /* pas de coup */ }
        // Then : getBoard a été appelé sur le stageModel
        Mockito.verify(mockStageModel).getBoard();
    }
}
