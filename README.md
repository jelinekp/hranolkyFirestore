# Hranolky Firestore

A warehouse management Android application for tracking timber beams (_hranolky_) and jointer boards (_spárovky_) inventory.

## Overview

This application runs on Zebra TC200J Android terminals (API level 27) with infrared QR code scanners. It manages inventory data stored in Google Firebase Firestore.

## Project Structure

```
hranolkyFirestore/
├── app/                          # Main Android application
│   └── src/main/java/eu/jelinek/hranolky/
│       ├── config/               # Centralized configuration (NS DVT)
│       ├── data/                 # Repository implementations
│       ├── domain/               # Business logic, use cases
│       │   ├── auth/             # Authentication states
│       │   ├── update/           # Update states
│       │   └── usecase/          # Extracted use cases
│       ├── model/                # Data classes
│       └── ui/                   # Presentation layer (Jetpack Compose)
├── firestore-config/             # CLI tool for Firestore config
├── firestore-dump/               # CLI tool for data export
├── firestore-up/                 # CLI tool for data upload
```

## Building

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK with API level 27+

### Build Commands

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Build release APK
./gradlew :app:assembleRelease

# Run unit tests
./gradlew :app:testDebugUnitTest

# Run lint analysis
./gradlew :app:lintDebug
```

## Testing

The project includes a comprehensive test suite (306 tests):

| Test Category | Count |
|--------------|-------|
| Domain Logic (InputValidator, Parsers) | 46 |
| Model Classes (WarehouseSlot, SlotAction) | 27 |
| Use Cases (Inventory, Quantity) | 35 |
| ViewModels (Start, History) | 14 |
| State Isolation (Update, Auth) | 37 |
... 

Run tests:
```bash
./gradlew :app:testDebugUnitTest
```

## Architecture

This application follows Normalized Systems (NS) Theory principles:

### Data Version Transparency (DVT)
- Configuration extracted to `config/` package
- Firestore collection names in `FirestoreConfig.kt`
- Quality mappings in `QualityConfig.kt`
- Dimension adjustments in `DimensionConfig.kt`

### Separation of Concerns (SoC)
- Business logic extracted to use cases
- `QuantityParser` for input parsing
- `CheckInventoryStatusUseCase` for inventory logic

### Action Version Transparency (AVT)
- Operations defined as interfaces (`SlotActionOperations.kt`)
- `AddSlotActionUseCase` and `UndoSlotActionUseCase`

### Separation of States (SoS)
- `UpdateStates.kt` - isolated update flow states
- `AuthStates.kt` - isolated authentication states

## License

Proprietary
