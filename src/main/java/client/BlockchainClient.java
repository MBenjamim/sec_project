package main.java.client;

import lombok.Getter;
import main.java.blockchain.Transaction;
import main.java.blockchain.TransactionResponse;
import main.java.blockchain.TransactionType;
import main.java.common.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import main.java.utils.DataUtils;
import org.apache.commons.cli.*;
import org.hyperledger.besu.datatypes.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a client in the blockchain network.
 * Listens from command line and send messages to the blockchain.
 */
@Getter
public class BlockchainClient {
    private static final Logger logger = LoggerFactory.getLogger(BlockchainClient.class);

    private final Map<Integer, NodeRegistry> networkNodes = new HashMap<>();
    private final Map<Integer, NodeRegistry> networkClients = new HashMap<>();

    private final int id;
    private int port;
    private int timeout;

    private final KeyManager keyManager;
    private NetworkManager networkManager;
    private BlockchainConfirmationCollector collector;

    /**
     * Constructor for the BlockchainClient class.
     *
     * @param clientId the unique identifier for the client
     */
    public BlockchainClient(int clientId) {
        this.id = clientId;
        this.keyManager = new KeyManager(id, "client");
    }

    /**
     * The main method to start the client.
     *
     * @param args command line arguments (serverId and serverPort)
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            logger.error("Usage: java BlockchainClient <clientId> <configFile>");
            System.exit(1);
        }

        int clientId = Integer.parseInt(args[0]);
        String configFile = args[1];

        ConfigLoader.getProcessId();

        BlockchainClient client = new BlockchainClient(clientId);
        client.loadConfig(configFile);
        client.networkManager = new NetworkManager(client.id, client.keyManager, client.timeout);
        client.collector = new BlockchainConfirmationCollector(client.networkNodes.size());
        client.start();
    }

    /**
     * Starts the client to listen for command line input and connections from blockchain members.
     */
    public void start() {
        ServerMessageHandler serverMessageHandler = new ServerMessageHandler(this);
        logger.info("Connecting to the network...");
        networkManager.startClientCommunications(port, serverMessageHandler, networkNodes.values());

        Scanner scanner = new Scanner(System.in);

        printWelcomeMessage();

        while (true) {
            try {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("exit")) {
                    logger.debug("Exiting...");
                    System.exit(0);
                    break;
                }
                // Process the received string
                processInput(input);
            } catch (NoSuchElementException e) {
                // This exception is thrown when testing because there is no terminal
            } catch (Exception e) {
                logger.error("Error reading input: {}", e.getMessage());
            }
        }
    }

    private void processInput(String input) {
        if (input == null || input.isBlank()) return;
        logger.debug("Received input: {}", input);

        if (input.startsWith("send")) {
            try {
                Transaction transaction = parseSendCommand(input);

                // Create and send a message to each node with different IDs
                String messageContent = transaction.toJson();
                logger.debug("Sending transaction: \n {}", messageContent);
                networkNodes.values().forEach(node -> networkManager.sendMessageThread(new Message(transaction.getTransactionId(), MessageType.CLIENT_WRITE, id, messageContent), node));
                TransactionResponse transactionResponse = collector.waitForConfirmation();
                printTransactionResponse(transactionResponse);
            } catch (ParseException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else if (input.startsWith("balance")) {
            try {
                Transaction transaction = parseBalanceCommand(input);

                // Create and send a message to each node with different IDs
                String messageContent = transaction.toJson();
                logger.debug("Sending BALANCE_OF transaction: \n {}", messageContent);
                networkNodes.values().forEach(node -> networkManager.sendMessageThread(new Message(transaction.getTransactionId(), MessageType.CLIENT_WRITE, id, messageContent), node));
                TransactionResponse transactionResponse = collector.waitForConfirmation();
                printTransactionResponse(transactionResponse);
            } catch (ParseException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else if (input.startsWith("approve")) {
            try {
                Transaction transaction = parseApproveCommand(input);

                // Create and send a message to each node with different IDs
                String messageContent = transaction.toJson();
                logger.debug("Sending APPROVE transaction: \n {}", messageContent);
                networkNodes.values().forEach(node -> networkManager.sendMessageThread(new Message(transaction.getTransactionId(), MessageType.CLIENT_WRITE, id, messageContent), node));
                TransactionResponse transactionResponse = collector.waitForConfirmation();
                printTransactionResponse(transactionResponse);
            } catch (ParseException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else if (input.startsWith("transfer_from")) {
            try {
                Transaction transaction = parseTransferFromCommand(input);

                // Create and send a message to each node with different IDs
                String messageContent = transaction.toJson();
                logger.debug("Sending TRANSFER_FROM transaction: \n {}", messageContent);
                networkNodes.values().forEach(node -> networkManager.sendMessageThread(new Message(transaction.getTransactionId(), MessageType.CLIENT_WRITE, id, messageContent), node));
                TransactionResponse transactionResponse = collector.waitForConfirmation();
                printTransactionResponse(transactionResponse);
            } catch (ParseException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else if (input.startsWith("help")) {
            printHelp();
        } else {
            System.out.println("Error: Unknown command '" + input + "'.");
        }
    }

    // TODO - implement more functions
    private Transaction parseSendCommand(String input) throws ParseException {
        String[] args = input.split("\\s+");

        Options options = new Options();
        options.addOption("amount", true, "Amount to send");
        options.addOption("toid", true, "Receiver client ID");
        options.addOption("to", true, "Receiver address");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (!cmd.hasOption("amount") || (!cmd.hasOption("toid") && !cmd.hasOption("to"))) {
            throw new IllegalArgumentException("Invalid command format. Expected: send -amount <value> -toid <clientID> or -to <address>");
        }

        double amount;
        try {
            amount = Double.parseDouble(cmd.getOptionValue("amount"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a number.");
        }

        Address receiverAddress = null;
        if (cmd.hasOption("toid")) {
            int receiverId;
            try {
                receiverId = Integer.parseInt(cmd.getOptionValue("toid"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Client ID must be an integer.");
            }
            if (!networkClients.containsKey(receiverId)) {
                throw new IllegalArgumentException("Client ID " + receiverId + " does not exist.");
            }
            receiverAddress = networkClients.get(receiverId).getAddress();
        } else if (cmd.hasOption("to")) {
            try {
                receiverAddress = Address.fromHexString(cmd.getOptionValue("to"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid address format.");
            }
        }

        Address senderAddress = networkClients.get(this.id).getAddress();

        long transactionId = networkManager.generateMessageId(); // same as message ID

        String functionSignature = DataUtils.getFunctionSignature("transfer(address,uint256)");

        Transaction transaction =  new Transaction(transactionId, senderAddress, receiverAddress, functionSignature, amount);

        // sign the transaction
        try {
            this.keyManager.signTransaction(transaction);
        } catch (Exception e) {
            logger.error("Failed to sign transaction to {}", receiverAddress, e);
        }

        return transaction;
    }

    private Transaction parseBalanceCommand(String input) throws ParseException {
        String[] args = input.split("\\s+");

        Options options = new Options();
        options.addOption("of", true, "Address to check balance");
        options.addOption("ofid", true, "Client ID to check balance");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (!cmd.hasOption("of") && !cmd.hasOption("ofid")) {
            throw new IllegalArgumentException("Invalid command format. Expected: balance -ofid <clientID> or -of <address>");
        }

        Address address = null;
        if (cmd.hasOption("ofid")) {
            int clientId;
            try {
                clientId = Integer.parseInt(cmd.getOptionValue("ofid"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Client ID must be an integer.");
            }
            if (!networkClients.containsKey(clientId)) {
                throw new IllegalArgumentException("Client ID " + clientId + " does not exist.");
            }
            address = networkClients.get(clientId).getAddress();
        } else if (cmd.hasOption("of")) {
            try {
                address = Address.fromHexString(cmd.getOptionValue("of"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid address format.");
            }
        }

        Address senderAddress = networkClients.get(this.id).getAddress();

        long transactionId = networkManager.generateMessageId(); // same as message ID

        String functionSignature = DataUtils.getFunctionSignature("balanceOf(address)");

        Transaction transaction = new Transaction(transactionId, senderAddress, address, functionSignature, null);

        // sign the transaction
        try {
            this.keyManager.signTransaction(transaction);
        } catch (Exception e) {
            logger.error("Failed to sign transaction to {}", address, e);
        }

        return transaction;
    }

    private Transaction parseApproveCommand(String input) throws ParseException {
        String[] args = input.split("\\s+");

        Options options = new Options();
        options.addOption("amount", true, "Amount to approve");
        options.addOption("id", true, "Spender client ID");
        options.addOption("address", true, "Spender address");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (!cmd.hasOption("amount") || (!cmd.hasOption("id") && !cmd.hasOption("address"))) {
            throw new IllegalArgumentException("Invalid command format. Expected: approve -amount <value> -id <clientID> or -address <address>");
        }

        double amount;
        try {
            amount = Double.parseDouble(cmd.getOptionValue("amount"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a number.");
        }

        Address spenderAddress = null;
        if (cmd.hasOption("id")) {
            int spenderId;
            try {
                spenderId = Integer.parseInt(cmd.getOptionValue("id"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Client ID must be an integer.");
            }
            if (!networkClients.containsKey(spenderId)) {
                throw new IllegalArgumentException("Client ID " + spenderId + " does not exist.");
            }
            spenderAddress = networkClients.get(spenderId).getAddress();
        } else if (cmd.hasOption("address")) {
            try {
                spenderAddress = Address.fromHexString(cmd.getOptionValue("address"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid address format.");
            }
        }

        Address senderAddress = networkClients.get(this.id).getAddress();

        long transactionId = networkManager.generateMessageId(); // same as message ID

        String functionSignature = DataUtils.getFunctionSignature("approve(address,uint256)");

        Transaction transaction = new Transaction(transactionId, senderAddress, spenderAddress, functionSignature, amount);

        // sign the transaction
        try {
            this.keyManager.signTransaction(transaction);
        } catch (Exception e) {
            logger.error("Failed to sign transaction to {}", spenderAddress, e);
        }

        return transaction;
    }

    private Transaction parseTransferFromCommand(String input) throws ParseException {
        String[] args = input.split("\\s+");

        Options options = new Options();
        options.addOption("amount", true, "Amount to transfer");
        options.addOption("fromid", true, "Sender client ID");
        options.addOption("from", true, "Sender address");
        options.addOption("toid", true, "Receiver client ID");
        options.addOption("to", true, "Receiver address");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (!cmd.hasOption("amount") || (!cmd.hasOption("fromid") && !cmd.hasOption("from")) || (!cmd.hasOption("toid") && !cmd.hasOption("to"))) {
            throw new IllegalArgumentException("Invalid command format. Expected: transfer_from -amount <value> -fromid <clientID> or -from <address> -toid <clientID> or -to <address>");
        }

        double amount;
        try {
            amount = Double.parseDouble(cmd.getOptionValue("amount"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a number.");
        }

        Address fromAddress = null;
        if (cmd.hasOption("fromid")) {
            int fromId;
            try {
                fromId = Integer.parseInt(cmd.getOptionValue("fromid"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Client ID must be an integer.");
            }
            if (!networkClients.containsKey(fromId)) {
                throw new IllegalArgumentException("Client ID " + fromId + " does not exist.");
            }
            fromAddress = networkClients.get(fromId).getAddress();
        } else if (cmd.hasOption("from")) {
            try {
                fromAddress = Address.fromHexString(cmd.getOptionValue("from"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid address format.");
            }
        }

        Address toAddress = null;
        if (cmd.hasOption("toid")) {
            int toId;
            try {
                toId = Integer.parseInt(cmd.getOptionValue("toid"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Client ID must be an integer.");
            }
            if (!networkClients.containsKey(toId)) {
                throw new IllegalArgumentException("Client ID " + toId + " does not exist.");
            }
            toAddress = networkClients.get(toId).getAddress();
        } else if (cmd.hasOption("to")) {
            try {
                toAddress = Address.fromHexString(cmd.getOptionValue("to"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid address format.");
            }
        }

        Address senderAddress = networkClients.get(this.id).getAddress();

        long transactionId = networkManager.generateMessageId(); // same as message ID

        String functionSignature = DataUtils.getFunctionSignature("transferFrom(address,address,uint256)");

        Transaction transaction = new Transaction(transactionId, senderAddress, fromAddress, toAddress, functionSignature, amount);

        // sign the transaction
        try {
            this.keyManager.signTransaction(transaction);
        } catch (Exception e) {
            logger.error("Failed to sign transaction from {} to {}", fromAddress, toAddress, e);
        }

        return transaction;
    }


    /**
     * Loads the server nodes from the configuration file.
     * Creates the NetworkManager.
     */
    public void loadConfig(String configFile) {
        ConfigLoader config = new ConfigLoader(configFile);

        int numServers = config.getIntProperty("NUM_SERVERS");
        int numClients = config.getIntProperty("NUM_CLIENTS");
        int basePort = config.getIntProperty("BASE_PORT_CLIENT_TO_SERVER");
        int basePortClients = config.getIntProperty("BASE_PORT_CLIENTS");

        this.port = config.getIntProperty("BASE_PORT_CLIENTS") + id;
        this.timeout = config.getIntProperty("TIMEOUT");

        for (int i = 0; i < numServers; i++) {
            int port = basePort + i;
            networkNodes.put(i, new NodeRegistry(i, "server", "localhost", port));
        }

        for (int i = 0; i < numClients; i++) {
            int port = basePortClients + i;
            networkClients.put(i, new NodeRegistry(i, "client", "localhost", port));
        }

        logger.debug("[CONFIG] Loaded nodes and clients from config:");
        networkNodes.values().forEach(node -> logger.debug("[CONFIG] {}{}: {}:{}", node.getType(), node.getId(), node.getIp(), node.getPort()));
        networkClients.values().forEach(node -> logger.debug("[CONFIG] client{}: {}:{}", node.getId(), node.getIp(), node.getPort()));
    }

    /**
     * Prints a welcome message and instructions for using the DepChain app.
     */
    private void printWelcomeMessage() {
        System.out.println("=============================================================");
        System.out.println("               Welcome to the DepChain Blockchain            ");
        System.out.println("                    Client ID: " + this.id);
        System.out.println("=============================================================");
        System.out.println("██████╗ ███████╗██████╗  ██████╗██╗  ██╗ █████╗ ██╗███╗   ██╗");
        System.out.println("██╔══██╗██╔════╝██╔══██╗██╔════╝██║  ██║██╔══██╗██║████╗  ██║");
        System.out.println("██║  ██║█████╗  ██████╔╝██║     ███████║███████║██║██╔██╗ ██║");
        System.out.println("██║  ██║██╔══╝  ██╔═══╝ ██║     ██╔══██║██╔══██║██║██║╚██╗██║");
        System.out.println("██████╔╝███████╗██║     ╚██████╗██║  ██║██║  ██║██║██║ ╚████║");
        System.out.println("╚═════╝ ╚══════╝╚═╝      ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝");
        System.out.println("=============================================================");
        System.out.println("  Your address is " + networkClients.get(this.id).getAddress().toHexString());
        System.out.println("=============================================================");
        System.out.println("        Instructions:");
        System.out.println("        1. To get list of commands, enter the command:");
        System.out.println("           help");
        System.out.println("        2. To perform a transfer, enter the command:");
        System.out.println("           send -amount <value> [-toid <clientID>, -to <address>]");
        System.out.println("        3. To check balance, enter the command:");
        System.out.println("           balance [-ofid <clientID>, -of <address>]");
        System.out.println("        4. To approve a spender, enter the command:");
        System.out.println("           approve -amount <value> [-id <clientID>, -address <address>]");
        System.out.println("        5. To transfer from a client to another, enter the command:");
        System.out.println("           transfer_from -amount <value> [-fromid <clientID>, -from <address>] [-toid <clientID>, -to <address>]");
        System.out.println("        6. To exit the application, type 'exit'.");
        System.out.println("=============================================================");
        System.out.println("        Available Clients:");
        networkClients.keySet().forEach(clientId -> System.out.println("        - Client ID: " + clientId));
        System.out.println("=============================================================");
    }

    private void printHelp() {
        System.out.println("Usage:");
        System.out.println("  send -amount <value> [-toid <clientID>, -to <address>]");
        System.out.println("  balance [-ofid <clientID>, -of <address>]");
        System.out.println("  approve -amount <value> [-id <clientID>, -address <address>]");
        System.out.println("  transfer_from -amount <value> [-fromid <clientID>, -from <address>] [-toid <clientID>, -to <address>]");
        System.out.println("  help");
    }

    private void printTransactionResponse(TransactionResponse response) {
        System.out.println("=============================================================");
        System.out.println("                   Transaction Response                      ");
        System.out.println("=============================================================");
        System.out.println("Transaction ID: " + response.getTransactionId());
        System.out.println("Block Hash: " + response.getBlockHash());
        System.out.println("Status: " + (response.isStatus() ? "Success" : "Failure"));
        System.out.println("Description: " + response.getDescription());
        System.out.println("Transaction Type: " + response.getTransactionType());
        System.out.println("Return Type: " + response.getReturnType());
        if (response.getResult() != null) {
            if (response.getTransactionType().equals(TransactionType.BALANCE_OF)) {
                System.out.println("Result: " + String.format("%.2f", DataUtils.convertAmountToDouble(response.getResult())));
            } else {
                System.out.println("Result: " + response.getResult());
            }
        }
        System.out.println("=============================================================");
    }
}
