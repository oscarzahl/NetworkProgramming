package no.ntnu.tcp;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.List;

import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.ActuatorCollection;
import no.ntnu.greenhouse.Sensor;

/**
 * GreenhouseNode represents a node in a greenhouse system.
 * It communicates with a server to send sensor data and receive actuator
 * commands.
 */
public class GreenhouseNode {
    private final int nodeId;
    private final String serverAddress;
    private final int port;
    private final List<Sensor> sensors;
    private final ActuatorCollection actuators;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    /**
     * Constructs a GreenhouseNode with the specified parameters.
     *
     * @param nodeId        the unique ID of the node
     * @param serverAddress the address of the server
     * @param port          the port to connect to
     * @param sensors       the list of sensors associated with the node
     * @param actuators     the actuators associated with the node
     */
    public GreenhouseNode(int nodeId, String serverAddress, int port, List<Sensor> sensors,
            ActuatorCollection actuators) {
        this.nodeId = nodeId;
        this.serverAddress = serverAddress;
        this.port = port;
        this.sensors = sensors;
        this.actuators = actuators;
    }

    /**
     * Starts the node by connecting to the server and managing communication.
     */
    public void start() {
        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connected to GreenhouseServer");

            new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        String sensorData = generateSensorData();
                        out.println("SENSOR:" + nodeId + ":" + sensorData);
                        System.out.println("Sent: " + sensorData);

                        String actuatorData = generateActuatorData();
                        if (actuatorData != null) {
                            out.println("ACTUATOR:" + nodeId + ":" + actuatorData);
                            System.out.println("Sent: " + actuatorData);
                        } else {
                            System.out.println("No actuator data to send for node " + nodeId);
                        }

                        Thread.sleep(5000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    if (e instanceof IOException && "Socket closed".equals(e.getMessage())) {
                        System.out.println("Socket closed, stopping sensor data thread.");
                    } else {
                        e.printStackTrace();
                    }
                }
            }).start();

            String response;
            while ((response = in.readLine()) != null) {
                handleServerMessage(response);
            }
        } catch (IOException e) {
            if ("Socket closed".equals(e.getMessage())) {
                System.out.println("Socket closed, stopping server response thread.");
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops the node by closing the socket and streams.
     */
    public void stop() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates a formatted string representing the state of all actuators.
     *
     * @return a formatted string of actuator data, or null if no actuators exist
     */
    private String generateActuatorData() {
        StringBuilder builder = new StringBuilder();
        Iterator<Actuator> iterator = actuators.iterator();

        while (iterator.hasNext()) {
            Actuator actuator = iterator.next();
            builder.append(actuator.getId())
                    .append(":")
                    .append(actuator.getType())
                    .append("=")
                    .append(actuator.isOn())
                    .append(",");
        }

        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        } else {
            return null;
        }

        return builder.toString();
    }

    /**
     * Generates a formatted string representing the state of all sensors.
     *
     * @return a formatted string of sensor data
     */
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
        return builder.substring(0, builder.length() - 1);
    }

    /**
     * Handles incoming messages from the server.
     *
     * @param message the message received from the server
     */
    private void handleServerMessage(String message) {
        if (message.startsWith("ACTUATOR:")) {
            handleActuatorStateChange(message);
        }
    }

    /**
     * Processes actuator state change messages from the server.
     *
     * @param message the message containing actuator state change information
     */
    private void handleActuatorStateChange(String message) {
        String[] parts = message.split(":");

        // Validate the base format
        if (parts.length != 4 || !"ACTUATOR".equals(parts[0])) {
            return;
        }

        try {
            int actuatorId = Integer.parseInt(parts[2]);

            // Validate the state
            String statePart = parts[3];
            boolean state;
            if ("true".equalsIgnoreCase(statePart) || "false".equalsIgnoreCase(statePart)) {
                state = Boolean.parseBoolean(statePart);
            } else {
                return;
            }

            // Find the actuator
            Actuator actuator = actuators.get(actuatorId);
            if (actuator != null) {
                // Update the actuator state
                actuator.set(state);
            } else {
                System.out.println("Actuator not found: " + actuatorId);
            }
        } catch (Exception e) {
            System.out.println("Error processing actuator state change message: " + message);
            e.printStackTrace();
        }
    }

}
