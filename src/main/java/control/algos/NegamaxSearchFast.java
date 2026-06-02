package control.algos;

import model.TablutBoard;
import model.TablutStageModel;


/**
 *  * board is a flat byte[81] array
 *
 *  * move = src | (dst << 7) and with src = 0..80 and same for dst (raster index)
 *
 *  * move list : just an int[MAX_MOVES] array
 *
 *  * moveCount : example :
 *        moveCount = generateMoves(turn, moves)
 *        for (int i =0 ; i < moveCount; i++) {
 *            int move = moves[i];
 *        }
 *
 *
 *  * Important part : ply stacks
 *    if depth is 8, we only need storage for 8 moves worth of undo data (e.g. captures)
 *    (not for every node or position, but just for the current search path)
 *    (every index in those arrays corresponds to a ply)
 *
 *    ply = STARING_DEPTH - currentDepth
 *
 *    *  king pos : byte[STARTING_DEPTH]
 *       before move : kingPosStack[ply] = kingPos;
 *       undo : kingPos = kingPoStack[ply]
 *
 *    * capture count : byte[STARTING_DEPTH]
 *      capCount[ply] = 2 means that 2 pieces were captured
 *
 *    * captured pieces : short[STARTING_DEPTH][3]
 *      with cap[ply][n] = (pos << 3) | piece
 *
 *  * making moves : makeMove() has to take in account the ply in order to write the undo info
 *    example :
 *    inline (oops no) void makeMove(int move, int depth) {
 *          make move shit
 *          captured[ply][0] = ...
 *          captured[ply][1] = ...
 *          capCount[ply] = count;
 *    }
 *
 *  * undoing moves : undoMove(move, ply)
 *    reads : captured[ply], capCount[ply], kingPosStack[ply]
 *
 *  * move generation : just write int moves into moveList[]
 *    problem : recursion can't share one move list (child nodes would overwrite the root moves, bad)
 *    solution : int moveLists[STARTING_DEPTH][MAX_MOVES]
 *               then : int count = generateMoves(turn, moveLists[ply])
 *
 *  * killer moves : int[STARTING_DEPTH][2]
 *    whenever a move causes an alpha/beta prune : store as killer
 *    later : generate killer first in move list
 *
 *  * history heuristic : int history[TOTAL_POSSIBLE_MOVES]
 *    index : history[moveSrc * 81 + moveDst]
 *    whenever a move causes a prune : history[index] += depth
 *    then store them first as well in the move list, because moves that historically prune well get searched first
 *
 *  *
 */
public class NegamaxSearchFast {
    public static final int NB_POSSIBLE_MOVES = 1296;
    public static final int MAX_CAPTURES = 3;


    private int startingDepth;
    private int ruleSet;


    private byte[]     board;

    private byte[]     captureCountStack;
    private byte[]     kingPosStack;
    private int[]      moveCountStack;
    private int[]      historyHeuristic;

    private short[][]  captureStack;
    private int[][]    movesStack;
    private int[][]    killerMoves;



    private record BestMove(RecurMove move, double score) {}


    public NegamaxSearchFast(TablutBoard tablutBoard, int level) {
        this.startingDepth = switch (level) {
            case 0, 1 -> 1;
            case 2 -> 2;
            case 3,4 -> 3;
            case 5 -> 4;
            case 6,7 -> 5;
            case 8,9 -> 6;
            case 10 -> 7;
            default -> 4;
        };

        TablutStageModel stageModel = (TablutStageModel) tablutBoard.getModel().getGameStage();
        ruleSet = stageModel.getRuleSet();

        board = FastBoard.fromTablutBoard(tablutBoard);

        kingPosStack = new byte[startingDepth+1];
        captureCountStack = new byte[startingDepth+1];
        moveCountStack = new int[startingDepth+1];
        historyHeuristic = new int[NB_POSSIBLE_MOVES];

        captureStack = new short[startingDepth+1][MAX_CAPTURES];
        movesStack = new int[startingDepth+1][NB_POSSIBLE_MOVES];
        killerMoves = new int[startingDepth+1][2];


        kingPosStack[0] = FastBoard.getKingPos(board);
    }


    public int findBestMove(int turn, boolean findAlternativeMode) {
        int bestMove = -1;


        double alpha = Double.NEGATIVE_INFINITY;
        double beta = Double.POSITIVE_INFINITY;
        double bestScore = Double.NEGATIVE_INFINITY;

        FastBoard.generateMoves(board, turn, 0, moveCountStack, movesStack, ruleSet);
        if (moveCountStack[0] == 0) return -1;
        for (int i = 0; i < moveCountStack[0]; i++) {
            int move = movesStack[0][i];

            kingPosStack[1] = kingPosStack[0];
            FastBoard.checkCaptures(board, move, 0, captureCountStack, captureStack);
            FastBoard.makeMove(board, move, 0, captureCountStack, captureStack, kingPosStack);

            double score = -negamax(board, startingDepth-1, 1, (turn+1) % 2, -beta, -alpha);

            FastBoard.undoMove(board, move, 0, captureCountStack, captureStack, kingPosStack);


            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, score);
            if (alpha >= beta) break;
        }

        return bestMove;
    }


    public double negamax(byte[] board, int depth, int ply, int turn, double alpha, double beta) {
        double win = FastBoard.checkWin(board, ply, kingPosStack, ruleSet);
        if (win != 0 || depth == 0) {
            return FastEvaluation.evaluate(board, turn, ply, depth, startingDepth, kingPosStack, ruleSet);
        }

        double maxScore = Double.NEGATIVE_INFINITY;
        FastBoard.generateMoves(board, turn, ply, moveCountStack, movesStack, ruleSet);

        if (moveCountStack[ply] == 0) { // can't make moves, automatically lose
            return Double.NEGATIVE_INFINITY - depth;
        }

        for (int i = 0; i < moveCountStack[ply]; i++) {
            int move = movesStack[ply][i];

            kingPosStack[ply+1] = kingPosStack[ply];
            FastBoard.checkCaptures(board, move, ply, captureCountStack, captureStack);
            FastBoard.makeMove(board, move, ply, captureCountStack, captureStack, kingPosStack);

            double score = -negamax(board, depth-1, ply+1, (turn+1) % 2, -beta, -alpha);

            FastBoard.undoMove(board, move, ply, captureCountStack, captureStack, kingPosStack);

            if (score > maxScore) maxScore = score;
            alpha = Math.max(alpha, score);
            if (alpha >= beta) break;
        }

        return maxScore;
    }


}
