#!/bin/bash

source config.cfg

mvn clean

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
