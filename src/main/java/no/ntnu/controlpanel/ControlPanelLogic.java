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
 * Central logic for the control panel node. Handles communication with
 * sensor/actuator nodes
 * and notifies registered listeners of events such as new sensor readings or
 * actuator state changes.
 * 
 * This class serves as an intermediary between the GUI and the networked
 * greenhouse nodes,
 * ensuring proper structure by separating logic from GUI code.
 */
public class ControlPanelLogic implements GreenhouseEventListener, ActuatorListener, CommunicationChannelListener {
  private final List<GreenhouseEventListener> listeners = new LinkedList<>(); // List of event listeners
  private final List<SensorActuatorNodeInfo> nodes = new LinkedList<>(); // List of known nodes

  private CommunicationChannel communicationChannel; // Communication channel for sending/receiving data
  private CommunicationChannelListener communicationChannelListener; // Listener for communication channel events

  /**
   * Sets the communication channel for the control panel to send/receive commands
   * and data.
   *
   * @param communicationChannel The communication channel
   */
  public void setCommunicationChannel(CommunicationChannel communicationChannel) {
    this.communicationChannel = communicationChannel;
  }

  /**
   * Sets the listener to be notified when the communication channel is closed.
   *
   * @param listener The listener
   */
  public void setCommunicationChannelListener(CommunicationChannelListener listener) {
    this.communicationChannelListener = listener;
  }

  /**
   * Adds a new listener to be notified of control panel events.
   *
   * @param listener The event listener
   */
  public void addListener(GreenhouseEventListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  /**
   * Checks if a node with the given ID exists.
   *
   * @param nodeId The ID of the node
   * @return True if the node exists, otherwise false
   */
  public boolean hasNode(int nodeId) {
    return nodes.stream().anyMatch(node -> node.getId() == nodeId);
  }

  /**
   * Retrieves information about a node with the given ID.
   *
   * @param nodeId The ID of the node
   * @return The node information, or null if not found
   */
  public SensorActuatorNodeInfo getNodeInfo(int nodeId) {
    return nodes.stream()
        .filter(node -> node.getId() == nodeId)
        .findFirst()
        .orElse(null);
  }

  /**
   * Handles initial actuator data for a node and notifies listeners of the
   * changes.
   *
   * @param nodeId    The ID of the node
   * @param actuators The actuators associated with the node
   */
  public void handleInitialActuatorData(int nodeId, ActuatorCollection actuators) {
    SensorActuatorNodeInfo nodeInfo = getNodeInfo(nodeId);
    if (nodeInfo != null) {
      for (Actuator actuator : actuators) {
        nodeInfo.addActuator(actuator);
        notifyActuatorAdded(nodeId, actuator); // Notify listeners of the new actuator
      }
      notifyActuatorData(nodeId, actuators); // Notify listeners of the actuator states
    } else {
      System.out.println("NodeInfo not found for nodeId: " + nodeId);
    }
  }

  /**
   * Ensures that a node with the given ID exists. If it doesn't, creates and adds
   * it.
   *
   * @param nodeId The ID of the node
   * @return The existing or newly created node
   */
  public SensorActuatorNodeInfo ensureNodeExists(int nodeId) {
    SensorActuatorNodeInfo nodeInfo = getNodeInfo(nodeId);
    if (nodeInfo == null) {
      nodeInfo = new SensorActuatorNodeInfo(nodeId);
      nodes.add(nodeInfo);
      onNodeAdded(nodeInfo); // Notify listeners of the new node
    }
    return nodeInfo;
  }

  /**
   * Notifies listeners of actuator state changes for a node.
   *
   * @param nodeId    The ID of the node
   * @param actuators The collection of actuators
   */
  private void notifyActuatorData(int nodeId, ActuatorCollection actuators) {
    for (Actuator actuator : actuators) {
      listeners.forEach(listener -> listener.onActuatorStateChanged(nodeId, actuator.getId(), actuator.isOn()));
    }
  }

  /**
   * Notifies listeners that a new actuator has been added to a node.
   *
   * @param nodeId   The ID of the node
   * @param actuator The new actuator
   */
  private void notifyActuatorAdded(int nodeId, Actuator actuator) {
    listeners.forEach(listener -> {
      if (listener instanceof ActuatorListener) {
        ((ActuatorListener) listener).actuatorUpdated(nodeId, actuator);
      }
    });
  }

  @Override
  public void onNodeAdded(SensorActuatorNodeInfo nodeInfo) {
    listeners.forEach(listener -> listener.onNodeAdded(nodeInfo));
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
   * Sends a command to change the state of an actuator on a specific node.
   *
   * @param nodeId     The ID of the node
   * @param actuatorId The ID of the actuator
   * @param isOn       The desired state of the actuator
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
