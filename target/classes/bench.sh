#!/usr/bin/env bash
# =============================================================================
# bench.sh  ·  Tablut engine benchmark orchestrator - Generated with Claude
# =============================================================================
# Usage:
#   ./bench.sh --machine <a|b> [options]
#
# Options:
#   --machine  <a|b>       Required. Which half of the matrix this machine runs.
#   --output   <file>      CSV output path. Default: ./results_<machine>.csv
#   --game-script <path>   Path to run_game.sh. Default: ./run_game.sh
#   --resume               Skip matchups already present in the output CSV.
#   --dry-run              Print the work plan without running anything.
#   --only-matchup <label> Run only the matchup with this label (for testing).
#
# Results from both machines can be merged afterwards with:
#   head -1 results_a.csv > results.csv
#   tail -n +2 results_a.csv >> results.csv
#   tail -n +2 results_b.csv >> results.csv
# =============================================================================
set -uo pipefail


# =============================================================================
# CONFIGURE
# =============================================================================
export JAVA_CMD="${JAVA_CMD:-java}"
export CLASSPATH="${CLASSPATH:-.:lib/*}"
export MAIN_CLASS="${MAIN_CLASS:-TablutConsole}"
export LOG_DIR="${LOG_DIR:-./logs}"

# Win-detection patterns forwarded to run_game.sh (must match what your Java
# build prints; see the note in run_game.sh)
export GREEN_WIN_PATTERN="${GREEN_WIN_PATTERN:-WINNER:GREEN}"
export YELLOW_WIN_PATTERN="${YELLOW_WIN_PATTERN:-WINNER:YELLOW}"


# =============================================================================
# TEST MATRIX
# =============================================================================
# Format per entry (7 space-separated fields):
#   green_engine  green_level  yellow_engine  yellow_level  num_games  ruleset_bits  label
#
# Engine codes:  0=Negamax  1=MonteCarlo  2=NegaMonteCarlo
#
# Ruleset bits:  0=base  1=ConstrainedKingSquares  2=ConstrainedKingMoves
#                4=CornerKingEscapes  (OR together for combinations)
#
# Sample sizes come from the two-proportion z-test (α=0.05, power=0.80),
# targeting detection of ~20 pp gaps at the level where time allows more games
# and ~35 pp gaps at high levels where per-game time is the bottleneck.
# All matchups are played with color-swapping built into the random first-player
# selection in TablutConsole; the CSV records first_player so you can verify
# balance post-hoc and reweight if needed.
#
MATRIX=(
# ── 1. Same-level, cross-engine, base rules ─────────────────────────────────
# Negamax vs NegaMonteCarlo
"0  1   2  1   20  0  NM1_vs_NMC1"
"0  3   2  3   20  0  NM3_vs_NMC3"
"0  5   2  5   20  0  NM5_vs_NMC5"
"0  7   2  7    20  0  NM7_vs_NMC7"
"0  10  2  10   20  0  NM10_vs_NMC10"

# Negamax vs MonteCarlo
"0  1   1  1   20  0  NM1_vs_MC1"
"0  3   1  3   20  0  NM3_vs_MC3"
"0  5   1  5   20  0  NM5_vs_MC5"
"0  7   1  7    20  0  NM7_vs_MC7"
"0  10  1  10   20  0  NM10_vs_MC10"

# NegaMonteCarlo vs MonteCarlo
"2  1   1  1   20  0  NMC1_vs_MC1"
"2  3   1  3   20  0  NMC3_vs_MC3"
"2  5   1  5   20  0  NMC5_vs_MC5"
"2  7   1  7    20  0  NMC7_vs_MC7"
"2  10  1  10   20  0  NMC10_vs_MC10"

# ── 2. Off-diagonal: how many levels compensate for weaker engine? ───────────
# Each pair tests: does level N+2 of the weaker engine beat level N of the stronger?
"2  3   0  5   20  0  NMC3_vs_NM5"
"2  5   0  7   20  0  NMC5_vs_NM7"
"1  3   0  5   20  0  MC3_vs_NM5"
"1  5   0  7   20  0  MC5_vs_NM7"
"1  3   2  5   20  0  MC3_vs_NMC5"
"1  5   2  7   20  0  MC5_vs_NMC7"

# ── 3. Variant matchups: same engines at level 5, all 7 non-base rulesets ────
# Ruleset bit meanings:
#   1 = ConstrainedKingSquares only
#   2 = ConstrainedKingMoves only
#   3 = ConstrainedKingSquares + ConstrainedKingMoves
#   4 = CornerKingEscapes only
#   5 = ConstrainedKingSquares + CornerKingEscapes
#   6 = ConstrainedKingMoves + CornerKingEscapes
#   7 = all three rules active

# NM vs NMC, level 5, all variants
"0  5  2  5  20  1  NM5_vs_NMC5_rs1"
"0  5  2  5  20  2  NM5_vs_NMC5_rs2"
"0  5  2  5  20  3  NM5_vs_NMC5_rs3"
"0  5  2  5  20  4  NM5_vs_NMC5_rs4"
"0  5  2  5  20  5  NM5_vs_NMC5_rs5"
"0  5  2  5  20 6  NM5_vs_NMC5_rs6"
"0  5  2  5  20  7  NM5_vs_NMC5_rs7"

# NM vs MC, level 5, all variants
"0  5  1  5  20  1  NM5_vs_MC5_rs1"
"0  5  1  5  20  2  NM5_vs_MC5_rs2"
"0  5  1  5  20  3  NM5_vs_MC5_rs3"
"0  5  1  5  20  4  NM5_vs_MC5_rs4"
"0  5  1  5  20  5  NM5_vs_MC5_rs5"
"0  5  1  5  20  6  NM5_vs_MC5_rs6"
"0  5  1  5  20  7  NM5_vs_MC5_rs7"

# NMC vs MC, level 5, all variants
"2  5  1  5  20  1  NMC5_vs_MC5_rs1"
"2  5  1  5  20  2  NMC5_vs_MC5_rs2"
"2  5  1  5  20  3  NMC5_vs_MC5_rs3"
"2  5  1  5  20  4  NMC5_vs_MC5_rs4"
"2  5  1  5  20  5  NMC5_vs_MC5_rs5"
"2  5  1  5  20  6  NMC5_vs_MC5_rs6"
"2  5  1  5  20  7  NMC5_vs_MC5_rs7"
)
# Total: 42 matchups, ~4470 games
#
# Machine assignment: even indices (0,2,4,...) → machine A
#                     odd  indices (1,3,5,...) → machine B
#
# Approximate time balance (rough, Negamax estimates are very conservative):
#   Machine A: ~8-10 h  (gets NM10_vs_NMC10, NMC10_vs_MC10 + lighter work)
#   Machine B: ~8-10 h  (gets NM10_vs_MC10, heavy off-diagonals + variants)
#
# If one machine is significantly slower, use --only-matchup to run individual
# matchups and fill the remaining time manually.


# =============================================================================
# ARGUMENT PARSING
# =============================================================================
MACHINE=""
OUTPUT_CSV=""
GAME_SCRIPT="./run_game.sh"
RESUME=false
DRY_RUN=false
ONLY_MATCHUP=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --machine)      MACHINE="$2";       shift 2 ;;
        --output)       OUTPUT_CSV="$2";    shift 2 ;;
        --game-script)  GAME_SCRIPT="$2";   shift 2 ;;
        --resume)       RESUME=true;        shift   ;;
        --dry-run)      DRY_RUN=true;       shift   ;;
        --only-matchup) ONLY_MATCHUP="$2";  shift 2 ;;
        *) echo "Unknown option: $1" >&2; exit 1 ;;
    esac
done

if [[ -z "$MACHINE" ]]; then
    echo "Error: --machine <a|b> is required." >&2
    exit 1
fi
if [[ "$MACHINE" != "a" && "$MACHINE" != "b" ]]; then
    echo "Error: --machine must be 'a' or 'b'." >&2
    exit 1
fi
if [[ -z "$OUTPUT_CSV" ]]; then
    OUTPUT_CSV="./results_${MACHINE}.csv"
fi
if [[ ! -x "$GAME_SCRIPT" ]]; then
    echo "Error: run_game.sh not found or not executable at: $GAME_SCRIPT" >&2
    echo "       Run:  chmod +x $GAME_SCRIPT" >&2
    exit 1
fi

mkdir -p "$(dirname "$OUTPUT_CSV")" "$LOG_DIR"


# =============================================================================
# HELPERS
# =============================================================================

# Estimate total seconds for one matchup (used in the dry-run plan)
estimate_matchup_time() {
    local ge="$1" gl="$2" ye="$3" yl="$4" ng="$5"
    local max_s=1
    for pair in "${ge}:${gl}" "${ye}:${yl}"; do
        local eng="${pair%%:*}" lvl="${pair##*:}" s
        if (( eng == 0 )); then
            case "$lvl" in
                0|1) s=2  ;; 2) s=4  ;; 3|4) s=8  ;; 5) s=15 ;;
                6|7) s=30 ;; 8|9) s=60 ;; 10) s=120 ;; *) s=15 ;;
            esac
        else
            case "$lvl" in
                0) s=1 ;; 1) s=2 ;; 2) s=3  ;; 3) s=4  ;; 4) s=6  ;;
                5) s=7 ;; 6) s=9 ;; 7) s=11 ;; 8) s=13 ;; 9) s=17 ;; 10) s=20 ;;
                *) s=8 ;;
            esac
        fi
        if (( s > max_s )); then max_s=$s; fi
    done
    # 60 plies × per-move estimate × num_games
    echo $(( max_s * 60 * ng ))
}

format_duration() {
    local s="$1"
    printf '%dh %02dm' $(( s/3600 )) $(( (s%3600)/60 ))
}

# Count games already recorded for a matchup label in the CSV
games_done() {
    local label="$1"
    if [[ ! -f "$OUTPUT_CSV" ]]; then echo 0; return; fi
    # The label isn't in the CSV directly; match by engine/level combo instead.
    # We pass ge, gl, ye, yl as args 2-5 for the grep
    local ge="$2" gl="$3" ye="$4" yl="$5" rs="$6"
    local en=("negamax" "montecarlo" "negamontecarlo")
    grep -c "^[^,]*,[^,]*,${en[$ge]},${gl},${en[$ye]},${yl},${rs}," "$OUTPUT_CSV" 2>/dev/null || true
}


# =============================================================================
# WORK PLAN
# =============================================================================

# Select this machine's subset (even index → A, odd index → B)
declare -a MY_WORK=()
MACHINE_IDX=0
for entry in "${MATRIX[@]}"; do
    if [[ "$MACHINE" == "a" && $(( MACHINE_IDX % 2 )) -eq 0 ]] || \
       [[ "$MACHINE" == "b" && $(( MACHINE_IDX % 2 )) -eq 1 ]]; then
        MY_WORK+=("$entry")
    fi
    (( MACHINE_IDX++ )) || true
done

# Filter to a single matchup if --only-matchup was passed
if [[ -n "$ONLY_MATCHUP" ]]; then
    declare -a FILTERED=()
    for entry in "${MATRIX[@]}"; do    # search full matrix, not just this machine's
        read -r ge gl ye yl ng rs label <<< "$entry"
        if [[ "$label" == "$ONLY_MATCHUP" ]]; then
            FILTERED+=("$entry")
        fi
    done
    MY_WORK=("${FILTERED[@]}")
    if [[ ${#MY_WORK[@]} -eq 0 ]]; then
        echo "Error: no matchup found with label '$ONLY_MATCHUP'" >&2
        exit 1
    fi
fi


# =============================================================================
# DRY-RUN: print plan and time estimate
# =============================================================================
TOTAL_GAMES=0
TOTAL_EST_S=0

echo ""
echo "════════════════════════════════════════════════════════════════"
echo " Tablut Benchmark — Machine ${MACHINE^^}   $(date)"
echo "════════════════════════════════════════════════════════════════"
echo " Output CSV : $OUTPUT_CSV"
echo " Log dir    : $LOG_DIR"
echo " Resume     : $RESUME"
echo "────────────────────────────────────────────────────────────────"
printf " %-32s  %5s  %3s  %10s\n" "Matchup" "Games" "RS" "Est. time"
echo "────────────────────────────────────────────────────────────────"

for entry in "${MY_WORK[@]}"; do
    read -r ge gl ye yl ng rs label <<< "$entry"
    est=$(estimate_matchup_time "$ge" "$gl" "$ye" "$yl" "$ng")
    TOTAL_GAMES=$(( TOTAL_GAMES + ng ))
    TOTAL_EST_S=$(( TOTAL_EST_S + est ))
    if $RESUME; then
        done_count=$(games_done "$label" "$ge" "$gl" "$ye" "$yl" "$rs")
        remaining=$(( ng - done_count ))
        if (( remaining < 0 )); then remaining=0; fi
        printf " %-32s  %5d  %3d  %10s  (%d done)\n" \
            "$label" "$ng" "$rs" "$(format_duration $est)" "$done_count"
    else
        printf " %-32s  %5d  %3d  %10s\n" \
            "$label" "$ng" "$rs" "$(format_duration $est)"
    fi
done

echo "────────────────────────────────────────────────────────────────"
printf " %-32s  %5d       %10s\n" "TOTAL" "$TOTAL_GAMES" "$(format_duration $TOTAL_EST_S)"
echo "════════════════════════════════════════════════════════════════"
echo ""

if $DRY_RUN; then
    echo "[dry-run] Exiting without running games."
    exit 0
fi


# =============================================================================
# MAIN LOOP
# =============================================================================
GLOBAL_START=$(date +%s)
MATCHUP_IDX=0
MATCHUP_TOTAL=${#MY_WORK[@]}

for entry in "${MY_WORK[@]}"; do
    read -r ge gl ye yl ng rs label <<< "$entry"
    (( MATCHUP_IDX++ )) || true

    # ── Resume: count how many games of this matchup are already in the CSV ──
    start_game=1
    if $RESUME; then
        done_count=$(games_done "$label" "$ge" "$gl" "$ye" "$yl" "$rs")
        start_game=$(( done_count + 1 ))
        if (( start_game > ng )); then
            echo "[$(date +%H:%M:%S)] SKIP  [$MATCHUP_IDX/$MATCHUP_TOTAL] $label  (all $ng games already done)"
            continue
        fi
        echo "[$(date +%H:%M:%S)] RESUME [$MATCHUP_IDX/$MATCHUP_TOTAL] $label  ($done_count/$ng done, continuing from game $start_game)"
    else
        echo "[$(date +%H:%M:%S)] START  [$MATCHUP_IDX/$MATCHUP_TOTAL] $label  ($ng games, ruleset=$rs)"
    fi

    MATCHUP_START=$(date +%s)
    green_wins=0
    yellow_wins=0
    errors=0

    for (( g=start_game; g<=ng; g++ )); do
        # Run one game; tolerate non-zero exit (timeout/error) without aborting bench
        set +e
        result=$("$GAME_SCRIPT" "$ge" "$gl" "$ye" "$yl" "$rs" "$OUTPUT_CSV")
        game_exit=$?
        set -e

        # Parse winner from the echoed result line (field 9, 0-indexed from 1)
        winner=$(echo "$result" | cut -d',' -f9)

        case "$winner" in
            green)   (( green_wins++  )) || true ;;
            yellow)  (( yellow_wins++ )) || true ;;
            *)       (( errors++      )) || true ;;
        esac

        # Progress line every 10 games (or at start/end)
        if (( g == start_game || g == ng || g % 10 == 0 )); then
            played=$(( g - start_game + 1 ))
            total_played=$(( green_wins + yellow_wins ))
            pct_green=0
            if (( total_played > 0 )); then
                pct_green=$(( green_wins * 100 / total_played ))
            fi
            elapsed=$(( $(date +%s) - MATCHUP_START ))
            rate=0
            if (( played > 0 )); then rate=$(( elapsed / played )); fi
            eta=0
            if (( rate > 0 )); then eta=$(( rate * (ng - g) )); fi
            printf "  [%H:%M:%S] %s  game %d/%d  green=%d%% (%d-%d)  errors=%d  ETA ~%s\n" \
                $(date +%H:%M:%S) \
                "$label" "$g" "$ng" "$pct_green" "$green_wins" "$yellow_wins" "$errors" \
                "$(format_duration $eta)" \
                || printf "  [%s] %s  game %d/%d  green=%d%% (%d-%d)  errors=%d  ETA ~%s\n" \
                "$(date +%H:%M:%S)" \
                "$label" "$g" "$ng" "$pct_green" "$green_wins" "$yellow_wins" "$errors" \
                "$(format_duration $eta)"
        fi
    done

    MATCHUP_END=$(date +%s)
    MATCHUP_DURATION=$(( MATCHUP_END - MATCHUP_START ))
    total_played=$(( green_wins + yellow_wins ))
    pct_green=0
    if (( total_played > 0 )); then pct_green=$(( green_wins * 100 / total_played )); fi

    echo "[$(date +%H:%M:%S)] DONE   [$MATCHUP_IDX/$MATCHUP_TOTAL] $label"
    echo "          green=${green_wins}  yellow=${yellow_wins}  errors=${errors}  green_rate=${pct_green}%  time=$(format_duration $MATCHUP_DURATION)"
    echo ""
done


# =============================================================================
# SUMMARY
# =============================================================================
GLOBAL_END=$(date +%s)
TOTAL_ELAPSED=$(( GLOBAL_END - GLOBAL_START ))

echo "════════════════════════════════════════════════════════════════"
echo " Machine ${MACHINE^^} finished at $(date)"
printf " Total wall time : %s\n" "$(format_duration $TOTAL_ELAPSED)"
echo " Results written to : $OUTPUT_CSV"
echo ""
echo " To merge results from both machines:"
echo "   head -1 results_a.csv > results.csv"
echo "   tail -n +2 results_a.csv >> results.csv"
echo "   tail -n +2 results_b.csv >> results.csv"
echo "════════════════════════════════════════════════════════════════"