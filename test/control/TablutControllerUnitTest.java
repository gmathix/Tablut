package control;

import boardifier.model.Model;
import boardifier.view.View;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

/**
 * Tests unitaires de TablutController en isolation.
 *
 * On mock Model et View pour tester uniquement le comportement
 * du contrôleur lui-même.
 */
public class TablutControllerUnitTest {

    private Model mockModel;
    private View  mockView;

    @BeforeEach
    public void setUp() {
        mockModel = Mockito.mock(Model.class);
        mockView  = Mockito.mock(View.class);
    }

    // --- Tests construction ---

    @Test
    void testConstructeurSimpleInstancieCorrrectement() {
        // Define / When / Then
        Assertions.assertDoesNotThrow(() ->
            new TablutController(mockModel, mockView, 2, "")
        );
    }

    @Test
    void testConstructeurAvecOptionsInstancieCorrrectement() {
        // Define / When / Then
        Assertions.assertDoesNotThrow(() ->
            new TablutController(mockModel, mockView, 2, "",
                TablutController.NEGAMAX_PLAYER, TablutController.NEGAMAX_PLAYER,
                new int[]{3, 3})
        );
    }

    // --- Tests setBotPlayer ---

    @Test
    void testSetBotPlayerValeurValide() {
        // Define
        TablutController controller = new TablutController(mockModel, mockView, 2, "");
        // When / Then : aucune exception
        Assertions.assertDoesNotThrow(() ->
            controller.setBotPlayer(0, TablutController.MONTECARLO_PLAYER)
        );
    }

    @Test
    void testSetBotPlayerValeurNegativeIgnoree() {
        // Define
        TablutController controller = new TablutController(mockModel, mockView, 2, "");
        // When / Then : aucune exception (la valeur invalide est ignorée)
        Assertions.assertDoesNotThrow(() ->
            controller.setBotPlayer(0, -1)
        );
    }

    // --- Tests getAvailableBots ---

    @Test
    void testGetAvailableBotsRetourneDeuxTableaux() {
        // Define
        TablutController controller = new TablutController(mockModel, mockView, 2, "");
        // When
        var bots = controller.getAvailableBots();
        // Then : 2 joueurs
        Assertions.assertEquals(2, bots.length);
    }

    @Test
    void testGetAvailableBotsContientNegamaxPourJoueur0() {
        // Define
        TablutController controller = new TablutController(mockModel, mockView, 2, "");
        // When
        var bots = controller.getAvailableBots();
        // Then : le joueur 0 a bien un bot Negamax disponible
        Assertions.assertTrue(bots[0].containsKey(TablutController.NEGAMAX_PLAYER));
    }

    @Test
    void testGetAvailableBotsContientMonteCarloPourJoueur1() {
        // Define
        TablutController controller = new TablutController(mockModel, mockView, 2, "");
        // When
        var bots = controller.getAvailableBots();
        // Then
        Assertions.assertTrue(bots[1].containsKey(TablutController.MONTECARLO_PLAYER));
    }

    // --- Tests constantes ---

    @Test
    void testConstanteNEGAMAX_PLAYER() {
        Assertions.assertEquals(0, TablutController.NEGAMAX_PLAYER);
    }

    @Test
    void testConstanteMONTECARLO_PLAYER() {
        Assertions.assertEquals(1, TablutController.MONTECARLO_PLAYER);
    }

    @Test
    void testConstanteNEGAMONTECARLO_PLAYER() {
        Assertions.assertEquals(2, TablutController.NEGAMONTECARLO_PLAYER);
    }
}
