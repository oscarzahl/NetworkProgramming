package no.ntnu.run;

import no.ntnu.tcp.GreenhouseNode;

public class NodeStarter {
    public static void main(String[] args) {
        String serverAddress = "localhost"; // IP-adresse til serveren
        int port = 12345; // Samme port som serveren

        // Start flere noder
        for (int i = 0; i < 3; i++) { // For eksempel 3 noder
            int nodeId = i + 1; // Gi hver node en unik ID
            new Thread(() -> {
                GreenhouseNode node = new GreenhouseNode(serverAddress, port);
                node.start();
            }, "Node-" + nodeId).start();
        }
    }
}
