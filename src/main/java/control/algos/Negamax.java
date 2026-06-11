package control.algos;

import com.sun.tools.attach.spi.AttachProvider;
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


    public static final int   NB_TT_ENTRIES              = 1 << 20;
    public static final int   NB_POSSIBLE_MOVES          = 1296; // calculated (not in my head :( )
    public static final int   MAX_CAPTURES               = 3;
    public static final int   MAX_DEPTH                  = 9; // yeah we'll never reach this in acceptable time anyway
    public static final float VIRTUAL_INF                = FastEvaluation.VIRTUAL_INF;
    public static final int   ASPIRATION_WINDOW_DELTA    = 400;


    // flags for TT entries
    public static final int LOWER_BOUND = 1;
    public static final int UPPER_BOUND = 2;
    public static final int EXACT       = 3;





    private static int ruleSet;
    private static int searchTimeSeconds;
    private static int positionsAnalyzed;


    // fun for experimenting
    private static long totalLegalMoveCountMoscovite = 0;
    private static long totalMoveGenerationsMoscovite = 0;
    private static long totalLegalMoveCountSwedish = 0;
    private static long totalMoveGenerationsSwedish = 0;
    // results : ~33 available moves for swedish, ~72 available moves for moscovites


    // number of TT hits that are a symmetrical representation of the current board
    private static long nbSymTTHits;
    private static long nbTTHits;

    private static long ttProbe;
    private static long ttHashMatch;
    private static long ttDepthMatch;

    private static long searchDeadlineNs;

    private static int currentMoveNumber = 0;


    private static final Random rng = new Random(12345);

    private static byte[]           board               = new byte  [81];
    private static final byte[]     captureCountStack   = new byte  [MAX_DEPTH + 1];
    private static final byte[]     kingPosStack        = new byte  [MAX_DEPTH + 1];
    private static final int[]      moveCountStack      = new int   [MAX_DEPTH + 1];
    private static final byte[]     soldierCountStack   = new byte  [MAX_DEPTH + 1];
    private static final byte[]     moscoviteCountStack = new byte  [MAX_DEPTH + 1];
    private static final int[]      bestMovesStack      = new int   [MAX_DEPTH + 1];

    private static final short[][]  captureStack        = new short [MAX_DEPTH + 1][MAX_CAPTURES];
    private static final int[][]    movesStack          = new int   [MAX_DEPTH + 1][NB_POSSIBLE_MOVES];
    private static final int[][]    killerMovesStack    = new int   [MAX_DEPTH + 1][2];


    private static final long[][]   zobrist             = new long  [4][81];
    private static final long[]     zobristKeys         = new long  [8];
    private static final long       sideToMove          = rng.nextLong();


    // currently (8 + 4 + 1 + 1 + 4) * (1 << 20) ~= 14.6MB transposition table
    private static final long[]    ttHash               = new long  [NB_TT_ENTRIES];
    private static final float[]   ttScore              = new float [NB_TT_ENTRIES];
    private static final byte[]    ttDepth              = new byte  [NB_TT_ENTRIES];
    private static final byte[]    ttFlag               = new byte  [NB_TT_ENTRIES];
    private static final int[]     ttBestMove           = new int   [NB_TT_ENTRIES];


    private static final int[][]   squareSymmetrics     = new int[8][81];
    static {
        // initialize all square symmetrics
        for (int sq = 0; sq < 81; sq++) {
            int y = sq / 9;
            int x = sq % 9;

            // rotation symmetrics (trigonometric way)
            squareSymmetrics[0][sq] = (  y) * 9 + (  x); //   0 deg
            squareSymmetrics[1][sq] = (8-x) * 9 + (  y); //  90 deg
            squareSymmetrics[2][sq] = (8-y) * 9 + (8-x); // 180 deg
            squareSymmetrics[3][sq] = (  x) * 9 + (8-y); // 270 deg

            // axis symmetrics
            squareSymmetrics[4][sq] = (  y) * 9 + (8-x); // vertical axis
            squareSymmetrics[5][sq] = (8-y) * 9 + (  x); // horizontal axis
            squareSymmetrics[6][sq] = (  x) * 9 + (  y); // top left  -> bottom right diagonal
            squareSymmetrics[7][sq] = (8-x) * 9 + (8-y); // top right -> bottom left diagonal
        }
    }


    private static final SearchTimeoutException TIMEOUT = new SearchTimeoutException();


    private record BestMove(int move, float score) {}



    public static void resetBuffers() {


        Arrays.fill(board, (byte) 0);
        Arrays.fill(kingPosStack, (byte) 0);
        Arrays.fill(captureCountStack, (byte) 0);
        Arrays.fill(moveCountStack, 0);
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

        Arrays.fill(zobristKeys, 0);
    }

    public static void configure(int level, TablutBoard tablutBoard) {
        searchTimeSeconds = level * 2;

        TablutStageModel stageModel = (TablutStageModel) tablutBoard.getModel().getGameStage();
        ruleSet = stageModel.getRuleSet();

        board                   = FastBoard.fromTablutBoard(tablutBoard);
        kingPosStack[0]         = FastBoard.getKingPos(board);
        soldierCountStack[0]    = FastEvaluation.countPieces(board, FastEvaluation.SOLDIER);
        moscoviteCountStack[0]  = FastEvaluation.countPieces(board, FastEvaluation.MOSCOVITE);

        // initialize random numbers
        for (int pieceType : FastBoard.pieceTypes) {
            for (int i = 0; i < 81; i++) {
                zobrist[pieceType][i] = rng.nextLong();
            }
        }


        // xor numbers corresponding to active pieces together
        for (int i = 0; i < 81; i++) {
            if (board[i] != FastBoard.EMPTY) {
//                zobristKey[0] ^= zobrist[board[i]][i];
                xorZobristKeys(zobrist, zobristKeys, board[i], i);
            }
        }
        xorZorbistKeysSideToMove(zobristKeys, sideToMove);
    }


    public static void xorZobristKeys(long[][] zobrist, long[] zobristKeys, int piece, int pos) {
        zobristKeys[0] ^= zobrist[piece][squareSymmetrics[0][pos]];
        zobristKeys[1] ^= zobrist[piece][squareSymmetrics[1][pos]];
        zobristKeys[2] ^= zobrist[piece][squareSymmetrics[2][pos]];
        zobristKeys[3] ^= zobrist[piece][squareSymmetrics[3][pos]];
        zobristKeys[4] ^= zobrist[piece][squareSymmetrics[4][pos]];
        zobristKeys[5] ^= zobrist[piece][squareSymmetrics[5][pos]];
        zobristKeys[6] ^= zobrist[piece][squareSymmetrics[6][pos]];
        zobristKeys[7] ^= zobrist[piece][squareSymmetrics[7][pos]];
    }

    public static void xorZorbistKeysSideToMove(long[] zobristKeys, long sideToMove) {
        zobristKeys[0] ^= sideToMove; zobristKeys[1] ^= sideToMove;
        zobristKeys[2] ^= sideToMove; zobristKeys[3] ^= sideToMove;
        zobristKeys[4] ^= sideToMove; zobristKeys[5] ^= sideToMove;
        zobristKeys[6] ^= sideToMove; zobristKeys[7] ^= sideToMove;
    }

    public static int moveSymmetric(int move, int z) {
        return squareSymmetrics[z][move & 0x7F] | (squareSymmetrics[z][(move >> 7) & 0x7F] << 7);
    }




    public static int findBestMove(int turn, boolean findAlternativeMove) {
        List<BestMove> bestMoves = new ArrayList<>();
        int currentBestMove = 0;
        float prevBestScore = Float.NEGATIVE_INFINITY;


        positionsAnalyzed = 0;
        searchDeadlineNs = System.nanoTime() + searchTimeSeconds * 1000000000L;
        nbTTHits = 0;
        nbSymTTHits = 0;

        ttProbe = 0;
        ttHashMatch = 0;
        ttDepthMatch = 0;

        try {
            for (int depth = 0; depth < MAX_DEPTH-1; depth++) { // iterative deepening
                if (System.nanoTime() >= searchDeadlineNs) break;

                // generate moves with the first move in the list being the current best one
                if (depth == 0)
                    FastBoard.generateMoves(board, turn, 0, moveCountStack, movesStack, killerMovesStack, 0, false, ruleSet);
                else
                    FastBoard.generateMoves(board, turn, 0, moveCountStack, movesStack, killerMovesStack, currentBestMove, true, ruleSet);

                if (turn == 0) {
                    totalLegalMoveCountSwedish += moveCountStack[0];
                    totalMoveGenerationsSwedish++;
                }
                else {
                    totalLegalMoveCountMoscovite += moveCountStack[0];
                    totalMoveGenerationsMoscovite++;
                }


                if (moveCountStack[0] == 0) return -1;


                float iterAlpha = (depth == 0) ? Float.NEGATIVE_INFINITY : prevBestScore - ASPIRATION_WINDOW_DELTA;
                float iterBeta = (depth == 0) ? Float.POSITIVE_INFINITY : prevBestScore + ASPIRATION_WINDOW_DELTA;
                List<BestMove> iterBestMoves = new ArrayList<>();
                float bestScore = Float.NEGATIVE_INFINITY;
                boolean retry; // retry loop that widens the aspiration window on failure

                do {
                    retry = false;
                    bestScore = Float.NEGATIVE_INFINITY;
                    iterBestMoves.clear();

                    // store for aspiration window test before the loop changes them
                    float origAlpha = iterAlpha;
                    float origBeta = iterBeta;


                    for (int i = 0; i < moveCountStack[0]; i++) {
                        int move = movesStack[0][i];

                        kingPosStack[1] = kingPosStack[0];
                        moscoviteCountStack[1] = moscoviteCountStack[0];
                        soldierCountStack[1] = soldierCountStack[0];

                        FastBoard.checkCaptures(board, move, 0, captureCountStack, captureStack, ruleSet);
                        FastBoard.makeMove(
                                board, move, 0, captureCountStack, captureStack, soldierCountStack, moscoviteCountStack,
                                kingPosStack, zobrist, zobristKeys, sideToMove
                        );

                        float score = -negamax(depth+1, 1, (turn + 1) % 2, -iterBeta, -iterAlpha);

                        FastBoard.undoMove(
                                board, move, 0, captureCountStack, captureStack,
                                kingPosStack, zobrist, zobristKeys, sideToMove
                        );

                        if (score > bestScore) {
                            bestScore = score;
                            currentBestMove = move;
                            iterBestMoves.add(new BestMove(move, score));
                        }
                        iterAlpha = Math.max(iterAlpha, score);
                        if (iterAlpha >= iterBeta) break;
                    }


                    if (bestScore <= origAlpha) { // failed low: widen down
                        iterAlpha = Float.NEGATIVE_INFINITY;
                        retry = true;
                    } else if (bestScore >= origBeta) { // failed high : widen up
                        iterBeta = Float.POSITIVE_INFINITY;
                        retry = true;
                    }
                } while (retry && depth > 0);

                bestMoves = iterBestMoves; // replace the best moves list, don't accumulate
                prevBestScore = bestScore;

                System.out.printf("Searched depth %d, analyzed %d positions so far\n\n", depth+1, positionsAnalyzed);
            }

        } catch (SearchTimeoutException e) {
            // ignore, keep the last fully completed iteration
        }

        System.out.printf("Analyzed %d positions in total\n\n", positionsAnalyzed);

//        System.out.printf("Average number of legal moves for swedish : %.3f\n" +
//                          "Average number of legal moves for moscovites : %.3f\n",
//                    (totalLegalMoveCountSwedish / (double) totalMoveGenerationsSwedish),
//                    (totalLegalMoveCountMoscovite / (double) totalMoveGenerationsMoscovite));


        bestMoves = new ArrayList<>(
                bestMoves.stream()
                .sorted((bm1, bm2) -> Double.compare(bm2.score, bm1.score))
                .toList()
        );
        BestMove bestMove = bestMoves.getFirst();

        if (bestMoves.size() > 1) {
            if (findAlternativeMove) {
                bestMove = bestMoves.get(1);
            }
        }

        currentMoveNumber++;


        return bestMove.move;
    }


    public static float negamax(int depth, int ply, int turn, float alpha, float beta) {
        if (System.nanoTime() >= searchDeadlineNs) throw TIMEOUT;

        float win = FastBoard.checkWin(board, ply, kingPosStack, ruleSet);
        if (win != 0) positionsAnalyzed++;
        if (win > 0) return turn == 0 ? VIRTUAL_INF - ply : -VIRTUAL_INF + ply;
        if (win < 0) return turn == 1 ? VIRTUAL_INF - ply : -VIRTUAL_INF + ply;

        boolean hasTTBestMove = false;
        int bestTTMove = 0;


        int ttIndex = (int) (zobristKeys[0] & (NB_TT_ENTRIES - 1));

        if (ttFlag[ttIndex] != 0) {
            ttProbe++;
            if (ttHash[ttIndex] == zobristKeys[0]) {
                ttHashMatch++;
                if (ttDepth[ttIndex] >= depth) {
                    ttDepthMatch++;
                }
            }
        }
        if (ttFlag[ttIndex] != 0 && zobristKeys[0] == ttHash[ttIndex] && ttDepth[ttIndex] >= depth) {
            nbTTHits++;

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
            return FastEvaluation.evaluate(board, turn, ply, soldierCountStack, moscoviteCountStack, kingPosStack, ruleSet);
        }

        float maxScore = -VIRTUAL_INF;


        FastBoard.generateMoves(board, turn, ply, moveCountStack, movesStack, killerMovesStack, bestTTMove, hasTTBestMove, ruleSet);

        if (turn == 0) {
            totalLegalMoveCountSwedish += moveCountStack[ply];
            totalMoveGenerationsSwedish++;
        }
        else {
            totalLegalMoveCountMoscovite += moveCountStack[ply];
            totalMoveGenerationsMoscovite++;
        }

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
            moscoviteCountStack[ply+1]  = moscoviteCountStack[ply];
            soldierCountStack[ply+1]    = soldierCountStack[ply];

            FastBoard.checkCaptures(board, move, ply, captureCountStack, captureStack, ruleSet);
            FastBoard.makeMove(
                    board, move, ply, captureCountStack, captureStack, soldierCountStack, moscoviteCountStack,
                    kingPosStack, zobrist, zobristKeys, sideToMove
            );

            float score = -negamax(depth-1, ply+1, (turn+1) % 2, -beta, -alpha);

            FastBoard.undoMove(
                    board, move, ply, captureCountStack, captureStack, kingPosStack ,
                    zobrist, zobristKeys, sideToMove
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

                for (int z = 0; z < 1; z++) { // store position info for every symmetric of the current board
                    ttIndex = (int) (zobristKeys[z] & (NB_TT_ENTRIES - 1));
                    ttScore[ttIndex] = storeScore;
                    ttHash[ttIndex]  = zobristKeys[z];
                    ttDepth[ttIndex] = (byte) depth;
                    ttFlag[ttIndex]  = LOWER_BOUND;
                }
                break;
            }
        }

        if (maxScore <= alphaOrig) flag = UPPER_BOUND;
        else if (maxScore >= beta) flag = LOWER_BOUND;
        else                       flag = EXACT;


        float storeMaxScore = maxScore;
        if (storeMaxScore > VIRTUAL_INF - 100)      storeMaxScore += ply;
        else if (storeMaxScore < -VIRTUAL_INF + 100) storeMaxScore -= ply;

        for (int z = 0; z < 1; z++) {
            ttIndex = (int) (zobristKeys[z] & (NB_TT_ENTRIES - 1));
            ttScore[ttIndex]    = storeMaxScore;
            ttHash[ttIndex]     = zobristKeys[z];
            ttDepth[ttIndex]    = (byte) depth;
            ttFlag[ttIndex]     = flag;
            ttBestMove[ttIndex] = moveSymmetric(bestMove, z);
        }

        return maxScore;
    }
}
