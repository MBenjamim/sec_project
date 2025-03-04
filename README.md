# SEC Project

This is a project for the Highly Dependable Systems course at Instituto Superior Técnico in Lisbon.
>This project aims to develop a simplified permissioned (closed membership) 
blockchain  system  with  high  dependability  guarantees,  called  Dependable 
Chain (DepChain). The system will be built iteratively throughout the first and 
second stages of the project: the first stage focuses on the consensus layer 
while the second stage will target the transaction processing layer.

## Current Stage
**Consensus Layer (Stage 1)**

## Participants
| Name              |   IST Number   |
|-------------------|----------------|
| Diogo Ribeiro     |     102484     |
| João Mestre       |     102779     |
| Miguel Benjamim   |     103560     |

## Prerequisites

- Java 17 or higher
- Maven
- Tmux (for running multiple servers in separate windows)

## Configuration

The configuration for the number of servers and the base port is specified in the `config.cfg` file:

```properties
NUM_SERVERS=3
BASE_PORT=5000
```

## Running the Project
1. **Load Configuration and Compile the Project**\
   Run the `load_config_and_compile.sh` script to load the configuration and compile the project using Maven:
```shell
./load_config_and_compile.sh
```

2. **Generate RSA Keys**\
   Run the `generate_keys.sh` script to generate RSA key pairs for each server and distribute the public keys:
```shell
./generate_keys.sh
```

3. **Run the Servers**\
    Run the run_wsl.sh script to start the servers in separate Tmux windows:
```shell
./run_wsl.sh
```

This script will start the first server in a new Tmux session and additional servers in separate Tmux windows.

## Cleanup
To clean up the generated files and directories, run the `cleanup.sh` script:
```shell
./cleanup.sh
```

## Project Structure
```
sec_project
├── README.md                        # This file
├──src/                              # Contains the Java source code for the project.
│    ├──BlockchainNetworkServer.java # Represents a server in the blockchain network.
│    ├──ClientHandler.java           # Handles client connections and processes incoming messages.
│    ├──NetworkManager.java          # Manages the network of nodes in the blockchain network.
│    ├──KeyManager.java              # Manages the cryptographic keys and operations for the network.
│    ├──Message.java                 # Represents a message in the blockchain network.
│    ├──Node.java                    # Represents a node in the blockchain network.
│    └── utils/                      # Contains utility classes for cryptographic operations.
│        ├──RSAAuthenticator.java    # Utility class for signing and verifying messages using RSA.
│        ├──RSAKeyGenerator.java     # Utility class for generating RSA key pairs and saving them to files.
│        ├──RSAKeyReader.java        # Utility class for reading RSA keys from files.
│        ├──AESKeyGenerator.java     # Utility class for generating and saving AES keys.
│        └──DataUtils.java           # Utility class for data conversion operations.
├──config.cfg                        # Configuration file for the number of servers and the base port.
├──pom.xml                           # Maven project configuration file.
├──load_config_and_compile.sh        # Script to load configuration and compile the project.
├──generate_keys.sh                  # Script to generate RSA keys for each server.
├──run_wsl.sh                        # Script to run the servers in separate Tmux windows.
└──cleanup.sh                        # Script to clean up the generated files and directories.
```
   