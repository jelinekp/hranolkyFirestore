# Firestore Upload/Migration Tool

This tool uploads data from a Firestore dump to a new Firestore database with a transformed structure according to the migration instructions.

## Overview

The migration script reads a JSON dump file created by `firestore-dump` and uploads it to Firestore with the following transformations:

### Performance

- **Batch writing**: Groups writes into batches of 500 documents for 10x faster uploads
- **Upload time**: ~1 minute for 3,714 documents (vs 6-9 minutes with individual writes)
- See [BATCH_WRITING.md](BATCH_WRITING.md) for details

### Data Transformations

1. **WarehouseSlots** → **Hranolky** & **Sparovky**
   - Documents starting with `S-` go to `Sparovky` collection (prefix removed)
   - Documents starting with `H-` go to `Hranolky` collection (prefix removed)
   - Other documents (e.g., `DUB-`) go to `Hranolky` as-is

2. **SlotWeeklyReport** optimizations
   - Document IDs are shortened: `2025_27` → `25_27` (first two characters removed)
   - **Quantity deduplication**: Reports with the same quantity as the previous week are automatically skipped to reduce data redundancy

3. **WeeklyBeamReports** → **WeeklyReports/Hranolky/WeeklyData**
   - Old: `WeeklyBeamReports/2025_27`
   - New: `WeeklyReports/Hranolky/WeeklyData/25_27`

4. **WeeklyJointerReports** → **WeeklyReports/Sparovky/WeeklyData**
   - Old: `WeeklyJointerReports/2025_46`
   - New: `WeeklyReports/Sparovky/WeeklyData/25_46`

5. **AppConfig** and **Devices** collections remain unchanged

### Indexed Fields (NEW!)

Each document automatically gets indexed fields extracted from the slot ID for efficient filtering:

- **quality**: `DUB-A` (first two segments)
- **thickness**: `20` (third segment)
- **width**: `50` (fourth segment)
- **length**: `1530` (fifth segment)

Example: `DUB-A-20-50-1530` → `{ quality: "DUB-A", thickness: 20, width: 50, length: 1530 }`

This enables efficient queries like:
```kotlin
db.collection("Hranolky")
  .whereEqualTo("quality", "DUB-A")
  .whereEqualTo("thickness", 20)
  .get()
```

See [FIRESTORE_INDEXES_GUIDE.md](FIRESTORE_INDEXES_GUIDE.md) for complete setup instructions.

## Usage

### Prerequisites

- A dump.json file (created by the firestore-dump tool)
- A Firebase service account JSON file (private_secret.json)

### Quick Start (Using Helper Script)

The easiest way to run the upload is using the provided `upload.sh` script:

```bash
cd firestore-up

# Dry run (recommended first)
./upload.sh --dry-run

# Actual upload (you'll be asked to confirm)
./upload.sh

# With custom files
./upload.sh --input /path/to/dump.json --credentials /path/to/credentials.json --dry-run
```

### Manual Usage with Gradle

### Dry Run (Recommended First)

Before uploading to Firestore, run a dry-run to verify the transformations:

```bash
./gradlew :firestore-up:upload \
  -Pinput=/path/to/dump.json \
  -Pcredentials=/path/to/private_secret.json \
  -PdryRun
```

### Actual Upload

Once you've verified the dry-run output, remove the `-PdryRun` flag to perform the actual upload:

```bash
./gradlew :firestore-up:upload \
  -Pinput=/path/to/dump.json \
  -Pcredentials=/path/to/private_secret.json
```

### Using Relative Paths

If running from the firestore-up directory:

```bash
cd firestore-up
../gradlew :firestore-up:upload \
  -Pinput=dump.json \
  -Pcredentials=private_secret.json \
  -PdryRun
```

## Options

- `--input <path>` (required) - Path to the dump.json file
- `--credentials <path>` (required) - Path to the Firebase service account JSON file
- `--dry-run` (optional) - Simulate the upload without actually writing to Firestore

## Output Example

```
[firestore-upload] Upload complete (DRY RUN - NO DATA WRITTEN)!
[firestore-upload] Statistics:
  - Hranolky documents: 234
  - Sparovky documents: 96
  - SlotActions: 2109
  - SlotWeeklyReports: 1238
  - Weekly Hranolky reports: 20
  - Weekly Sparovky reports: 2
  - AppConfig documents: 1
  - Devices: 14
  - Total documents written: 3714
```

Note: The script automatically skips SlotWeeklyReport documents that have the same quantity as the previous week, reducing unnecessary data duplication (~156 documents optimized away in this example).

## Important Notes

- **Always run a dry-run first** to verify the transformations
- The upload process is synchronous and may take some time for large datasets
- Progress is logged for each document written
- All timestamps, GeoPoints, and other Firestore data types are preserved
- Subcollections are properly migrated along with their parent documents

## Troubleshooting

### File not found error
Make sure to use absolute paths or correct relative paths to your dump.json and credentials files.

### Permission errors
Ensure your service account has write permissions to the target Firestore database.

### Out of memory
For very large databases, you may need to increase the JVM heap size:
```bash
./gradlew :firestore-up:upload -Pinput=... -Pcredentials=... -Dorg.gradle.jvmargs=-Xmx4g
```

