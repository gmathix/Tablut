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
 */
public class Negamax {


    public static final int   NB_TT_ENTRIES           = 1 << 20;
    public static final int   NB_POSSIBLE_MOVES       = 1296; // calculated (not in my head :( )
    public static final int   MAX_CAPTURES            = 3;
    public static final int   MAX_DEPTH               = 10;
    public static final float VIRTUAL_INF             = FastEvaluation.VIRTUAL_INF;
    public static final int   ASPIRATION_WINDOW_DELTA = 35;

    // flags for TT entries
    public static final int LOWER_BOUND = 1;
    public static final int UPPER_BOUND = 2;
    public static final int EXACT       = 3;




    private static int startingDepth;
    private static int ruleSet;
    private static int positionsAnalyzed;


    private static final Random rng = new Random(12345);

    private static byte[]           board               = new byte  [81];
    private static final byte[]     captureCountStack   = new byte  [MAX_DEPTH + 1];
    private static final byte[]     kingPosStack        = new byte  [MAX_DEPTH + 1];
    private static final int[]      moveCountStack      = new int   [MAX_DEPTH + 1];
    private static final byte[]     materialDiffStack   = new byte  [MAX_DEPTH + 1];
    private static final byte[]     soldierCountStack   = new byte  [MAX_DEPTH + 1];
    private static final byte[]     moscoviteCountStack = new byte  [MAX_DEPTH + 1];
    private static final int[]      bestMovesStack      = new int   [MAX_DEPTH + 1];

    private static final short[][]  captureStack        = new short [MAX_DEPTH + 1][MAX_CAPTURES];
    private static final int[][]    movesStack          = new int   [MAX_DEPTH + 1][NB_POSSIBLE_MOVES];
    private static final int[][]    killerMovesStack    = new int   [MAX_DEPTH + 1][2];


    private static final long[][]   zobrist             = new long  [4][81];
    private static final long[]     zobristKey          = new long  [1];
    private static final long       sideToMove          = rng.nextLong();


    // currently (8 + 4 + 1 + 1 + 4) * (1 << 20) ~= 14.6MB transposition table
    private static final long[]    ttHash               = new long  [NB_TT_ENTRIES];
    private static final float[]   ttScore              = new float [NB_TT_ENTRIES];
    private static final byte[]    ttDepth              = new byte  [NB_TT_ENTRIES];
    private static final byte[]    ttFlag               = new byte  [NB_TT_ENTRIES];
    private static final int[]     ttBestMove           = new int   [NB_TT_ENTRIES];




    private record BestMove(int move, float score) {}



    public static void resetBuffers() {
        Arrays.fill(board, (byte) 0);
        Arrays.fill(kingPosStack, (byte) 0);
        Arrays.fill(captureCountStack, (byte) 0);
        Arrays.fill(moveCountStack, 0);
        Arrays.fill(materialDiffStack, (byte) 0);
        Arrays.fill(soldierCountStack, (byte) 0);
        Arrays.fill(moscoviteCountStack, (byte) 0);
        Arrays.fill(bestMovesStack, 0);


        for (short[] cap : captureStack) Arrays.fill(cap, (short) 0);
        for (int[] moves : movesStack) Arrays.fill(moves, 0);
        for (int[] moves : killerMovesStack) Arrays.fill(moves, 0);
        for (long[] squares : zobrist) Arrays.fill(squares, 0);

        Arrays.fill(ttHash, 0);
        Arrays.fill(ttScore, 0);
        Arrays.fill(ttDepth, (byte) 0);
        Arrays.fill(ttFlag, (byte) 0);
        Arrays.fill(ttBestMove, 0);

        zobristKey[0] = 0;
    }

    public static void configure(int level, TablutBoard tablutBoard) {
        startingDepth = switch (level) {
            case 1 -> 1;
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

        board                   = FastBoard.fromTablutBoard(tablutBoard);
        kingPosStack[0]         = FastBoard.getKingPos(board);
        materialDiffStack[0]    = FastEvaluation.countMaterialDiff(board);
        soldierCountStack[0]    = FastEvaluation.countPieces(board, FastEvaluation.SWEDISH);
        moscoviteCountStack[0]  = FastEvaluation.countPieces(board, FastEvaluation.MOSCOVITE);

        // initialize random numbers
        for (int pieceType : FastBoard.pieceTypes) {
            for (int i = 0; i < 81; i++) {
                zobrist[pieceType][i] = rng.nextLong();
            }
        }

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
        List<BestMove> bestMoves = new ArrayList<>();



        int currentBestMove = 0;
        positionsAnalyzed = 0;

        float alpha     = Float.NEGATIVE_INFINITY;
        float beta      = Float.POSITIVE_INFINITY;
        float bestScore = Float.NEGATIVE_INFINITY;


        for (int depth = 0; depth < startingDepth; depth++) { // iterative deepening



            // generate moves with the first move in the list being the current best one
            if (depth == 0) FastBoard.generateMoves(board, turn, 0, moveCountStack, movesStack, killerMovesStack, 0, false, ruleSet);
            else FastBoard.generateMoves(board, turn, 0, moveCountStack, movesStack, killerMovesStack, currentBestMove, true, ruleSet);


            if (moveCountStack[0] == 0) return -1;

            for (int i = 0; i < moveCountStack[0]; i++) {
                int move = movesStack[0][i];

                kingPosStack[1]         = kingPosStack[0];
                materialDiffStack[1]    = materialDiffStack[0];
                moscoviteCountStack[1]  = moscoviteCountStack[0];
                soldierCountStack[1]    = soldierCountStack[0];

                FastBoard.checkCaptures(board, move, 0, captureCountStack, captureStack, ruleSet);
                FastBoard.makeMove(
                        board, move, 0, captureCountStack, captureStack, materialDiffStack, soldierCountStack, moscoviteCountStack,
                        kingPosStack, zobrist, zobristKey, sideToMove
                );

                float score = -negamax(startingDepth, 1, (turn+1) % 2, -beta, -alpha);

                FastBoard.undoMove(
                        board, move, 0, captureCountStack, captureStack, materialDiffStack,
                        kingPosStack, zobrist, zobristKey, sideToMove
                );

                if (score > bestScore) {
                    bestScore       = score;
                    currentBestMove = move;
                    BestMove bm     = new BestMove(move, score);
                    boolean movePresent = false;
                    for (BestMove bestMove : bestMoves) if (bestMove.move == move) movePresent = true;
                    if (!movePresent) bestMoves.add(bm);
                }
                alpha = Math.max(alpha, score);
                if (alpha >= beta) break;
            }

            alpha = bestScore - ASPIRATION_WINDOW_DELTA;
            beta  = bestScore + ASPIRATION_WINDOW_DELTA;

        }


//        System.out.printf("Analyzed %d positions\n", positionsAnalyzed);


        bestMoves = bestMoves.stream().sorted(
                (bm1, bm2) -> Double.compare(
                        bm2.score, bm1.score
                )
        ).toList();
        int bestMove = bestMoves.getFirst().move;
        if (findAlternativeMode && bestMoves.size() > 1) {
            bestMove = bestMoves.get(1).move;
        }

        return bestMove;
    }


    public static float negamax(int depth, int ply, int turn, float alpha, float beta) {
        float win = FastBoard.checkWin(board, ply, kingPosStack, ruleSet);
        if (win != 0) positionsAnalyzed++;
        if (win > 0) return turn == 0 ? VIRTUAL_INF - ply : -VIRTUAL_INF + ply;
        if (win < 0) return turn == 1 ? VIRTUAL_INF - ply : -VIRTUAL_INF + ply;

        boolean hasTTBestMove = false;
        int bestTTMove = 0;

        int ttIndex = (int) (zobristKey[0] & (NB_TT_ENTRIES - 1));
        if (ttFlag[ttIndex] != 0 && zobristKey[0] == ttHash[ttIndex] && ttDepth[ttIndex] >= depth) {
            float score = ttScore[ttIndex];

            // re-adjust cached winning/losing values relative to the current ply
            if (score > VIRTUAL_INF - 100) {
                score -= ply;
            } else if (score < -VIRTUAL_INF + 100) {
                score += ply;
            }

            switch (ttFlag[ttIndex]) {
                case EXACT:       return score;
                case LOWER_BOUND: alpha = Math.max(alpha, score); break;
                case UPPER_BOUND: beta  = Math.min(beta, score); break;
            }
            if (alpha >= beta) return score;

            bestTTMove    = ttBestMove[ttIndex];
            hasTTBestMove = true;
        }


        if (depth == 0) {
            positionsAnalyzed++;
            return FastEvaluation.evaluate(board, turn, ply, depth, startingDepth, materialDiffStack, soldierCountStack, moscoviteCountStack, kingPosStack, ruleSet);
        }

        float maxScore = -VIRTUAL_INF;


        FastBoard.generateMoves(board, turn, ply, moveCountStack, movesStack, killerMovesStack, bestTTMove, hasTTBestMove, ruleSet);

        if (moveCountStack[ply] == 0) { // can't make moves, automatically lose
            return -VIRTUAL_INF + depth;
        }

        byte flag;
        float alphaOrig = alpha;
        int bestMove = 0;
        for (int i = 0; i < moveCountStack[ply]; i++) {
            int move = movesStack[ply][i];

            // init variables for next ply to same as current ply
            kingPosStack[ply+1]         = kingPosStack[ply];
            materialDiffStack[ply+1]    = materialDiffStack[ply];
            moscoviteCountStack[ply+1]  = moscoviteCountStack[ply];
            soldierCountStack[ply+1]    = soldierCountStack[ply];

            FastBoard.checkCaptures(board, move, ply, captureCountStack, captureStack, ruleSet);
            FastBoard.makeMove(
                    board, move, ply, captureCountStack, captureStack, materialDiffStack, soldierCountStack, moscoviteCountStack,
                    kingPosStack, zobrist, zobristKey, sideToMove
            );

            float score = -negamax(depth-1, ply+1, (turn+1) % 2, -beta, -alpha);

            FastBoard.undoMove(
                    board, move, ply, captureCountStack, captureStack, kingPosStack
                    , materialDiffStack, zobrist, zobristKey, sideToMove
            );

            if (score > maxScore) {
                maxScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, score);
            if (alpha >= beta) {
                if (killerMovesStack[ply][0] != move) {
                    killerMovesStack[ply][1] = killerMovesStack[ply][0];
                    killerMovesStack[ply][0] = move;
                }
                float storeScore = score;
                if (storeScore > VIRTUAL_INF - 100) storeScore += ply;
                else if (storeScore < -VIRTUAL_INF + 100) storeScore -= ply;
                ttScore[ttIndex]  = storeScore;
                ttHash[ttIndex]   = zobristKey[0];
                ttDepth[ttIndex]  = (byte) depth;
                ttFlag[ttIndex]   = LOWER_BOUND;
                break;
            }
        }

        if (maxScore <= alphaOrig) flag = UPPER_BOUND;
        else if (maxScore >= beta) flag = LOWER_BOUND;
        else                       flag = EXACT;


        float storeMaxScore = maxScore;
        if (storeMaxScore > VIRTUAL_INF - 100)      storeMaxScore += ply;
        else if (storeMaxScore < -VIRTUAL_INF + 100) storeMaxScore -= ply;

        ttScore[ttIndex]    = maxScore;
        ttHash[ttIndex]     = zobristKey[0];
        ttDepth[ttIndex]    = (byte) depth;
        ttFlag[ttIndex]     = flag;
        ttBestMove[ttIndex] = bestMove;

        return maxScore;
    }
}
