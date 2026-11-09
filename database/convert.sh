#!/usr/bin/env bash

set -e

DATABASE=""
INPUT=""
OUTPUT=""

usage() {
    echo "Usage:"
    echo "  ./convert.sh --database <postgres|mysql> <input> [output]"
    exit 1
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --database)
            [[ -n "${2:-}" ]] || usage
            DATABASE="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        -*)
            echo "Error: Unknown option: $1"
            usage
            ;;
        *)
            if [[ -z "$INPUT" ]]; then
                INPUT="$1"
            elif [[ -z "$OUTPUT" ]]; then
                OUTPUT="$1"
            else
                usage
            fi
            shift
            ;;
    esac
done

# Validate arguments
[[ -n "$DATABASE" && -n "$INPUT" ]] || usage

case "$DATABASE" in
    sqlite|postgres|mysql) ;;
    *)
        echo "Error: Unsupported database: $DATABASE"
        exit 1
        ;;
esac

# Check input file
if [[ ! -f "$INPUT" ]]; then
    echo "Error: File not found: $INPUT"
    exit 1
fi

# Check DBML CLI
if ! command -v sql2dbml >/dev/null 2>&1 || \
   ! command -v dbml2sql >/dev/null 2>&1; then
    echo "Error: DBML CLI not installed."
    echo "Install: npm install -g @dbml/cli"
    exit 1
fi

# Detect format and convert
case "$INPUT" in
    *.sql)
        [[ -n "$OUTPUT" ]] || OUTPUT="${INPUT%.sql}.dbml"

        echo "Converting SQL -> DBML ($DATABASE)..."

        sql2dbml "$INPUT" \
            --out-file "$OUTPUT" \
            --$DATABASE
        ;;

    *.dbml)
        [[ -n "$OUTPUT" ]] || OUTPUT="${INPUT%.dbml}.sql"

        echo "Converting DBML -> SQL ($DATABASE)..."

        dbml2sql "$INPUT" \
            --out-file "$OUTPUT" \
            --$DATABASE
        ;;

    *)
        echo "Error: Unsupported file format: $INPUT"
        exit 1
        ;;
esac

echo "Success: $OUTPUT"