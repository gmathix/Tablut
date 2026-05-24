#!/usr/bin/env bash
# ===================================================================================================
# run_game.sh  ·  Run one Tablut Bot-vs-Bot game and record the result as CSV - Generated with Claude
# ===================================================================================================
# Usage:
#   ./run_game.sh <green_engine> <green_level> \
#                 <yellow_engine> <yellow_level> \
#                 <ruleset_bits> <output_csv>
#
# Engine codes:  0 = Negamax  |  1 = MonteCarlo  |  2 = NegaMonteCarlo
#
# Ruleset bits (OR them together for combined rules):
#   bit 0 → 1  : ConstrainedKingSquares
#   bit 1 → 2  : ConstrainedKingMoves
#   bit 2 → 4  : CornerKingEscapes
#   e.g. 5 (0b101) activates ConstrainedKingSquares + CornerKingEscapes
#
# CSV columns written:
#   game_id, timestamp, green_engine, green_level,
#   yellow_engine, yellow_level, ruleset_bits,
#   first_player, winner, duration_s, status
#
# Exit codes:  0 = game recorded  |  1 = timeout  |  2 = Java error
#              3 = result unparseable (check log, see WIN DETECTION note below)
# =============================================================================
set -uo pipefail

# ── CONFIGURE ─────────────────────────────────────────────────────────────────
JAVA_CMD="${JAVA_CMD:-java}"
CLASSPATH="${CLASSPATH:-.:lib/*}"     # path to your compiled classes / fat-jar
MAIN_CLASS="${MAIN_CLASS:-TablutConsole}"
LOG_DIR="${LOG_DIR:-./logs}"

# ── WIN DETECTION ─────────────────────────────────────────────────────────────
# TablutConsole exits with code 0 for green win, 1 for yellow win.
# No stdout parsing needed for the result; only first_player is read from the log.
# Caveat: a Java exception also exits with code 1. If yellow's win rate looks
# suspiciously high, check logs/ for stack traces and filter status=java_error rows.
# ─────────────────────────────────────────────────────────────────────────────

# ── Arguments ─────────────────────────────────────────────────────────────────
GREEN_ENGINE="${1:?  missing arg1: green engine  (0=NM 1=MC 2=NMC)}"
GREEN_LEVEL="${2:?   missing arg2: green level   (0-10)}"
YELLOW_ENGINE="${3:? missing arg3: yellow engine (0=NM 1=MC 2=NMC)}"
YELLOW_LEVEL="${4:?  missing arg4: yellow level  (0-10)}"
RULESET_BITS="${5:-0}"
OUTPUT_CSV="${6:?    missing arg6: output CSV path}"

readonly ENGINE_NAMES=("negamax" "montecarlo" "negamontecarlo")
mkdir -p "$LOG_DIR"


# ── stdin sequence for TablutConsole launched as  java TablutConsole 2 ────────
# Mode 2 is passed as CLI arg; everything below comes from stdin:
#
#   [optional rule toggles]   ← one line per rule to toggle (1, 2, or 3)
#   0                         ← confirm ruleset
#   <green_engine>            ← bot-type selection for player 0 (Green)
#   <green_level>             ← level for player 0
#   <yellow_engine>           ← bot-type selection for player 1 (Yellow)
#   <yellow_level>            ← level for player 1
#
build_stdin() {
    local rs=""
    if (( (RULESET_BITS & 1) != 0 )); then rs+="1\n"; fi
    if (( (RULESET_BITS & 2) != 0 )); then rs+="2\n"; fi
    if (( (RULESET_BITS & 4) != 0 )); then rs+="3\n"; fi
    rs+="0\n"   # confirm ruleset selection

    printf '%b%s\n%s\n%s\n%s\n' \
        "$rs" \
        "$GREEN_ENGINE" "$GREEN_LEVEL" \
        "$YELLOW_ENGINE" "$YELLOW_LEVEL"
}


# ── Compute a safe per-matchup timeout (seconds) ──────────────────────────────
# For Negamax the timeout is based on empirical per-move estimates at each depth.
# For MCTS the configured time-limit IS the per-move budget; we add 25% overhead.
estimate_timeout() {
    local max_s=1
    for pair in "${GREEN_ENGINE}:${GREEN_LEVEL}" "${YELLOW_ENGINE}:${YELLOW_LEVEL}"; do
        local eng="${pair%%:*}" lvl="${pair##*:}" s
        if (( eng == 0 )); then                   # Negamax
            case "$lvl" in
                0|1) s=2  ;;  2) s=4  ;;  3|4) s=8  ;;
                5)   s=15 ;;  6|7) s=30 ;;  8|9) s=60 ;;
                10)  s=120 ;; *) s=15 ;;
            esac
        else                                       # MCTS (time-limited)
            case "$lvl" in
                0)  s=1  ;;  1) s=2  ;;  2) s=3  ;;  3) s=4  ;;  4) s=6  ;;
                5)  s=7  ;;  6) s=9  ;;  7) s=11 ;;  8) s=13 ;;  9) s=17 ;;
                10) s=20 ;;  *) s=8  ;;
            esac
        fi
        if (( s > max_s )); then max_s=$s; fi
    done
    # 60 total plies × worst-case per-move time × 1.5 safety factor
    echo $(( max_s * 60 * 3 / 2 ))
}


# ── Run ───────────────────────────────────────────────────────────────────────
GAME_ID="g_$(date +%s%3N)_$$"     # millisecond timestamp + PID
LOG_FILE="${LOG_DIR}/${GAME_ID}.log"
TIMEOUT_S=$(estimate_timeout)

T_START=$(date +%s)
set +e
build_stdin | timeout "$TIMEOUT_S" \
    "$JAVA_CMD" -cp "$CLASSPATH" "$MAIN_CLASS" 2 \
    > "$LOG_FILE" 2>&1
EXIT_CODE=$?
set -e
T_END=$(date +%s)
DURATION=$(( T_END - T_START ))


# ── Parse ─────────────────────────────────────────────────────────────────────
FIRST_PLAYER="unknown"
WINNER="unknown"
STATUS="ok"

if   [[ $EXIT_CODE -eq 124 ]]; then
    STATUS="timeout"; WINNER="timeout"
elif [[ $EXIT_CODE -eq   0 ]]; then
    WINNER="green"
elif [[ $EXIT_CODE -eq   1 ]]; then
    WINNER="yellow"
else
    # Exit codes 2+ are genuine Java errors (exception, OOM, etc.)
    STATUS="java_error"; WINNER="error"
fi

# First player is always read from stdout regardless of outcome
if   grep -qiE "Green makes the first move"  "$LOG_FILE"; then FIRST_PLAYER="green"
elif grep -qiE "Yellow makes the first move" "$LOG_FILE"; then FIRST_PLAYER="yellow"
fi

# ── Append CSV row (flock makes this safe for future parallel use) ────────────
TIMESTAMP="$(date -Iseconds)"
ROW="${GAME_ID},${TIMESTAMP},${ENGINE_NAMES[$GREEN_ENGINE]},${GREEN_LEVEL},${ENGINE_NAMES[$YELLOW_ENGINE]},${YELLOW_LEVEL},${RULESET_BITS},${FIRST_PLAYER},${WINNER},${DURATION},${STATUS}"

(
    flock -x 200
    if [[ ! -s "$OUTPUT_CSV" ]]; then
        echo "game_id,timestamp,green_engine,green_level,yellow_engine,yellow_level,ruleset_bits,first_player,winner,duration_s,status" >> "$OUTPUT_CSV"
    fi
    echo "$ROW" >> "$OUTPUT_CSV"
) 200>"${OUTPUT_CSV}.lock"

# Echo to stdout so bench.sh can display live progress
echo "$ROW"

# Map status to exit code
case "$STATUS" in
    ok)         exit 0 ;;
    timeout)    exit 1 ;;
    java_error) exit 2 ;;
    no_result)  exit 3 ;;
    *)          exit 3 ;;
esac