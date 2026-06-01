package control.algos;

import model.Move;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NegamaxSearch {
    private int depth;

    public static int nbEvals = 0;


    private record BestMove(Move move, double score) {}


    public NegamaxSearch(int level) {
        this.depth = level;

        this.depth = switch (level) {
            case 0, 1 -> 1;
            case 2 -> 2;
            case 3,4 -> 3;
            case 5 -> 4;
            case 6,7 -> 5;
            case 8,9 -> 6;
            case 10 -> 7;
            default -> 4;
        };
    }


    public Move findBestMove(RecurBoard recurBoard, int turn, boolean findAlternativeMode) {
        double alpha = Double.NEGATIVE_INFINITY;
        double beta = Double.POSITIVE_INFINITY;

        List<Move> legalMoves = recurBoard.getLegalMoves(turn);
        if (legalMoves.isEmpty()) {
            return null;
        }

        int nextTurn = (turn+1) % 2;

        nbEvals = 0;

        List<BestMove> bestMoves = new ArrayList<>();

        for (Move m : recurBoard.getLegalMoves(turn)) {
//            RecurBoard newRecurBoard = new RecurBoard(recurBoard);
//            newRecurBoard.makeMove(m);
            int prevKingX = recurBoard.getKingX();
            int prevKingY = recurBoard.getKingY();
            recurBoard.makeMove(m);
            List<RecurBoard.Capture> lastCaps = recurBoard.getLastCaptures();


            // evaluate the opponent's score, then inverse it
            double score = -negamax(recurBoard, depth-1, nextTurn, -beta, -alpha);

            recurBoard.undoMove(m, lastCaps, prevKingX, prevKingY);

            alpha = Math.max(alpha, score);

            bestMoves.add(new BestMove(m, score));
        }

//        System.out.printf("evaluated %d positions (negamax)\n", nbEvals);

        bestMoves = bestMoves.stream().sorted(
                (bm1, bm2) -> Double.compare(
                        bm2.score, bm1.score
                )
        ).toList();
        Move bestMove = bestMoves.getFirst().move();
        if (findAlternativeMode && bestMoves.size() > 1) {
            bestMove = bestMoves.get(1).move;
        }

        return bestMove;
    }

    public double negamax(RecurBoard recurBoard, int depth, int turn, double alpha, double beta) {
        double win = recurBoard.checkWin();
        if (win != 0 || depth == 0) {
            nbEvals++;
            return Evaluation.evaluate(recurBoard, turn, depth, this.depth);
        }

        double maxScore = Double.NEGATIVE_INFINITY;
        List<Move> legalMoves = recurBoard.getLegalMoves(turn);

        if (legalMoves.isEmpty()) {
            return Double.NEGATIVE_INFINITY - depth;
        }


        for (Move move : legalMoves) {
//            RecurBoard newRecurBoard = new RecurBoard(recurBoard);
//            newRecurBoard.makeMove(move);

            int prevKingX = recurBoard.getKingX();
            int prevKingY = recurBoard.getKingY();
            recurBoard.makeMove(move);

            List<RecurBoard.Capture> prevCaps = recurBoard.getLastCaptures();

            // negamax core trick: negative sign and swapped alpha/beta
            double score = -negamax(recurBoard, depth-1, (turn+1) % 2, -beta, -alpha);

            recurBoard.undoMove(move, prevCaps, prevKingX, prevKingY);

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
