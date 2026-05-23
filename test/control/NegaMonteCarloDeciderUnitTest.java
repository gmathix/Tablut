package control;

import boardifier.model.Model;
import boardifier.control.Controller;
import model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

/**
 * Tests unitaires de NegaMonteCarloDecider en isolation avec Mockito.
 */
public class NegaMonteCarloDeciderUnitTest {

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

    @Test
    void testInstanciationSansException() {
        Assertions.assertDoesNotThrow(() ->
            new NegaMonteCarloDecider(mockModel, mockController, 0)
        );
    }

    @Test
    void testDecideAppelleGetGameStage() {
        // Define
        NegaMonteCarloDecider decider = new NegaMonteCarloDecider(mockModel, mockController, 0);
        Mockito.when(mockBoard.getElement(Mockito.anyInt(), Mockito.anyInt())).thenReturn(null);
        Mockito.when(mockModel.getIdPlayer()).thenReturn(0);
        // When
        try { decider.decide(); } catch (Exception e) { /* pas de coup */ }
        // Then
        Mockito.verify(mockModel).getGameStage();
    }

    @Test
    void testDecideAppelleGetBoard() {
        // Define
        NegaMonteCarloDecider decider = new NegaMonteCarloDecider(mockModel, mockController, 0);
        Mockito.when(mockBoard.getElement(Mockito.anyInt(), Mockito.anyInt())).thenReturn(null);
        Mockito.when(mockModel.getIdPlayer()).thenReturn(0);
        // When
        try { decider.decide(); } catch (Exception e) { /* pas de coup */ }
        // Then
        Mockito.verify(mockStageModel).getBoard();
    }
}
