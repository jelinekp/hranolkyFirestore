#!/bin/bash
# APK Upload Script for Hranolky Firestore
# Uploads app-release.apk to FTP server and renames it to update.apk
# Optionally updates Firestore AppConfig with version info
set -e  # Exit on error

# Configuration
FTP_HOST="ftp4.webzdarma.cz"
FTP_USER="jelinekp.wz.cz"
FTP_REMOTE_PATH="/app/update.apk"
LOCAL_APK="app/release/app-release.apk"
PASSWORD_FILE=".ftp_password"
FIRESTORE_CREDENTIALS="firestore-config/private_secret.json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color
echo -e "${GREEN}=== Hranolky Firestore APK Upload ===${NC}"
echo
# Check if APK exists
if [ ! -f "$LOCAL_APK" ]; then
    echo -e "${RED}Error: APK file not found at $LOCAL_APK${NC}"
    echo "Please build the release APK first:"
    echo "  ./gradlew assembleRelease"
    exit 1
fi
# Get APK info
APK_SIZE=$(du -h "$LOCAL_APK" | cut -f1)
echo -e "Found APK: ${GREEN}$LOCAL_APK${NC} (${APK_SIZE})"

# Extract version information directly from APK using aapt2
AAPT2=$(find ~/Android/Sdk/build-tools -name aapt2 2>/dev/null | sort -V | tail -1)

VERSION_NAME=""
VERSION_CODE=""

if [ -n "$AAPT2" ] && [ -f "$AAPT2" ]; then
    echo -e "${YELLOW}Extracting version info from APK...${NC}"

    # Use aapt2 dump badging to extract version info
    BADGING=$("$AAPT2" dump badging "$LOCAL_APK" 2>/dev/null || true)

    if [ -n "$BADGING" ]; then
        VERSION_NAME=$(echo "$BADGING" | grep -oP "versionName='\K[^']+" | head -1)
        VERSION_CODE=$(echo "$BADGING" | grep -oP "versionCode='\K[^']+" | head -1)
    fi
fi

# Fallback to extracting from build.gradle.kts if aapt2 didn't work
if [ -z "$VERSION_NAME" ] || [ -z "$VERSION_CODE" ]; then
    BUILD_GRADLE="app/build.gradle.kts"
    if [ -f "$BUILD_GRADLE" ]; then
        VERSION_NAME=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$BUILD_GRADLE")
        VERSION_CODE=$(grep -oP 'versionCode\s*=\s*\K\d+' "$BUILD_GRADLE")
    fi
fi

if [ -n "$VERSION_NAME" ] && [ -n "$VERSION_CODE" ]; then
    echo -e "App Version: ${GREEN}$VERSION_NAME${NC} (code: $VERSION_CODE)"
else
    echo -e "${YELLOW}Warning: Could not extract version info${NC}"
fi
echo

# === FTP Upload ===
echo -e "${BLUE}=== FTP Upload ===${NC}"
# Check for stored password
if [ -f "$PASSWORD_FILE" ]; then
    echo -e "${YELLOW}Using stored FTP password${NC}"
    FTP_PASSWORD=$(cat "$PASSWORD_FILE")
else
    echo -e "${YELLOW}No stored password found. Please enter FTP password:${NC}"
    read -s -p "Password for $FTP_USER: " FTP_PASSWORD
    echo
    # Ask if password should be saved
    read -p "Save password for future use? (y/n): " SAVE_PASSWORD
    if [ "$SAVE_PASSWORD" = "y" ] || [ "$SAVE_PASSWORD" = "Y" ]; then
        echo "$FTP_PASSWORD" > "$PASSWORD_FILE"
        chmod 600 "$PASSWORD_FILE"  # Make it readable only by owner
        echo -e "${GREEN}Password saved to $PASSWORD_FILE${NC}"
    fi
fi
echo
echo -e "${YELLOW}Uploading to FTP server...${NC}"
echo "Host: $FTP_HOST"
echo "User: $FTP_USER"
echo "Remote path: $FTP_REMOTE_PATH"
echo
# Upload using curl
if curl --ftp-create-dirs \
        -T "$LOCAL_APK" \
        -u "$FTP_USER:$FTP_PASSWORD" \
        "ftp://$FTP_HOST$FTP_REMOTE_PATH" \
        --progress-bar \
        -o /dev/null; then
    echo
    echo -e "${GREEN}✓ Upload successful!${NC}"
    echo -e "APK uploaded to: ftp://$FTP_HOST$FTP_REMOTE_PATH"
    echo -e "Download URL: ${GREEN}https://jelinekp.cz/app/update.apk${NC}"
    echo
    echo "Upload completed at: $(date '+%Y-%m-%d %H:%M:%S')"
else
    echo
    echo -e "${RED}✗ Upload failed!${NC}"
    echo
    echo "Possible issues:"
    echo "  - Incorrect password (delete $PASSWORD_FILE to re-enter)"
    echo "  - FTP server unreachable"
    echo "  - Permission issues"
    exit 1
fi
echo

# === Firestore AppConfig Update ===
echo -e "${BLUE}=== Firestore AppConfig Update ===${NC}"
read -p "Do you want to update Firestore AppConfig? (y/n): " UPDATE_FIRESTORE

if [ "$UPDATE_FIRESTORE" = "y" ] || [ "$UPDATE_FIRESTORE" = "Y" ]; then
    # Check for credentials file
    if [ ! -f "$FIRESTORE_CREDENTIALS" ]; then
        echo -e "${RED}Error: Firestore credentials not found at $FIRESTORE_CREDENTIALS${NC}"
        echo "Please copy private_secret.json to firestore-config/"
        exit 1
    fi

    # Check if version info is available
    if [ -z "$VERSION_NAME" ] || [ -z "$VERSION_CODE" ]; then
        echo -e "${RED}Error: Version info not available. Cannot update Firestore.${NC}"
        exit 1
    fi

    # Get release notes from user
    echo -e "${YELLOW}Enter release notes (press Enter when done):${NC}"
    read -r RELEASE_NOTES

    if [ -z "$RELEASE_NOTES" ]; then
        echo -e "${RED}Error: Release notes cannot be empty${NC}"
        exit 1
    fi

    echo
    echo -e "${YELLOW}Firestore update summary:${NC}"
    echo -e "  Version: ${GREEN}$VERSION_NAME${NC}"
    echo -e "  Version Code: ${GREEN}$VERSION_CODE${NC}"
    echo -e "  Release Notes: ${GREEN}$RELEASE_NOTES${NC}"
    echo
    read -p "Proceed with Firestore update? (y/n): " CONFIRM_FIRESTORE

    if [ "$CONFIRM_FIRESTORE" = "y" ] || [ "$CONFIRM_FIRESTORE" = "Y" ]; then
        echo -e "${YELLOW}Updating Firestore AppConfig...${NC}"

        # Run the Gradle task to update Firestore
        ./gradlew :firestore-config:updateConfig \
            -Pversion="$VERSION_NAME" \
            -PversionCode="$VERSION_CODE" \
            -PreleaseNotes="$RELEASE_NOTES" \
            -Pcredentials="$FIRESTORE_CREDENTIALS" \
            --quiet

        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ Firestore AppConfig updated successfully!${NC}"
        else
            echo -e "${RED}✗ Firestore update failed!${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}Skipping Firestore update${NC}"
    fi
fi

echo
echo -e "${GREEN}=== All done! ===${NC}"

