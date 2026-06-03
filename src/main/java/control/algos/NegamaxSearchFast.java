package control.algos;

import model.TablutBoard;
import model.TablutStageModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    // 2**20 * 8 bytes ~= 9MB
    public static final int NB_TT_ENTRIES       = 1 << 20;
    public static final int NB_POSSIBLE_MOVES   = 1296; // calculated (not in my head :( )
    public static final int MAX_CAPTURES        = 3;
    public static final int MAX_DEPTH           = 10;

    // flags for TT entries
    public static final int LOWER_BOUND = 1;
    public static final int UPPER_BOUND = 2;
    public static final int EXACT       = 3;




    private static int startingDepth;
    private static int ruleSet;


    private static Random rng = new Random(12345);

    private static byte[]     board = new byte[81];
    private static byte[]     captureCountStack = new byte[MAX_DEPTH + 1];
    private static byte[]     kingPosStack = new byte[MAX_DEPTH + 1];
    private static int[]      moveCountStack = new int[MAX_DEPTH + 1];
    private static byte[]    materialDiffStack = new byte[MAX_DEPTH + 1];

    private static short[][]  captureStack = new short[MAX_DEPTH + 1][MAX_CAPTURES];
    private static int[][]    movesStack = new int[MAX_DEPTH + 1][NB_POSSIBLE_MOVES];
    private static int[][]    killerMoves = new int[NB_POSSIBLE_MOVES][2];


    private static long[][]   zobrist = new long[4][81];
    private static long       sideToMove = rng.nextLong();
    private static long[]     zobristKey = new long[1];


    // data-oriented layout is better than a TTEntry class/record for cache behavior
    private static long[]    ttHash = new long[NB_TT_ENTRIES];
    private static float[]   ttScore = new float[NB_TT_ENTRIES];
    private static byte[]    ttDepth = new byte[NB_TT_ENTRIES];
    private static byte[]    ttFlag = new byte[NB_TT_ENTRIES];
    private static int[]     ttBestMove = new int[NB_TT_ENTRIES];




    private record BestMove(int move, float score) {}



    public static void resetBuffers() {
        Arrays.fill(board, (byte) 0);
        Arrays.fill(kingPosStack, (byte) 0);
        Arrays.fill(captureCountStack, (byte) 0);
        Arrays.fill(moveCountStack, 0);
        Arrays.fill(materialDiffStack, (byte) 0);

        for (short[] cap : captureStack) Arrays.fill(cap, (short) 0);
        for (int[] moves : movesStack) Arrays.fill(moves, 0);
        for (int[] moves : killerMoves) Arrays.fill(moves, 0);
        for (long[] squares : zobrist) Arrays.fill(squares, 0);

        Arrays.fill(ttHash, 0);
        Arrays.fill(ttScore, 0);
        Arrays.fill(ttDepth, (byte) 0);
        Arrays.fill(ttFlag, (byte) 0);
        Arrays.fill(ttBestMove, 0);

        zobristKey[0] = 0;
        sideToMove = 0;
    }

    public static void configure(int level, TablutBoard tablutBoard) {
        startingDepth = switch (level) {
            case 0, 1 -> 1;
            case 2 -> 2;
            case 3,4 -> 3;
            case 5 -> 4;
            case 6,7 -> 5;
            case 8 -> 6;
            case 9 -> 7;
            case 10 -> 8;
            default -> 4;
        };

        TablutStageModel stageModel = (TablutStageModel) tablutBoard.getModel().getGameStage();
        ruleSet = stageModel.getRuleSet();

        board = FastBoard.fromTablutBoard(tablutBoard);
        kingPosStack[0]   = FastBoard.getKingPos(board);
        materialDiffStack[0] = FastEvaluation.countMaterialDiff(board);

        // initialize random numbers
        for (int pieceType : FastBoard.pieceTypes) {
            for (int i = 0; i < 81; i++) {
                zobrist[pieceType][i] = rng.nextLong();
            }
        }
        sideToMove = rng.nextLong();

        zobristKey[0] = 0;
        // xor numbers corresponding to active pieces together
        for (int i = 0; i < 81; i++) {
            if (board[i] != FastBoard.EMPTY) {
                zobristKey[0] ^= zobrist[board[i]][i];
            }
        }
        zobristKey[0] ^= sideToMove;
    }


    public static int findBestMove(int turn, boolean findAlternativeMode) {
        List<BestMove> moves = new ArrayList<>();


        float alpha = Float.NEGATIVE_INFINITY;
        float beta = Float.POSITIVE_INFINITY;

        float bestScore = Float.NEGATIVE_INFINITY;


        FastBoard.generateMoves(board, turn, 0, moveCountStack, movesStack, ruleSet);
        if (moveCountStack[0] == 0) return -1;
        for (int i = 0; i < moveCountStack[0]; i++) {
            int move = movesStack[0][i];

            kingPosStack[1] = kingPosStack[0];
            materialDiffStack[1] = materialDiffStack[0];
            FastBoard.checkCaptures(board, move, 0, captureCountStack, captureStack);
            FastBoard.makeMove(
                    board, move, 0, captureCountStack, captureStack, materialDiffStack,
                    kingPosStack, zobrist, zobristKey, sideToMove
            );

            float score = -negamax(startingDepth-1, 1, (turn+1) % 2, -beta, -alpha);

            FastBoard.undoMove(
                    board, move, 0, captureCountStack, captureStack, materialDiffStack,
                    kingPosStack, zobrist, zobristKey, sideToMove
            );


            if (score > bestScore) {
                bestScore = score;
                moves.add(new BestMove(move, score));
            }
            alpha = Math.max(alpha, score);
            if (alpha >= beta) break;
        }

        moves = moves.stream().sorted(
                (bm1, bm2) -> Double.compare(
                        bm2.score, bm1.score
                )
        ).toList();
        int bestMove = moves.getFirst().move;
        if (findAlternativeMode && moves.size() > 1) {
            bestMove = moves.get(1).move;
        }

        return bestMove;
    }


    public static float negamax(int depth, int ply, int turn, float alpha, float beta) {
        float win = FastBoard.checkWin(board, ply, kingPosStack, ruleSet);

        int ttIndex = (int) (zobristKey[0] & (NB_TT_ENTRIES - 1));

        if (ttFlag[ttIndex] != 0) {
            if (zobristKey[0] == ttHash[ttIndex]) {
                if (ttFlag[ttIndex] == EXACT)
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
            return FastEvaluation.evaluate(board, turn, ply, depth, startingDepth, materialDiffStack, kingPosStack, ruleSet);
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
            materialDiffStack[ply+1] = materialDiffStack[ply];
            FastBoard.checkCaptures(board, move, ply, captureCountStack, captureStack);
            FastBoard.makeMove(
                    board, move, ply, captureCountStack, captureStack, materialDiffStack,
                    kingPosStack, zobrist, zobristKey, sideToMove
            );

            float score = -negamax(depth-1, ply+1, (turn+1) % 2, -beta, -alpha);

            FastBoard.undoMove(
                    board, move, ply, captureCountStack, captureStack, kingPosStack
                    , materialDiffStack, zobrist, zobristKey, sideToMove
            );

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
