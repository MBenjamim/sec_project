#!/bin/bash

source load_config_and_compile.sh

LOG_LEVEL="info"
if [[ "$1" == "-DEBUG" ]]; then
    LOG_LEVEL="debug"
fi

# Start the first client in a new tmux session
tmux new-session -d -s clients -n client_0 "mvn exec:java -Dexec.mainClass=main.java.client.BlockchainClient -Dexec.args=\"0 $BASE_PORT_CLIENTS\" -DLOG_LEVEL=$LOG_LEVEL"
if [ $? -ne 0 ]; then
    echo "Failed to create tmux session."
    exit 1
fi

# Start additional clients in separate tmux panes
for ((i=1; i<$NUM_CLIENTS; i++)); do
    CLIENT_PORT=$((BASE_PORT_CLIENTS + i))
    tmux new-window -n client_$i "mvn exec:java -Dexec.mainClass=main.java.client.BlockchainClient -Dexec.args=\"$i $CLIENT_PORT\" -DLOG_LEVEL=$LOG_LEVEL"
    sleep 1
done

# Attach to tmux session
tmux attach-session -t clients
