#!/bin/bash
# Script to generate diagram images from PlantUML and Mermaid sources
# Run this before compiling the Typst report

set -e

MEDIA_DIR="$(cd "$(dirname "$0")/media" && pwd)"
cd "$MEDIA_DIR"

echo "Generating diagrams in $MEDIA_DIR..."

# Check for PlantUML
if command -v plantuml &> /dev/null; then
    echo "✓ Using system PlantUML"
    plantuml -tpng *.puml
elif command -v docker &> /dev/null; then
    echo "✓ Using PlantUML via Docker"
    docker run --rm -v "$MEDIA_DIR:/data" plantuml/plantuml:latest -tpng *.puml
elif command -v java &> /dev/null; then
    echo "✓ Using PlantUML via Java JAR"
    PLANTUML_JAR="/tmp/plantuml.jar"
    if [ ! -f "$PLANTUML_JAR" ]; then
        echo "  Downloading PlantUML JAR..."
        curl -sL https://github.com/plantuml/plantuml/releases/download/v1.2024.8/plantuml-1.2024.8.jar -o "$PLANTUML_JAR"
    fi
    java -jar "$PLANTUML_JAR" -tpng *.puml
else
    echo "❌ Error: PlantUML not available. Install PlantUML, Docker, or Java."
    echo "   Debian/Ubuntu: sudo apt install plantuml"
    echo "   Arch: sudo pacman -S plantuml"
    echo "   macOS: brew install plantuml"
    exit 1
fi

# Check for Mermaid CLI (optional)
if command -v mmdc &> /dev/null; then
    echo "✓ Generating Mermaid diagrams"
    for mmd in *.mmd; do
        if [ -f "$mmd" ]; then
            basename="${mmd%.mmd}"
            mmdc -i "$mmd" -o "${basename}.png" -b transparent
        fi
    done
else
    echo "⚠ Mermaid CLI not found (optional)"
    echo "  Install: npm install -g @mermaid-js/mermaid-cli"
fi

echo "✓ Diagram generation complete!"
ls -lh *.png 2>/dev/null || echo "No PNG files generated"
