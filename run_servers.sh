#!/bin/bash

source config.cfg

CONFIG_FILE="config.cfg"

LOG_LEVEL="info"
if [[ "$1" == "-DEBUG" ]]; then
    LOG_LEVEL="debug"
fi

# Start the first server in a new tmux session
tmux new-session -d -s blockchain_network -n server_0 "mvn exec:java -Dexec.mainClass=main.java.server.BlockchainNetworkServer -Dexec.args=\"0 $CONFIG_FILE\" -DLOG_LEVEL=$LOG_LEVEL"
if [ $? -ne 0 ]; then
    echo "Failed to create tmux session."
    exit 1
fi

# Start additional servers in separate tmux panes
for ((i=1; i<$NUM_SERVERS; i++)); do
    tmux new-window -n server_$i "mvn exec:java -Dexec.mainClass=main.java.server.BlockchainNetworkServer -Dexec.args=\"$i $CONFIG_FILE\" -DLOG_LEVEL=$LOG_LEVEL"
    sleep 1
done

# Attach to tmux session
tmux attach-session -t blockchain_network
