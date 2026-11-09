#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Load local dev settings (for example DB_PASSWORD) if a .env file exists.
# The file is gitignored; see .env.example.
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi

usage() {
  cat <<'EOF'
Usage: ./grad.sh <command> [gradle-args...]

Useful commands:
  run, boot      Start the Spring Boot app (alias for bootRun)
  ui, desktop    Start the Spring + Swing desktop app
  ui-demo, demo  Start the component demo (no database)
  build          Full build
  test           Run tests
  check          Run all checks
  generate, gen  Generate models/entities from MySQL (generateJava)
  jar            Build the executable Spring Boot jar
  clean          Clean build outputs
  tasks          List available Gradle tasks
  deps           Show project dependencies
  help           Show this help

Examples:
  ./grad.sh run
  ./grad.sh test
  ./grad.sh generate
  ./grad.sh build -x test

Extra arguments are passed through to Gradle.
EOF
}

if [ "$#" -eq 0 ]; then
  usage
  exit 0
fi

command_name="$1"
shift

case "$command_name" in
  run|boot|bootRun)
    exec ./gradlew run "$@"
    ;;
  ui|desktop)
    exec ./gradlew runUi "$@"
    ;;
  ui-demo|demo)
    exec ./gradlew runUiDemo "$@"
    ;;
  build)
    exec ./gradlew build "$@"
    ;;
  test)
    exec ./gradlew test "$@"
    ;;
  check)
    exec ./gradlew check "$@"
    ;;
  generate|gen|models)
    exec ./gradlew generateJava "$@"
    ;;
  jar|bootjar|bootJar)
    exec ./gradlew bootJar "$@"
    ;;
  clean)
    exec ./gradlew clean "$@"
    ;;
  tasks)
    exec ./gradlew tasks --all "$@"
    ;;
  deps|dependencies)
    exec ./gradlew dependencies "$@"
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    echo "Unknown command: $command_name" >&2
    echo >&2
    usage >&2
    exit 1
    ;;
esac
