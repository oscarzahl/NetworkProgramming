package no.ntnu.TCP;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

import no.ntnu.controlpanel.CommunicationChannel;
import no.ntnu.controlpanel.ControlPanelLogic;
import no.ntnu.controlpanel.SensorActuatorNodeInfo;
import no.ntnu.greenhouse.SensorReading;

public class SocketCommunicationChannel implements CommunicationChannel, Runnable {
    private final Socket socket;
    private final ControlPanelLogic logic;
    private PrintWriter out;
    private BufferedReader in;

    public SocketCommunicationChannel(Socket socket, ControlPanelLogic logic) {
        this.socket = socket;
        this.logic = logic;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received from server: " + message);
                handleServerMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("NODE_ADDED:")) {
            int nodeId = Integer.parseInt(message.substring(11));
            SensorActuatorNodeInfo nodeInfo = new SensorActuatorNodeInfo(nodeId);
            logic.onNodeAdded(nodeInfo);
        } else if (message.startsWith("NODE_REMOVED:")) {
            int nodeId = Integer.parseInt(message.substring(13));
            logic.onNodeRemoved(nodeId);
        } else if (message.startsWith("SENSOR_DATA:")) {
            String[] parts = message.substring(12).split(";");
            int nodeId = Integer.parseInt(parts[0]);
            List<SensorReading> sensorReadings = parseSensorData(parts[1]);
            logic.onSensorData(nodeId, sensorReadings);
        }
        // Add more cases for other events if needed
        }


    private List<SensorReading> parseSensorData(String data) {
            // Parse sensor data string into SensorReading objects
            // Example: "temperature=25.4 °C,humidity=67 %"
        return SensorReading.parse(data);
    }
    // Methods to send messages to the server
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @Override
    public void sendActuatorChange(int nodeId, int actuatorId, boolean isOn) {
        String message = String.format("ACTUATOR_CHANGE:%d:%d:%b", nodeId, actuatorId, isOn);
        sendMessage(message);
    }

    @Override
    public boolean open() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'open'");
    }

    private void closeConnection() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Failed to close socket: " + e.getMessage());
        }
    }
}
