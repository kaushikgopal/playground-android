#!/bin/bash
# This script handles both file content and directory structure changes

set -e  # Exit immediately if a command exits with a non-zero status

# Check if a package name was provided
if [ $# -ne 1 ]; then
  echo "Usage: $0 <new_package_name>"
  echo "Example: $0 app.pudi.android"
  exit 1
fi

NEW_PACKAGE=$1
OLD_PACKAGE="sh.kau.playground"

echo "Refactoring Android project..."
echo "  From package: $OLD_PACKAGE"
echo "  To package: $NEW_PACKAGE"

# Convert package notation to directory structure
OLD_PACKAGE_DIR=$(echo $OLD_PACKAGE | tr '.' '/')
NEW_PACKAGE_DIR=$(echo $NEW_PACKAGE | tr '.' '/')

echo "Updating file contents..."
# Replace all occurrences of the old package name in files
find . -type f -not -path "*/\.*" -not -path "*/build/*" -exec grep -l "$OLD_PACKAGE" {} \; | while read file; do
  echo "  Updating $file"
  sed -i '' "s/$OLD_PACKAGE/$NEW_PACKAGE/g" "$file"
done

echo "Restructuring directories..."
# Find the Java/Kotlin source directories
SRC_DIRS=$(find . -type d -path "*/src/*/java" -o -path "*/src/*/kotlin")

for SRC_DIR in $SRC_DIRS; do
  if [ -d "$SRC_DIR/$OLD_PACKAGE_DIR" ]; then
    echo "  Restructuring in $SRC_DIR"

    # Create new package directory structure if it doesn't exist
    mkdir -p "$SRC_DIR/$NEW_PACKAGE_DIR"

    # Copy entire directory structure with subdirectories preserved
    if [ "$(uname)" == "Darwin" ]; then
      # macOS version
      cp -R "$SRC_DIR/$OLD_PACKAGE_DIR"/* "$SRC_DIR/$NEW_PACKAGE_DIR/" 2>/dev/null || true
    else
      # Linux version
      cp -R "$SRC_DIR/$OLD_PACKAGE_DIR"/* "$SRC_DIR/$NEW_PACKAGE_DIR/" 2>/dev/null || :
    fi

    # Remove old package directory
    rm -rf "$SRC_DIR/$OLD_PACKAGE_DIR"

    # Check and remove parent directories if empty, starting from deepest level
    OLD_PACKAGE_PARTS=$(echo $OLD_PACKAGE_DIR | tr '/' ' ')
    CURRENT_PATH="$SRC_DIR"
    PARENT_DIRS=()

    # First, build the list of all parent directories
    for part in $OLD_PACKAGE_PARTS; do
      CURRENT_PATH="$CURRENT_PATH/$part"
      PARENT_DIRS+=("$CURRENT_PATH")
    done

    # Then, remove directories from deepest to shallowest if they're empty
    for ((i=${#PARENT_DIRS[@]}-1; i>=0; i--)); do
      DIR="${PARENT_DIRS[i]}"
      if [ -d "$DIR" ] && [ -z "$(ls -A "$DIR")" ]; then
        echo "  Removing empty directory: $DIR"
        rmdir "$DIR"
      fi
    done
  fi
done

echo "Refactoring complete!"
echo "Note: You may need to clean and rebuild your project."
