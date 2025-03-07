package main.java.server;

import lombok.Getter;
import lombok.Setter;
import main.java.common.KeyManager;
import main.java.common.NetworkManager;
import main.java.common.Node;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ServerState {
    private final Map<Integer, Node> networkNodes = new HashMap<>();
    private final Map<Integer, Node> networkClients = new HashMap<>();

    private final int id;
    private final KeyManager keyManager;
    private NetworkManager networkManager;

    public ServerState(int serverId) {
        this.id = serverId;
        this.keyManager = new KeyManager(id, "server");
    }

    public void putServerNode(int nodeId, Node node) {
        networkNodes.put(nodeId, node);
    }

    public void putClientNode(int nodeId, Node node) {
        networkClients.put(nodeId, node);
    }
}
