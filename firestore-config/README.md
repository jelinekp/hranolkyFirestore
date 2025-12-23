# Firestore Config Module

This module provides a utility for updating the `AppConfig/latest` document in Firestore with new version information.

## Features

- Updates `AppConfig/latest` document with new version, versionCode, and releaseNotes
- Archives the previous "latest" document by copying it to `AppConfig/{version}` before updating
- Supports dry-run mode to preview changes without modifying Firestore

## Usage

### Via Gradle Task

```bash
./gradlew :firestore-config:updateConfig \
    -Pversion="3.0.5" \
    -PversionCode="19" \
    -PreleaseNotes="Bug fixes and improvements" \
    -Pcredentials="firestore-config/private_secret.json"
```

Add `-PdryRun` flag to simulate without writing:

```bash
./gradlew :firestore-config:updateConfig \
    -Pversion="3.0.5" \
    -PversionCode="19" \
    -PreleaseNotes="Bug fixes and improvements" \
    -Pcredentials="firestore-config/private_secret.json" \
    -PdryRun
```

### Via upload_apk.sh Script

The `upload_apk.sh` script in the root directory integrates this module. When running the script, you'll be prompted to optionally update Firestore AppConfig with the new version info.

## Firestore Document Structure

### `AppConfig/latest`
```
{
  "downloadUrl": "https://jelinekp.cz/app/update.apk",
  "releaseNotes": "Description of changes",
  "version": "3.0.5",
  "versionCode": 19
}
```

### Archived versions (e.g., `AppConfig/3.0.4`)
Same structure as `latest`, preserved for historical reference.

## Setup

1. Copy `private_secret.json` (Firebase service account key) to this directory
2. Make sure the service account has write access to the `AppConfig` collection

## Requirements

- JDK 17+
- Firebase Admin SDK credentials (`private_secret.json`)

