package model;

import boardifier.model.*;

/**
 * TablutStageModel defines the model for the single stage in "The Tablut". Indeed,
 * there are no levels in this game: a party starts and when it's done, the game is also done.
 *
 * TablutStageModel must define all that is needed to manage a party : state variables and game elements.
 * In the present case, there are only 2 state variables that represent the number of pawns to play by each player.
 * It is used to detect the end of the party.
 * For game elements, it depends on what is chosen as a final UI design. For that demo, there are 12 elements used
 * to represent the state : the main board, 2 pots, 8 pawns, and a text for current player.
 *
 * WARNING ! TablutStageModel DOES NOT create itself the game elements because it would prevent the possibility to mock
 * game element classes for unit testing purposes. This is why TablutStageModel just defines the game elements and the methods
 * to set this elements.
 * The instanciation of the elements is done by the TablutStageFactory, which uses the provided setters.
 *
 * TablutStageModel must also contain methods to check/modify the game state when given events occur. This is the role of
 * setupCallbacks() method that defines a callback function that must be called when a pawn is put in a container.
 * This is done by calling onPutInContainer() method, with the callback function as a parameter. After that call, boardifier
 * will be able to call the callback function automatically when a pawn is put in a container.
 * NB1: callback functions MUST BE defined with a lambda expression (i.e. an arrow function).
 * NB2:  there are other methods to defines callbacks for other events (see onXXX methods in GameStageModel)
 * In "The Tablut", everytime a pawn is put in the main board, we have to check if the party is ended and in this case, who is the winner.
 * This is the role of computePartyResult(), which is called by the callback function if there is no more pawn to play.
 *
 */
public class TablutStageModel extends GameStageModel {

    // define stage state variables
    private int blackPawnsToPlay;
    private int redPawnsToPlay;

    // define stage game elements
    private TablutBoard board;
    private TablutPawnPot blackPot;
    private TablutPawnPot redPot;
    private Pawn[] moscovitePawns;
    private Pawn[] soldierPawns;
    private Pawn[] kingPawns;
    private TextElement playerName;
    // Uncomment next line if the example with a main container is used. see end of TablutStageFactory and TablutStageView
    //private ContainerElement mainContainer;

    public TablutStageModel(String name, Model model) {
        super(name, model);
        setupCallbacks();
    }

    public TablutBoard getBoard() {
        return board;
    }

    public TablutPawnPot getBlackPot() {
        return blackPot;
    }

    public TablutPawnPot getRedPot() {
        return redPot;
    }

    public Pawn[] getMoscovitePawns() {
        return moscovitePawns;
    }

    public Pawn[] getSoldierPawns() {
        return soldierPawns;
    }

    public Pawn[] getKingPawns() { return kingPawns; }

    public TextElement getPlayerName() {
        return playerName;
    }

    public void setBoard(TablutBoard board) {
        this.board = board;
        addContainer(board);
    }
    public void setBlackPot(TablutPawnPot blackPot) {
        this.blackPot = blackPot;
        addContainer(blackPot);
    }
    public void setRedPot(TablutPawnPot redPot) {
        this.redPot = redPot;
        addContainer(redPot);
    }
    public void setMoscovitePawns(Pawn[] moscovitePawns) {
        this.moscovitePawns = moscovitePawns;
        for(int i = 0; i< moscovitePawns.length; i++) {
            addElement(moscovitePawns[i]);
        }
    }
    public void setSoldierPawns(Pawn[] soldierPawns) {
        this.soldierPawns = soldierPawns;
        for(int i = 0; i< soldierPawns.length; i++) {
            addElement(soldierPawns[i]);
        }
    }
    public void setKingPawns(Pawn[] kingPawns) {
        this.kingPawns = kingPawns;
        for(int i = 0; i< kingPawns.length; i++) {
            addElement(kingPawns[i]);
        }
    }
    public void setPlayerName(TextElement playerName) {
        this.playerName = playerName;
        addElement(playerName);
    }


    private void setupCallbacks() {

        onMoveInContainer(new ContainerOpCallback() {
            @Override
            public void execute(GameElement element, ContainerElement containerDest, int rowDest, int colDest) {
                computePartyResult();
            }
        });
    }

    public void checkCapture(boolean isYellow, int colSrc, int colDest, int rowSrc, int rowDest) {
        // check capture
        int horizontalDirection = 0;
        int verticalDirection = 0;

        if (colSrc - colDest != 0)
            horizontalDirection = colDest - colSrc > 0 ? 1 : -1; // 1 for right, -1 for left
        if (rowSrc - rowDest != 0)
            verticalDirection = rowDest - rowSrc > 0 ? 1 : -1;   // 1 for down, -1 for up

        int[] dy_vals = {-1, 0, 1, 0};
        int[] dx_vals = {0, -1, 0, 1};

        for (int i = 0; i < 4; i++) {
            int dy = dy_vals[i];
            int dx = dx_vals[i];

            // do not check the squares on the path the pawn came from
            if (dx == -horizontalDirection && horizontalDirection != 0) continue;
            if (dy == -verticalDirection && verticalDirection != 0) continue;

            // check bounds for pawn 2 squares away
            if (rowDest + 2*dy < 0 || rowDest + 2*dy >= 9) continue;
            if (colDest + 2*dx < 0 || colDest + 2*dx >= 9) continue;


            GameElement sideEl = getBoard().getElement(rowDest + dy, colDest + dx);
            GameElement sideEl2 = getBoard().getElement(rowDest + 2*dy, colDest + 2*dx);

            if ((sideEl instanceof Pawn sideP) && (sideEl2 instanceof Pawn sideP2)) {
                if (isYellow) {
                    if (sideP.getColor() == Pawn.PAWN_SOLDIER && sideP2.getColor() == Pawn.PAWN_MOSCOVITE) {
                        getBoard().removeElement(sideEl);
                        removeElement(sideEl);
                    }
                } else {
                    if (sideP.getColor() == Pawn.PAWN_MOSCOVITE && sideP2.getColor() != Pawn.PAWN_MOSCOVITE) {
                        getBoard().removeElement(sideEl);
                        removeElement(sideEl);
                    }
                }
            }
        }
    }

    private void computePartyResult() {
        int idWinner = -1;
        String winMessage = "";


        // dy and dx vals for clockwise rotation
        int[] dy_vals = {-1, 0, 1, 0};
        int[] dx_vals = {0, 1, 0, -1};


        int kingX = getBoard().getKingX();
        int kingY = getBoard().getKingY();



        // check if the king can reach one edge or two
        int nbEdgesRechable = 0;
        for (int i = 0; i < 4; i++) {
            int y = kingY;
            int x = kingX;
            boolean isFreeWay = true;

            for (int j = 0; j < 9; j++) {
                y += dy_vals[i];
                x += dx_vals[i];
                if (y < 0 || y > 8 || x < 0 || x > 8) break;
                if (getBoard().getElement(y, x) instanceof Pawn) {
                    isFreeWay = false;
                }
            }

            if (isFreeWay) {
                nbEdgesRechable++;
            }
        }
        if (nbEdgesRechable == 1) {
            System.out.printf("\nPlayer 1 : Raichi\n\n");
        } else if (nbEdgesRechable >= 2) {
            System.out.printf("\nPlayer 1 : Tuichi!\n\n");
        }


        // check if the king has reached an edge
        if (kingY == 0 || kingY == 8 || kingX == 0 || kingX == 8) {
            // exclude starting moscovite squares
            if (((kingY == 0 || kingY == 8) && (kingX <= 2 || kingX >= 6)) ||
                ((kingX == 0 || kingX == 8) && (kingY <= 2 || kingY >= 6))) {

                idWinner = 0;
                winMessage = "king reached an edge";
            }
        }

        // count surrounding moscovites
        int nbSurrounging = 0;
        boolean hasCenterNeighbor = false;


        for (int i = 0; i < 4; i++) {
            int y = kingY + dy_vals[i];
            int x = kingX + dx_vals[i];
            if (y < 0 || y > 8 || x < 0 || x > 8) continue;
            if (y == 4 && x == 4) hasCenterNeighbor = true;
            if (getBoard().getElement(y, x) instanceof Pawn p) {
                if (p.getColor() == Pawn.PAWN_MOSCOVITE) {
                    nbSurrounging++;
                }
            }
        }


        /* either the king is next to the center square and is surrounded by 3 moscovites,
         * or it is surrounded by 4 moscovites.
         */
        if ((hasCenterNeighbor && nbSurrounging == 3) || (nbSurrounging == 4)) {
            idWinner = 1;
            winMessage = "yellow surrounded the king";
        }



        if (idWinner != -1) {
            System.out.printf("Winner is player %d : %s\n", idWinner+1, winMessage);
            model.setIdWinner(idWinner);
            model.stopStage();
        }
    }

    @Override
    public StageElementsFactory getDefaultElementFactory() {
        return new TablutStageFactory(this);
    }
}
