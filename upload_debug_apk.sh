#!/bin/bash
# APK Upload Script for Hranolky Firestore
# Uploads app-debug.apk to FTP server and renames it to update.apk
set -e  # Exit on error
# Configuration
FTP_HOST="ftp4.webzdarma.cz"
FTP_USER="jelinekp.wz.cz"
FTP_REMOTE_PATH="/app/update.apk"
LOCAL_APK="app/debug/app-debug.apk"
PASSWORD_FILE=".ftp_password"
# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color
echo -e "${GREEN}=== Hranolky Firestore APK Upload ===${NC}"
echo
# Check if APK exists
if [ ! -f "$LOCAL_APK" ]; then
    echo -e "${RED}Error: APK file not found at $LOCAL_APK${NC}"
    echo "Please build the debug APK first:"
    echo "  ./gradlew assembleDebug"
    exit 1
fi
# Get APK info
APK_SIZE=$(du -h "$LOCAL_APK" | cut -f1)
echo -e "Found APK: ${GREEN}$LOCAL_APK${NC} (${APK_SIZE})"
echo
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
    # Show file modification time
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
