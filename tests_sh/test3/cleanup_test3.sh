#!/bin/bash

TEST_DIR="tests_sh/test3"
CONFIG_FILE="$TEST_DIR/test3_config.cfg"

# shellcheck disable=SC1090
source $CONFIG_FILE


if [ -z "$NUM_SERVERS" ] || [ -z "$NUM_CLIENTS" ] || [ -z "$BASE_PORT_SERVER_TO_SERVER" ] || [ -z "$BASE_PORT_CLIENT_TO_SERVER" ]; then
    echo "Configuration values not set properly in config.cfg"
    exit 1
fi

# Cleanup directory
remove_dir() {
    local DIR_NAME=$1

    if [ -d "$DIR_NAME" ]; then
        echo "Removing directory: $DIR_NAME"
        rm -rf "$DIR_NAME"
    else
        echo "Directory $DIR_NAME does not exist, skipping..."
    fi
}

for ((i=0; i<NUM_SERVERS; i++)); do
    remove_dir "server${i}"

done

for ((i=0; i<NUM_CLIENTS; i++)); do
    remove_dir "client${i}"
done

remove_dir "public_keys"

echo "Cleanup complete. All server directories and keys have been removed."
