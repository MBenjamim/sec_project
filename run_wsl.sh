#!/bin/bash

source config.cfg

if [ -z "$NUM_SERVERS" ] || [ -z "$BASE_PORT" ]; then
    echo "Configuration values (NUM_SERVERS or BASE_PORT) not set properly in config.cfg"
    exit 1
fi

# Build the project using Maven
mvn clean compile
if [ $? -ne 0 ]; then
    echo "Maven build failed!"
    exit 1
fi

# Start the first server in a new tmux session
tmux new-session -d -s blockchain_network -n server_0 "mvn exec:java -Dexec.mainClass=BlockchainNetworkServer -Dexec.args=\"0 $BASE_PORT\""
if [ $? -ne 0 ]; then
    echo "Failed to create tmux session."
    exit 1
fi

# Start additional servers in separate tmux windows
for ((i=1; i<$NUM_SERVERS; i++)); do
    PORT=$((BASE_PORT + i))
    tmux new-window -n server_$i "mvn exec:java -Dexec.mainClass=BlockchainNetworkServer -Dexec.args=\"$i $PORT\""
    sleep 1
done

# Attach to tmux session
tmux attach-session -t blockchain_network
