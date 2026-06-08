package control.algos;


import model.TablutBoard;
import model.TablutStageModel;

import java.util.*;




/**
 * MCTS (monte-carlo tree search) builds an asymmetric tree over time, by repeating a 4-step cycle,
 * for as many iterations as the time/iterations limit allows (for example 20000 iterations per turn or 3 seconds max).
 * Each node in teh MCTS tree has two number : w (the number of simulation wins) and n (the number of times
 * that node has been visited).
 *
 * the 4 steps in the MCTS :
 *
 * 1. Selection:
 *      start at the root node (current board state), look at its child nodes (legal moves) and pick the "best" one.
 *      you repeat this down the tree until you hit a node that hasn't fully expanded (meaning that is has no legal
 *      moves that haven't been turned into child nodes yet)
 *        - to choose the best child, we use the formula called UCB1 (upper confidence bound) :
 *              UCB1 = (w/n) + c * sqrt(ln(N) / n)
 *        - why this formula ? the left side (w/n) prioritizes exploitation (picking moves that have won a lot before)
 *          and the right side prioritizes exploration (picking moves that haven't been visited much yet, where N
 *          is the parent's node total visits).
 *          the c constant balances the two (usually around sqrt(2))
 *
 * 2. Expansion :
 *      once you reach a node that has unvisited moves, you randomly pick one of those legal moves, create a new
 *      child node for it, and step into it.
 *
 * 3. Simulation :
 *      from this new mode, the bot plays a fast game all the way to the end. in pure MCTS, both sides just make
 *      completely random moves until someone wins or loses (which can be REALLY long in tablut)
 *
 * 4. Backpropagation :
 *      once the simulated game ends, you walk up backwards up the path you took from the root node.
 *      you increment the visit count n for every node you passed through and you add a win point w
 *      to every node that belonged to the winning side.
 *
 *  Once the timer runs out, you look at the root node's first children and choose the move with the highest visit count n
 *  (not highest win rate w, as visit count is statistically more stable)
 */

public class MonteCarlo {

    // constant that balances exploitation vs exploration (win/visits ratio vs visits/parent visits "ratio")
    public static final float C = 0.4f;

    // constant that balances (exploitation and exploration) vs prior score (which children look more promising)
    public static final float W = 1.5f;

    // leaf negamax scores are mapped to [0,1] with a soft scale so the tree keeps
    // meaningful separation between "good", "great", and "catastrophic".
    private static final float LEAF_SCORE_SCALE = 500f;
    private static final int   MAX_SEARCH_PLY   = 1024;
    private static final int   MAX_PATH_LEN     = 512;


    private static long timeLimitMs;
    private static int  ruleSet;
    private static int  nbEvals;


    /**
     *  Use static reusable buffers across a game to avoid reallocating them each time we call a decide()
     *  Allocated once, have to zeroed out before calling findBestMove()
     */

    private static final Random rng = new Random(12345);

    private static final byte[]    board               = new byte  [81];
    private static final byte[]    kingPosStack        = new byte  [MAX_SEARCH_PLY + 1];
    private static final byte[]    captureCountStack   = new byte  [MAX_SEARCH_PLY + 1];
    private static final int[]     moveCountStack      = new int   [MAX_SEARCH_PLY];
    private static final byte[]    materialDiffStack   = new byte  [MAX_SEARCH_PLY + 1];
    private static final byte[]    moscoviteCountStack = new byte  [MAX_SEARCH_PLY + 1];
    private static final byte[]    soldierCountStack   = new byte  [MAX_SEARCH_PLY + 1];
    private static final int []    selectedMoveIndexStack = new int[MAX_SEARCH_PLY + 1];

    private static final short[][] captureStack        = new short [MAX_SEARCH_PLY + 1][Negamax.MAX_CAPTURES];
    private static final int[][]   movesStack          = new int   [MAX_SEARCH_PLY + 1][Negamax.NB_POSSIBLE_MOVES];
    private static final int[][]   killerMovesStack    = new int   [MAX_SEARCH_PLY + 1][2];

    // not used here, just kept for compatibility with FastBoard methods
    private static final long[][]  zobrist             = new long  [4][81];
    private static final long[]    zobristKey          = new long  [1];
    private static final long      sideToMove          = rng.nextLong();


    private static class Node {
        Node parent;
        int  moveFromParent;
        int  turn;
        int  ply;

        int[]   legalMoves;
        Node[]  children;
        boolean isTerminal;

        int   expandedChildren;
        int   visits;  // n
        int   wins; // double to support fractional scores from negamax


        public Node(Node parent, int moveFromParent, int turn, int ply, int[] legalMoves, boolean isTerminal) {
            this.parent           = parent;
            this.moveFromParent   = moveFromParent;
            this.turn             = turn;
            this.ply              = ply;
            this.legalMoves       = legalMoves;
            this.children         = new Node[legalMoves.length];
            this.isTerminal       = isTerminal;
            this.expandedChildren = 0;
            this.visits           = 0;
            this.wins             = 0;
        }

        boolean isFullyExpanded() {
            return expandedChildren >= legalMoves.length;
        }
    }

    private record SearchResult(int move, float score, int visits) {}




    // level between 0 and 10
    public static void configure(int level, TablutBoard tablutBoard) {
        timeLimitMs = switch (level) {
            case  0 ->   500;
            case  1 ->  1000;
            case  2 ->  2000;
            case  3 ->  3000;
            case  4 ->  4000;
            case  5 ->  5000;
            case  6 ->  7000;
            case  7 ->  8500;
            case  8 -> 10000;
            case  9 -> 12500;
            case 10 -> 15000;
            default ->  5000;
        };

        TablutStageModel stageModel = (TablutStageModel) tablutBoard.getModel().getGameStage();
        ruleSet = stageModel.getRuleSet();

        for (int piece : FastBoard.pieceTypes) {
            for (int i = 0; i < 81; i++) {
                zobrist[piece][i] = rng.nextLong();
            }
        }
        zobristKey[0] = 0;
    }


    public static void resetBuffers() {
        Arrays.fill(board, (byte) 0);
        Arrays.fill(kingPosStack, (byte) 0);
        Arrays.fill(captureCountStack, (byte) 0);
        Arrays.fill(moveCountStack, 0);
        Arrays.fill(materialDiffStack, (byte) 0);
        Arrays.fill(moscoviteCountStack, (byte) 0);
        Arrays.fill(soldierCountStack, (byte) 0);
        Arrays.fill(selectedMoveIndexStack, (byte) 0);

        for (short[] cap : captureStack) Arrays.fill(cap, (short) 0);
        for (int[] moves : movesStack) Arrays.fill(moves, 0);
        for (int[] moves : killerMovesStack) Arrays.fill(moves, 0);

        zobristKey[0] = 0;
    }




    public static int findBestMove(TablutBoard tablutBoard, int turn, boolean findAlternativeMove) {
        byte[] rootBoard  = FastBoard.fromTablutBoard(tablutBoard);
        int kingPos       = FastBoard.getKingPos(rootBoard);
        int materialDiff  = FastEvaluation.countMaterialDiff(rootBoard);

        return findBestMove(rootBoard, kingPos, materialDiff, turn, findAlternativeMove);
    }

    public static int findBestMove(byte[] rootBoard, int kingPos, int materialDiff, int turn, boolean findAlternativeMove) {
        loadRootState(rootBoard, kingPos, materialDiff);

        Node root = createNode(null, -1, turn, 0);
        if (root.isTerminal || root.legalMoves.length == 0) {
            return -1;
        }

        long startTimeMillis = System.currentTimeMillis();
        nbEvals = 0;

        int[] pathMoves = new int[MAX_PATH_LEN];

        while (System.currentTimeMillis() - startTimeMillis <= timeLimitMs) {
            Node currentNode = root;
            int ply = 0;
            int pathLen = 0;

            // 1. Selection
            while (currentNode.isFullyExpanded() && !currentNode.isTerminal) {
                int childIdx    = 0;
                float bestScore = Float.NEGATIVE_INFINITY;
                int bestIdx     = -1;

                for (int i = 0; i < currentNode.legalMoves.length; i++) {
                    Node child = currentNode.children[i];
                    if (child == null || child.visits == 0) {
                        bestIdx = i;
                        break;
                    }


                    float exploit = 1f - child.wins / (float) child.visits;
                    float explore = (float) (C * Math.sqrt(Math.log(Math.max(1, currentNode.visits)) / child.visits));
                    float prior   = (float) (W * (movePrior(currentNode, currentNode.legalMoves[i], ply) / (1.0 + child.visits)));
                    float score   = exploit + explore + prior;

                    if (score >= bestScore) {
                        bestScore = score;
                        bestIdx = i;
                    }
                }
                childIdx = bestIdx;

                if (childIdx < 0) {
                    break;
                }

                int move = currentNode.legalMoves[childIdx];
                applyMove(move, ply);
                pathMoves[pathLen++] = move;

                currentNode = currentNode.children[childIdx];
                ply++;
            }

            // 2. Expansion
            if (!currentNode.isTerminal && currentNode.expandedChildren < currentNode.legalMoves.length) {
                int remaining = currentNode.legalMoves.length - currentNode.expandedChildren;
                if (remaining <= 0) {
                    return -1;
                }
                int expandIdx = 0;

                for (int i = 0; i < currentNode.legalMoves.length; i++) {
                    if (currentNode.children[i] == null) {
                        expandIdx = i;
                        break;
                    }
                }

                int move = currentNode.legalMoves[expandIdx];

                applyMove(move, ply);
                pathMoves[pathLen++] = move;

                Node child = createNode(currentNode, move, (currentNode.turn + 1) % 2, ply + 1);
                currentNode.children[expandIdx] = child;
                currentNode.expandedChildren++;
                currentNode = child;
                ply++;
            }

            // 3. Simulation
            int winningTurn = simulateRollout(currentNode.turn, ply);
            nbEvals++;


            // 4. Backpropagation
            Node back = currentNode;
            while (back != null) {
                back.visits++;
                if (back.turn == winningTurn) {
                    back.wins++;
                }
                back = back.parent;
            }

            // 5. Undo to root
            for (int i = pathLen - 1; i >= 0; i--) {
                undoMove(pathMoves[i], i);
            }
        }

        SearchResult best = chooseBestMove(root, findAlternativeMove);
//        System.out.printf("performed %d iterations\n", nbEvals);
        return best.move;
    }

    private static void loadRootState(byte[] rootBoard, int kingPos, int materialDiff) {
        System.arraycopy(rootBoard, 0, board, 0, 81);

        kingPosStack[0]         = (byte) kingPos;
        materialDiffStack[0]    = (byte) materialDiff;
        moscoviteCountStack[0]  = FastEvaluation.countPieces(board, FastBoard.MOSCOVITE);
        soldierCountStack[0]    = FastEvaluation.countPieces(board, FastBoard.SWEDISH);

        for (int i = 0; i < 81; i++) {
            if (board[i] != FastBoard.EMPTY) {
                zobristKey[0] ^= zobrist[board[i]][i];
            }
        }
        zobristKey[0] ^= sideToMove;
    }

    private static Node createNode(Node parent, int moveFromParent, int turn, int ply) {
        float win = FastBoard.checkWin(board, ply, kingPosStack, ruleSet);

        if (win != 0) {
            return new Node(parent, moveFromParent, turn, ply, new int[0], true);
        }

        FastBoard.generateMoves(board, turn, ply, moveCountStack, movesStack, killerMovesStack, 0, false, ruleSet);
        int legalCount   = moveCountStack[ply];
        int[] legalMoves = Arrays.copyOf(movesStack[ply], legalCount);

        boolean terminal = legalCount == 0;
        return new Node(parent, moveFromParent, turn, ply, legalMoves, terminal);
    }



    private static double movePrior(Node node, int move, int ply) {
        float prior = 0.0f;
        if (FastBoard.isCapture(board, move, node.ply, ruleSet)) {
            prior += 0.5f;
        }


        boolean kingCanEscape = FastEvaluation.kingCanEscapeIn1(board, kingPosStack[ply], ruleSet);
        if (node.turn == 0) {  // Swedish: reward moves that keep/create escape
            int dst = (move >> 7) & 0x7F;
            if (dst == 0 || dst == 8 || dst == 72 || dst == 80 ||    // corner escape
                    dst / 9 == 0 || dst / 9 == 8 || dst % 9 == 0 || dst % 9 == 8) { // edge escape
                prior += 2.0f;  // this IS a king escape move
            }
        } else {  // Moscovite: reward blocking moves when king threatens escape
            if (kingCanEscape) {
                applyMove(move, ply);
                boolean stillEscapes = FastEvaluation.kingCanEscapeIn1(board, kingPosStack[ply + 1], ruleSet);
                undoMove(move, ply);
                if (!stillEscapes) prior += 2.5f;   // successfully blocks escape
                else               prior -= 0.5f;    // doesn't block, penalise
            }
        }


        return prior;
    }

    private static void applyMove(int move, int ply) {
        FastBoard.checkCaptures(board, move, ply, captureCountStack, captureStack, ruleSet);
        kingPosStack[ply+1]         = kingPosStack[ply];
        materialDiffStack[ply+1]    = materialDiffStack[ply];
        moscoviteCountStack[ply+1]  = moscoviteCountStack[ply];
        soldierCountStack[ply+1]    = soldierCountStack[ply];

        FastBoard.makeMove(
                board, move, ply, captureCountStack, captureStack,
                materialDiffStack, soldierCountStack, moscoviteCountStack, kingPosStack, zobrist, zobristKey, sideToMove
        );
    }

    private static void undoMove(int move, int ply) {
        FastBoard.undoMove(
                board, move, ply, captureCountStack, captureStack,
                materialDiffStack, kingPosStack, zobrist, zobristKey, sideToMove
        );
    }

    private static SearchResult chooseBestMove(Node root, boolean findAlternativeMove) {
        if (root.legalMoves.length == 0) {
            return new SearchResult(-1, Float.NEGATIVE_INFINITY, 0);
        }

        int bestIdx       = -1;
        int secondIdx     = -1;
        int bestVisits    = -1;
        int secondVisits  = -1;

        for (int i = 0; i < root.legalMoves.length; i++) {
            Node child = root.children[i];
            if (child == null) continue;
            if (child.visits > bestVisits) {
                secondIdx     = bestIdx;
                secondVisits  = bestVisits;
                bestIdx       = i;
                bestVisits    = child.visits;
            } else if (child.visits > secondVisits) {
                secondIdx    = i;
                secondVisits = child.visits;
            }
        }

        int chosenIdx = bestIdx;
        if (findAlternativeMove && secondIdx >= 0) {
            chosenIdx = secondIdx;
        }

        int chosenMove    = chosenIdx >= 0 ? root.legalMoves[chosenIdx] : -1;
        float chosenScore = chosenIdx >= 0 && root.children[chosenIdx] != null
                ? (float) (1.0 - root.children[chosenIdx].wins / Math.max(1.0, root.children[chosenIdx].visits))
                : Float.NEGATIVE_INFINITY;
        int chosenVisits  = chosenIdx >= 0 && root.children[chosenIdx] != null ? root.children[chosenIdx].visits : 0;

        return new SearchResult(chosenMove, chosenScore, chosenVisits);
    }


    private static int simulateRollout(int turn, int ply) {
        int winningTurn = -1;

        int currTurn = turn;
        int currPly = ply;
        while (true) {
            float win = FastBoard.checkWin(board, currPly, kingPosStack, ruleSet);
            if (win != 0) {
                winningTurn = win > 0 ? 0 : 1;
                break;
            } else if (currPly - ply > MAX_SEARCH_PLY) {
                winningTurn = -1;
            }

            FastBoard.generateMoves(board, currTurn, currPly, moveCountStack, movesStack, killerMovesStack, 0, false, ruleSet);
            if (moveCountStack[currPly] == 0) {
                winningTurn = (currTurn + 1) % 2;
                break;
            }

            int moveIndex = (int) (Math.random() * moveCountStack[currPly]);
            int move = movesStack[currPly][moveIndex];
            selectedMoveIndexStack[currPly] = moveIndex;

            applyMove(move, currPly);
            currPly++;
            currTurn = (currTurn + 1) % 2;
        }

        for (int i = currPly; i > ply; i--) {
            undoMove(movesStack[i-1][selectedMoveIndexStack[i-1]], i-1);
        }

        return winningTurn;
    }
}