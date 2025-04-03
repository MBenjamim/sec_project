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
        } else if (input.startsWith("total_supply")) {
            try {
                Transaction transaction = parseTotalSupplyCommand(input);

                // Create and send a message to each node with different IDs
                String messageContent = transaction.toJson();
                logger.debug("Sending TOTAL_SUPPLY transaction: \n {}", messageContent);
                networkNodes.values().forEach(node -> networkManager.sendMessageThread(new Message(transaction.getTransactionId(), MessageType.CLIENT_WRITE, id, messageContent), node));
                TransactionResponse transactionResponse = collector.waitForConfirmation();
                printTransactionResponse(transactionResponse);
            } catch (ParseException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else if (input.startsWith("allow")) {
            try {
                Transaction transaction = parseAllowCommand(input);

                // Create and send a message to each node with different IDs
                String messageContent = transaction.toJson();
                logger.debug("Sending ALLOW transaction: \n {}", messageContent);
                networkNodes.values().forEach(node -> networkManager.sendMessageThread(new Message(transaction.getTransactionId(), MessageType.CLIENT_WRITE, id, messageContent), node));
                TransactionResponse transactionResponse = collector.waitForConfirmation();
                printTransactionResponse(transactionResponse);
            } catch (ParseException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else if (input.startsWith("add_to_blacklist")) {
            try {
                Transaction transaction = parseAddToBlacklistCommand(input);

                // Create and send a message to each node with different IDs
                String messageContent = transaction.toJson();
                logger.debug("Sending ADD_TO_BLACKLIST transaction: \n {}", messageContent);
                networkNodes.values().forEach(node -> networkManager.sendMessageThread(new Message(transaction.getTransactionId(), MessageType.CLIENT_WRITE, id, messageContent), node));
                TransactionResponse transactionResponse = collector.waitForConfirmation();
                printTransactionResponse(transactionResponse);
            } catch (ParseException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else if (input.startsWith("is_blacklisted")) {
            try {
                Transaction transaction = parseIsBlacklistedCommand(input);

                // Create and send a message to each node with different IDs
                String messageContent = transaction.toJson();
                logger.debug("Sending IS_BLACKLISTED transaction: \n {}", messageContent);
                networkNodes.values().forEach(node -> networkManager.sendMessageThread(new Message(transaction.getTransactionId(), MessageType.CLIENT_WRITE, id, messageContent), node));
                TransactionResponse transactionResponse = collector.waitForConfirmation();
                printTransactionResponse(transactionResponse);
            } catch (ParseException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else if (input.startsWith("remove_from_blacklist")) {
            try {
                Transaction transaction = parseRemoveFromBlacklistCommand(input);

                // Create and send a message to each node with different IDs
                String messageContent = transaction.toJson();
                logger.debug("Sending REMOVE_FROM_BLACKLIST transaction: \n {}", messageContent);
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

    private Transaction parseTotalSupplyCommand(String input) throws ParseException {
        Address senderAddress = networkClients.get(this.id).getAddress();
        long transactionId = networkManager.generateMessageId(); // same as message ID
        String functionSignature = DataUtils.getFunctionSignature("totalSupply()");
        Transaction transaction = new Transaction(transactionId, senderAddress, null, functionSignature, null);

        // sign the transaction
        try {
            this.keyManager.signTransaction(transaction);
        } catch (Exception e) {
            logger.error("Failed to sign transaction for total supply", e);
        }

        return transaction;
    }

    private Transaction parseAllowCommand(String input) throws ParseException {
        String[] args = input.split("\\s+");

        Options options = new Options();
        options.addOption("ownerid", true, "Owner client ID");
        options.addOption("owner", true, "Owner address");
        options.addOption("spenderid", true, "Spender client ID");
        options.addOption("spender", true, "Spender address");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if ((!cmd.hasOption("ownerid") && !cmd.hasOption("owner")) || (!cmd.hasOption("spenderid") && !cmd.hasOption("spender"))) {
            throw new IllegalArgumentException("Invalid command format. Expected: allow -ownerid <clientID> or -owner <address> -spenderid <clientID> or -spender <address>");
        }

        Address ownerAddress = null;
        if (cmd.hasOption("ownerid")) {
            int ownerId;
            try {
                ownerId = Integer.parseInt(cmd.getOptionValue("ownerid"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Client ID must be an integer.");
            }
            if (!networkClients.containsKey(ownerId)) {
                throw new IllegalArgumentException("Client ID " + ownerId + " does not exist.");
            }
            ownerAddress = networkClients.get(ownerId).getAddress();
        } else if (cmd.hasOption("owner")) {
            try {
                ownerAddress = Address.fromHexString(cmd.getOptionValue("owner"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid address format.");
            }
        }

        Address spenderAddress = null;
        if (cmd.hasOption("spenderid")) {
            int spenderId;
            try {
                spenderId = Integer.parseInt(cmd.getOptionValue("spenderid"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Client ID must be an integer.");
            }
            if (!networkClients.containsKey(spenderId)) {
                throw new IllegalArgumentException("Client ID " + spenderId + " does not exist.");
            }
            spenderAddress = networkClients.get(spenderId).getAddress();
        } else if (cmd.hasOption("spender")) {
            try {
                spenderAddress = Address.fromHexString(cmd.getOptionValue("spender"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid address format.");
            }
        }

        Address senderAddress = networkClients.get(this.id).getAddress();

        long transactionId = networkManager.generateMessageId(); // same as message ID

        String functionSignature = DataUtils.getFunctionSignature("allowance(address,address)");

        Transaction transaction = new Transaction(transactionId, senderAddress, ownerAddress, spenderAddress, functionSignature, null);

        // sign the transaction
        try {
            this.keyManager.signTransaction(transaction);
        } catch (Exception e) {
            logger.error("Failed to sign transaction for allowance from {} to {}", ownerAddress, spenderAddress, e);
        }

        return transaction;
    }

    private Transaction parseAddToBlacklistCommand(String input) throws ParseException {
        if (this.id != 0) {
            throw new IllegalArgumentException("Only client0 can execute the add_to_blacklist command.");
        }

        String[] args = input.split("\\s+");

        Options options = new Options();
        options.addOption("id", true, "Client ID to add to blacklist");
        options.addOption("address", true, "Address to add to blacklist");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (!cmd.hasOption("id") && !cmd.hasOption("address")) {
            throw new IllegalArgumentException("Invalid command format. Expected: add_to_blacklist -id <clientID> or -address <address>");
        }

        Address addressToBlacklist = null;
        if (cmd.hasOption("id")) {
            int clientId;
            try {
                clientId = Integer.parseInt(cmd.getOptionValue("id"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Client ID must be an integer.");
            }
            if (!networkClients.containsKey(clientId)) {
                throw new IllegalArgumentException("Client ID " + clientId + " does not exist.");
            }
            addressToBlacklist = networkClients.get(clientId).getAddress();
        } else if (cmd.hasOption("address")) {
            try {
                addressToBlacklist = Address.fromHexString(cmd.getOptionValue("address"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid address format.");
            }
        }

        Address senderAddress = networkClients.get(this.id).getAddress();
        long transactionId = networkManager.generateMessageId();
        String functionSignature = DataUtils.getFunctionSignature("addToBlacklist(address)");

        Transaction transaction = new Transaction(transactionId, senderAddress, addressToBlacklist, functionSignature, null);

        // sign the transaction
        try {
            this.keyManager.signTransaction(transaction);
        } catch (Exception e) {
            logger.error("Failed to sign transaction to add {} to blacklist", addressToBlacklist, e);
        }

        return transaction;
    }

    private Transaction parseIsBlacklistedCommand(String input) throws ParseException {
        if (this.id != 0) {
            throw new IllegalArgumentException("Only client0 can execute the is_blacklisted command.");
        }

        String[] args = input.split("\\s+");

        Options options = new Options();
        options.addOption("id", true, "Client ID to check if blacklisted");
        options.addOption("address", true, "Address to check if blacklisted");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (!cmd.hasOption("id") && !cmd.hasOption("address")) {
            throw new IllegalArgumentException("Invalid command format. Expected: is_blacklisted -id <clientID> or -address <address>");
        }

        Address addressToCheck = null;
        if (cmd.hasOption("id")) {
            int clientId;
            try {
                clientId = Integer.parseInt(cmd.getOptionValue("id"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Client ID must be an integer.");
            }
            if (!networkClients.containsKey(clientId)) {
                throw new IllegalArgumentException("Client ID " + clientId + " does not exist.");
            }
            addressToCheck = networkClients.get(clientId).getAddress();
        } else if (cmd.hasOption("address")) {
            try {
                addressToCheck = Address.fromHexString(cmd.getOptionValue("address"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid address format.");
            }
        }

        Address senderAddress = networkClients.get(this.id).getAddress();
        long transactionId = networkManager.generateMessageId();
        String functionSignature = DataUtils.getFunctionSignature("isBlacklisted(address)");

        Transaction transaction = new Transaction(transactionId, senderAddress, addressToCheck, functionSignature, null);

        // sign the transaction
        try {
            this.keyManager.signTransaction(transaction);
        } catch (Exception e) {
            logger.error("Failed to sign transaction to check if {} is blacklisted", addressToCheck, e);
        }

        return transaction;
    }

    private Transaction parseRemoveFromBlacklistCommand(String input) throws ParseException {
        if (this.id != 0) {
            throw new IllegalArgumentException("Only client0 can execute the remove_from_blacklist command.");
        }

        String[] args = input.split("\\s+");

        Options options = new Options();
        options.addOption("id", true, "Client ID to remove from blacklist");
        options.addOption("address", true, "Address to remove from blacklist");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (!cmd.hasOption("id") && !cmd.hasOption("address")) {
            throw new IllegalArgumentException("Invalid command format. Expected: remove_from_blacklist -id <clientID> or -address <address>");
        }

        Address addressToRemove = null;
        if (cmd.hasOption("id")) {
            int clientId;
            try {
                clientId = Integer.parseInt(cmd.getOptionValue("id"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Client ID must be an integer.");
            }
            if (!networkClients.containsKey(clientId)) {
                throw new IllegalArgumentException("Client ID " + clientId + " does not exist.");
            }
            addressToRemove = networkClients.get(clientId).getAddress();
        } else if (cmd.hasOption("address")) {
            try {
                addressToRemove = Address.fromHexString(cmd.getOptionValue("address"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid address format.");
            }
        }

        Address senderAddress = networkClients.get(this.id).getAddress();
        long transactionId = networkManager.generateMessageId();
        String functionSignature = DataUtils.getFunctionSignature("removeFromBlacklist(address)");

        Transaction transaction = new Transaction(transactionId, senderAddress, addressToRemove, functionSignature, null);

        // sign the transaction
        try {
            this.keyManager.signTransaction(transaction);
        } catch (Exception e) {
            logger.error("Failed to sign transaction to remove {} from blacklist", addressToRemove, e);
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
        System.out.println("        1. To exit the application, type 'exit'.");
        System.out.println("        2. To get list of commands, enter the command:");
        System.out.println("           help");
        System.out.println("        3. To perform a transfer, enter the command:");
        System.out.println("           send -amount <value> [-toid <clientID>, -to <address>]");
        System.out.println("        4. To check balance, enter the command:");
        System.out.println("           balance [-ofid <clientID>, -of <address>]");
        System.out.println("        5. To approve a spender, enter the command:");
        System.out.println("           approve -amount <value> [-id <clientID>, -address <address>]");
        System.out.println("        6. To transfer from a client to another, enter the command:");
        System.out.println("           transfer_from -amount <value> [-fromid <clientID>, -from <address>] [-toid <clientID>, -to <address>]");
        System.out.println("        7. To get the total supply, enter the command:");
        System.out.println("           total_supply");
        System.out.println("        8. To check allowance, enter the command:");
        System.out.println("           allow [-ownerid <clientID>, -owner <address>] [-spenderid <clientID>, -spender <address>]");
        if (this.id == 0) {
            System.out.println("        9. To add to blacklist, enter the command:");
            System.out.println("           add_to_blacklist [-id <clientID>, -address <address>]");
            System.out.println("        10. To check if blacklisted, enter the command:");
            System.out.println("           is_blacklisted [-id <clientID>, -address <address>]");
            System.out.println("        11. To remove from blacklist, enter the command:");
            System.out.println("           remove_from_blacklist [-id <clientID>, -address <address>]");
        }
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
        System.out.println("  total_supply");
        System.out.println("  allow [-ownerid <clientID>, -owner <address>] [-spenderid <clientID>, -spender <address>]");
        if (this.id == 0) {
            System.out.println("  add_to_blacklist [-id <clientID>, -address <address>]");
            System.out.println("  is_blacklisted [-id <clientID>, -address <address>]");
            System.out.println("  remove_from_blacklist [-id <clientID>, -address <address>]");
        }
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
