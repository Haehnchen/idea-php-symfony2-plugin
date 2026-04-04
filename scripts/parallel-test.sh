#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

usage() {
  cat <<'EOF'
Usage: scripts/parallel-test.sh [options] [-- <extra gradle args>]

Runs IntelliJ plugin tests in isolated shards across multiple Gradle processes.
Each worker gets one fixed shard of test classes and runs it in a dedicated process.

Options:
  --workers N       Number of parallel workers (default: 2)
  --test PATTERN    Restrict classes using a Gradle-style wildcard, repeatable
  --list            Print the selected test classes and exit
  --no-warmup       Skip the initial `testClasses` compilation step
  -h, --help        Show this help

Examples:
  scripts/parallel-test.sh
  scripts/parallel-test.sh --workers 3 --test '*Routing*'
  scripts/parallel-test.sh --test 'fr.adrienbrault.idea.symfony2plugin.tests.dic.*'
  scripts/parallel-test.sh -- --info
EOF
}

workers="${PARALLEL_TEST_WORKERS:-2}"
warmup=1
list_only=0
declare -a test_patterns=()
declare -a extra_gradle_args=()

while (($# > 0)); do
  case "$1" in
    --workers)
      workers="$2"
      shift 2
      ;;
    --test)
      test_patterns+=("$2")
      shift 2
      ;;
    --list)
      list_only=1
      shift
      ;;
    --no-warmup)
      warmup=0
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    --)
      shift
      extra_gradle_args=("$@")
      break
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if ! [[ "$workers" =~ ^[1-9][0-9]*$ ]]; then
  echo "--workers must be a positive integer" >&2
  exit 1
fi

mapfile -t all_test_classes < <(
  rg --files src/test \
    | rg 'Test\.(java|kt)$' \
    | sed 's#^src/test/java/##; s#^src/test/kotlin/##; s#/#.#g; s#\.java$##; s#\.kt$##' \
    | sort
)

declare -a selected_test_classes=()
if ((${#test_patterns[@]} == 0)); then
  selected_test_classes=("${all_test_classes[@]}")
else
  for class_name in "${all_test_classes[@]}"; do
    for pattern in "${test_patterns[@]}"; do
      if [[ "$class_name" == $pattern ]]; then
        selected_test_classes+=("$class_name")
        break
      fi
    done
  done
fi

if ((${#selected_test_classes[@]} == 0)); then
  echo "No test classes matched the requested scope." >&2
  exit 1
fi

if ((list_only == 1)); then
  printf '%s\n' "${selected_test_classes[@]}"
  exit 0
fi

tmp_root="$(mktemp -d "${TMPDIR:-/tmp}/symfony-parallel-test.XXXXXX")"
run_stamp="$(date +%Y%m%d-%H%M%S)"
results_dir="$ROOT_DIR/build/parallel-test/$run_stamp"
worker_status_dir="$tmp_root/worker-status"
mkdir -p "$worker_status_dir"
mkdir -p "$results_dir"

cleanup() {
  local exit_code=$?

  if ((${#worker_pids[@]} > 0)); then
    for pid in "${worker_pids[@]}"; do
      kill "$pid" 2>/dev/null || true
    done
  fi

  rm -rf "$tmp_root" || true
  exit "$exit_code"
}

declare -a worker_pids=()
trap cleanup EXIT INT TERM

shards_dir="$tmp_root/shards"
mkdir -p "$shards_dir"

for ((worker_id = 1; worker_id <= workers; worker_id++)); do
  : > "$(printf '%s/worker-%02d.txt' "$shards_dir" "$worker_id")"
done

for i in "${!selected_test_classes[@]}"; do
  worker_id=$(( (i % workers) + 1 ))
  printf '%s\n' "${selected_test_classes[$i]}" >> "$(printf '%s/worker-%02d.txt' "$shards_dir" "$worker_id")"
done

if (( warmup == 1 )); then
  echo "Warmup: preparing shared test artifacts once before parallel execution"
  ./gradlew testClasses instrumentTestCode instrumentedJar composedJar --no-parallel --no-configuration-cache "${extra_gradle_args[@]}"
fi

active_workers=0
for ((worker_id = 1; worker_id <= workers; worker_id++)); do
  shard_file="$(printf '%s/worker-%02d.txt' "$shards_dir" "$worker_id")"
  if [[ -s "$shard_file" ]]; then
    active_workers=$((active_workers + 1))
  fi
done

echo "Running ${#selected_test_classes[@]} test classes across $active_workers workers"

print_worker_summary() {
  echo
  echo "Worker summary:"

  local found=0
  for summary_file in "$worker_status_dir"/worker-*.summary; do
    if [[ ! -f "$summary_file" ]]; then
      continue
    fi

    found=1
    # shellcheck disable=SC1090
    source "$summary_file"

    local duration_text
    duration_text="$(printf '%ss' "$duration_seconds")"

    echo "  worker $worker_id: $status, ${test_count} classes, ${duration_text}, log: $log_file"
  done

  if (( found == 0 )); then
    echo "  no worker data"
  fi
}

run_worker() {
  local worker_id="$1"
  local worker_tmp_dir="$tmp_root/worker-$worker_id"
  local current_gradle_pid=""
  local shard_file
  shard_file="$(printf '%s/worker-%02d.txt' "$shards_dir" "$worker_id")"

  trap 'if [[ -n "$current_gradle_pid" ]]; then kill "$current_gradle_pid" 2>/dev/null || true; wait "$current_gradle_pid" 2>/dev/null || true; fi; exit 143' TERM INT

  if [[ ! -s "$shard_file" ]]; then
    return 0
  fi

  mkdir -p "$worker_tmp_dir"
  if [[ -f "$tmp_root/failed" ]]; then
    return 1
  fi

  mapfile -t shard_tests < "$shard_file"
  local run_id
  run_id="$(printf 'parallel-w%02d' "$worker_id")"
  local run_root="$worker_tmp_dir/$run_id"
  local worker_log
  worker_log="$results_dir/$run_id.log"
  local summary_file
  summary_file="$(printf '%s/worker-%02d.summary' "$worker_status_dir" "$worker_id")"
  local start_ts
  start_ts="$(date +%s)"

  mkdir -p \
    "$run_root/sandbox-container" \
    "$run_root/system" \
    "$run_root/config" \
    "$run_root/plugins" \
    "$run_root/log" \
    "$run_root/tmp"

  local -a cmd=(
    ./gradlew
    test
    --no-parallel
    --no-configuration-cache
    "--project-cache-dir=$run_root/project-cache"
    "-PtestRunId=$run_id"
    "-PtestSandboxContainer=$run_root/sandbox-container"
    "-Didea.system.path=$run_root/system"
    "-Didea.config.path=$run_root/config"
    "-Didea.plugins.path=$run_root/plugins"
    "-Didea.log.path=$run_root/log"
    "-Djava.io.tmpdir=$run_root/tmp"
    "-Dvfs.additional-allowed-roots=$run_root/sandbox-container"
  )

  if (( warmup == 1 )); then
    cmd+=(
      -x checkKotlinGradlePluginConfigurationErrors
      -x initializeIntellijPlatformPlugin
      -x patchPluginXml
      -x processResources
      -x generateManifest
      -x processTestResources
      -x compileKotlin
      -x compileJava
      -x classes
      -x instrumentCode
      -x jar
      -x compileTestKotlin
      -x compileTestJava
      -x testClasses
      -x instrumentTestCode
      -x instrumentedJar
      -x composedJar
    )
  fi

  for class_name in "${shard_tests[@]}"; do
    cmd+=(--tests "$class_name")
  done

  cmd+=("${extra_gradle_args[@]}")

  echo "[worker $worker_id] start: ${#shard_tests[@]} classes"

  "${cmd[@]}" >"$worker_log" 2>&1 &
  current_gradle_pid=$!
  wait "$current_gradle_pid"
  local status=$?
  current_gradle_pid=""
  local end_ts
  end_ts="$(date +%s)"
  local duration_seconds=$((end_ts - start_ts))

  cat > "$summary_file" <<EOF
worker_id=$worker_id
test_count=${#shard_tests[@]}
duration_seconds=$duration_seconds
status=$([[ $status -eq 0 ]] && printf 'ok' || printf 'failed')
log_file=$worker_log
EOF

  if (( status != 0 )); then
    : > "$tmp_root/failed"
    echo "[worker $worker_id] failed after ${duration_seconds}s; log: $worker_log"
    return "$status"
  fi

  echo "[worker $worker_id] done in ${duration_seconds}s; log: $worker_log"
}

active_pids=()
for ((worker_id = 1; worker_id <= workers; worker_id++)); do
  shard_file="$(printf '%s/worker-%02d.txt' "$shards_dir" "$worker_id")"
  if [[ ! -s "$shard_file" ]]; then
    continue
  fi

  run_worker "$worker_id" &
  pid=$!
  worker_pids+=("$pid")
  active_pids+=("$pid")
done

overall_status=0
while ((${#active_pids[@]} > 0)); do
  finished_pid=""
  if wait -n -p finished_pid "${active_pids[@]}"; then
    status=0
  else
    status=$?
  fi

  remaining_pids=()
  for pid in "${active_pids[@]}"; do
    if [[ "$pid" != "$finished_pid" ]]; then
      remaining_pids+=("$pid")
    fi
  done
  active_pids=("${remaining_pids[@]}")

  if (( status != 0 && overall_status == 0 )); then
    overall_status="$status"
    : > "$tmp_root/failed"
    for pid in "${active_pids[@]}"; do
      kill "$pid" 2>/dev/null || true
    done
  fi
done

print_worker_summary

exit "$overall_status"
