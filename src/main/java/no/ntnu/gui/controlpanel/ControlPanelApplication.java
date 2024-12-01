package no.ntnu.gui.controlpanel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import no.ntnu.controlpanel.CommunicationChannel;
import no.ntnu.controlpanel.ControlPanelLogic;
import no.ntnu.controlpanel.SensorActuatorNodeInfo;
import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.SensorReading;
import no.ntnu.gui.common.ActuatorPane;
import no.ntnu.gui.common.SensorPane;
import no.ntnu.listeners.common.CommunicationChannelListener;
import no.ntnu.listeners.controlpanel.GreenhouseEventListener;
import no.ntnu.tools.Logger;

/**
 * Run a control panel with a graphical user interface (GUI), with JavaFX.
 */
public class ControlPanelApplication extends Application implements GreenhouseEventListener,
    CommunicationChannelListener {
  private static ControlPanelLogic logic;
  private static final int WIDTH = 500;
  private static final int HEIGHT = 400;
  private static CommunicationChannel channel;

  private TabPane nodeTabPane;
  private Scene mainScene;
  private final Map<Integer, SensorPane> sensorPanes = new HashMap<>();
  private final Map<Integer, ActuatorPane> actuatorPanes = new HashMap<>();
  private final Map<Integer, SensorActuatorNodeInfo> nodeInfos = new HashMap<>();
  private final Map<Integer, Tab> nodeTabs = new HashMap<>();

  /**
   * Application entrypoint for the GUI of a control panel.
   * Note - this is a workaround to avoid problems with JavaFX not finding the
   * modules!
   * We need to use another wrapper-class for the debugger to work.
   *
   * @param logic   The logic of the control panel node
   * @param channel Communication channel for sending control commands and
   *                receiving events
   */
  public static void startApp(ControlPanelLogic logic, CommunicationChannel channel) {
    if (logic == null) {
      throw new IllegalArgumentException("Control panel logic can't be null");
    }
    ControlPanelApplication.logic = logic;
    ControlPanelApplication.channel = channel;
    Logger.info("Running control panel GUI...");
    launch();
  }

  @Override
  public void start(Stage stage) {
    if (channel == null) {
      throw new IllegalStateException(
          "No communication channel. See the README on how to use fake event spawner!");
    }

    stage.setMinWidth(WIDTH);
    stage.setMinHeight(HEIGHT);
    stage.setTitle("Control panel");
    mainScene = new Scene(createEmptyContent(), WIDTH, HEIGHT);
    stage.setScene(mainScene);
    stage.show();
    logic.addListener(this);
    logic.setCommunicationChannelListener(this);
    if (!channel.open()) {
      logic.onCommunicationChannelClosed();
    }
  }

  private static Label createEmptyContent() {
    Label l = new Label("Waiting for node data...");
    l.setAlignment(Pos.CENTER);
    return l;
  }

  @Override
  public void onNodeAdded(SensorActuatorNodeInfo nodeInfo) {
    Platform.runLater(() -> addNodeTab(nodeInfo));
  }

  @Override
  public void onNodeRemoved(int nodeId) {
    Tab nodeTab = nodeTabs.get(nodeId);
    if (nodeTab != null) {
      Platform.runLater(() -> {
        removeNodeTab(nodeId, nodeTab);
        forgetNodeInfo(nodeId);
        if (nodeInfos.isEmpty()) {
          removeNodeTabPane();
        }
      });
      Logger.info("Node " + nodeId + " removed");
    } else {
      Logger.error("Can't remove node " + nodeId + ", there is no Tab for it");
    }
  }

  private void removeNodeTabPane() {
    mainScene.setRoot(createEmptyContent());
    nodeTabPane = null;
  }

  @Override
  public void onSensorData(int nodeId, List<SensorReading> sensors) {
      Logger.info("Sensor data from node " + nodeId);
      SensorPane sensorPane = sensorPanes.get(nodeId);
      if (sensorPane != null) {
          sensorPane.update(sensors);
      } else {
          Logger.error("No sensor section for node " + nodeId);
      }
  }

  @Override
  public void onActuatorStateChanged(int nodeId, int actuatorId, boolean isOn) {
    String state = isOn ? "ON" : "off";
    Logger.info("actuator[" + actuatorId + "] on node " + nodeId + " is " + state);
    ActuatorPane actuatorPane = actuatorPanes.get(nodeId);
    if (actuatorPane != null) {
      Actuator actuator = getStoredActuator(nodeId, actuatorId);
      if (actuator != null) {
        if (isOn) {
          actuator.turnOn();
        } else {
          actuator.turnOff();
        }
        actuatorPane.update(actuator);
      } else {
        Logger.error(" actuator not found");
      }
    } else {
      Logger.error("No actuator section for node " + nodeId);
    }
  }

  private Actuator getStoredActuator(int nodeId, int actuatorId) {
    Actuator actuator = null;
    SensorActuatorNodeInfo nodeInfo = nodeInfos.get(nodeId);
    if (nodeInfo != null) {
      actuator = nodeInfo.getActuator(actuatorId);
    }
    return actuator;
  }

  private void forgetNodeInfo(int nodeId) {
    sensorPanes.remove(nodeId);
    actuatorPanes.remove(nodeId);
    nodeInfos.remove(nodeId);
  }

  private void removeNodeTab(int nodeId, Tab nodeTab) {
    nodeTab.getTabPane().getTabs().remove(nodeTab);
    nodeTabs.remove(nodeId);
  }

  private void addNodeTab(SensorActuatorNodeInfo nodeInfo) {
    if (nodeTabPane == null) {
      nodeTabPane = new TabPane();
      mainScene.setRoot(nodeTabPane);
    }

    // Avoid creating duplicate tabs
    if (!nodeTabs.containsKey(nodeInfo.getId())) {
      Logger.info("Creating tab for Node #" + nodeInfo.getId());
      Tab nodeTab = createNodeTab(nodeInfo);
      nodeTabs.put(nodeInfo.getId(), nodeTab);
      nodeTabPane.getTabs().add(nodeTab);
    } else {
      Logger.info("Duplicate node spawned, ignore it");
    }
  }

  private Tab createNodeTab(SensorActuatorNodeInfo nodeInfo) {
    Tab tab = new Tab("Node " + nodeInfo.getId());
    SensorPane sensorPane = createEmptySensorPane();
    sensorPanes.put(nodeInfo.getId(), sensorPane);
    ActuatorPane actuatorPane = new ActuatorPane(nodeInfo.getActuators());
    actuatorPane.setActuatorListener((nodeId, actuator) -> {
      System.out.println("ActuatorListener triggered for nodeId: " + nodeId + ", actuatorId: " + actuator.getId());
      logic.sendActuatorChange(nodeId, actuator.getId(), actuator.isOn());
  });
    actuatorPanes.put(nodeInfo.getId(), actuatorPane);
    tab.setContent(new VBox(sensorPane, actuatorPane));
    nodeTabs.put(nodeInfo.getId(), tab);
    nodeInfos.put(nodeInfo.getId(), nodeInfo); // Ensure nodeInfo is stored
    return tab;
  }

  private static SensorPane createEmptySensorPane() {
    return new SensorPane();
  }

  @Override
  public void onCommunicationChannelClosed() {
    Logger.info("Communication closed, closing the GUI");
    Platform.runLater(Platform::exit);
  }

  public static ControlPanelLogic getLogic() {
    return logic;
  }
}