package no.ntnu.tcp;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * GreenhouseServer is responsible for managing client connections, broadcasting
 * messages, and handling sensor and actuator data in a greenhouse simulation.
 */
public class GreenhouseServer {
    private final int port; // Port number for the server to listen on
    private final List<ClientHandler> clients = new ArrayList<>(); // List of connected clients

    /**
     * Constructs a GreenhouseServer with the specified port.
     *
     * @param port the port number the server will listen on
     */
    public GreenhouseServer(int port) {
        this.port = port;
    }

    /**
     * Starts the server to accept client connections and handle communication.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("GreenhouseServer is listening on port " + port);

            while (true) {
                // Accept incoming client connections
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());

                // Create a handler for the connected client
                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);

                // Start a new thread to handle client communication
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a message to all connected clients.
     *
     * @param message the message to broadcast
     */
    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    /**
     * Removes a client from the list of connected clients.
     *
     * @param clientHandler the client handler to remove
     */
    public synchronized void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    /**
     * Handles incoming sensor data from a client.
     *
     * @param nodeId     the ID of the node sending the data
     * @param sensorData the sensor data sent by the client
     */
    public synchronized void handleSensorData(int nodeId, String sensorData) {
        String formattedMessage = "SENSOR:" + nodeId + ":" + sensorData;
        broadcast(formattedMessage); // Broadcast sensor data to all clients
    }

    /**
     * Handles incoming actuator data from a client.
     *
     * @param nodeId       the ID of the node sending the data
     * @param actuatorData the actuator data sent by the client
     */
    public synchronized void handleActuatorData(int nodeId, String actuatorData) {
        String formattedMessage = "ACTUATOR:" + nodeId + ":" + actuatorData;
        broadcast(formattedMessage); // Broadcast actuator data to all clients
    }
}

/**
 * ClientHandler manages communication between the server and a connected
 * client.
 */
class ClientHandler implements Runnable {
    private final Socket socket; // Socket representing the client's connection
    private final GreenhouseServer server; // Reference to the server
    private PrintWriter out; // Output stream to send messages to the client

    /**
     * Constructs a ClientHandler for a connected client.
     *
     * @param socket the client's socket
     * @param server the server instance managing the connection
     */
    public ClientHandler(Socket socket, GreenhouseServer server) {
        this.socket = socket;
        this.server = server;
    }

    /**
     * Listens for and processes messages from the client.
     */
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);
                handleMessage(message); // Process incoming messages
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Remove client from the server and close the socket
            server.removeClient(this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Processes messages from the client based on their type.
     *
     * @param message the message received from the client
     */
    private void handleMessage(String message) {
        if (message.startsWith("SENSOR:")) {
            handleSensorMessage(message);
        } else if (message.startsWith("ACTUATOR:")) {
            handleActuatorMessage(message);
        } else {
            System.out.println("Unknown message type: " + message);
        }
    }

    /**
     * Parses and handles a SENSOR message.
     *
     * @param message the SENSOR message
     */
    private void handleSensorMessage(String message) {
        try {
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                int nodeId = Integer.parseInt(parts[1].trim());
                String sensorData = parts[2].trim();
                server.handleSensorData(nodeId, sensorData);
            } else {
                System.out.println("Invalid SENSOR message format: " + message);
            }
        } catch (Exception e) {
            System.out.println("Error processing SENSOR message: " + e.getMessage());
        }
    }

    /**
     * Parses and handles an ACTUATOR message.
     *
     * @param message the ACTUATOR message
     */
    private void handleActuatorMessage(String message) {
        try {
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                int nodeId = Integer.parseInt(parts[1].trim());
                String actuatorData = parts[2].trim();
                server.handleActuatorData(nodeId, actuatorData);
            } else {
                System.out.println("Invalid ACTUATOR message format: " + message);
            }
        } catch (Exception e) {
            System.out.println("Error processing ACTUATOR message: " + e.getMessage());
        }
    }

    /**
     * Sends a message to the connected client.
     *
     * @param message the message to send
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}
