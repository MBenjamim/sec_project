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
