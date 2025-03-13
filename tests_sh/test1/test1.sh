#!/bin/bash

source test1_config.cfg

CONFIG_FILE="tests_sh/test1/test1_config.cfg"

./check_config_and_compile_test1.sh > /dev/null 2>&1
./generate_keys_test1.sh > /dev/null 2>&1

LOG_LEVEL="info"
if [[ "$1" == "-DEBUG" ]]; then
    LOG_LEVEL="debug"
fi

# Delete the logs from the previous run if it exists
rm -f tests_sh/test1/logs/*.log

# Create named pipes (FIFO) for client input
# shellcheck disable=SC2004
for ((i=0; i<$NUM_CLIENTS; i++)); do
    PIPE_PATH="/tmp/blockchain_client_fifo_$i"
    rm -f "$PIPE_PATH"
    mkfifo "$PIPE_PATH"
done

# Function to kill background processes and clean up
cleanup() {
    kill $SERVER_BYZANTINE_PID > /dev/null 2>&1
    for ((i=0; i<$((NUM_SERVERS-1)); i++)); do
        eval kill \$SERVER_${i}_PID > /dev/null 2>&1
    done
    # shellcheck disable=SC2004
    for ((i=0; i<$NUM_CLIENTS; i++)); do
        eval kill \$CLIENT_${i}_PID > /dev/null 2>&1
        rm -f "/tmp/blockchain_client_fifo_$i"
    done
}
trap cleanup EXIT

cd ../.. # Move to the root directory of the project

#RUN SERVERS
BEHAVIOR=NO_RESPONSE_TO_ALL_SERVERS

# Start correct servers
# shellcheck disable=SC2004
for ((i=0; i<$((NUM_SERVERS-1)); i++)); do
    SERVER_PORT=$((BASE_PORT_SERVER_TO_SERVER + i))
    CLIENT_PORT=$((BASE_PORT_CLIENT_TO_SERVER + i))
    mvn exec:java -Dexec.mainClass=main.java.server.BlockchainNetworkServer -Dexec.args="$i $SERVER_PORT $CLIENT_PORT $CONFIG_FILE" -DLOG_LEVEL=$LOG_LEVEL &> tests_sh/test1/logs/server_$i.log &
    eval SERVER_${i}_PID=$!
    # shellcheck disable=SC2181
    if [ $? -ne 0 ]; then
        echo "Failed to start server $i."
        exit 1
    fi
    sleep 1
done

# Start the byzantine server
mvn exec:java -Dexec.mainClass=main.java.server.BlockchainNetworkServer -Dexec.args="$((NUM_SERVERS-1)) $((BASE_PORT_SERVER_TO_SERVER + NUM_SERVERS-1)) $((BASE_PORT_CLIENT_TO_SERVER + NUM_SERVERS-1)) $CONFIG_FILE $BEHAVIOR" -DLOG_LEVEL=$LOG_LEVEL &> tests_sh/test1/logs/server_byzantine.log &
SERVER_BYZANTINE_PID=$!



#RUN CLIENTS
# Start clients and redirect input from their respective named pipes
# shellcheck disable=SC2004
for ((i=0; i<$NUM_CLIENTS; i++)); do
    PIPE_PATH="/tmp/blockchain_client_fifo_$i"
    mvn exec:java -Dexec.mainClass=main.java.client.BlockchainClient -Dexec.args="$i $BASE_PORT_CLIENTS $CONFIG_FILE" -DLOG_LEVEL=$LOG_LEVEL < "$PIPE_PATH" &> tests_sh/test1/logs/client_$i.log &
    eval CLIENT_${i}_PID=$!
    if [ $? -ne 0 ]; then
        echo "Failed to start client $i."
        exit 1
    fi
done

sleep 5

# Send input to the client process through the named pipe
echo "value_to_append" > "/tmp/blockchain_client_fifo_0"

# Wait for the system to process the input
sleep 10

# Check the log file for the expected log entry
if grep -q "Value 'value_to_append' appended to the blockchain with timestamp" tests_sh/test1/logs/client_0.log; then
    printf "\e[32m1 - Test passed: Expected log entry found.\e[0m\n"
else
    printf "\e[31m1 - Test failed: Expected log entry not found.\e[0m\n"
fi

./cleanup_test1 > /dev/null 2>&1