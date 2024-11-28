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
        List<SensorReading> readings = parseSensorData(sensorData);
        String formattedMessage = "SENSOR:" + nodeId + ":" + sensorData;
        broadcast(formattedMessage); // Send til alle klienter, inkludert kontrollpanelet
    }

    private List<SensorReading> parseSensorData(String data) {
        List<SensorReading> readings = new ArrayList<>();
        String[] sensors = data.split(",");
        for (String sensor : sensors) {
            String[] parts = sensor.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid sensor data format: " + sensor);
            }
            String type = parts[0].trim();

            // Handle values like "24.98°C" by separating the number and the unit
            String valueAndUnit = parts[1].trim();
            int unitStartIndex = findUnitStartIndex(valueAndUnit);
            if (unitStartIndex == -1) {
                throw new IllegalArgumentException("Invalid sensor value/unit format: " + valueAndUnit);
            }

            double value = Double.parseDouble(valueAndUnit.substring(0, unitStartIndex).trim());
            String unit = valueAndUnit.substring(unitStartIndex).trim();
            readings.add(new SensorReading(type, value, unit));
        }
        return readings;
    }

    // Helper method to find where the unit starts in a value string
    private int findUnitStartIndex(String valueAndUnit) {
        for (int i = 0; i < valueAndUnit.length(); i++) {
            if (!Character.isDigit(valueAndUnit.charAt(i)) && valueAndUnit.charAt(i) != '.') {
                return i;
            }
        }
        return -1;
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
