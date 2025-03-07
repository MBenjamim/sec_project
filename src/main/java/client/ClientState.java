package main.java.client;

import lombok.Getter;
import lombok.Setter;
import main.java.common.KeyManager;
import main.java.common.NetworkManager;
import main.java.common.Node;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ClientState {
    private final Map<Integer, Node> networkNodes = new HashMap<>();

    private final int id;
    private final KeyManager keyManager;
    private NetworkManager networkManager;

    public ClientState(int clientId) {
        this.id = clientId;
        this.keyManager = new KeyManager(id, "client");
    }

    public void putServerNode(int nodeId, Node node) {
        networkNodes.put(nodeId, node);
    }
}
