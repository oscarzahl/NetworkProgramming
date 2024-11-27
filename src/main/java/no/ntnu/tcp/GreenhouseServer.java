package no.ntnu.tcp;

import java.io.*;
import java.net.*;
import java.util.*;

import no.ntnu.controlpanel.ControlPanelLogic;
import no.ntnu.greenhouse.SensorReading;

public class GreenhouseServer {
    private final int port;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final ControlPanelLogic controlPanelLogic;

    public GreenhouseServer(int port) {
        this.port = port;
        this.controlPanelLogic = new ControlPanelLogic();
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

    private List<SensorReading> parseSensorData(String data) {
        // Parse sensor data string (e.g., "temperature=22.5,humidity=50%")
        // Return as a list of SensorReading
        // Example implementation needed
        return new ArrayList<>();
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
        if (message.startsWith("SENSOR")) {
            String[] parts = message.split(":");
            int nodeId = Integer.parseInt(parts[1]);
            String sensorData = parts[2];
            server.handleSensorData(nodeId, sensorData);
        }
    }
    

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}
