package no.ntnu.tcp;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import no.ntnu.greenhouse.Sensor;

public class GreenhouseNode {
    private final int nodeId;
    private final String serverAddress;
    private final int port;
    private final List<Sensor> sensors = new ArrayList<>();

    public GreenhouseNode(int nodeId, String serverAddress, int port) {
        this.nodeId = nodeId;
        this.serverAddress = serverAddress;
        this.port = port;
        initializeSensors();
    }

    private void initializeSensors() {
        sensors.add(new Sensor("temperature", 15, 35, 25, "°C"));
        sensors.add(new Sensor("humidity", 30, 80, 50, "%"));
    }

    public void start() {
        try (Socket socket = new Socket(serverAddress, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to GreenhouseServer");

            new Thread(() -> {
                while (true) {
                    try {
                        String sensorData = generateSensorData();
                        out.println("SENSOR:" + nodeId + ":" + sensorData);
                        System.out.println("Sent: " + sensorData);
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            String response;
            while ((response = in.readLine()) != null) {
                handleServerMessage(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateSensorData() {
        StringBuilder builder = new StringBuilder();
        for (Sensor sensor : sensors) {
            sensor.addRandomNoise();
            builder.append(sensor.getType())
                   .append("=")
                   .append(sensor.getReading().getValue())
                   .append(sensor.getReading().getUnit())
                   .append(",");
        }
        return builder.substring(0, builder.length() - 1); // Fjern siste komma
    }

    private void handleServerMessage(String message) {
        // Håndter kommandoer som påvirker aktuatorer
        System.out.println("Handling server command: " + message);
    }
}

