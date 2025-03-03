#!/bin/bash

source load_config_and_compile.sh

BASE_NAME="server"

# Generate keys for each server
for ((i=0; i<NUM_SERVERS; i++)); do
    SERVER="${BASE_NAME}${i}"

    mkdir -p "$SERVER"
    PRIV_KEY_PATH="$SERVER/private.key"
    PUB_KEY_PATH="$SERVER/public.key"

    # Run the Java key generator
    java -cp target/classes utils.RSAKeyGenerator "$PRIV_KEY_PATH" "$PUB_KEY_PATH"

    # Verify key generation
    if [ ! -f "$PRIV_KEY_PATH" ] || [ ! -f "$PUB_KEY_PATH" ]; then
        echo "Key generation failed for $SERVER!"
        exit 1
    fi
done

# Distribute public keys to all servers
for ((i=0; i<NUM_SERVERS; i++)); do
    SERVER_DIR="${BASE_NAME}${i}"

    for ((j=0; j<NUM_SERVERS; j++)); do
        if [ "$i" -ne "$j" ]; then  # Don't copy its own public key
            cp "${BASE_NAME}${j}/public.key" "$SERVER_DIR/server${j}_public.key"
        fi
    done

    echo "All public keys distributed in $SERVER_DIR."
done

echo "All keys have been generated and distributed successfully!"
