package no.ntnu.run;

import no.ntnu.tcp.GreenhouseServer;

/**
 * Entry point to start the GreenhouseServer.
 */
public class ServerStarter {

    /**
     * Main method to initialize and start the GreenhouseServer.
     *
     * @param args Command-line arguments (not used in this implementation)
     */
    public static void main(String[] args) {
        int port = 12345; // Port number for the server to listen on

        // Create an instance of GreenhouseServer
        GreenhouseServer server = new GreenhouseServer(port);

        // Start the server to accept client connections and handle communication
        server.start();
    }
}
