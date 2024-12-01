package no.ntnu.controlpanel;

import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.ActuatorCollection;
import no.ntnu.greenhouse.SensorReading;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TcpCommunicationChannel manages communication between the Control Panel and
 * the server using the TCP protocol.
 */
public class TcpCommunicationChannel implements CommunicationChannel {
    private final String serverAddress; // Server address to connect to
    private final int port; // Port for the server connection
    private final ControlPanelLogic logic; // Logic handler for the Control Panel
    private Socket socket; // Socket for server communication
    private PrintWriter out; // Output stream for sending messages
    private BufferedReader in; // Input stream for receiving messages

    /**
     * Constructs a TcpCommunicationChannel instance.
     *
     * @param serverAddress the server address to connect to
     * @param port          the server port
     * @param logic         the logic handler for the control panel
     */
    public TcpCommunicationChannel(String serverAddress, int port, ControlPanelLogic logic) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.logic = logic;
    }

    /**
     * Opens a connection to the server and starts a listener thread for incoming
     * messages.
     *
     * @return true if the connection is successfully established, false otherwise
     */
    @Override
    public boolean open() {
        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a new thread to listen for messages from the server
            new Thread(this::listenToServer).start();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Listens for incoming messages from the server and processes them.
     */
    private void listenToServer() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                handleServerMessage(message);
            }
        } catch (SocketException e) {
            if ("Socket closed".equals(e.getMessage())) {
                System.out.println("Socket closed, stopping listener thread.");
            } else {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles incoming messages from the server.
     *
     * @param message the message received from the server
     */
    private void handleServerMessage(String message) {
        if (message.startsWith("SENSOR:")) {
            handleSensorMessage(message);
        } else if (message.startsWith("ACTUATOR:")) {
            handleActuatorMessage(message);
        }
    }

    /**
     * Processes a SENSOR message and updates the logic with sensor readings.
     *
     * @param message the SENSOR message from the server
     */
    private void handleSensorMessage(String message) {
        String[] parts = message.split(":");
        int nodeId = Integer.parseInt(parts[1]); // Extract node ID
        String sensorData = parts[2];

        // Ensure the node exists in the control panel
        logic.ensureNodeExists(nodeId);

        // Parse and update sensor data
        List<SensorReading> readings = parseSensorData(sensorData);
        logic.onSensorData(nodeId, readings);
    }

    /**
     * Processes an ACTUATOR message and updates the logic with actuator states.
     *
     * @param message the ACTUATOR message from the server
     */
    private void handleActuatorMessage(String message) {
        String[] parts = message.split(":", 3); // Split into three parts
        int nodeId = Integer.parseInt(parts[1]);
        String actuatorData = parts[2];

        ActuatorCollection actuators = new ActuatorCollection();

        // Parse actuator data
        String[] actuatorInfos = actuatorData.split(",");
        for (String actuatorInfo : actuatorInfos) {
            String[] idAndTypeAndState = actuatorInfo.split(":");
            if (idAndTypeAndState.length != 2) {
                continue; // Skip invalid data
            }

            int actuatorId = Integer.parseInt(idAndTypeAndState[0]);
            String[] typeAndState = idAndTypeAndState[1].split("=");

            if (typeAndState.length != 2) {
                continue; // Skip invalid data
            }

            String type = typeAndState[0];
            boolean state = Boolean.parseBoolean(typeAndState[1]);

            Actuator actuator = new Actuator(actuatorId, type, nodeId);
            actuator.set(state);
            actuators.add(actuator);
        }

        // Ensure the node exists in the control panel
        logic.ensureNodeExists(nodeId);

        // Update the control panel with actuator data
        logic.handleInitialActuatorData(nodeId, actuators);
    }

    /**
     * Parses sensor data into a list of SensorReading objects.
     *
     * @param sensorData the raw sensor data string
     * @return a list of SensorReading objects
     */
    private List<SensorReading> parseSensorData(String sensorData) {
        List<SensorReading> readings = new ArrayList<>();
        String[] sensors = sensorData.split(",");

        for (String sensor : sensors) {
            String[] parts = sensor.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid sensor data format: " + sensor);
            }

            String type = parts[0].trim();

            // Extract value and unit
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

    /**
     * Finds the starting index of the unit in a value string.
     *
     * @param valueAndUnit the value string containing both number and unit
     * @return the starting index of the unit, or -1 if no unit is found
     */
    private int findUnitStartIndex(String valueAndUnit) {
        for (int i = 0; i < valueAndUnit.length(); i++) {
            if (!Character.isDigit(valueAndUnit.charAt(i)) && valueAndUnit.charAt(i) != '.'
                    && valueAndUnit.charAt(i) != '-') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Sends an actuator state change command to the server.
     *
     * @param nodeId     the ID of the node
     * @param actuatorId the ID of the actuator
     * @param isOn       the new state of the actuator
     */
    @Override
    public void sendActuatorChange(int nodeId, int actuatorId, boolean isOn) {
        String command = String.format("ACTUATOR:%d:%d:%b", nodeId, actuatorId, isOn);
        if (out != null) {
            out.println(command);
        }
    }

    /**
     * Closes the communication channel by shutting down the socket and streams.
     */
    @Override
    public void close() {
        try {
            if (socket != null)
                socket.close();
            if (out != null)
                out.close();
            if (in != null)
                in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
