package model;

import boardifier.model.*;
import control.algos.RecurBoard;

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

    public static final int STATE_SELECTPAWN = 1;
    public static final int STATE_SELECTDEST = 2;



    private TablutBoard board;
    private Pawn[] moscovitePawns;
    private Pawn[] soldierPawns;
    private Pawn[] kingPawns;

    private TextElement playerName;
    private TextElement titleText;
    private TextElement subtitleText;
    private TextElement helpText;
    private TextElement legendText;
    private TextElement fullBackdrop;
    private TextElement boardBackdrop;
    private TextElement panelBackdrop;

    private int state;


    public TablutStageModel(String name, Model model) {
        super(name, model);
        state = STATE_SELECTPAWN;
        setupCallbacks();
    }

    // GETTERS
    public int getState() { return state; }
    public TablutBoard getBoard() { return board; }
    public Pawn[] getMoscovitePawns() { return moscovitePawns; }
    public Pawn[] getSoldierPawns() { return soldierPawns; }
    public Pawn[] getKingPawns() { return kingPawns; }
    public TextElement getPlayerName() { return playerName; }
    public TextElement getTitleText() { return titleText; }
    public TextElement getSubtitleText() { return subtitleText; }
    public TextElement getHelpText() { return helpText; }
    public TextElement getLegendText() { return legendText; }
    public TextElement getFullBackdrop() { return fullBackdrop; }
    public TextElement getBoardBackdrop() { return boardBackdrop; }
    public TextElement getPanelBackdrop() { return panelBackdrop; }

    // SETTERS
    public void setState(int state) { this.state = state; }
    public void setBoard(TablutBoard board) {
        this.board = board;
        addContainer(board);
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
    public void setTitleText(TextElement titleText) {
        this.titleText = titleText;
        addElement(titleText);
    }
    public void setSubtitleText(TextElement subtitleText) {
        this.subtitleText = subtitleText;
        addElement(subtitleText);
    }
    public void setHelpText(TextElement helpText) {
        this.helpText = helpText;
        addElement(helpText);
    }
    public void setLegendText(TextElement legendText) {
        this.legendText = legendText;
        addElement(legendText);
    }
    public void setFullBackdrop(TextElement fullBackdrop) {
        this.fullBackdrop = fullBackdrop;
        addElement(fullBackdrop);
    }
    public void setBoardBackdrop(TextElement boardBackdrop) {
        this.boardBackdrop = boardBackdrop;
        addElement(boardBackdrop);
    }
    public void setPanelBackdrop(TextElement panelBackdrop) {
        this.panelBackdrop = panelBackdrop;
        addElement(panelBackdrop);
    }


    private void setupCallbacks() {
        onSelectionChange(() -> {
            if (selected.size() == 0) {
                board.resetReachableCells(false);
                return;
            }
            Pawn pawn = (Pawn) selected.getFirst();
            board.setValidCells(pawn.getNumber());
        });

        onMoveInContainer((el, gridDest, rowDest, colDest) -> {
            if (gridDest != board) return;
            computePartyResult();
        });
    }

    public int checkCapture(boolean isYellow, int colSrc, int colDest, int rowSrc, int rowDest) {
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


            int dstY1 = rowDest + dy;
            int dstX1 = colDest + dx;

            int dstY2 = rowDest + 2*dy;
            int dstX2 = colDest + 2*dx;

            // check bounds for pawn 2 squares away
            if (dstY2 < 0 || dstY2 >= 9) continue;
            if (dstX2 < 0 || dstX2 >= 9) continue;


            GameElement sideEl = getBoard().getElement(dstY1, dstX1);
            GameElement sideEl2 = getBoard().getElement(dstY2, dstX2);

            if ((sideEl instanceof Pawn sideP) && (sideEl2 instanceof Pawn sideP2)) {
                if (isYellow) {
                    if (sideP.getColor() == Pawn.PAWN_SOLDIER && sideP2.getColor() == Pawn.PAWN_MOSCOVITE) {
                        return dstY1 * 9 + dstX1;
                    }
                } else {
                    if (sideP.getColor() == Pawn.PAWN_MOSCOVITE && sideP2.getColor() != Pawn.PAWN_MOSCOVITE) {
                        return dstY1 * 9 + dstX1;
                    }
                }
            }
        }

        return -1;
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
        int maxDistance = 8;
        if (RuleSets.isConstrainedKingMoves()) {
            maxDistance = 4;
        }
        for (int i = 0; i < 4; i++) {
            int y = kingY;
            int x = kingX;
            boolean isFreeWay = true;

            for (int j = 1; j <= maxDistance; j++) {
                y += dy_vals[i];
                x += dx_vals[i];

                if (x == 0 || x == 8 || y == 0 || y == 8) { // king on edge
                    if (RuleSets.isCornerKingEscapes() && !RecurBoard.cornerSquares.contains(y * 9 + x)) {
                        isFreeWay = false;
                        break;
                    } else if (RuleSets.isConstrainedKingSquares() && RecurBoard.constrainedKingSquares.contains(y * 9 + x)) {
                        isFreeWay = false;
                        break;
                    }
                }

                if (y < 0 || y > 8 || x < 0 || x > 8) break;

                if (getBoard().getElement(y, x) instanceof Pawn) {
                    isFreeWay = false;
                    break;
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
            if (RuleSets.isConstrainedKingSquares() || RuleSets.isCornerKingEscapes()) {
                if (RuleSets.isCornerKingEscapes()) {
                    if (RecurBoard.cornerSquares.contains(kingY * 9 + kingX)) {
                        idWinner = 0;
                        winMessage = "king reached a corner";
                    }
                } else if (RuleSets.isConstrainedKingSquares()) {
                    if (!RecurBoard.constrainedKingSquares.contains(kingY * 9 + kingX)) {
                        idWinner = 0;
                        winMessage = "king reached an edge";
                    }
                }
            } else {
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
