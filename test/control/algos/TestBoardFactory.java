package control.algos;

/**
 * Utilitaire de test pour construire des instances de Board
 * avec un état arbitraire, sans passer par TablutBoard.
 *
 * Utilise le constructeur Board(int[][], int, int) ajouté pour les tests.
 */
public class TestBoardFactory {

    /**
     * Crée un Board 9x9 entièrement vide, roi fictif en (8, 8).
     */
    public static Board empty() {
        return new Board(new int[9][9], 8, 8);
    }

    /**
     * Crée un Board avec la grille et la position du roi fournies.
     */
    public static Board from(int[][] grid, int kingY, int kingX) {
        return new Board(grid, kingY, kingX);
    }
}
