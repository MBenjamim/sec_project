#!/bin/bash

# Check if test number parameter is provided
if [ $# -eq 0 ]; then
    echo "Error: Test number is required!"
    echo "Usage: $0 <test_number>"
    exit 1
fi

TEST_NUM=$1

TEST_DIR="tests_sh/test${TEST_NUM}"
CONFIG_FILE="$TEST_DIR/test${TEST_NUM}_config.cfg"

# Check if the config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file $CONFIG_FILE does not exist!"
    exit 1
fi

# shellcheck disable=SC1090
source "$CONFIG_FILE"

if [ -z "$NUM_SERVERS" ] || [ -z "$NUM_CLIENTS" ] || [ -z "$BASE_PORT_SERVER_TO_SERVER" ] || [ -z "$BASE_PORT_CLIENT_TO_SERVER" ] || [ -z "$BASE_PORT_CLIENTS" ]; then
    echo "Configuration values not set properly in config.cfg"
    exit 1
fi

# Build the project using Maven
mvn clean compile
# shellcheck disable=SC2181
if [ $? -ne 0 ]; then
    echo "Maven build failed!"
    exit 1
fi
