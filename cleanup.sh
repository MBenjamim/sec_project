#!/bin/bash

source config.cfg

if [ -z "$NUM_SERVERS" ] || [ -z "$BASE_PORT" ]; then
    echo "Configuration values (NUM_SERVERS or BASE_PORT) not set properly in config.cfg"
    exit 1
fi

mvn clean

BASE_NAME="server"

# Cleanup each server directory
for ((i=0; i<NUM_SERVERS; i++)); do
    SERVER_DIR="${BASE_NAME}${i}"

    if [ -d "$SERVER_DIR" ]; then
        echo "Removing directory: $SERVER_DIR"
        rm -rf "$SERVER_DIR"
    else
        echo "Directory $SERVER_DIR does not exist, skipping..."
    fi
done

echo "Cleanup complete. All server directories and keys have been removed."
