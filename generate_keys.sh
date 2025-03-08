#!/bin/bash

source load_config_and_compile.sh

# Function to generate and distribute keys
generate_and_copy_keys() {
    local DIR_NAME=$1
    local PRIV_KEY_PATH="$DIR_NAME/private.key"
    local PUB_KEY_PATH="$DIR_NAME/public.key"

    mkdir -p "$DIR_NAME"

    # Run the Java key generator
    java -cp target/classes main.java.crypto_utils.RSAKeyGenerator "$PRIV_KEY_PATH" "$PUB_KEY_PATH"

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
