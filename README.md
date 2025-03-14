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

- [Java](https://www.java.com) 17 or higher
- [Maven](https://maven.apache.org)
- [Tmux](https://github.com/tmux/tmux/wiki) (for running multiple servers in separate windows)

## Configuration

The configuration for the number of servers and the base port is specified in the `config.cfg` file:

```properties
# for having a faulty process this must be at least 4 (F < N/3)
NUM_SERVERS=4

NUM_CLIENTS=3

BASE_PORT_SERVER_TO_SERVER=5000

BASE_PORT_CLIENT_TO_SERVER=3000

BASE_PORT_CLIENTS=4000

LEADER_ID=0

# number of relays (1 relay = 200ms) total timeout = 3s
TIMEOUT=5
```

## Running the Project
1. **Load Configuration and Compile the Project**\
   Run the `check_config_and_compile.sh` script to check the configuration and compile the project using Maven:
```shell
./check_config_and_compile.sh
```

2. **Generate RSA Keys**\
   Run the `generate_keys.sh` script to generate RSA key pairs for each server and distribute the public keys:
```shell
./generate_keys.sh
```

3. **Run the Servers**\
    Run the run_servers.sh script to start the servers in separate Tmux windows:\
    To move between windows use `C-b n (Ctrl + B then N)` learn how to use Tmux [here](https://hamvocke.com/blog/a-quick-and-easy-guide-to-tmux/)

```shell
./run_servers.sh
```
This script will start the first server in a new Tmux session and additional servers in separate Tmux windows.

4. **Run the Clients**\
   Run the run_servers.sh script to start the servers in separate Tmux windows:\
   To move between windows use `C-b n (Ctrl + B then N)` learn how to use Tmux [here](https://hamvocke.com/blog/a-quick-and-easy-guide-to-tmux/)

```shell
./run_clients.sh
#write the string to put in the blockchain
```

## Cleanup
To clean up the generated files and directories, run the `cleanup.sh` script:
```shell
./cleanup.sh
```

## Testing
### RUN
To test the project, run the `test_all.sh` script:
```shell
./test_all.sh
```
Note: The waiting time per test may need to be adjusted depending on the number of processes, network latency and computation power.
      you can change the waiting time in the `tests_sh/test<n>/test<n>_config.cfg` config file using `SLEEP_TIME`.
### Description
The tests are in the `tests_sh directory`. Here you will find the following tests:

- `test1`: Test the functionality of the blockchain network with the maximum faulty processes with Byzantine behavior of not responding to other servers messages.

- `test2`: Test the functionality of the blockchain network with the maximum faulty processes with Byzantine behavior of not responding to the leader.

- `test3`: Test the functionality of the blockchain network with the maximum faulty processes with Byzantine behavior of sending corrupted `WRITE` consensus messages.

- `test4`: Test the functionality of the blockchain network with the maximum faulty processes with Byzantine behavior of sending corrupted `STATE` consensus messages in response to the leader `READ` message.

### Logs
The logs of the previous run are stored in the `logs` directory. The logs are named:
- `server_<server_id>.log`.
- `client_<client_id>.log`.
- `server_byzantine_<server_id>.log`.
   