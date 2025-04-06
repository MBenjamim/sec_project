# SEC Project

This is a project for the Highly Dependable Systems course at Instituto Superior Técnico in Lisbon.
>This project aims to develop a simplified permissioned (closed membership) 
blockchain  system  with  high  dependability  guarantees,  called  Dependable 
Chain (DepChain). The system will be built iteratively throughout the first and 
second stages of the project: the first stage focuses on the consensus layer 
while the second stage will target the transaction processing layer.

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
# Must be at least 4 for fault tolerance, F < N/3
NUM_SERVERS=7

NUM_CLIENTS=5

# Base port where servers listen for communications from other servers
BASE_PORT_SERVER_TO_SERVER=5000

# Base port where servers listen for communications from clients
BASE_PORT_CLIENT_TO_SERVER=3000

# Base port where clients listen for communications from servers
BASE_PORT_CLIENTS=4000

LEADER_ID=0
```

## Running the Project

### Automatic Run
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
    To move between windows use `C-b n (Ctrl + B then N)` or [enable mouse](#enable-mouse-in-tmux). Learn how to use Tmux [here](https://hamvocke.com/blog/a-quick-and-easy-guide-to-tmux/)

```shell
./run_servers.sh
```
This script will start the first server in a new Tmux session and additional servers in separate Tmux windows.

4. **Run the Clients**\
   Run the run_servers.sh script to start the servers in separate Tmux windows:\
   To move between windows use `C-b n (Ctrl + B then N)` or [enable mouse](#enable-mouse-in-tmux). Learn how to use Tmux [here](https://hamvocke.com/blog/a-quick-and-easy-guide-to-tmux/)

```shell
./run_clients.sh
```

### Manual Run
1. **Load Configuration and Compile the Project**\
   Run the `check_config_and_compile.sh` script to check the configuration and compile the project using Maven:
```shell
./check_config_and_compile.sh
```

2. **Initialize System**\
   Run the `init_system.sh` script to generate RSA key pairs for each server and distribute the public keys, and initialize the Genesis block containing block zero:
```shell
./init_system.sh
```

3. **Run Each Server**\
   Run each server in a separate window. In each window, run a command like the following, changing the ID for each server (configuration file and log level can also be changed):
```shell
mvn exec:java -Dexec.mainClass=main.java.server.BlockchainNetworkServer -Dexec.args="0 config.cfg" -DLOG_LEVEL="info"
```

4. **Run Each Client**\
    Run each client in a separate window. In each window, run a command like the following, changing the ID for each client (configuration file and log level can also be changed):
```shell
mvn exec:java -Dexec.mainClass=main.java.client.BlockchainClient -Dexec.args="0 config.cfg" -DLOG_LEVEL="info"
```

5. **Run Server with Byzantine Behavior**\
    Use the following command to start a byzantine server, change `<behavior>` to one of the predefined behavior types:
```shell
mvn exec:java -Dexec.mainClass=main.java.server.BlockchainNetworkServer -Dexec.args="1 config.cfg <behavior>" -DLOG_LEVEL="info"
```

5. **Run Client with Byzantine Behavior**\
    Use the following command to start a byzantine client, change `<behavior>` to one of the predefined behavior types:
```shell
mvn exec:java -Dexec.mainClass=main.java.client.BlockchainClient -Dexec.args="1 config.cfg <behavior>" -DLOG_LEVEL="info"
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

- `test5`:  Test the functionality of the blockchain network with the maximum faulty processes with Byzantine behavior that fail to verify transactions when a client authorized to send a limited number of tokens on behalf of another process attempts a replay attack.

- `test6`: Test the blockchain network's functionality with `f+1` faulty processes exhibiting Byzantine behavior by sending corrupted `WRITE` consensus messages after 40 seconds of execution, at which point consensus is expected to stop functioning.

### Logs
The logs of the previous run are stored in the `logs` directory. The logs are named:
- `server_<server_id>.log`.
- `client_<client_id>.log`.
- `server_byzantine_<server_id>.log`.

## Enable Mouse in tmux
To enable mouse support in `tmux`, follow the steps below. Once enabled, you can interact with the terminal using the mouse for actions like resizing panes, scrolling through output, and switching between windows or panes.

### **Enable Mouse Temporarily**
To enable the mouse for the current `tmux` session, run the following command:

```bash
tmux setw -g mouse on
```

This will enable mouse support for the duration of the session. After you exit the session, mouse support will be disabled, and you'll need to enable it again if you start a new session.

### **Enable Mouse Permanently**
To enable mouse support automatically every time you start a new `tmux` session, you need to modify the `tmux` configuration file:

1. Open (or create) the `.tmux.conf` file in your home directory:

    ```bash
    nano ~/.tmux.conf
    ```

2. Add the following line to the file:

    ```bash
    set -g mouse on
    ```

3. Save the file and exit the editor.

To apply the changes immediately, reload the `tmux` configuration file with the following command:

```bash
tmux source-file ~/.tmux.conf
```

Now, mouse support will be enabled automatically every time you start a new tmux session.

### What You Can Do with the Mouse

Once mouse support is enabled in `tmux`, you can perform various actions using the mouse. Here are some of the things you can do:

1. **Switch Between Panes**\
You can click on any pane to focus on it. This makes it easier to interact with different parts of your `tmux` session without needing to use keyboard shortcuts to navigate between panes.

2. **Resize Panes**\
You can click and drag the borders between panes to resize them. This is especially useful when you want to adjust the layout of your `tmux` session to suit your needs.

3. **Scroll Through Output**\
You can use the mouse wheel to scroll through the output inside a pane. This allows you to easily review logs, previous commands, or any long outputs without needing to rely on `tmux`'s keyboard shortcuts for scrolling.

4. **Select and Switch Between Windows**\
You can click on the window list at the bottom of the `tmux` status bar to switch between different windows in the session. This provides a more intuitive way to navigate between different workspaces.

5. **Select and Copy Text**\
Once mouse support is enabled, you can click and drag to select text in any pane. After selecting text, you can copy it to your clipboard (depending on your terminal settings) for easy pasting elsewhere.
