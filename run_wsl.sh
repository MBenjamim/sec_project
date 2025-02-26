source config.cfg

if [ -z "$NUM_SERVERS" ] || [ -z "$BASE_PORT" ]; then
    echo "Configuration values (NUM_SERVERS or BASE_PORT) not set properly in config.cfg"
    exit 1
fi

javac -d out src/*.java

# Start tmux session
tmux new-session -d -s blockchain_network -n server_0 "java -cp out BlockchainNetworkServer $BASE_PORT"
if [ $? -ne 0 ]; then
    echo "Failed to create tmux session."
    exit 1
fi

# Start multiple servers in tmux windows
for ((i=1; i<$NUM_SERVERS; i++)); do
    PORT=$((BASE_PORT + i))
    tmux new-window -n server_$i "java -cp out BlockchainNetworkServer $PORT"
    sleep 1
done

# Attach to tmux session
tmux attach-session -t blockchain_network