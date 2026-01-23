#!/usr/bin/env bash
  # bash

  cd diagrams

  set -euo pipefail

  usage() {
    echo "Usage: $0 svg|png"
    exit 1
  }

  if [ $# -ne 1 ]; then usage; fi
  fmt="$1"
  if [ "$fmt" != "svg" ] && [ "$fmt" != "png" ]; then usage; fi

  outdir="$fmt"
  mkdir -p "$outdir"

  # detect renderer
  if command -v plantuml >/dev/null 2>&1; then
    RENDER_CMD=(plantuml)
  elif [ -n "${PLANTUML_JAR:-}" ] && [ -f "$PLANTUML_JAR" ]; then
    RENDER_CMD=(java -jar "$PLANTUML_JAR")
  elif [ -f "../plantuml.jar" ]; then
    RENDER_CMD=(java -jar "../plantuml.jar")
  else
    echo "No plantuml found. Downloading plantuml.jar..."
    PLANTUML_URL="https://github.com/plantuml/plantuml/releases/download/v1.2025.10/plantuml-1.2025.10.jar"
    PLANTUML_PATH="../plantuml.jar"

    if command -v curl >/dev/null 2>&1; then
      curl -L "$PLANTUML_URL" -o "$PLANTUML_PATH"
    elif command -v wget >/dev/null 2>&1; then
      wget "$PLANTUML_URL" -O "$PLANTUML_PATH"
    else
      echo "Error: Neither curl nor wget found. Cannot download plantuml.jar"
      exit 2
    fi

    echo "Downloaded plantuml.jar to $PLANTUML_PATH"
    RENDER_CMD=(java -jar "$PLANTUML_PATH")
  fi

  count=0
  while IFS= read -r -d '' file; do
    name="$(basename "$file" .puml)"
    out="$outdir/$name.$fmt"

    if "${RENDER_CMD[@]}" --help >/dev/null 2>&1; then
      "${RENDER_CMD[@]}" -t"$fmt" -pipe < "$file" > "$out"
    else
      "${RENDER_CMD[@]}" -t"$fmt" -pipe < "$file" > "$out"
    fi

    echo "Rendered: $file -> $out"
    count=$((count+1))
  done < <(find . -type f -name '*.puml' -print0)

  echo "Done. Rendered $count file(s) into \`$outdir\`."