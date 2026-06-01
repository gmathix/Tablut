package control.algos;


import model.Move;

import java.util.*;
import java.util.Map.*;



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
    public static final double C = Math.sqrt(2);


    private final long timeLimitMs;






    private static class Node {
        boolean isRoot;
        public Node parent;
        public Map<RecurMove, Node> children;

        public int turn;
        public RecurBoard recurBoard;
        public List<RecurMove> unvisitedMoves;
        public int nbTotalLegalMoves;


        public int wins;   // w
        public int visits; // n
        // N = parent.visits

        public Node(boolean isRoot, Node parent, RecurBoard recurBoard, int turn) {
            this.isRoot = isRoot;
            this.parent = parent;
            this.children = new HashMap<>();
            this.recurBoard = recurBoard;
            this.turn = turn;
            this.unvisitedMoves = this.recurBoard.getLegalMoves(this.turn);
            this.nbTotalLegalMoves = this.unvisitedMoves.size();

            this.wins = 0;
            this.visits = 0;
        }

        public boolean isFullyExpanded() {
            return nbTotalLegalMoves == children.size();
        }
    }




    // level between 0 and 10
    public MonteCarlo(int level) {
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
    }


    private double UCB(Node node) {
        return ((double) node.wins / node.visits) + C * Math.sqrt(Math.log(node.parent.visits) / node.visits);
    }

    public RecurMove findBestMove(RecurBoard recurBoard, int turn, boolean findAlternativeMove) {
        RecurMove bestMove = recurBoard.getLegalMoves(turn).getFirst();


        Node root = new Node(true, null, recurBoard, turn);
        root.parent = root; // just to avoid null pointer errors

        Node currentNode;



        long startTimeMillis = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTimeMillis <= timeLimitMs) {

            /*
             * 1. Selection
             */
            currentNode = root;
            while (currentNode.isFullyExpanded()) {
                double bestScore = Double.NEGATIVE_INFINITY;
                Node bestNode = null;
                for (Node child : currentNode.children.values()) {
                    double score = UCB(child);
                    if (score > bestScore) {
                        bestScore = score;
                        bestNode = child;
                    }
                }
                currentNode = bestNode; // step into best node
            }

            /**
             * 2. Expansion
             */
            int moveIndex = (int) (Math.random() * currentNode.unvisitedMoves.size());
            RecurMove randomMove = currentNode.unvisitedMoves.get(moveIndex);
            RecurBoard childRecurBoard = new RecurBoard(currentNode.recurBoard);
            childRecurBoard.makeMove(randomMove);
            Node childNode = new Node(false, currentNode, childRecurBoard, (currentNode.turn + 1) % 2);

            currentNode.children.put(randomMove, childNode);
            currentNode.unvisitedMoves.remove(moveIndex);
            currentNode = childNode;



            /**
             * 3. Simulation
             */
            int playoutTurn = currentNode.turn;
            RecurBoard playoutRecurBoard = new RecurBoard(currentNode.recurBoard);
            List<RecurMove> legalMoves;
            double winScore = 0;

            while (winScore == 0) { // play a fast, random game until the game is over
                legalMoves = playoutRecurBoard.getLegalMoves(playoutTurn);
                moveIndex = (int) (Math.random() * legalMoves.size());

                playoutRecurBoard.makeMove(legalMoves.get(moveIndex));
                playoutTurn = (playoutTurn + 1) % 2;

                winScore = playoutRecurBoard.checkWin();
            }


            /**
             * 4. Backpropagation
             */
            int winningTurn = winScore == Integer.MAX_VALUE ? 0 : 1;
            while (currentNode != root) {

                currentNode.visits++;

                // only increment w to the nodes that belong to the winning side
                if (currentNode.turn == winningTurn) {
                    currentNode.wins++;
                }

                currentNode = currentNode.parent;
            }
            root.visits++;
        }

        List<Map.Entry<RecurMove, Node>> sorted = root.children.entrySet().stream()
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


        return bestMove;
    }
}
