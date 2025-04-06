#!/bin/bash

# Check if test number parameter is provided
if [ $# -eq 0 ]; then
    echo "Error: Test number is required!"
    echo "Usage: $0 <test_number>"
    exit 1
fi

TEST_NUM=$1

TEST_DIR="tests_sh/test${TEST_NUM}"
CONFIG_FILE="$TEST_DIR/test${TEST_NUM}_config.cfg"

# Check if the config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file $CONFIG_FILE does not exist!"
    exit 1
fi

# shellcheck disable=SC1090
source "$CONFIG_FILE"

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

rm genesis_block.json

echo "Cleanup complete. All server directories and keys have been removed."
