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
    public static final int LOWER_BOUND = 1;
    public static final int UPPER_BOUND = 2;
    public static final int EXACT       = 3;




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


    // data-oriented layout is better than a TTEntry class/record for cache behavior
    private long[]    ttHash;
    private float[]  ttScore;
    private byte[]    ttDepth;
    private byte[]    ttFlag;
    private int[]     ttBestMove;




    private record BestMove(RecurMove move, float score) {}


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


        ttHash = new long[NB_TT_ENTRIES];
        ttScore = new float[NB_TT_ENTRIES];
        ttDepth = new byte[NB_TT_ENTRIES];
        ttFlag = new byte[NB_TT_ENTRIES];
        ttBestMove = new int[NB_TT_ENTRIES];
    }


    public int findBestMove(int turn, boolean findAlternativeMode) {
        int bestMove = -1;


        float alpha = Float.NEGATIVE_INFINITY;
        float beta = Float.POSITIVE_INFINITY;
        float bestScore = Float.NEGATIVE_INFINITY;

        FastBoard.generateMoves(board, turn, 0, moveCountStack, movesStack, ruleSet);
        if (moveCountStack[0] == 0) return -1;
        for (int i = 0; i < moveCountStack[0]; i++) {
            int move = movesStack[0][i];

            kingPosStack[1] = kingPosStack[0];
            FastBoard.checkCaptures(board, move, 0, captureCountStack, captureStack);
            FastBoard.makeMove(board, move, 0, captureCountStack, captureStack, kingPosStack, zobrist, zobristKey, sideToMove);

            float score = -negamax(board, startingDepth-1, 1, (turn+1) % 2, -beta, -alpha);

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


    public float negamax(byte[] board, int depth, int ply, int turn, float alpha, float beta) {
        float win = FastBoard.checkWin(board, ply, kingPosStack, ruleSet);

        int ttIndex = (int) (zobristKey[0] & (NB_TT_ENTRIES - 1));

        if (ttFlag[ttIndex] != 0) {
            if (zobristKey[0] == ttHash[ttIndex]) {
                if (ttHash[ttIndex] == EXACT)
                    return ttScore[ttIndex];
                else if (ttFlag[ttIndex] == LOWER_BOUND)
                    alpha = Math.max(alpha, ttScore[ttIndex]);
                else if (ttFlag[ttIndex] == UPPER_BOUND)
                    beta = Math.min(beta, ttScore[ttIndex]);

                if (alpha >= beta)
                    return ttScore[ttIndex];
            }
        }

        if (win != 0 || depth == 0) {
            return FastEvaluation.evaluate(board, turn, ply, depth, startingDepth, kingPosStack, ruleSet);
        }

        float maxScore = Float.NEGATIVE_INFINITY;
        FastBoard.generateMoves(board, turn, ply, moveCountStack, movesStack, ruleSet);

        if (moveCountStack[ply] == 0) { // can't make moves, automatically lose
            return Float.NEGATIVE_INFINITY - depth;
        }

        byte flag;
        float alphaOrig = alpha;
        for (int i = 0; i < moveCountStack[ply]; i++) {
            int move = movesStack[ply][i];

            kingPosStack[ply+1] = kingPosStack[ply];
            FastBoard.checkCaptures(board, move, ply, captureCountStack, captureStack);
            FastBoard.makeMove(board, move, ply, captureCountStack, captureStack, kingPosStack, zobrist, zobristKey, sideToMove);

            float score = -negamax(board, depth-1, ply+1, (turn+1) % 2, -beta, -alpha);

            FastBoard.undoMove(board, move, ply, captureCountStack, captureStack, kingPosStack, zobrist, zobristKey, sideToMove);

            if (score > maxScore) maxScore = score;
            alpha = Math.max(alpha, score);
            if (alpha >= beta) {
                ttScore[ttIndex] = score;
                ttHash[ttIndex] = zobristKey[0];
                ttDepth[ttIndex] = (byte) depth;
                ttFlag[ttIndex] = LOWER_BOUND;
                break;
            }
        }

        if (maxScore <= alphaOrig)
            flag = UPPER_BOUND;
        else if (maxScore >= beta)
            flag = LOWER_BOUND;
        else
            flag = EXACT;

        ttScore[ttIndex] = maxScore;
        ttHash[ttIndex] = zobristKey[0];
        ttDepth[ttIndex] = (byte) depth;
        ttFlag[ttIndex] = flag;

        return maxScore;
    }
}
