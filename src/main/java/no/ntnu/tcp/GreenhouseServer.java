package no.ntnu.tcp;

import java.io.*;
import java.net.*;
import java.util.*;

public class GreenhouseServer {
    private final int port;
    private final List<ClientHandler> clients = new ArrayList<>();

    public GreenhouseServer(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("GreenhouseServer is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);

                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public synchronized void handleSensorData(int nodeId, String sensorData) {
        String formattedMessage = "SENSOR:" + nodeId + ":" + sensorData;
        broadcast(formattedMessage); // Send til alle klienter, inkludert kontrollpanelet
    }

    public synchronized void handleActuatorData(int nodeId, String actuatorData) {
        String formattedMessage = "ACTUATOR:" + nodeId + ":" + actuatorData;
        broadcast(formattedMessage);
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    private final GreenhouseServer server;
    private PrintWriter out;

    public ClientHandler(Socket socket, GreenhouseServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);
                handleMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.removeClient(this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessage(String message) {
        // if (message.startsWith("SENSOR")) {
        // String[] parts = message.split(":");
        // if(parts.length >= 3){
        // int nodeId = Integer.parseInt(parts[1]);
        // String sensorData = parts[2];
        // server.handleSensorData(nodeId, sensorData);
        // }
        // }

        if (message.startsWith("SENSOR:")) {
            try {
                // Parse SENSOR message
                String[] parts = message.split(":", 3); // Split into SENSOR, nodeId, and sensorData
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
        } else if (message.startsWith("ACTUATOR:")) {
            try {
                // Parse ACTUATOR message
                String[] parts = message.split(":", 3); // Split into ACTUATOR, nodeId, and actuatorData
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
        } else {
            System.out.println("Unknown message type: " + message);
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}
