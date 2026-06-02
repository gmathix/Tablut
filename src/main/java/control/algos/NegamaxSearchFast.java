package control.algos;

import model.TablutBoard;
import model.TablutStageModel;

import java.util.Random;


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

    // 2**23 * 8 bytes ~= 67MB
    public static final int NB_TT_ENTRIES = 1 << 23;
    public static final int NB_POSSIBLE_MOVES = 1296;
    public static final int MAX_CAPTURES = 3;

    // flags for TT entries
    public static final int LOWER_BOUND = 0;
    public static final int UPPER_BOUND = 1;
    public static final int EXACT       = 2;


    private class TTEntry {
        boolean processed;
        long    hash;
        double  score;
        int     depth;
        int     flag;
        public TTEntry() {
            processed = false;
            hash  = 0;
            score = 0;
            depth = 0;
            flag = -1;
        }
    }


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

    private Random     rng;
    private long[][]   zobrist;
    private long       sideToMove;
    private long[]     zobristKey;

    TTEntry[]          tt;




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




        rng = new Random(12345);
        zobrist = new long[4][81];

        // initialize random numbers
        for (int pieceType : FastBoard.pieceTypes) {
            for (int i = 0; i < 81; i++) {
                zobrist[pieceType][i] = rng.nextLong();
            }
        }
        sideToMove = rng.nextLong();


        zobristKey = new long[]{0};
        // xor numbers corresponding to active pieces together
        for (int i = 0; i < 81; i++) {
            if (board[i] != FastBoard.EMPTY) {
                zobristKey[0] ^= zobrist[board[i]][i];
            }
        }
        zobristKey[0] ^= sideToMove;


        tt = new TTEntry[NB_TT_ENTRIES];
        for (int i = 0; i < NB_TT_ENTRIES; i++) {
            tt[i] = new TTEntry();
            tt[i].processed = false;
        }
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
            FastBoard.makeMove(board, move, 0, captureCountStack, captureStack, kingPosStack, zobrist, zobristKey, sideToMove);

            double score = -negamax(board, startingDepth-1, 1, (turn+1) % 2, -beta, -alpha);

            FastBoard.undoMove(board, move, 0, captureCountStack, captureStack, kingPosStack, zobrist, zobristKey, sideToMove);


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

        int ttIndex = (int) (zobristKey[0] & (NB_TT_ENTRIES - 1));
        TTEntry entry = tt[ttIndex];
        if (entry.processed) {
            if (zobristKey[0] == entry.hash) {
                if (entry.flag == EXACT)
                    return entry.score;
                else if (entry.flag == LOWER_BOUND)
                    alpha = Math.max(alpha, entry.score);
                else if (entry.flag == UPPER_BOUND)
                    beta = Math.min(beta, entry.score);

                if (alpha >= beta)
                    return entry.score;
            }
        }

        if (win != 0 || depth == 0) {
            return FastEvaluation.evaluate(board, turn, ply, depth, startingDepth, kingPosStack, ruleSet);
        }

        double maxScore = Double.NEGATIVE_INFINITY;
        FastBoard.generateMoves(board, turn, ply, moveCountStack, movesStack, ruleSet);

        if (moveCountStack[ply] == 0) { // can't make moves, automatically lose
            return Double.NEGATIVE_INFINITY - depth;
        }

        int flag;
        double alphaOrig = alpha;
        for (int i = 0; i < moveCountStack[ply]; i++) {
            int move = movesStack[ply][i];

            kingPosStack[ply+1] = kingPosStack[ply];
            FastBoard.checkCaptures(board, move, ply, captureCountStack, captureStack);
            FastBoard.makeMove(board, move, ply, captureCountStack, captureStack, kingPosStack, zobrist, zobristKey, sideToMove);

            double score = -negamax(board, depth-1, ply+1, (turn+1) % 2, -beta, -alpha);

            FastBoard.undoMove(board, move, ply, captureCountStack, captureStack, kingPosStack, zobrist, zobristKey, sideToMove);

            if (score > maxScore) maxScore = score;
            alpha = Math.max(alpha, score);
            if (alpha >= beta) {
                tt[ttIndex].score = score;
                tt[ttIndex].hash = zobristKey[0];
                tt[ttIndex].depth = depth;
                tt[ttIndex].processed = true;
                tt[ttIndex].flag = LOWER_BOUND;
                break;
            }
        }

        if (maxScore <= alphaOrig)
            flag = UPPER_BOUND;
        else if (maxScore >= beta)
            flag = LOWER_BOUND;
        else
            flag = EXACT;

        tt[ttIndex].score = maxScore;
        tt[ttIndex].hash = zobristKey[0];
        tt[ttIndex].depth = depth;
        tt[ttIndex].processed = true;
        tt[ttIndex].flag = flag;

        return maxScore;
    }
}
