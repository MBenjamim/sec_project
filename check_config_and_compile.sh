#!/bin/bash

source config.cfg

if [ -z "$NUM_SERVERS" ] || [ -z "$NUM_CLIENTS" ] || [ -z "$BASE_PORT_SERVER_TO_SERVER" ] || [ -z "$BASE_PORT_CLIENT_TO_SERVER" ] || [ -z "$BASE_PORT_CLIENTS" ]; then
    echo "Configuration values not set properly in config.cfg"
    exit 1
fi

# Build the project using Maven
mvn clean compile
if [ $? -ne 0 ]; then
    echo "Maven build failed!"
    exit 1
fi
