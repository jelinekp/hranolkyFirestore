# Report Compilation Guide

This directory contains the Typst report for the Normalized Systems Theory refactoring assignment.

## Prerequisites

- **Typst** compiler (https://github.com/typst/typst)
- **Java** (for PlantUML diagram generation)
- **PlantUML** (optional, script downloads it automatically)

## Quick Start

### 1. Generate Diagrams

Before compiling the report, generate the diagram images:

```bash
./generate-diagrams.sh
```

This script will:
- Download PlantUML JAR if needed (requires Java)
- Generate PNG images from all `.puml` files in `media/`
- Optionally generate Mermaid diagrams if `mmdc` is installed

**Manual generation** (if script fails):
```bash
cd media/
java -jar /tmp/plantuml.jar -tpng dependency-graph.puml
```

### 2. Compile the Report

```bash
typst compile main.typ
```

This generates `main.pdf` in the current directory.

**Watch mode** (auto-recompile on changes):
```bash
typst watch main.typ
```

## Directory Structure

```
report/
├── main.typ                    # Main report file
├── generate-diagrams.sh        # Diagram generation script
├── references.bib              # Bibliography
├── sections/                   # Report sections
│   ├── 01_introduction.typ     # Introduction
│   ├── 02_theory.typ           # NS Theory background
│   ├── 03_current.typ          # Current system analysis
│   ├── 04_refactored.typ       # Refactoring design
│   └── 08_conclusion.typ       # Conclusion
└── media/                      # Diagrams and images
    ├── dependency-graph.puml   # Component dependency diagram (source)
    ├── dependency-graph.png    # Generated diagram image
    ├── architecture-*.mmd      # Architecture diagrams (Mermaid source)
    ├── architecture-*.puml     # Architecture diagrams (PlantUML source)
    └── logo-*.pdf              # University logo
```

## Diagrams in the Report

The report includes the following diagrams:

### Component Dependency Graph
- **Source:** `media/dependency-graph.puml`
- **Location:** Section 4 (Refactored System Design)
- **Description:** Shows component relationships across all layers (UI, Domain, Data, Configuration, External Services)

### Architecture Diagrams
- **Sources:** `media/architecture-before.{mmd,puml}` and `media/architecture-after.{mmd,puml}`
- **Purpose:** Illustrate architectural changes from original to NS-compliant design

## Troubleshooting

### Diagram generation fails
- Ensure Java is installed: `java -version`
- Download PlantUML manually: `wget https://github.com/plantuml/plantuml/releases/download/v1.2024.8/plantuml-1.2024.8.jar -O /tmp/plantuml.jar`
- Generate: `java -jar /tmp/plantuml.jar -tpng media/dependency-graph.puml`

### Typst compilation errors
- Update Typst: Check https://github.com/typst/typst/releases
- Check syntax: `typst compile --help`

### Missing images in PDF
- Verify PNG files exist: `ls -l media/*.png`
- Check image paths in `.typ` files match actual filenames

## Editing the Report

1. Edit section files in `sections/`
2. Modify diagrams in `media/` (`.puml` or `.mmd` files)
3. Regenerate diagrams: `./generate-diagrams.sh`
4. Recompile: `typst compile main.typ`
5. View: `xdg-open main.pdf` (Linux) or `open main.pdf` (macOS)

## Bibliography

Add references to `references.bib` in BibTeX format. Cite in Typst using `@key` notation.

## Contact

For questions about this report, contact Pavel Jelínek.
