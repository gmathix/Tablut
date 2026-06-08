package control.algos;


import model.TablutBoard;
import model.TablutStageModel;

import java.util.*;

import static control.algos.Negamax.VIRTUAL_INF;



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
 *    - running a random simulation of tablut could take 120+ moves, when a depth-1/2/3 nega takes the blink of an eye.
 *    - if the evaluation function used by negamax is not perfectly tuned for tablut (which is a
 *      pretty complicated game, by the way, and can get very tactical very quickly), it doesn't matter as much as in the
 *      actual negamax search alone. negamax ensures immediate survival while MCTS handles the long term strategy
 *      of surrounding the king or opening ways to the edges.
 *
 *
 *  TL;DR:
 *      this is MonteCarlo, but better.:)
 */

public class NegaMonteCarlo {

    // constant that balances exploitation vs exploration (win/visits ratio vs visits/parent visits "ratio")
    public static final float C = 0.4f;

    // constant that balances (exploitation and exploration) vs prior score (which children look more promising)
    public static final float W = 1.5f;

    // leaf negamax scores are mapped to [0,1] with a soft scale so the tree keeps
    // meaningful separation between "good", "great", and "catastrophic".
    private static final float LEAF_SCORE_SCALE = 500f;
    private static final int   MAX_SEARCH_PLY   = 256;
    private static final int   MAX_PATH_LEN     = 256;


    private static long timeLimitMs;
    private static int  negamaxDepth;
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

    private static final short[][] captureStack        = new short [MAX_SEARCH_PLY + 1][Negamax.MAX_CAPTURES];
    private static final int[][]   movesStack          = new int   [MAX_SEARCH_PLY + 1][Negamax.NB_POSSIBLE_MOVES];
    private static final int[][]   killerMovesStack    = new int   [MAX_SEARCH_PLY + 1][2];

    // [FIX-3] zobrist is initialised ONCE in configure() and never touched by resetBuffers().
    private static final long[][]  zobrist             = new long  [4][81];
    private static final long[]    zobristKey          = new long  [1];
    private static final long      sideToMove          = rng.nextLong();


    // currently (8 + 4 + 1 + 1 + 4) * (1 << 20) ~= 14.6MB transposition table
    private static final long[]    ttHash              = new long  [Negamax.NB_TT_ENTRIES];
    private static final float[]   ttScore             = new float [Negamax.NB_TT_ENTRIES];
    private static final byte[]    ttDepth             = new byte  [Negamax.NB_TT_ENTRIES];
    private static final byte[]    ttFlag              = new byte  [Negamax.NB_TT_ENTRIES];
    private static final int[]     ttBestMove          = new int   [Negamax.NB_TT_ENTRIES];


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
        float wins; // float to support fractional scores from negamax


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
            this.wins             = 0.0f;
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

        negamaxDepth = 1;

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

        for (short[] cap : captureStack) Arrays.fill(cap, (short) 0);
        for (int[] moves : movesStack) Arrays.fill(moves, 0);
        for (int[] moves : killerMovesStack) Arrays.fill(moves, 0);

        Arrays.fill(ttHash, 0);
        Arrays.fill(ttScore, 0);
        Arrays.fill(ttDepth, (byte) 0);
        Arrays.fill(ttFlag, (byte) 0);
        Arrays.fill(ttBestMove, 0);

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

            // 3. Simulation using shallow fast negamax from the current leaf
            float score = evaluateLeaf(currentNode, ply);
            nbEvals++;


            // 4. Backpropagation
            int leafTurn = currentNode.turn;
            Node back = currentNode;
            while (back != null) {
                back.visits++;
                if (back.turn == leafTurn) {
                    back.wins += score;
                } else {
                    back.wins += 1 - score;
                }

                back = back.parent;
            }

            // 5. Undo to root
            for (int i = pathLen - 1; i >= 0; i--) {
                undoMove(pathMoves[i], i);
            }
        }

        SearchResult best = chooseBestMove(root, findAlternativeMove);
        System.out.printf("performed %d iterations\n", nbEvals);
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
        if (node.turn == 0) {
            int dst = (move >> 7) & 0x7F;
            if (dst == 0 || dst == 8 || dst == 72 || dst == 80 ||
                    dst / 9 == 0 || dst / 9 == 8 || dst % 9 == 0 || dst % 9 == 8) {
                prior += 2.0f;
            }
        } else {
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

    private static float evaluateLeaf(Node leaf, int ply) {
        int depthBonus = 0;
        if (leaf.turn == 0) {
            // search deeper for swedish when there are few moscovites near the king (potentially easier exit)
            int nbMoscovitesIn5x5Region = FastEvaluation.countMoscovitesIn5x5Region(board, ply, kingPosStack);
            if (nbMoscovitesIn5x5Region == 4 || nbMoscovitesIn5x5Region == 5) depthBonus += 1;
            if (nbMoscovitesIn5x5Region < 4)                                  depthBonus += 2;
        } else {
            // search deeper for moscovites when the king is under pressure
            double kingEncerclement = FastEvaluation.countKingEncerclement(board, ply, kingPosStack);
            if (kingEncerclement <= -4) depthBonus += 1;
            if (kingEncerclement <= -7) depthBonus += 1;
        }

        int bonusDepth = negamaxDepth + depthBonus;
        float negaScore = negamax(bonusDepth, bonusDepth, ply, leaf.turn,
                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

//        // map the leaf score to a smooth win probability (tanh, same idea as AlphaZero)
//        float normalized = (float) Math.tanh(negaScore / LEAF_SCORE_SCALE);
//        return Math.max(0f, Math.min(1f, .5f + .5f * normalized));

        return (float) (1.0 / (1.0 + Math.exp(-negaScore / 100)));
    }

    private static float negamax(int depth, int rootDepth, int ply, int turn, float alpha, float beta) {
        float win = FastBoard.checkWin(board, ply, kingPosStack, ruleSet);
        if (win > 0) return turn == 0 ? VIRTUAL_INF - ply : -VIRTUAL_INF + ply;
        if (win < 0) return turn == 1 ? VIRTUAL_INF - ply : -VIRTUAL_INF + ply;


        boolean hasTTBestMove = false;
        int bestTTMove        = 0;
        int ttIndex           = (int) (zobristKey[0] & (Negamax.NB_TT_ENTRIES - 1));

        if (ttFlag[ttIndex] != 0 && ttHash[ttIndex] == zobristKey[0] && ttDepth[ttIndex] >= depth) {
            switch (ttFlag[ttIndex]) {
                case Negamax.EXACT:
                    return ttScore[ttIndex];
                case Negamax.LOWER_BOUND:
                    alpha = Math.max(alpha, ttScore[ttIndex]);
                    break;
                case Negamax.UPPER_BOUND:
                    beta = Math.min(beta, ttScore[ttIndex]);
                    break;
            }
            if (alpha >= beta) return ttScore[ttIndex];

            bestTTMove = ttBestMove[ttIndex];
            if (bestTTMove != 0) hasTTBestMove = true;
        }

        if (depth == 0) {
            return FastEvaluation.evaluate(board, turn, ply, depth, rootDepth, materialDiffStack, soldierCountStack, moscoviteCountStack, kingPosStack, ruleSet);
        }

        float maxScore = Float.NEGATIVE_INFINITY;
        int   bestMove = 0;

        FastBoard.generateMoves(board, turn, ply, moveCountStack, movesStack, killerMovesStack, bestTTMove, hasTTBestMove, ruleSet);
        if (moveCountStack[ply] == 0) {
            return -FastEvaluation.VIRTUAL_INF + depth;
        }

        float alphaOrig = alpha;
        boolean cutoff  = false;

        for (int i = 0; i < moveCountStack[ply]; i++) {
            int move = movesStack[ply][i];

            applyMove(move, ply);

            float score = -negamax(depth - 1, rootDepth, ply + 1, (turn + 1) % 2, -beta, -alpha);

            undoMove(move, ply);

            if (score > maxScore) {
                maxScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, score);

            if (alpha >= beta) {
                cutoff = true;
                ttScore   [ttIndex]  = score;
                ttHash    [ttIndex]  = zobristKey[0];
                ttDepth   [ttIndex]  = (byte) depth;
                ttFlag    [ttIndex]  = (byte) Negamax.LOWER_BOUND;
                ttBestMove[ttIndex]  = move;  // preserve the refutation move
                break;
            }
        }

        if (!cutoff) {
            byte flag;
            if (maxScore <= alphaOrig) flag = (byte) Negamax.UPPER_BOUND;
            else if (maxScore >= beta) flag = (byte) Negamax.LOWER_BOUND;
            else                       flag = (byte) Negamax.EXACT;

            ttScore   [ttIndex]  = maxScore;
            ttHash    [ttIndex]  = zobristKey[0];
            ttDepth   [ttIndex]  = (byte) depth;
            ttFlag    [ttIndex]  = flag;
            ttBestMove[ttIndex]  = bestMove;
        }

        return maxScore;
    }
}