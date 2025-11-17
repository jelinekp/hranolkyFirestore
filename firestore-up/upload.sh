#!/bin/bash

# Firestore Upload Helper Script
# This script simplifies running the Firestore upload/migration tool

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default values
INPUT_FILE="$SCRIPT_DIR/dump.json"
CREDENTIALS_FILE="$SCRIPT_DIR/private_secret.json"
DRY_RUN=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN="-PdryRun"
            shift
            ;;
        --input)
            INPUT_FILE="$2"
            shift 2
            ;;
        --credentials)
            CREDENTIALS_FILE="$2"
            shift 2
            ;;
        --help|-h)
            echo "Firestore Upload Helper"
            echo ""
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --dry-run              Perform a dry run without uploading to Firestore"
            echo "  --input <file>         Path to dump.json (default: ./dump.json)"
            echo "  --credentials <file>   Path to credentials JSON (default: ./private_secret.json)"
            echo "  --help, -h             Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0 --dry-run"
            echo "  $0 --input my_dump.json --credentials my_secret.json"
            echo "  $0"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Check if files exist
if [ ! -f "$INPUT_FILE" ]; then
    echo "Error: Input file not found: $INPUT_FILE"
    exit 1
fi

if [ ! -f "$CREDENTIALS_FILE" ]; then
    echo "Error: Credentials file not found: $CREDENTIALS_FILE"
    exit 1
fi

# Run the upload
echo "Running Firestore upload..."
echo "  Input: $INPUT_FILE"
echo "  Credentials: $CREDENTIALS_FILE"
if [ -n "$DRY_RUN" ]; then
    echo "  Mode: DRY RUN (no data will be written)"
else
    echo "  Mode: ACTUAL UPLOAD"
    echo ""
    echo "WARNING: This will upload data to Firestore!"
    read -p "Are you sure you want to continue? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        echo "Upload cancelled."
        exit 0
    fi
fi
echo ""

cd "$PROJECT_ROOT"
./gradlew :firestore-up:upload \
    -Pinput="$INPUT_FILE" \
    -Pcredentials="$CREDENTIALS_FILE" \
    $DRY_RUN

