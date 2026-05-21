package control.algos;

import model.Move;

import java.util.List;

public class NegamaxSearch {
    private int depth;

    public NegamaxSearch(int depth) {
        this.depth = depth;
        if (this.depth < 0) this.depth = 0;
        if (this.depth > 10) this.depth = 10;
    }


    public Move findBestMove(Board board, int turn) {
        double alpha = Double.NEGATIVE_INFINITY;
        double beta = Double.POSITIVE_INFINITY;

        List<Move> legalMoves = board.getLegalMoves(turn);
        if (legalMoves.isEmpty()) {
            return null;
        }

        Move bestMove = legalMoves.getFirst();
        double bestScore = Double.NEGATIVE_INFINITY;
        int nextTurn = (turn+1) % 2;

        for (Move m : board.getLegalMoves(turn)) {
            Board newBoard = new Board(board);
            newBoard.makeMove(m);

            // evaluate the opponent's score, then inverse it
            double score = -negamax(newBoard, depth-1, nextTurn, -beta, -alpha);

            if (score > bestScore) {
                bestScore = score;
                bestMove = m;
            }
            alpha = Math.max(alpha, score);
        }

        return bestMove;
    }

    private double negamax(Board board, int depth, int turn, double alpha, double beta) {
        double win = board.checkWin();
        if (win != 0 || depth == 0) {
            return Evaluation.evaluate(board, turn, depth, this.depth);
        }

        double maxScore = Double.NEGATIVE_INFINITY;
        List<Move> legalMoves = board.getLegalMoves(turn);

        if (legalMoves.isEmpty()) {
            return Double.NEGATIVE_INFINITY - depth;
        }

        int nextTurn = (turn+1) % 2;

        for (Move move : legalMoves) {
            Board newBoard = new Board(board);
            newBoard.makeMove(move);

            // negamax core trick: negative sign and swapped alpha/beta
            double score = -negamax(newBoard, depth-1, nextTurn, -beta, -alpha);

            if (score > maxScore) {
                maxScore = score;
            }
            alpha = Math.max(alpha, score);
            if (alpha >= beta) {
                break;
            }
        }

        return maxScore;
    }


}
