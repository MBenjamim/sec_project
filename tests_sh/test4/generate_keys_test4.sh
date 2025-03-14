#!/bin/bash

TEST_DIR="tests_sh/test4"
CONFIG_FILE="$TEST_DIR/test4_config.cfg"

# shellcheck disable=SC1090
source $CONFIG_FILE

LOG_LEVEL="info"
if [[ "$1" == "-DEBUG" ]]; then
    LOG_LEVEL="debug"
fi

# Function to generate and distribute keys
generate_and_copy_keys() {
    local DIR_NAME=$1
    local PRIV_KEY_PATH="$DIR_NAME/private.key"
    local PUB_KEY_PATH="$DIR_NAME/public.key"

    mkdir -p "$DIR_NAME"

    # Run the Java key generator
    mvn exec:java -Dexec.mainClass=main.java.crypto_utils.RSAKeyGenerator -Dexec.args="$PRIV_KEY_PATH $PUB_KEY_PATH" -DLOG_LEVEL=$LOG_LEVEL

    # Verify key generation
    if [ ! -f "$PRIV_KEY_PATH" ] || [ ! -f "$PUB_KEY_PATH" ]; then
        echo "Key generation failed for $DIR_NAME!"
        exit 1
    fi

    cp "${DIR_NAME}/public.key" "public_keys/${DIR_NAME}_public.key"
}

mkdir -p "public_keys"

# Generate and distribute keys for each server
for ((i=0; i<NUM_SERVERS; i++)); do
    generate_and_copy_keys "server${i}"

done

# Generate and distribute keys for each client
for ((i=0; i<NUM_CLIENTS; i++)); do
    generate_and_copy_keys "client${i}"
done

echo "All keys have been generated and distributed successfully!"
