import boardifier.model.Model;
import control.algos.MonteCarlo;
import control.algos.NegaMonteCarlo;
import control.algos.Negamax;
import control.algos.OpeningPlayer;
import control.algos.OsarracinoBridge;
import model.Move;
import model.Pawn;
import model.RuleSets;
import model.TablutBoard;
import model.TablutStageModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * headless Tablut match runner to automate bot matches
 *
 * usage:
 *   java HeadlessTablut <ruleset> <swedishBot> <moscoviteBot> <swedishLevel> <moscoviteLevel> [output.tpgn]
 *
 * bot names accepted:
 *   negamax, montecarlo, negamontecarlo, osarracino
 *   or their numeric ids: 0, 1, 2, 3
 *
 * exit codes:
 *   0 = swedish wins
 *   1 = moscovite wins
 *   2 = draw / max moves (rare) / error
 */
public class HeadlessTablut {

    private static final int TURN_SWEDISH               = 0;
    private static final int TURN_MOSCOVITE             = 1;

    private static final int BOT_NEGAMAX                = 0;
    private static final int BOT_MONTECARLO             = 1;
    private static final int BOT_NEGAMONTECARLO         = 2;
    private static final int BOT_OSARRACINO             = 3;

    private static final int MAX_MOVES                  = 1000;
    private static final int BOARD_HISTORY_SIZE         = 50;
    private static final int BOARD_REPETITION_THRESHOLD = 3;

    private record BotConfig(int type, int level, String label, int port) {}

    public static void main(String[] args) {
        int exitCode = 2;
        try {
            if (args.length < 5) {
                System.err.println("Usage:");
                System.err.println("  java HeadlessTablut <ruleset> <swedishBot> <moscoviteBot> <swedishLevel> <moscoviteLevel> [output.tpgn] [swedishPort] [moscovitePort]");
                System.err.println();
                System.err.println("Examples:");
                System.err.println("  java HeadlessTablut 1 negamax montecarlo 6 5 out.tpgn");
                System.err.println("  java HeadlessTablut ashton 2 0 7 6 game.tpgn");
                System.exit(2);
                return;
            }

            int ruleSet = parseRuleSet(args[0]);
            BotConfig swedishBot = parseBotConfig(args[1], Integer.parseInt(args[3]), "Swedish", Integer.parseInt(args[6]));
            BotConfig moscoviteBot = parseBotConfig(args[2], Integer.parseInt(args[4]), "Moscovite", Integer.parseInt(args[7]));
            Path output = Path.of(args.length >= 6 ? args[5] : "match.tpgn");

            exitCode = runMatch(ruleSet, swedishBot, moscoviteBot, output);
        } catch (Exception e) {
            System.err.println("[HeadlessTablut] " + e.getMessage());
            e.printStackTrace();
            exitCode = 2;
        } finally {
            try {
                OsarracinoBridge.stop();
            } catch (Throwable ignored) {
                // if osarracino was never started, good. one less little brat to clean up
            }
        }

        System.exit(exitCode);
    }

    private static int runMatch(int ruleSet, BotConfig swedishBot, BotConfig moscoviteBot, Path outputFile) throws IOException {
        Model model = new Model();
        model.addComputerPlayer(swedishBot.label());
        model.addComputerPlayer(moscoviteBot.label());

        TablutStageModel stage = new TablutStageModel("tablut", model);
        stage.setMode(TablutStageModel.MODE_PLAY);
        stage.setRuleSet(ruleSet);

        TablutBoard board = new TablutBoard(0, 0, stage);
        stage.setBoard(board);
        initBoard(stage, board);

        model.startGame(stage);

        int startingSide = RuleSets.isAshtonRules(ruleSet) ? TURN_MOSCOVITE : TURN_SWEDISH;
        model.setIdPlayer(startingSide);
        model.setIdWinner(-1);

        String swedishName = swedishBot.label() + "@" + swedishBot.level();
        String moscoviteName = moscoviteBot.label() + "@" + moscoviteBot.level();
        MoveHistoryLite history = new MoveHistoryLite(swedishName, moscoviteName, startingSide, ruleSet);

        String[] boardHistory = new String[BOARD_HISTORY_SIZE];
        Arrays.fill(boardHistory, "");
        int boardHistoryIndex = 0;
        boolean boardRepeated = false;

        int ply = 0;

        while (model.getIdWinner() == -1 && ply < MAX_MOVES) {
            int turn = model.getIdPlayer();
            BotConfig bot = (turn == TURN_SWEDISH) ? swedishBot : moscoviteBot;

            int moveInt = decide(bot, board, turn, history, boardRepeated);
            if (moveInt == -1) {
                model.setIdWinner(1 - turn);
                break;
            }

            int src = moveInt & 0x7F;
            int dst = (moveInt >> 7) & 0x7F;

            Move move = new Move(board, src % 9, src / 9, dst % 9, dst / 9);
            history.addMove(move);
            board.applyMove(move);

            int winner = computePartyResult(board, ruleSet, turn, move);
            if (winner != -1) {
                model.setIdWinner(winner);
                break;
            }

            model.setNextPlayer();
            boardHistoryIndex = (boardHistoryIndex + 1) % BOARD_HISTORY_SIZE;
            boardRepeated = processBoardRepetition(board, boardHistory, boardHistoryIndex);
            ply++;
        }

        int winner = model.getIdWinner();
        String pgn = buildTpgn(history, winner);
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.toAbsolutePath().getParent());
        }
        Files.writeString(outputFile, pgn, StandardCharsets.UTF_8);

        if (winner == -1) {
            return 2;
        }
        return winner;
    }

    private static int decide(BotConfig bot, TablutBoard board, int turn, MoveHistoryLite history, boolean boardRepeated) {
        String moveSeq = history.moveSequence().trim();
        int openingMove = OpeningPlayer.makeOpeningMove(moveSeq);
        boolean greenFirstMove = moveSeq.isEmpty() && turn == TURN_SWEDISH;

        if (openingMove != -1 && !greenFirstMove) {
            return openingMove;
        }

        switch (bot.type) {
            case BOT_NEGAMAX -> {
                Negamax.resetBuffers();
                Negamax.configure(bot.level(), board);
                return Negamax.findBestMove(turn, boardRepeated);
            }
            case BOT_MONTECARLO -> {
                MonteCarlo.resetBuffers();
                MonteCarlo.configure(bot.level(), board);
                return MonteCarlo.findBestMove(board, turn, boardRepeated);
            }
            case BOT_NEGAMONTECARLO -> {
                NegaMonteCarlo.resetBuffers();
                NegaMonteCarlo.configure(bot.level(), board);
                return NegaMonteCarlo.findBestMove(board, turn, boardRepeated);
            }
            case BOT_OSARRACINO -> {
                OsarracinoBridge.startEngine(turn, bot.level());
                return OsarracinoBridge.requestMove(board, turn);
            }
            default -> throw new IllegalArgumentException("Unknown bot type: " + bot.type());
        }
    }

    private static int computePartyResult(TablutBoard board, int ruleSet, int currentTurn, Move lastMove) {
        int kingX = board.getKingX();
        int kingY = board.getKingY();

        int[] dyVals = {-1, 0, 1, 0};
        int[] dxVals = {0, 1, 0, -1};



        if (kingY == 0 || kingY == 8 || kingX == 0 || kingX == 8) {
            if (RuleSets.isConstrainedKingSquares(ruleSet) || RuleSets.isCornerKingEscapes(ruleSet)) {
                if (RuleSets.isConstrainedKingMoves(ruleSet)) {
                    if (RuleSets.cornerSquares.contains(kingY * 9 + kingX)) {
                        return TURN_SWEDISH;
                    }
                } else if (RuleSets.isConstrainedKingSquares(ruleSet)) {
                    if (!RuleSets.constrainedKingSquares.contains(kingY * 9 + kingX)) {
                        return TURN_SWEDISH;
                    }
                }
            } else {
                return TURN_SWEDISH;
            }
        }


        int nbSurrounding = 0;
        int surroundMask = 0;
        boolean kingInCenter = kingY == 4 && kingX == 4;
        boolean hasCenterNeighbor = false;
        boolean playedPawnNextToKing = false;

        for (int i = 0; i < 4; i++) {
            int y = kingY + dyVals[i];
            int x = kingX + dxVals[i];
            if (y < 0 || y > 8 || x < 0 || x > 8) continue;

            if (y == 4 && x == 4) {
                hasCenterNeighbor = true;
                nbSurrounding++;
                surroundMask |= 1 << i;
            } else if ((board.getElement(y, x) instanceof Pawn p && p.getColor() == Pawn.PAWN_MOSCOVITE) ||
                    (RuleSets.isAshtonRules(ruleSet) && RuleSets.isCampSquare(y * 9 + x))) {
                nbSurrounding++;
                surroundMask |= 1 << i;
            }

            if (lastMove.dstY() + dyVals[i] == kingY && lastMove.dstX() + dxVals[i] == kingX) {
                playedPawnNextToKing = true;
            }
        }

        if (currentTurn == TURN_MOSCOVITE && playedPawnNextToKing) {
            if (RuleSets.isAshtonRules(ruleSet)) {
                if ((kingInCenter && nbSurrounding == 4) ||
                        (hasCenterNeighbor && nbSurrounding == 4) ||
                        (!kingInCenter && !hasCenterNeighbor && nbSurrounding >= 2 &&
                                ((surroundMask & 0b1010) == 0b1010 || (surroundMask & 0b0101) == 0b0101))) {
                    return TURN_MOSCOVITE;
                }
            } else if (nbSurrounding == 4) {
                return TURN_MOSCOVITE;
            }
        }


        for (int side = 0; side < 2; side++) {
            List<Integer> legalMoves = new ArrayList<>();
            for (int y = 0; y < 9; y++) {
                for (int x = 0; x < 9; x++) {
                    if (board.getElement(y, x) instanceof Pawn p) {
                        if ((side == TURN_SWEDISH && p.getColor() != Pawn.PAWN_MOSCOVITE) ||
                                (side == TURN_MOSCOVITE && p.getColor() == Pawn.PAWN_MOSCOVITE)) {
                            legalMoves.addAll(board.computeValidCells(p.getNumber()).stream()
                                    .map(pt -> pt.y * 9 + pt.x)
                                    .toList());
                        }
                    }
                }
            }
            if (legalMoves.isEmpty()) {
                return (side + 1) % 2;
            }
        }

        return -1;
    }

    private static boolean processBoardRepetition(TablutBoard board, String[] boardHistory, int boardHistoryIndex) {
        String current = board.getStringRepresentation();
        boardHistory[boardHistoryIndex] = current;

        int matches = 0;
        for (int i = 0; i < boardHistory.length; i++) {
            if (i == boardHistoryIndex) continue;
            if (current.equals(boardHistory[i])) {
                matches++;
            }
        }
        return matches >= BOARD_REPETITION_THRESHOLD;
    }

    private static void initBoard(TablutStageModel stage, TablutBoard board) {
        Pawn[] moscovites = new Pawn[16];
        Pawn[] soldiers = new Pawn[8];
        Pawn[] kings = new Pawn[1];

        for (int i = 0; i < 16; i++) {
            moscovites[i] = new Pawn(i + 1, Pawn.PAWN_MOSCOVITE, stage);
        }
        for (int i = 0; i < 8; i++) {
            soldiers[i] = new Pawn(17 + i, Pawn.PAWN_SOLDIER, stage);
        }
        kings[0] = new Pawn(25, Pawn.PAWN_KING, stage);

        stage.setMoscovitePawns(moscovites);
        stage.setSoldierPawns(soldiers);
        stage.setKingPawns(kings);

        int moscoviteIdx = 0;
        int soldierIdx = 0;
        for (int y = 0; y < TablutBoard.BOARD_SIZE; y++) {
            for (int x = 0; x < TablutBoard.BOARD_SIZE; x++) {
                int type = TablutBoard.startingBoard[y][x];
                if (type == Pawn.PAWN_MOSCOVITE) {
                    board.addElement(moscovites[moscoviteIdx], y, x);
                    moscovites[moscoviteIdx].setBoardX(x);
                    moscovites[moscoviteIdx].setBoardY(y);
                    moscoviteIdx++;
                } else if (type == Pawn.PAWN_SOLDIER) {
                    board.addElement(soldiers[soldierIdx], y, x);
                    soldiers[soldierIdx].setBoardX(x);
                    soldiers[soldierIdx].setBoardY(y);
                    soldierIdx++;
                } else if (type == Pawn.PAWN_KING) {
                    board.addElement(kings[0], y, x);
                    kings[0].setBoardX(x);
                    kings[0].setBoardY(y);
                }
            }
        }
    }

    private static int parseRuleSet(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "ashton", "ashton_rules", "ashtonrules" -> RuleSets.RULESET_ASHTON_RULES;
            case "normal", "default" -> RuleSets.RULESET_NORMAL;
            default -> Integer.parseInt(raw);
        };
    }

    private static BotConfig parseBotConfig(String raw, int level, String sideName, int port) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        int type = switch (s) {
            case "0", "negamax" -> BOT_NEGAMAX;
            case "1", "montecarlo", "monte-carlo" -> BOT_MONTECARLO;
            case "2", "negamontecarlo", "nega-monte-carlo", "nega_mc" -> BOT_NEGAMONTECARLO;
            case "3", "osarracino" -> BOT_OSARRACINO;
            default -> throw new IllegalArgumentException("Unknown " + sideName + " bot: " + raw);
        };
        return new BotConfig(type, level, raw, port);
    }

    private static String buildTpgn(MoveHistoryLite history, int winner) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[SwedishPlayer \"%s\"]\n", history.swedishPlayer()));
        sb.append(String.format("[MoscovitePlayer \"%s\"]\n", history.moscovitePlayer()));
        sb.append(String.format("[StartingSide \"%d\"]\n", history.startingSide()));
        sb.append(String.format("[Ruleset \"%d\"]\n", history.ruleSet()));
        sb.append(String.format("[Result \"%s\"]\n\n", resultString(winner)));

        int moveNumber = 1;
        for (int i = 0; i < history.moves().size(); i++) {
            if (i % 2 == 0) {
                sb.append(moveNumber++).append(". ").append(history.moves().get(i)).append(" ");
            } else {
                sb.append(history.moves().get(i)).append("\n");
            }
        }

        if (history.moves().size() % 2 == 1) {
            sb.append("\n");
        }
        sb.append(resultString(winner)).append("\n");
        return sb.toString();
    }

    private static String resultString(int winner) {
        return switch (winner) {
            case TURN_SWEDISH -> "1-0";
            case TURN_MOSCOVITE -> "0-1";
            default -> "*";
        };
    }

    private record MoveHistoryLite(List<Move> moves,
                                   String swedishPlayer,
                                   String moscovitePlayer,
                                   int startingSide,
                                   int ruleSet) {
        MoveHistoryLite(String swedishPlayer, String moscovitePlayer, int startingSide, int ruleSet) {
            this(new ArrayList<>(), swedishPlayer, moscovitePlayer, startingSide, ruleSet);
        }

        void addMove(Move move) {
            moves.add(move);
        }

        String moveSequence() {
            StringBuilder sb = new StringBuilder();
            for (Move move : moves) {
                sb.append(move).append(' ');
            }
            return sb.toString();
        }
    }
}
