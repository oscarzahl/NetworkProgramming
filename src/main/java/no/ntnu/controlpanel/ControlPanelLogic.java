package no.ntnu.controlpanel;

import java.util.LinkedList;
import java.util.List;
import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.ActuatorCollection;
import no.ntnu.greenhouse.SensorReading;
import no.ntnu.listeners.common.ActuatorListener;
import no.ntnu.listeners.common.CommunicationChannelListener;
import no.ntnu.listeners.controlpanel.GreenhouseEventListener;
import no.ntnu.tools.Logger;

/**
 * The central logic of a control panel node. It uses a communication channel to
 * send commands
 * and receive events. It supports listeners who will be notified on changes
 * (for example, a new
 * node is added to the network, or a new sensor reading is received).
 * Note: this class may look like unnecessary forwarding of events to the GUI.
 * In real projects
 * (read: "big projects") this logic class may do some "real processing" - such
 * as storing events
 * in a database, doing some checks, sending emails, notifications, etc. Such
 * things should never
 * be placed inside a GUI class (JavaFX classes). Therefore, we use proper
 * structure here, even
 * though you may have no real control-panel logic in your projects.
 */
public class ControlPanelLogic implements GreenhouseEventListener, ActuatorListener,
    CommunicationChannelListener {
  private final List<GreenhouseEventListener> listeners = new LinkedList<>();
  private final List<SensorActuatorNodeInfo> nodes = new LinkedList<>();

  private CommunicationChannel communicationChannel;
  private CommunicationChannelListener communicationChannelListener;

  /**
   * Set the channel over which control commands will be sent to sensor/actuator
   * nodes.
   *
   * @param communicationChannel The communication channel, the event sender
   */
  public void setCommunicationChannel(CommunicationChannel communicationChannel) {
    this.communicationChannel = communicationChannel;
  }

  /**
   * Set listener which will get notified when communication channel is closed.
   *
   * @param listener The listener
   */
  public void setCommunicationChannelListener(CommunicationChannelListener listener) {
    this.communicationChannelListener = listener;
  }

  /**
   * Add an event listener.
   *
   * @param listener The listener who will be notified on all events
   */
  public void addListener(GreenhouseEventListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  public boolean hasNode(int nodeId) {
    return nodes.stream().anyMatch(node -> node.getId() == nodeId);
  }

  public SensorActuatorNodeInfo getNodeInfo(int nodeId) {
    return nodes.stream()
        .filter(node -> node.getId() == nodeId)
        .findFirst()
        .orElse(null);
  }

  public void handleInitialActuatorData(int nodeId, ActuatorCollection actuators) {
    SensorActuatorNodeInfo nodeInfo = getNodeInfo(nodeId);
    if (nodeInfo != null) {
      for (Actuator actuator : actuators) {
        nodeInfo.addActuator(actuator);
        System.out.println("Initial actuator added to nodeInfo: " + actuator);
      }
      notifyActuatorData(nodeId, actuators);
    } else {
      System.out.println("NodeInfo not found for nodeId: " + nodeId);
    }
  }

  public SensorActuatorNodeInfo ensureNodeExists(int nodeId) {
    SensorActuatorNodeInfo nodeInfo = getNodeInfo(nodeId);
    if (nodeInfo == null) {
      nodeInfo = new SensorActuatorNodeInfo(nodeId);
      nodes.add(nodeInfo);
      onNodeAdded(nodeInfo);
      System.out.println("NodeInfo created for nodeId: " + nodeId);
    }
    System.out.println("NodeInfo exists for nodeId: " + nodeId);
    return nodeInfo;
  }

  private void notifyActuatorData(int nodeId, ActuatorCollection actuators) {
    for (Actuator actuator : actuators) {
      listeners.forEach(listener -> listener.onActuatorStateChanged(nodeId, actuator.getId(), actuator.isOn()));
    }
  }

  @Override
  public void onNodeAdded(SensorActuatorNodeInfo nodeInfo) {
    // Notify all listeners about the new node
    listeners.forEach(listener -> listener.onNodeAdded(nodeInfo));
    System.out.println("Node " + nodeInfo.getId() + " actuators: " + nodeInfo.getActuators());
    nodeInfo.getActuators().forEach(actuator -> System.out.println("Actuator: " + actuator));
  }

  @Override
  public void onNodeRemoved(int nodeId) {
    nodes.removeIf(node -> node.getId() == nodeId);
    listeners.forEach(listener -> listener.onNodeRemoved(nodeId));
  }

  @Override
  public void onSensorData(int nodeId, List<SensorReading> sensors) {
    listeners.forEach(listener -> listener.onSensorData(nodeId, sensors));
  }

  @Override
  public void onActuatorStateChanged(int nodeId, int actuatorId, boolean isOn) {
    listeners.forEach(listener -> listener.onActuatorStateChanged(nodeId, actuatorId, isOn));
  }

  @Override
  public void actuatorUpdated(int nodeId, Actuator actuator) {
    if (communicationChannel != null) {
      communicationChannel.sendActuatorChange(nodeId, actuator.getId(), actuator.isOn());
    }
    listeners.forEach(listener -> listener.onActuatorStateChanged(nodeId, actuator.getId(), actuator.isOn()));
  }

  /**
   * Send actuator state change to the node.
   *
   * @param nodeId The ID of the node
   * @param actuatorId The ID of the actuator
   * @param isOn The new state of the actuator
   */
  public void sendActuatorChange(int nodeId, int actuatorId, boolean isOn) {
    if (communicationChannel != null) {
      communicationChannel.sendActuatorChange(nodeId, actuatorId, isOn);
    }
    listeners.forEach(listener -> listener.onActuatorStateChanged(nodeId, actuatorId, isOn));
  }

  @Override
  public void onCommunicationChannelClosed() {
    Logger.info("Communication closed, updating logic...");
    if (communicationChannelListener != null) {
      communicationChannelListener.onCommunicationChannelClosed();
    }
  }
}