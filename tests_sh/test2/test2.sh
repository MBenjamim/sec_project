#!/bin/bash

TN=2
BEHAVIOR=NO_RESPONSE_TO_LEADER

TEST_DIR="./tests_sh/test${TN}"
CONFIG_FILE="$TEST_DIR/test${TN}_config.cfg"
LOG_DIR="$TEST_DIR/logs"
TMP_DIR="/tmp"


# shellcheck disable=SC1090
source $CONFIG_FILE

bash $TEST_DIR/check_config_and_compile_test${TN}.sh > /dev/null 2>&1
bash $TEST_DIR/generate_keys_test${TN}.sh > /dev/null 2>&1

LOG_LEVEL="info"
if [[ "$1" == "-DEBUG" ]]; then
    LOG_LEVEL="debug"
fi

# Delete the logs from the previous run if it exists
rm -f $LOG_DIR/*.log

# Create the tmp directory if it does not exist
mkdir -p $TMP_DIR

# Create named pipes (FIFO) for client input
for ((i=0; i<NUM_CLIENTS; i++)); do
    PIPE_PATH="$TMP_DIR/blockchain_client_fifo_$i"
    rm -f "$PIPE_PATH"
    mkfifo "$PIPE_PATH"
done

# Calculate the number of Byzantine processes
NUM_BYZANTINE=$(((NUM_SERVERS-1)/3))

# Print the test description
echo "------------------------------------------------------------"
echo "Test${TN} Description:"
echo "    Number of servers: $NUM_SERVERS"
echo "    Number of clients: $NUM_CLIENTS"
echo "    Number of Byzantine processes: $NUM_BYZANTINE"
echo "    Leader ID: $LEADER_ID"
echo "    Byzantine behavior: $BEHAVIOR"

# Function to kill background processes and clean up
cleanup() {
    for ((i=0; i<NUM_BYZANTINE; i++)); do
          eval kill \$SERVER_BYZANTINE_${i}_PID > /dev/null 2>&1
    done
    for ((i=0; i<$((NUM_SERVERS-1)); i++)); do
        eval kill \$SERVER_${i}_PID > /dev/null 2>&1
    done
    for ((i=0; i<NUM_CLIENTS; i++)); do
        eval kill \$CLIENT_${i}_PID > /dev/null 2>&1
        rm -f "$TMP_DIR/blockchain_client_fifo_$i"
    done

    bash $TEST_DIR/cleanup_test${TN}.sh > /dev/null 2>&1
}
trap cleanup EXIT

#RUN SERVERS
# Start correct servers
for ((i=0; i<$((NUM_SERVERS-NUM_BYZANTINE)); i++)); do
    SERVER_PORT=$((BASE_PORT_SERVER_TO_SERVER + i))
    CLIENT_PORT=$((BASE_PORT_CLIENT_TO_SERVER + i))
    mvn exec:java -Dexec.mainClass=main.java.server.BlockchainNetworkServer -Dexec.args="$i $SERVER_PORT $CLIENT_PORT $CONFIG_FILE" -DLOG_LEVEL=$LOG_LEVEL &> $LOG_DIR/server_$i.log &
    eval SERVER_${i}_PID=$!
    # shellcheck disable=SC2181
    if [ $? -ne 0 ]; then
        echo "Failed to start server $i."
        exit 1
    fi
    sleep 1
done

# Start Byzantine servers
for ((i=0; i<NUM_BYZANTINE; i++)); do
    SERVER_INDEX=$((NUM_SERVERS-1-i))
    mvn exec:java -Dexec.mainClass=main.java.server.BlockchainNetworkServer -Dexec.args="$SERVER_INDEX $((BASE_PORT_SERVER_TO_SERVER + SERVER_INDEX)) $((BASE_PORT_CLIENT_TO_SERVER + SERVER_INDEX)) $CONFIG_FILE $BEHAVIOR" -DLOG_LEVEL=$LOG_LEVEL &> $LOG_DIR/server_byzantine_$i.log &
    eval SERVER_BYZANTINE_${i}_PID=$!
done



#RUN CLIENTS
# Start clients and redirect input from their respective named pipes
for ((i=0; i<NUM_CLIENTS; i++)); do
    PIPE_PATH="$TMP_DIR/blockchain_client_fifo_$i"
    PORT=$((BASE_PORT_CLIENTS + i))
    mvn exec:java -Dexec.mainClass=main.java.client.BlockchainClient -Dexec.args="$i $PORT $CONFIG_FILE" -DLOG_LEVEL=$LOG_LEVEL < "$PIPE_PATH" &> $LOG_DIR/client_$i.log &
    eval CLIENT_${i}_PID=$!
    # shellcheck disable=SC2181
    if [ $? -ne 0 ]; then
        echo "Failed to start client $i."
        exit 1
    fi
done

sleep 5

# Send input to the client process through the named pipe
for ((i=0; i<NUM_CLIENTS; i++)); do
    echo "value_to_append_$i" > "$TMP_DIR/blockchain_client_fifo_$i"
done

# Wait for the system to process the input
printf "Sleeping for %d seconds to allow the system to process the input...\n" "$SLEEP_TIME"
sleep "$SLEEP_TIME"

# Check the log files for the expected log entry
ALL_PASSED=true
for ((i=0; i<NUM_CLIENTS; i++)); do
    if ! grep -q "Value 'value_to_append_$i' appended to the blockchain with timestamp" $LOG_DIR/client_$i.log; then
        printf "\e[31m[FAILED] TEST%d: Expected log entry not found in client_%d.log.\e[0m\n" "$TN" "$i"
        ALL_PASSED=false
    fi
done

if [ "$ALL_PASSED" = true ]; then
    printf "\e[32m[PASSED] TEST%d: Expected log entry found in all client logs.\e[0m\n" "$TN"
fi
echo "------------------------------------------------------------"
