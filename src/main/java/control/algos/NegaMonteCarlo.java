package control.algos;


import model.TablutBoard;
import model.TablutStageModel;

import java.util.*;

/**
 *    The weakest thing in pure MCTS (MonteCarlo) is step 3 (simulation). as tablut has very
 * "sudden-death" like conditions (for example the king goes to an edge or gets surrounded), a simulation
 * making completely random moves is very blind. it very well might miss a 1-move win or 1-move loss, which
 * completely biases the win/loss stats.
 *    This is where Negamax comes : instead of running a long and vry chaotic random playout until a game over,
 * we can stop early and use Negamax to evaluate the board.
 *
 * Instead of simulating until a random win/loss, we change step 3 like this :
 *    1. from the new expanded node, instead of running a random game, we execute a simple Negamax search(
 *       (depth 2-3)
 *    2. the search returns a relative score
 *    3. we map this score to a simulated value between 0.0 (forced loss) and 1.0 (forced win).
 *       a nice and common way to do map a large range of such score to a probability is using a
 *       sigmoig function like in logistic regression (ML) : https://developers.google.com/machine-learning/crash-course/logistic-regression/sigmoid-function
 *       sim value = 1 / (1 + exp(-score))
 *    4. instead of backpropagating a strict int (0 or 1) we backpropagate this fractional score up the tree
 *
 *
 * This is much better for tablut because :
 *    - if the new expanded node allows a sneaky and vicious king escape while swedish has 2 pawns remaining 2 moves
 *      down the line, a random simulation will certainly miss it, when the depth-2/3 negamax will spawn kill it,
 *      return a massively crushing score (MAX_VALUE ~ 1e7) and propagate an almost perfect 1.0 value,
 *      which MCTS will recognize as very valuable
 *    - running a random simulation of tablut could take 120+ moves, when a depth-2/3 nega takes the blink of an eye.
 *    - if the evaluation function used by negamax is not perfectly tuned for tablut (which is a
 *      pretty complicated game, by the way, and can get very tactical very quickly), it doesn't matter as much as in the
 *      actual negamax search alone. negamax ensures immediate survival while MCTS handles the long term strategy
 *      of surrounding the king or opening ways to the edges.
 *
 *
 *  TL;DR:
 *      this is MonteCarlo, but better. sorry damien ;)
 */



public class NegaMonteCarlo {

    // constant that balances exploitation vs exploration (win/visits ratio vs visits/parent visits "ratio")
    public static final float C = (float) Math.sqrt(2);

    // constant that balances (exploitation and exploration) vs prior score (which children look more promising)
    public static final float W = 0.5f;

    private static final int MAX_SEARCH_PLY = 256;
    private static final int MAX_PATH_LEN = 256;



    private static long timeLimitMs;
    private static int negamaxDepth;
    private static int ruleSet;



    /**
     *  Use static reusable buffers across a game to avoid reallocating them each time we call a decide()
     *  Allocated once, have to zeroed out before calling findBestMove()
     */

    private static final Random rng = new Random(12345);

    private static final byte[] board               = new byte [81];
    private static final byte[] kingPosStack        = new byte [MAX_SEARCH_PLY + 1];
    private static final byte[] captureCountStack   = new byte [MAX_SEARCH_PLY + 1];
    private static final int[]  moveCountStack      = new int  [MAX_SEARCH_PLY];
    private static final byte[] materialDiffStack   = new byte [MAX_SEARCH_PLY + 1];

    private static final short[][] captureStack     = new short[MAX_SEARCH_PLY + 1][NegamaxSearchFast.MAX_CAPTURES];
    private static final int[][]   movesStack       = new int  [MAX_SEARCH_PLY + 1][NegamaxSearchFast.NB_POSSIBLE_MOVES];
    private static final int[][]   killerMovesStack = new int  [MAX_SEARCH_PLY + 1][2];

    private static final long[][] zobrist           = new long[4][81];
    private static final long[]   zobristKey        = new long[1];
    private static final long     sideToMove        = rng.nextLong();


    // currently (8 + 4 + 1 + 1 + 4) * (1 << 20) ~= 14.6MB transposition table
    private static final long[]  ttHash             = new long[NegamaxSearchFast.NB_TT_ENTRIES];
    private static final float[] ttScore            = new float[NegamaxSearchFast.NB_TT_ENTRIES];
    private static final byte[]  ttDepth            = new byte[NegamaxSearchFast.NB_TT_ENTRIES];
    private static final byte[]  ttFlag             = new byte[NegamaxSearchFast.NB_TT_ENTRIES];
    private static final int[]   ttBestMove         = new int[NegamaxSearchFast.NB_TT_ENTRIES];


    private static class Node {
        Node parent;
        int moveFromParent;
        int turn;
        int ply;

        int[] legalMoves;
        Node[] children;
        boolean isTerminal;

        int expandedChildren;
        int visits;  // n
        float wins; // double to support fractional scores from negamax


        public Node(Node parent, int moveFromParent, int turn, int ply, int[] legalMoves, boolean isTerminal) {
            this.parent = parent;
            this.moveFromParent = moveFromParent;
            this.turn = turn;
            this.ply = ply;
            this.legalMoves = legalMoves;
            this.children = new Node[legalMoves.length];
            this.isTerminal = isTerminal;
            this.expandedChildren = 0;
            this.visits = 0;
            this.wins = 0.0f;
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

        /** going from depth 2 -> 3 gives much better information to the backprop
         *  the tradeoff is that we do less iterations but high-quality ones beat shallow evaluations in a tactical game like tablut
         */
        negamaxDepth = switch (level) {
            case 1,2,3,4,5 -> 2;
            case 6,7,8,9,10 -> 2;
            default -> 2;
        };


        TablutStageModel stageModel = (TablutStageModel) tablutBoard.getModel().getGameStage();
        ruleSet = stageModel.getRuleSet();


        for (int piece : FastBoard.pieceTypes) {
            for (int i =0 ; i < 81; i++ ){
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

        for (short[] cap : captureStack) Arrays.fill(cap, (short) 0);
        for (int[] moves : movesStack) Arrays.fill(moves, 0);
        for (long[] squares : zobrist) Arrays.fill(squares, 0);
        for (int[] moves : killerMovesStack) Arrays.fill(moves, 0);

        Arrays.fill(ttHash, 0);
        Arrays.fill(ttScore, 0);
        Arrays.fill(ttDepth, (byte) 0);
        Arrays.fill(ttFlag, (byte) 0);
        Arrays.fill(ttBestMove, 0);

        zobristKey[0] = 0;
    }




    public static int findBestMove(TablutBoard tablutBoard, int turn, boolean findAlternativeMove) {
        byte[] rootBoard = FastBoard.fromTablutBoard(tablutBoard);
        int kingPos = FastBoard.getKingPos(rootBoard);
        int materialDiff = FastEvaluation.countMaterialDiff(rootBoard);
        return findBestMove(rootBoard, kingPos, materialDiff, turn, findAlternativeMove);
    }

    public static int findBestMove(byte[] rootBoard, int kingPos, int materialDiff, int turn, boolean findAlternativeMove) {
        loadRootState(rootBoard, kingPos, materialDiff);

        Node root = createNode(null, -1, turn, 0);
        if (root.isTerminal || root.legalMoves.length == 0) {
            return -1;
        }

        long startTimeMillis = System.currentTimeMillis();
        int nbEvals = 0;

        int[] pathMoves = new int[MAX_PATH_LEN];

        while (System.currentTimeMillis() - startTimeMillis <= timeLimitMs) {
            Node currentNode = root;
            int ply = 0;
            int pathLen = 0;

            // 1. Selection
            while (currentNode.isFullyExpanded() && !currentNode.isTerminal) {
                int childIdx = selectChildIndex(currentNode);
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
                int expandIdx = pickUnexpandedIndex(currentNode);
                int move = currentNode.legalMoves[expandIdx];

                applyMove(move, ply);
                pathMoves[pathLen++] = move;

                Node child = createNode(currentNode, move, (currentNode.turn + 1) % 2, ply + 1);
                currentNode.children[expandIdx] = child;
                currentNode.expandedChildren++;
                currentNode = child;
                ply++;
            }

            // 3. Simulation using shallow fast negamax from the current leaf
            double score = evaluateLeaf(currentNode, ply);
            nbEvals++;

            // 4. Backpropagation
            double result = score; // probability that currentNode.turn wins
            Node back = currentNode;
            while (back != null) {
                back.visits++;
                back.wins += result;
                result = 1.0 - result;
                back = back.parent;
            }

            // 5. Undo to root
            for (int i = pathLen - 1; i >= 0; i--) {
                undoMove(pathMoves[i], i);
            }
        }

        SearchResult best = chooseBestRootMove(root, findAlternativeMove);
        System.out.printf("evaluated %d positions%n", nbEvals);
        return best.move;
    }

    private static void loadRootState(byte[] rootBoard, int kingPos, int materialDiff) {
        System.arraycopy(rootBoard, 0, board, 0, 81);

        kingPosStack[0] = (byte) kingPos;
        materialDiffStack[0] = (byte) materialDiff;
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

        FastBoard.generateMoves(board, turn, ply, moveCountStack, movesStack, killerMovesStack, ruleSet);
        int legalCount = moveCountStack[ply];
        int[] legalMoves = Arrays.copyOf(movesStack[ply], legalCount);

        boolean terminal = legalCount == 0;
        return new Node(parent, moveFromParent, turn, ply, legalMoves, terminal);
    }

    private static int selectChildIndex(Node node) {
        float bestScore = Float.NEGATIVE_INFINITY;
        int bestIdx = -1;
        for (int i = 0; i < node.legalMoves.length; i++) {
            Node child = node.children[i];
            if (child == null || child.visits == 0) {
                return i;
            }

            /** W add a prior policy score like AlphaZero does :
             *  a light score that biases which children get selected
             *  it can be sth cheap :
             *     - does the move threaten capture ? (for both sides)
             *     - does the move open an escape path ? (for swedish)
             *     - does the move close an escape path ? (for moscovite)
             *     - etc
             *  calculated fast (preferably O(1)) and applied at every selection step in the loop
             */
            float exploit = 1.0f - (child.wins / (float) child.visits);
            float explore = (float) (C * Math.sqrt(Math.log(Math.max(1, node.visits)) / child.visits));
            float prior = (float) (W * (movePrior(node, node.legalMoves[i]) / (1.0 + child.visits)));
            float score = exploit + explore + prior;

            if (score >= bestScore) {
                bestScore = score;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private static int pickUnexpandedIndex(Node node) {
        int remaining = node.legalMoves.length - node.expandedChildren;
        if (remaining <= 0) {
            return -1;
        }

        int target = rng.nextInt(remaining);
        for (int i = 0; i < node.legalMoves.length; i++) {
            if (node.children[i] == null) {
                if (target == 0) {
                    return i;
                }
                target--;
            }
        }

        return -1;
    }

    private static double movePrior(Node node, int move) {
        float prior = 0.0f;
        if (FastBoard.isCapture(board, move, node.ply)) {
            prior += 0.5f;
        }
        return prior;
    }

    private static void applyMove(int move, int ply) {
        FastBoard.checkCaptures(board, move, ply, captureCountStack, captureStack);
        kingPosStack[ply + 1] = kingPosStack[ply];
        materialDiffStack[ply + 1] = materialDiffStack[ply];

        FastBoard.makeMove(
                board, move, ply, captureCountStack, captureStack,
                materialDiffStack, kingPosStack, zobrist, zobristKey, sideToMove
        );

        // Fix the multi-capture material update so the fast path stays exact.
        int diff = materialDiffStack[ply];
        for (int i = 0; i < captureCountStack[ply]; i++) {
            int capPiece = captureStack[ply][i] & 0x07;
            if (capPiece == FastBoard.MOSCOVITE) {
                diff += 2;
            } else if (capPiece == FastBoard.SWEDISH) {
                diff -= 4;
            }
        }
        materialDiffStack[ply + 1] = (byte) diff;
    }

    private static void undoMove(int move, int ply) {
        FastBoard.undoMove(
                board, move, ply, captureCountStack, captureStack,
                materialDiffStack, kingPosStack, zobrist, zobristKey, sideToMove
        );
    }

    private static SearchResult chooseBestRootMove(Node root, boolean findAlternativeMove) {
        if (root.legalMoves.length == 0) {
            return new SearchResult(-1, Float.NEGATIVE_INFINITY, 0);
        }

        int bestIdx = -1;
        int secondIdx = -1;
        int bestVisits = -1;
        int secondVisits = -1;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < root.legalMoves.length; i++) {
            Node child = root.children[i];
            if (child == null) {
                continue;
            }

            int visits = child.visits;
            double score = visits == 0 ? Double.NEGATIVE_INFINITY : child.wins / (double) visits;

            if (visits > bestVisits || (visits == bestVisits && score > bestScore)) {
                secondIdx = bestIdx;
                secondVisits = bestVisits;
                bestIdx = i;
                bestVisits = visits;
                bestScore = score;
            } else if (visits > secondVisits) {
                secondIdx = i;
                secondVisits = visits;
            }
        }

        int chosenIdx = bestIdx;
        if (findAlternativeMove && secondIdx >= 0) {
            chosenIdx = secondIdx;
        }

        int chosenMove = chosenIdx >= 0 ? root.legalMoves[chosenIdx] : -1;
        float chosenScore = chosenIdx >= 0 && root.children[chosenIdx] != null
                ? (float) (root.children[chosenIdx].wins / Math.max(1.0, root.children[chosenIdx].visits))
                : Float.NEGATIVE_INFINITY;
        int chosenVisits = chosenIdx >= 0 && root.children[chosenIdx] != null ? root.children[chosenIdx].visits : 0;

        return new SearchResult(chosenMove, chosenScore, chosenVisits);
    }

    private static float evaluateLeaf(Node leaf, int ply) {
        int depthBonus = 0;
        if (leaf.turn == 0) {
            int nbMoscovitesIn5x5Region = FastEvaluation.countMoscovitesIn5x5Region(board, ply, kingPosStack);
            if (nbMoscovitesIn5x5Region == 4 || nbMoscovitesIn5x5Region == 5) {
                depthBonus += 1;
            }
            if (nbMoscovitesIn5x5Region < 4) {
                depthBonus += 2;
            }
        } else {
            double kingEncerclement = FastEvaluation.countKingEncerclement(board, ply, kingPosStack);
            if (kingEncerclement <= -20) {
                depthBonus += 1;
            }
            if (kingEncerclement <= -30) {
                depthBonus += 1;
            }
        }

        int rolloutDepth = negamaxDepth + depthBonus;
        float negaScore = negamax(rolloutDepth, rolloutDepth, ply, leaf.turn,
                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

        double temp = leaf.turn == 0 ? 80.0 : 120.0;
        return (float) (1.0 / (1.0 + Math.exp(-negaScore / temp)));
    }

    private static float negamax(int depth, int rootDepth, int ply, int turn, float alpha, float beta) {
        float win = FastBoard.checkWin(board, ply, kingPosStack, ruleSet);

        int ttIndex = (int) (zobristKey[0] & (NegamaxSearchFast.NB_TT_ENTRIES - 1));
        if (ttFlag[ttIndex] != 0 && ttHash[ttIndex] == zobristKey[0] && ttDepth[ttIndex] >= depth) {
            if (ttFlag[ttIndex] == NegamaxSearchFast.EXACT) {
                return ttScore[ttIndex];
            } else if (ttFlag[ttIndex] == NegamaxSearchFast.LOWER_BOUND) {
                alpha = Math.max(alpha, ttScore[ttIndex]);
            } else if (ttFlag[ttIndex] == NegamaxSearchFast.UPPER_BOUND) {
                beta = Math.min(beta, ttScore[ttIndex]);
            }

            if (alpha >= beta) {
                return ttScore[ttIndex];
            }
        }

        if (win != 0 || depth == 0) {
            return FastEvaluation.evaluate(board, turn, ply, depth, rootDepth, materialDiffStack, kingPosStack, ruleSet);
        }

        float maxScore = Float.NEGATIVE_INFINITY;
        FastBoard.generateMoves(board, turn, ply, moveCountStack, movesStack, killerMovesStack, ruleSet);

        if (moveCountStack[ply] == 0) {
            return -FastEvaluation.VIRTUAL_INF + depth;
        }

        byte flag;
        float alphaOrig = alpha;

        for (int i = 0; i < moveCountStack[ply]; i++) {
            int move = movesStack[ply][i];

            applyMove(move, ply);
            float score = -negamax(depth - 1, rootDepth, ply + 1, (turn + 1) % 2, -beta, -alpha);
            undoMove(move, ply);

            if (score > maxScore) {
                maxScore = score;
            }
            alpha = Math.max(alpha, score);

            if (alpha >= beta) {
                ttScore[ttIndex] = score;
                ttHash[ttIndex] = zobristKey[0];
                ttDepth[ttIndex] = (byte) depth;
                ttFlag[ttIndex] = (byte) NegamaxSearchFast.LOWER_BOUND;
                ttBestMove[ttIndex] = move;
                break;
            }
        }

        if (maxScore <= alphaOrig) {
            flag = (byte) NegamaxSearchFast.UPPER_BOUND;
        } else if (maxScore >= beta) {
            flag = (byte) NegamaxSearchFast.LOWER_BOUND;
        } else {
            flag = (byte) NegamaxSearchFast.EXACT;
        }

        ttScore[ttIndex] = maxScore;
        ttHash[ttIndex] = zobristKey[0];
        ttDepth[ttIndex] = (byte) depth;
        ttFlag[ttIndex] = flag;
        ttBestMove[ttIndex] = 0;

        return maxScore;
    }
}
