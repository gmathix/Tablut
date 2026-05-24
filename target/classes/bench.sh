#!/usr/bin/env bash
# =============================================================================
# bench.sh  ·  Tablut engine benchmark orchestrator (PARALLEL edition)
# =============================================================================
# Usage:
#   ./bench.sh --machine <a|b> [options]
#
# Options:
#   --machine    <a|b>    Required. Which half of the matrix this machine runs.
#   --jobs       <N>      Parallel game slots. Default: nproc (all cores).
#                         Set to 1 to reproduce the original serial behaviour.
#   --output     <file>   CSV output path. Default: ./results_<machine>.csv
#   --game-script <path>  Path to run_game.sh. Default: ./run_game.sh
#   --resume              Skip games already present in the output CSV.
#   --dry-run             Print work plan without running anything.
#   --only-matchup <lbl>  Run only this matchup label (for testing).
#
# Parallelism note:
#   Each game is an independent JVM process; run_game.sh is flock-safe on the
#   CSV and uses unique per-game log files, so any level of parallelism is safe
#   PROVIDED your Java engines are single-threaded (typical for Negamax/MCTS).
#   If the engines spawn thread pools internally you will just saturate the CPU
#   across competing games and gain nothing — verify before going wide open.
# =============================================================================
set -uo pipefail


# =============================================================================
# CONFIGURE
# =============================================================================
export JAVA_CMD="${JAVA_CMD:-java}"
export CLASSPATH="${CLASSPATH:-.:lib/*}"
export MAIN_CLASS="${MAIN_CLASS:-TablutConsole}"
export LOG_DIR="${LOG_DIR:-./logs}"


# =============================================================================
# TEST MATRIX  (unchanged from original)
# =============================================================================
# Format per entry (7 space-separated fields):
#   green_engine  green_level  yellow_engine  yellow_level  num_games  ruleset_bits  label
#
# Engine codes:  0=Negamax  1=MonteCarlo  2=NegaMonteCarlo
#
MATRIX=(
# ── 1. Same-level, cross-engine, base rules ─────────────────────────────────
"0  10  2  10   10  0  NM10_vs_NMC10"
"2  10  0  10    10  0  NMC10_vs_NM10"

# ── 2. Variant matchups: all 7 non-base rulesets ────────────────────────────
"0  10  2  10   10  1  NM10_vs_NMC10"
"2  10  0  10    10  1  NMC10_vs_NM10"

"0  10  2  10   10  2  NM10_vs_NMC10"
"2  10  0  10    10  2  NMC10_vs_NM10"

"0  10  2  10   10  3  NM10_vs_NMC10"
"2  10  0  10    10  3  NMC10_vs_NM10"

"0  10  2  10   10  4  NM10_vs_NMC10"
"2  10  0  10    10  4  NMC10_vs_NM10"

"0  10  2  10   10  5  NM10_vs_NMC10"
"2  10  0  10    10  5  NMC10_vs_NM10"

"0  10  2  10   10  6  NM10_vs_NMC10"
"2  10  0  10    10  6  NMC10_vs_NM10"

"0  10  2  10   10  7  NM10_vs_NMC10"
"2  10  0  10    10  7  NMC10_vs_NM10"
)


# =============================================================================
# ARGUMENT PARSING
# =============================================================================
MACHINE=""
OUTPUT_CSV=""
GAME_SCRIPT="./run_game.sh"
RESUME=false
DRY_RUN=false
ONLY_MATCHUP=""
JOBS=$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)

while [[ $# -gt 0 ]]; do
    case "$1" in
        --machine)      MACHINE="$2";       shift 2 ;;
        --output)       OUTPUT_CSV="$2";    shift 2 ;;
        --game-script)  GAME_SCRIPT="$2";   shift 2 ;;
        --jobs)         JOBS="$2";          shift 2 ;;
        --resume)       RESUME=true;        shift   ;;
        --dry-run)      DRY_RUN=true;       shift   ;;
        --only-matchup) ONLY_MATCHUP="$2";  shift 2 ;;
        *) echo "Unknown option: $1" >&2; exit 1 ;;
    esac
done

if [[ -z "$MACHINE" ]]; then
    echo "Error: --machine <a|b> is required." >&2; exit 1
fi
if [[ "$MACHINE" != "a" && "$MACHINE" != "b" ]]; then
    echo "Error: --machine must be 'a' or 'b'." >&2; exit 1
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
readonly ENGINE_NAMES=("negamax" "montecarlo" "negamontecarlo")

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
    echo $(( max_s * 60 * ng ))
}

format_duration() {
    local s="$1"
    printf '%dh %02dm' $(( s/3600 )) $(( (s%3600)/60 ))
}

games_done() {
    local ge="$2" gl="$3" ye="$4" yl="$5" rs="$6"
    if [[ ! -f "$OUTPUT_CSV" ]]; then echo 0; return; fi
    local en=("negamax" "montecarlo" "negamontecarlo")
    grep -c "^[^,]*,[^,]*,${en[$ge]},${gl},${en[$ye]},${yl},${rs}," "$OUTPUT_CSV" 2>/dev/null || true
}


# =============================================================================
# WORK PLAN  — select this machine's matchups
# =============================================================================
declare -a MY_WORK=()
MACHINE_IDX=0
for entry in "${MATRIX[@]}"; do
    if [[ "$MACHINE" == "a" && $(( MACHINE_IDX % 2 )) -eq 0 ]] || \
       [[ "$MACHINE" == "b" && $(( MACHINE_IDX % 2 )) -eq 1 ]]; then
        MY_WORK+=("$entry")
    fi
    (( MACHINE_IDX++ )) || true
done

if [[ -n "$ONLY_MATCHUP" ]]; then
    declare -a FILTERED=()
    for entry in "${MATRIX[@]}"; do
        read -r ge gl ye yl ng rs label <<< "$entry"
        if [[ "$label" == "$ONLY_MATCHUP" ]]; then FILTERED+=("$entry"); fi
    done
    MY_WORK=("${FILTERED[@]:-}")
    if [[ ${#MY_WORK[@]} -eq 0 ]]; then
        echo "Error: no matchup found with label '$ONLY_MATCHUP'" >&2; exit 1
    fi
fi


# =============================================================================
# BUILD FLAT WORK QUEUE  (one entry per individual game)
# =============================================================================
# Each token:  "ge gl ye yl rs label"
# Resume skips already-completed games before we enqueue them.
declare -a QUEUE=()

for entry in "${MY_WORK[@]}"; do
    read -r ge gl ye yl ng rs label <<< "$entry"
    already=0
    if $RESUME; then
        already=$(games_done "$label" "$ge" "$gl" "$ye" "$yl" "$rs")
    fi
    remaining=$(( ng - already ))
    if (( remaining <= 0 )); then
        echo "[$(date +%H:%M:%S)] SKIP  $label  (all $ng games already done)"
        continue
    fi
    if $RESUME && (( already > 0 )); then
        echo "[$(date +%H:%M:%S)] RESUME $label  ($already/$ng done, enqueuing $remaining more)"
    fi
    for (( i=0; i<remaining; i++ )); do
        QUEUE+=("$ge $gl $ye $yl $rs $label")
    done
done

TOTAL_QUEUED=${#QUEUE[@]}


# =============================================================================
# DRY-RUN: print plan and exit
# =============================================================================
TOTAL_GAMES=0; TOTAL_EST_S=0

echo ""
echo "════════════════════════════════════════════════════════════════"
echo " Tablut Benchmark (PARALLEL) — Machine ${MACHINE^^}   $(date)"
echo "════════════════════════════════════════════════════════════════"
echo " Output CSV  : $OUTPUT_CSV"
echo " Log dir     : $LOG_DIR"
echo " Parallel jobs: $JOBS"
echo " Resume      : $RESUME"
echo "────────────────────────────────────────────────────────────────"
printf " %-32s  %5s  %3s  %12s\n" "Matchup" "Games" "RS" "Est.(serial)"
echo "────────────────────────────────────────────────────────────────"
for entry in "${MY_WORK[@]}"; do
    read -r ge gl ye yl ng rs label <<< "$entry"
    est=$(estimate_matchup_time "$ge" "$gl" "$ye" "$yl" "$ng")
    TOTAL_GAMES=$(( TOTAL_GAMES + ng ))
    TOTAL_EST_S=$(( TOTAL_EST_S + est ))
    printf " %-32s  %5d  %3d  %12s\n" "$label" "$ng" "$rs" "$(format_duration $est)"
done
echo "────────────────────────────────────────────────────────────────"
printf " %-32s  %5d       %12s  (serial)\n" "TOTAL" "$TOTAL_GAMES" "$(format_duration $TOTAL_EST_S)"
PARALLEL_EST=$(( TOTAL_EST_S / JOBS + 1 ))
printf " %-32s  %5d       %12s  (with --jobs %d)\n" "" "$TOTAL_QUEUED" "$(format_duration $PARALLEL_EST)" "$JOBS"
echo "════════════════════════════════════════════════════════════════"
echo ""

if $DRY_RUN; then
    echo "[dry-run] Exiting without running games."
    exit 0
fi

if (( TOTAL_QUEUED == 0 )); then
    echo "Nothing to do — all games already completed."
    exit 0
fi


# =============================================================================
# PARALLEL JOB POOL
# =============================================================================
# Uses a simple bash background-process pool.
# GNU parallel is NOT required (though if present, see the note at the bottom).
#
# How it works:
#   • We keep an array ACTIVE_PIDS of running background PIDs.
#   • Before launching each new game we drain any finished slots (non-blocking
#     kill -0 check), then block-wait on the oldest PID if we're at capacity.
#   • A shared PROGRESS_FILE (one line = games finished) is updated by each
#     worker subshell so the main process can print a live counter.
# =============================================================================

PROGRESS_FILE=$(mktemp)
echo "0" > "$PROGRESS_FILE"
# Lock file for the progress counter (separate from the CSV lock)
PROGRESS_LOCK="${PROGRESS_FILE}.lock"

# Worker function — runs in a subshell via &
run_one() {
    local ge="$1" gl="$2" ye="$3" yl="$4" rs="$5" label="$6"
    local result exit_code winner

    set +e
    result=$("$GAME_SCRIPT" "$ge" "$gl" "$ye" "$yl" "$rs" "$OUTPUT_CSV" 2>&1)
    exit_code=$?
    set -e

    winner=$(echo "$result" | cut -d',' -f9)

    # Atomically increment the progress counter
    (
        flock -x 201
        local done_count
        done_count=$(cat "$PROGRESS_FILE")
        echo $(( done_count + 1 )) > "$PROGRESS_FILE"
    ) 201>"$PROGRESS_LOCK"

    # Print the result row (same as original) plus a summary tag for easy grepping
    local done_now
    done_now=$(cat "$PROGRESS_FILE")
    printf "[%s] (%d/%d) %s  winner=%-7s  %s\n" \
        "$(date +%H:%M:%S)" "$done_now" "$TOTAL_QUEUED" \
        "$label" "$winner" "$(echo "$result" | cut -d',' -f1)"
}
export -f run_one
export GAME_SCRIPT OUTPUT_CSV PROGRESS_FILE PROGRESS_LOCK TOTAL_QUEUED

# ── Main dispatch loop ────────────────────────────────────────────────────────
declare -a ACTIVE_PIDS=()
GLOBAL_START=$(date +%s)

drain_finished() {
    # Remove PIDs that have already exited (non-blocking)
    local alive=()
    for pid in "${ACTIVE_PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            alive+=("$pid")
        fi
    done
    ACTIVE_PIDS=("${alive[@]:-}")
}

wait_for_slot() {
    # Block until at least one slot is free
    while (( ${#ACTIVE_PIDS[@]} >= JOBS )); do
        # Wait on the oldest PID (FIFO-ish), then re-drain
        wait "${ACTIVE_PIDS[0]}" 2>/dev/null || true
        drain_finished
    done
}

echo "[$(date +%H:%M:%S)] Dispatching $TOTAL_QUEUED games across $JOBS parallel slots…"
echo ""

for work in "${QUEUE[@]}"; do
    read -r ge gl ye yl rs label <<< "$work"

    drain_finished
    wait_for_slot

    run_one "$ge" "$gl" "$ye" "$yl" "$rs" "$label" &
    ACTIVE_PIDS+=($!)
done

# Wait for all remaining games to finish
echo ""
echo "[$(date +%H:%M:%S)] All games dispatched — waiting for last $((${#ACTIVE_PIDS[@]})) to finish…"
for pid in "${ACTIVE_PIDS[@]}"; do
    wait "$pid" 2>/dev/null || true
done


# =============================================================================
# SUMMARY
# =============================================================================
rm -f "$PROGRESS_FILE" "$PROGRESS_LOCK"
GLOBAL_END=$(date +%s)
TOTAL_ELAPSED=$(( GLOBAL_END - GLOBAL_START ))

echo ""
echo "════════════════════════════════════════════════════════════════"
echo " Machine ${MACHINE^^} finished at $(date)"
printf " Wall time   : %s  (serial estimate was %s)\n" \
    "$(format_duration $TOTAL_ELAPSED)" "$(format_duration $TOTAL_EST_S)"
printf " Speedup     : ~%.1fx\n" "$(echo "scale=1; $TOTAL_EST_S / ($TOTAL_ELAPSED + 1)" | bc 2>/dev/null || echo '?')"
echo " Results     : $OUTPUT_CSV"
echo ""
echo " To merge results from both machines:"
echo "   head -1 results_a.csv > results.csv"
echo "   tail -n +2 results_a.csv >> results.csv"
echo "   tail -n +2 results_b.csv >> results.csv"
echo "════════════════════════════════════════════════════════════════"

# =============================================================================
# NOTE: GNU parallel alternative
# =============================================================================
# If you have GNU parallel installed, you can skip this script entirely and
# just run the games directly.  Example for all games on machine A:
#
#   printf '%s\n' "${QUEUE[@]}" | \
#     parallel -j$(nproc) --colsep ' ' \
#       ./run_game.sh {1} {2} {3} {4} {5} ./results_a.csv
#
# The CSV is flock-safe so parallel writes are fine.
# =============================================================================