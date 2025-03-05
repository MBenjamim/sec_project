#!/bin/bash

source load_config_and_compile.sh

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
