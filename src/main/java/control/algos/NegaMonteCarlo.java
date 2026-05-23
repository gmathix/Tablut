package control.algos;


import model.Move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 *    - if the new expanded node allows a sneaky and vicious king escape while green has 2 pawns remaining 2 moves
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
    public static final double C = Math.sqrt(2);

    private final long timeLimitMs;
    private final int negamaxDepth = 2;

    private NegamaxSearch negamaxSearch;



    private static class Node {
        boolean isRoot;
        public Node parent;
        public Map<Move, Node> children;

        public int turn;
        public Board board;
        public List<Move> unvisitedMoves;
        public int nbTotalLegalMoves;


        public double wins; // double to support fractional scores from negamax
        public int visits;  // n

        public Node(boolean isRoot, Node parent, Board board, int turn) {
            this.isRoot = isRoot;
            this.parent = parent;
            this.children = new HashMap<>();
            this.board = board;
            this.turn = turn;

            // check if board is already terminal before getting moves over there
            if (board.checkWin() != 0) {
                this.unvisitedMoves = new ArrayList<>();
            } else {
                this.unvisitedMoves = this.board.getLegalMoves(this.turn);
            }
            this.nbTotalLegalMoves = this.unvisitedMoves.size();

            this.wins = 0;
            this.visits = 0;
        }

        public boolean isFullyExpanded() {
            return nbTotalLegalMoves == children.size();
        }
        public boolean isTerminal() {
            return board.checkWin() != 0 || (nbTotalLegalMoves == 0 && !isRoot);
        }
    }




    // level between 0 and 10
    public NegaMonteCarlo(int level) {
        this.timeLimitMs = switch (level) {
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

        this.negamaxSearch = new NegamaxSearch(this.negamaxDepth);
    }


    private double UCB(Node node) {
        if (node.visits == 0) return Double.POSITIVE_INFINITY; // should be impossible but let's avoid having to debug a NaN
        return ((double) node.wins / (double) node.visits) + C * Math.sqrt(Math.log(node.parent.visits) / node.visits);
    }

    public Move findBestMove(Board board, int turn) {
        Move bestMove = board.getLegalMoves(turn).getFirst();


        Node root = new Node(true, null, board, turn);
        root.parent = root; // just to avoid null pointer errors

        Node currentNode;

        int nbEvals = 0;

        long startTimeMillis = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTimeMillis <= timeLimitMs) {

            /*
             * 1. Selection
             */
            currentNode = root;
            while (currentNode.isFullyExpanded() && !currentNode.isTerminal()) {
                double bestScore = Double.NEGATIVE_INFINITY;
                Node bestNode = null;
                for (Node child : currentNode.children.values()) {
                    double score = UCB(child);
                    if (score > bestScore) {
                        bestScore = score;
                        bestNode = child;
                    }
                }
                if (bestNode == null) break;
                currentNode = bestNode; // step into best node
            }

            /**
             * 2. Expansion
             */
            // only expand if the selected node isn't a game over state
            if (!currentNode.isTerminal() && !currentNode.unvisitedMoves.isEmpty()) {
                int moveIndex = (int) (Math.random() * currentNode.unvisitedMoves.size());
                Move randomMove = currentNode.unvisitedMoves.get(moveIndex);

                Board childBoard = new Board(currentNode.board);
                childBoard.makeMove(randomMove);

                Node childNode = new Node(false, currentNode, childBoard, (currentNode.turn + 1) % 2);
                currentNode.children.put(randomMove, childNode);
                currentNode.unvisitedMoves.remove(moveIndex);
                currentNode = childNode;
            }


            /**
             * 3. Simulation (replaced with low depth negamax)
             */
            double score;
            double negamaxScore = negamaxSearch.negamax(currentNode.board, this.negamaxDepth, currentNode.turn, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
//            double negamaxScore = smallNegamax(currentNode.board, this.negamaxDepth, currentNode.turn, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            score = 1 / (1 + Math.exp(-negamaxScore / 100));



            /**
             * 4. Backpropagation
             */
            int leafTurn = currentNode.turn;
            while (currentNode != root) {
                currentNode.visits++;


                if (currentNode.turn == leafTurn) {
                    currentNode.wins += 1 - score;
                } else {
                    currentNode.wins += score;
                }


                currentNode = currentNode.parent;
            }
            root.visits++;
        }


        // now, look at the root node's first children and choose the move with the highest visit count
        int highestVisits = -1;
        for (Map.Entry<Move, Node> item : root.children.entrySet()) {
            Move currMove = item.getKey();
            Node currNode = item.getValue();
            if (currNode.visits > highestVisits) {
                highestVisits = currNode.visits;
                bestMove = currMove;
            }
        }

        System.out.printf("evaluated %d positions\n", nbEvals);

        return bestMove;
    }


    /**
     * helper low-depth negamax tailored for MCTS leaf tracking
     */
    private double smallNegamax(Board board, int depth, int turn, double alpha, double beta) {
        double win = board.checkWin();
        if (win != 0 || depth == 0) {
            return Evaluation.evaluate(board, turn, depth, negamaxDepth);
        }

        double bestScore = Double.NEGATIVE_INFINITY;
        List<Move> moves = board.getLegalMoves(turn);
        if (moves.isEmpty()) return turn == 0 ? -1000 : 1000;

        for (Move m : moves) {
            Board newBoard = new Board(board);
            newBoard.makeMove(m);
            double score = -smallNegamax(newBoard, depth-1, (turn+1) % 2, -beta, -alpha);
            bestScore = Math.max(bestScore, score);
            alpha = Math.max(alpha, score);
            if (alpha >= beta) break;
        }

        return bestScore;
    }
}
