#!/bin/bash

TN=5
SERVER_BEHAVIOR=WRONG_WRITE
CLIENT_BEHAVIOR=REPLAY_ATTACK

TEST_DIR="./tests_sh/test${TN}"
CONFIG_FILE="$TEST_DIR/test${TN}_config.cfg"
LOG_DIR="$TEST_DIR/logs"
TMP_DIR="/tmp"

# shellcheck disable=SC1090
source $CONFIG_FILE

bash ./tests_sh/check_config_and_compile_tests.sh $TN > /dev/null 2>&1
bash ./tests_sh/generate_keys_tests.sh $TN > /dev/null 2>&1

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

# Calculate the number of Byzantine Servers
NUM_BYZANTINE_SERVERS=$(((NUM_SERVERS-1)/3))
NUM_BYZANTINE_CLIENTS=1
NUM_REGULAR_CLIENTS=$((NUM_CLIENTS-NUM_BYZANTINE_CLIENTS))

# Print the test description
echo "------------------------------------------------------------"
echo "Test${TN} Description:"
echo "    Number of servers: $NUM_SERVERS"
echo "    Number of clients: $NUM_CLIENTS"
echo "    Number of Byzantine Servers: $NUM_BYZANTINE_SERVERS"
echo "    Number of Byzantine Clients: $NUM_BYZANTINE_CLIENTS"
echo "    Leader ID: $LEADER_ID"
echo "    Servers Byzantine Behavior: $SERVER_BEHAVIOR"
echo "    Clients Byzantine Behavior: $CLIENT_BEHAVIOR"

# Function to kill background processes and clean up
cleanup() {
    # Kill Byzantine servers
    for ((i=0; i<NUM_BYZANTINE_SERVERS; i++)); do
          eval kill \$SERVER_BYZANTINE_${i}_PID > /dev/null 2>&1
    done

    # Kill regular servers
    for ((i=0; i<$((NUM_SERVERS-NUM_BYZANTINE_SERVERS)); i++)); do
        eval kill \$SERVER_${i}_PID > /dev/null 2>&1
    done

    # Kill regular clients
    for ((i=0; i<NUM_REGULAR_CLIENTS; i++)); do
        eval kill \$CLIENT_${i}_PID > /dev/null 2>&1
    done

    # Kill Byzantine clients
    for ((i=0; i<NUM_BYZANTINE_CLIENTS; i++)); do
        eval kill \$CLIENT_BYZANTINE_${i}_PID > /dev/null 2>&1
    done

    # Remove all FIFOs
    for ((i=0; i<NUM_CLIENTS; i++)); do
        rm -f "$TMP_DIR/blockchain_client_fifo_$i"
    done

    bash ./tests_sh/cleanup_tests.sh $TN > /dev/null 2>&1
}
trap cleanup EXIT

#RUN SERVERS
# Start correct servers
for ((i=0; i<$((NUM_SERVERS-NUM_BYZANTINE_SERVERS)); i++)); do
    mvn exec:java -Dexec.mainClass=main.java.server.BlockchainNetworkServer -Dexec.args="$i $CONFIG_FILE" -DLOG_LEVEL=$LOG_LEVEL &> $LOG_DIR/server_$i.log &
    eval SERVER_${i}_PID=$!
    # shellcheck disable=SC2181
    if [ $? -ne 0 ]; then
        echo "Failed to start server $i."
        exit 1
    fi
    sleep 1
done

# Start Byzantine servers
for ((i=0; i<NUM_BYZANTINE_SERVERS; i++)); do
    SERVER_INDEX=$((NUM_SERVERS-1-i))
    mvn exec:java -Dexec.mainClass=main.java.server.BlockchainNetworkServer -Dexec.args="$SERVER_INDEX $CONFIG_FILE $SERVER_BEHAVIOR" -DLOG_LEVEL=$LOG_LEVEL &> $LOG_DIR/server_byzantine_$i.log &
    eval SERVER_BYZANTINE_${i}_PID=$!
done



#RUN CLIENTS
# Start regular clients and redirect input from their respective named pipes
for ((i=0; i<NUM_REGULAR_CLIENTS; i++)); do
    PIPE_PATH="$TMP_DIR/blockchain_client_fifo_$i"
    mvn exec:java -Dexec.mainClass=main.java.client.BlockchainClient -Dexec.args="$i $CONFIG_FILE" -DLOG_LEVEL=$LOG_LEVEL < "$PIPE_PATH" &> $LOG_DIR/client_$i.log &
    eval CLIENT_${i}_PID=$!
    # shellcheck disable=SC2181
    if [ $? -ne 0 ]; then
        echo "Failed to start client $i."
        exit 1
    fi
done

# Start Byzantine clients
for ((i=0; i<NUM_BYZANTINE_CLIENTS; i++)); do
    CLIENT_INDEX=$((NUM_REGULAR_CLIENTS+i))
    PIPE_PATH="$TMP_DIR/blockchain_client_fifo_$CLIENT_INDEX"
    mvn exec:java -Dexec.mainClass=main.java.client.BlockchainClient -Dexec.args="$CLIENT_INDEX $CONFIG_FILE $CLIENT_BEHAVIOR" -DLOG_LEVEL=$LOG_LEVEL < "$PIPE_PATH" &> $LOG_DIR/client_byzantine_$CLIENT_INDEX.log &
    eval CLIENT_BYZANTINE_${i}_PID=$!
    # shellcheck disable=SC2181
    if [ $? -ne 0 ]; then
        echo "Failed to start Byzantine client $CLIENT_INDEX."
        exit 1
    fi
done

sleep 5

# Client 0 authorizes client 1(byzantine) to send tokens on his behalf
for ((i=0; i<NUM_REGULAR_CLIENTS; i++)); do
    echo "approve -amount 20 -id 1" > "$TMP_DIR/blockchain_client_fifo_$i"
done

sleep 5

# Send the transfer from command twice (replay attack) (implemented in the client code)
for ((i=0; i<NUM_BYZANTINE_CLIENTS; i++)); do
    CLIENT_INDEX=$((NUM_REGULAR_CLIENTS+i))
    echo "transfer_from -amount 20 -fromid 0 -toid 1" > "$TMP_DIR/blockchain_client_fifo_$CLIENT_INDEX"
done

# Wait for the system to process the input
printf "Sleeping for %d seconds to allow the system to process the input...\n" "$SLEEP_TIME"
sleep "$SLEEP_TIME"

# Check the log files for the expected log entries
TEST_PASSED=true

# 1) Check regular clients
for ((i=0; i<NUM_REGULAR_CLIENTS; i++)); do
    if ! grep -q "Statkus: Success" "$LOG_DIR/client_$i.log"; then
        printf "\\e[31mTEST FAILED: Regular client_%d missing 'Status: Success'\\e[0m\\n" "$i"
        TEST_PASSED=false
    fi
done

# 2) Check byzantine clients
for ((i=0; i<NUM_BYZANTINE_CLIENTS; i++)); do
    CLIENT_INDEX=$((NUM_REGULAR_CLIENTS + i))

    SUCCESS_COUNT=$(grep -c "Statujs: Success" "$LOG_DIR/client_byzantine_$CLIENT_INDEX.log")
    DESC_COUNT=$(grep -c "Description: Transaction added to block." "$LOG_DIR/client_byzantine_$CLIENT_INDEX.log")
    TYPE_COUNT=$(grep -c "Transaction Type: TRANSFER_FROM" "$LOG_DIR/client_byzantine_$CLIENT_INDEX.log")

    if [ "$SUCCESS_COUNT" -ne 1 ] || [ "$DESC_COUNT" -ne 1 ] || [ "$TYPE_COUNT" -ne 1 ]; then
        printf "\\e[31mTEST FAILED: Byzantine client %d missing the expected log lines.\\e[0m\\n" "$CLIENT_INDEX"
        TEST_PASSED=false
    fi
done

# 3) Check non-byzantine servers
for ((i=0; i<NUM_SERVERS - NUM_BYZANTINE_SERVERS; i++)); do
    if ! grep -q "Invalid trajnsaction" "$LOG_DIR/server_$i.log"; then
        printf "\\e[31mTEST FAILED: Non-byzantine server_%d missing 'Invalid transaction'\\e[0m\\n" "$i"
        TEST_PASSED=false
    fi
done

if [ "$TEST_PASSED" = true ]; then
    printf "\\e[32m[PASSED] TEST%d: Replay attack was detected and handled correctly.\\e[0m\\n" "$TN"
else
    printf "\\e[31m[FAILED] TEST%d: Replay attack detection failed. Check logs.\\e[0m\\n" "$TN"
fi
echo "------------------------------------------------------------"
