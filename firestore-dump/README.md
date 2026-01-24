# Firestore Dump Utility

Standalone Kotlin/JVM tool to export entire Firestore database to JSON.

## Build

```bash
./gradlew :firestore-dump:build
```

## Run

Provide service account credentials and output file:

```bash
./gradlew :firestore-dump:run --args="--output dump.json --credentials /path/to/serviceAccount.json --pretty"
```
Or set environment variable `GOOGLE_APPLICATION_CREDENTIALS`:
```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json
./gradlew :firestore-dump:run --args="--output dump.json --flat"
```

Args:
- `--output <file>`: Destination JSON file (required)
- `--projectId <id>`: Override project ID if needed
- `--credentials <file>`: Service account JSON (else GOOGLE_APPLICATION_CREDENTIALS used)
- `--pretty`: Pretty-print JSON
- `--flat`: Output flat map of `collection/doc[/subcol/doc...]` -> document JSON including optional `_subcollections`

## Output Structure

Nested mode: root object with each top-level collection as an object of its documents. Documents that have subcollections include `_subcollections` object.

Flat mode: key is full document path; value is document fields plus `_subcollections` if present.

Firestore special value types are encoded with `__type` discriminator: `timestamp`, `geopoint`, `blob` (base64), `docref`.

## Notes
- This uses blocking calls (`get().get()`) for simplicity; adequate for one-off dumps.
- If database large, consider splitting into streaming writer or pagination enhancements.
- Unknown types serialized with `__type=unknown` and class name.

## License
Internal use.

