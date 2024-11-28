package no.ntnu.controlpanel;

import no.ntnu.controlpanel.ControlPanelLogic;
import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.SensorReading;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class TcpCommunicationChannel implements CommunicationChannel {
    private final String serverAddress;
    private final int port;
    private final ControlPanelLogic logic;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public TcpCommunicationChannel(String serverAddress, int port, ControlPanelLogic logic) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.logic = logic;
    }

    @Override
    public boolean open() {
        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start en ny tråd for å lytte på meldinger fra serveren
            new Thread(this::listenToServer).start();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

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

    private void handleServerMessage(String message) {
        if (message.startsWith("SENSOR:")) {
            String[] parts = message.split(":");
            int nodeId = Integer.parseInt(parts[1]); // Extract unique node ID
            System.out.println("Parsed Node ID: " + nodeId); // Debug output
            
            String sensorData = parts[2];
            System.out.println("Parsed Sensor Data: " + sensorData); // Debug output
    
            if (!logic.hasNode(nodeId)) {
                SensorActuatorNodeInfo newNode = new SensorActuatorNodeInfo(nodeId);
                logic.onNodeAdded(newNode);
            }
    
            // Process sensor data
            List<SensorReading> readings = parseSensorData(sensorData);
            logic.onSensorData(nodeId, readings); // Update GUI with sensor data
        }

        if (message.startsWith("ACTUATOR:")) {
            String[] parts = message.split(":");
            int nodeId = Integer.parseInt(parts[1]);
            System.out.println("Parsed Node ID: " + nodeId); 
    
            String actuatorData = parts[2];
            System.out.println("Parsed Actuator Data: " + actuatorData);
    
            // Split actuator data by commas (multiple actuators)
            String[] actuators = actuatorData.split(",");
            for (String actuatorInfo : actuators) {
                String[] idAndState = actuatorInfo.split(":");
                
                if (idAndState.length != 2) {
                    System.out.println("Invalid actuator data format: " + actuatorInfo);
                    continue; // Skip this invalid actuator data
                }
                
                int actuatorId = Integer.parseInt(idAndState[0]);
                String[] keyValue = idAndState[1].split("=");
                
                if (keyValue.length != 2) {
                    System.out.println("Invalid actuator state format: " + actuatorInfo);
                    continue;
                }
    
                String type = keyValue[0];
                boolean state = Boolean.parseBoolean(keyValue[1]);
    
                System.out.println("Actuator ID: " + actuatorId + ", Type: " + type + ", State: " + state);
    
                if (logic.hasNode(nodeId)) {
                    logic.onActuatorStateChanged(nodeId, actuatorId, state);
                }
            }
        }
    }
    

    private List<SensorReading> parseSensorData(String sensorData) {
        List<SensorReading> readings = new ArrayList<>();
        String[] sensors = sensorData.split(",");
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
            if (!Character.isDigit(valueAndUnit.charAt(i)) && valueAndUnit.charAt(i) != '.'
                    && valueAndUnit.charAt(i) != '-') {
                return i;
            }
        }
        return -1; // No unit found
    }

    @Override
    public void sendActuatorChange(int nodeId, int actuatorId, boolean isOn) {
        String command = String.format("ACTUATOR:%d:%d:%b", nodeId, actuatorId, isOn);
        if (out != null) {
            out.println(command);
        }
    }

    @Override
    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            if(in != null) {
                in.close();
            }
        }  catch (IOException e) {
            e.printStackTrace();
        }
    }
}
