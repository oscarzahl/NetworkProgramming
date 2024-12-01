package no.ntnu.greenhouse;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import no.ntnu.listeners.greenhouse.NodeStateListener;
import no.ntnu.tcp.GreenhouseNode;
import no.ntnu.tools.Logger;

/**
 * A simulator for a greenhouse system. Handles the initialization and
 * management of sensor/actuator nodes,
 * periodic switches, and communication with a central server.
 */
public class GreenhouseSimulator {
  private final Map<Integer, SensorActuatorNode> nodes = new HashMap<>(); // Map of node IDs to sensor/actuator nodes
  private final List<GreenhouseNode> greenhouseNodes = new LinkedList<>(); // List of TCP communication nodes
  private final List<PeriodicSwitch> periodicSwitches = new LinkedList<>(); // List of periodic switches
  private final boolean fake; // Indicates whether to use fake communication (for testing)

  /**
   * Constructs the GreenhouseSimulator.
   *
   * @param fake Whether the simulator uses fake communication (true for testing,
   *             false for real communication)
   */
  public GreenhouseSimulator(boolean fake) {
    this.fake = fake;
  }

  /**
   * Initializes the greenhouse by creating predefined nodes.
   */
  public void initialize() {
    createNode(1, 2, 1, 0, 0);
    createNode(1, 0, 0, 2, 1);
    createNode(2, 0, 0, 0, 0);
    createNode(2, 3, 2, 1, 0);
    Logger.info("Greenhouse initialized");
  }

  /**
   * Creates a new sensor/actuator node with specified attributes and adds it to
   * the simulator.
   *
   * @param temperature Number of temperature sensors
   * @param humidity    Number of humidity sensors
   * @param windows     Number of window actuators
   * @param fans        Number of fan actuators
   * @param heaters     Number of heater actuators
   */
  private void createNode(int temperature, int humidity, int windows, int fans, int heaters) {
    SensorActuatorNode node = DeviceFactory.createNode(temperature, humidity, windows, fans, heaters);
    nodes.put(node.getId(), node); // Add the node to the map
    System.out.println("Node created: " + node.getId());
  }

  /**
   * Starts the greenhouse simulator by initializing communication and starting
   * all nodes and periodic switches.
   */
  public void start() {
    initiateCommunication();
    for (SensorActuatorNode node : nodes.values()) {
      node.start(); // Start each node
    }
    for (PeriodicSwitch periodicSwitch : periodicSwitches) {
      periodicSwitch.start(); // Start each periodic switch
    }
    Logger.info("Simulator started");
  }

  /**
   * Initiates communication based on the mode (fake or real).
   */
  private void initiateCommunication() {
    if (fake) {
      initiateFakePeriodicSwitches(); // Use fake periodic switches for testing
    } else {
      initiateRealCommunication(); // Establish real communication with a server
    }
  }

  /**
   * Establishes real TCP communication with a server for all nodes.
   */
  private void initiateRealCommunication() {
    for (SensorActuatorNode node : nodes.values()) {
      int nodeId = node.getId();
      ActuatorCollection actuators = node.getActuators();
      List<Sensor> sensors = node.getSensors();
      GreenhouseNode tcpNode = new GreenhouseNode(nodeId, "localhost", 12345, sensors, actuators);
      greenhouseNodes.add(tcpNode); // Add the node to the list of TCP nodes
      new Thread(tcpNode::start).start(); // Start the TCP node in a new thread
    }
  }

  /**
   * Creates fake periodic switches for testing purposes.
   */
  private void initiateFakePeriodicSwitches() {
    periodicSwitches.add(new PeriodicSwitch("Window DJ", nodes.get(1), 2, 20000));
    periodicSwitches.add(new PeriodicSwitch("Heater DJ", nodes.get(2), 7, 8000));
  }

  /**
   * Stops the simulator by stopping all nodes and periodic switches, and closing
   * communication.
   */
  public void stop() {
    stopCommunication();
    for (SensorActuatorNode node : nodes.values()) {
      node.stop(); // Stop each node
    }
  }

  /**
   * Stops communication by shutting down periodic switches or TCP nodes,
   * depending on the mode.
   */
  private void stopCommunication() {
    if (fake) {
      for (PeriodicSwitch periodicSwitch : periodicSwitches) {
        periodicSwitch.stop(); // Stop each periodic switch
      }
    } else {
      for (GreenhouseNode node : greenhouseNodes) {
        node.stop(); // Stop each TCP node
      }
    }
  }

  /**
   * Subscribes a listener to lifecycle updates for all nodes.
   *
   * @param listener The listener to be notified of lifecycle events
   */
  public void subscribeToLifecycleUpdates(NodeStateListener listener) {
    for (SensorActuatorNode node : nodes.values()) {
      node.addStateListener(listener); // Add the listener to each node
    }
  }
}
