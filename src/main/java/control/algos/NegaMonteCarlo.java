package control.algos;


import model.Move;

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

    // constant that balances (exploitation and exploration) vs prior score (which children look more promising)
    public static final double W = 0.5;



    private long timeLimitMs;
    private int negamaxDepth;

    private NegamaxSearch negamaxSearch;



    private static class Node {
        boolean isRoot;
        public Node parent;
        public Map<Move, Node> children;

        public int turn;
        public RecurBoard recurBoard;
        public List<Move> unvisitedMoves;
        public int nbTotalLegalMoves;


        public double wins; // double to support fractional scores from negamax
        public int visits;  // n

        public Node(boolean isRoot, Node parent, RecurBoard recurBoard, int turn) {
            this.isRoot = isRoot;
            this.parent = parent;
            this.children = new HashMap<>();
            this.recurBoard = recurBoard;
            this.turn = turn;

            // check if board is already terminal before getting moves over there
            if (recurBoard.checkWin() != 0) {
                this.unvisitedMoves = new ArrayList<>();
            } else {
                this.unvisitedMoves = this.recurBoard.getLegalMoves(this.turn);
            }
            this.nbTotalLegalMoves = this.unvisitedMoves.size();

            this.wins = 0;
            this.visits = 0;
        }

        public boolean isFullyExpanded() {
            return nbTotalLegalMoves == children.size();
        }
        public boolean isTerminal() {
            return recurBoard.checkWin() != 0 || (nbTotalLegalMoves == 0 && !isRoot);
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

        /** going from depth 2 -> 3 gives much better information to the backprop
         *  the tradeoff is that we do less iterations but high-quality ones beat shallow evaluations in a tactical game like tablut
         */
        this.negamaxDepth = switch (level) {
            case 1,2,3,4,5 -> 2;
            case 6,7,8,9,10 -> 2;
            default -> 2;
        };

        this.negamaxSearch = new NegamaxSearch(this.negamaxDepth);
    }


    private double UCB(Node node, double priorScore) {
        if (node.visits == 0) return Double.POSITIVE_INFINITY; // should be impossible but let's avoid having to debug a NaN

        /** W add a prior policy score like AlphaZero does :
         *  a light score that biases which children get selected
         *  it can be sth cheap :
         *     - does the move threaten capture ? (for both sides)
         *     - does the move open an escape path ? (for green)
         *     - does the move close an escape path ? (for yellow)
         *     - etc
         *  calculated fast (preferably O(1)) and applied at every selection step in the loop
         */
        return (node.wins / (double) node.visits)
                + C * Math.sqrt(Math.log(node.parent.visits) / node.visits)
                + W * (priorScore / (1 + node.visits));
    }

    public Move findBestMove(RecurBoard recurBoard, int turn, boolean findAlternativeMove) {
        Move bestMove = recurBoard.getLegalMoves(turn).getFirst();


        Node root = new Node(true, null, recurBoard, turn);
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
                for (Map.Entry<Move, Node> entry : currentNode.children.entrySet()) {
                    Move move = entry.getKey();
                    Node child = entry.getValue();

                    double priorScore = 0;
                    if (!currentNode.recurBoard.checkCaptures(move).isEmpty()) priorScore += 0.5;
                    if (currentNode.turn != turn) priorScore *= -1;

                    double score = UCB(child, priorScore);
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

                RecurBoard childRecurBoard = new RecurBoard(currentNode.recurBoard);
                childRecurBoard.makeMove(randomMove);

                Node childNode = new Node(false, currentNode, childRecurBoard, (currentNode.turn + 1) % 2);
                currentNode.children.put(randomMove, childNode);
                currentNode.unvisitedMoves.remove(moveIndex);
                currentNode = childNode;
            }


            /**
             * 3. Simulation (replaced with low depth negamax)
             * all nodes run a search at depth a least two
             * when green is playing :
             *    - if there are 4 or 5 moscovites in the same 5x5 region as the king, run at depth 3
             *    - there are less than 4 moscovites in the same 5x5 region, run at depth 4
             *    because those situations are more promising.
             * when yellow is playing :
             *    depending on the king encerclement score :
             *    - when it is lower than -20 (~2 moscovites surrounding the king orthogonally), increment depth by 1
             *    - when it is lower than -30 (~3 moscovites surrounding the king orthogonally), increment depth by 1
             */
            double score;

            int depthBonus = 0;
            if (turn == 0) {
                int nbMoscovitesIn5x5Region = Evaluation.countMoscovitesIn5x5Region(currentNode.recurBoard);
                if (nbMoscovitesIn5x5Region == 4 || nbMoscovitesIn5x5Region == 5) depthBonus += 1;
                if (nbMoscovitesIn5x5Region < 4) depthBonus += 2;
            } else if (turn == 1) {
                double kingEncerclement = Evaluation.countKingEncerclement(currentNode.recurBoard);
                if (kingEncerclement <= -20) depthBonus += 1;
                if (kingEncerclement <= -30) depthBonus += 1;
            }

            double negamaxScore = negamaxSearch.negamax(currentNode.recurBoard, this.negamaxDepth + depthBonus, currentNode.turn, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

            // green needs one escape path, yellow needs full encerclement.
            // the value of the score means different stuff depending on who's playing, so we use different "temperatures"
            double temp = currentNode.turn == 0 ? 80 : 120;
            score = 1 / (1 + Math.exp(-negamaxScore / temp));
            nbEvals++;



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


        List<Map.Entry<Move, Node>> sorted = root.children.entrySet().stream()
                .sorted((e1, e2) ->
                        Integer.compare(
                                e2.getValue().visits,
                                e1.getValue().visits
                        )
                ).toList();


        bestMove = sorted.getFirst().getKey();
        if (findAlternativeMove && sorted.size() > 1) {
            bestMove = sorted.get(1).getKey();
        }

//        System.out.printf("evaluated %d positions\n", nbEvals);

        return bestMove;
    }


    /**
     * helper low-depth negamax tailored for MCTS leaf tracking
     */
    private double smallNegamax(RecurBoard recurBoard, int depth, int turn, double alpha, double beta) {
        double win = recurBoard.checkWin();
        if (win != 0 || depth == 0) {
            return Evaluation.evaluate(recurBoard, turn, depth, negamaxDepth);
        }

        double bestScore = Double.NEGATIVE_INFINITY;
        List<Move> moves = recurBoard.getLegalMoves(turn);
        if (moves.isEmpty()) return turn == 0 ? -1000 : 1000;

        for (Move m : moves) {
            RecurBoard newRecurBoard = new RecurBoard(recurBoard);
            newRecurBoard.makeMove(m);
            double score = -smallNegamax(newRecurBoard, depth-1, (turn+1) % 2, -beta, -alpha);
            bestScore = Math.max(bestScore, score);
            alpha = Math.max(alpha, score);
            if (alpha >= beta) break;
        }

        return bestScore;
    }
}
