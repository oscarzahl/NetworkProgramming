package no.ntnu.gui.common;

import java.util.HashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.ActuatorCollection;
import no.ntnu.listeners.common.ActuatorListener;

/**
 * A section of the GUI representing a list of actuators. Can be used both on
 * the sensor/actuator
 * node, and on a control panel node.
 */
public class ActuatorPane extends TitledPane {
  private final Map<Integer, SimpleStringProperty> actuatorValue = new HashMap<>();
  private final Map<Integer, SimpleBooleanProperty> actuatorActive = new HashMap<>();
  private ActuatorListener actuatorListener;

  /**
   * Create an actuator pane.
   *
   * @param actuators A list of actuators to display in the pane.
   */
  public ActuatorPane(ActuatorCollection actuators) {
    super();
    setText("Actuators");
    VBox vbox = new VBox();
    vbox.setSpacing(10);
    setContent(vbox);
    addActuatorControls(actuators, vbox);
    GuiTools.stretchVertically(this);
  }

  public void setActuatorListener(ActuatorListener listener) {
    this.actuatorListener = listener;
  }

  private void addActuatorControls(ActuatorCollection actuators, Pane parent) {
    actuators.forEach(actuator -> parent.getChildren().add(createActuatorGui(actuator)));
  }

  private Node createActuatorGui(Actuator actuator) {
    HBox actuatorGui = new HBox(createActuatorLabel(actuator), createActuatorCheckbox(actuator));
    actuatorGui.setSpacing(5);
    return actuatorGui;
  }

  private CheckBox createActuatorCheckbox(Actuator actuator) {
    CheckBox checkbox = new CheckBox();
    SimpleBooleanProperty isSelected = new SimpleBooleanProperty(actuator.isOn());
    actuatorActive.put(actuator.getId(), isSelected);
    checkbox.selectedProperty().bindBidirectional(isSelected);
    checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        actuator.set(newValue);
        if (actuatorListener != null) {
          actuatorListener.actuatorUpdated(actuator.getNodeId(), actuator);
        }
      }
    });
    return checkbox;
  }

  private Label createActuatorLabel(Actuator actuator) {
    SimpleStringProperty props = new SimpleStringProperty(generateActuatorText(actuator));
    actuatorValue.put(actuator.getId(), props);
    Label label = new Label();
    label.textProperty().bind(props);
    return label;
  }

  private String generateActuatorText(Actuator actuator) {
    String onOff = actuator.isOn() ? "ON" : "off";
    return actuator.getType() + ": " + onOff;
  }

  /**
   * An actuator has been updated, update the corresponding GUI parts.
   *
   * @param actuator The actuator which has been updated
   */
  public void update(Actuator actuator) {
    SimpleStringProperty actuatorText = actuatorValue.get(actuator.getId());
    SimpleBooleanProperty actuatorSelected = actuatorActive.get(actuator.getId());
    if (actuatorText == null || actuatorSelected == null) {
      throw new IllegalStateException("Can't update GUI for an unknown actuator: " + actuator);
    }

    Platform.runLater(() -> {
      actuatorText.set(generateActuatorText(actuator));
      actuatorSelected.set(actuator.isOn());
    });
  }

  public void addActuator(Actuator actuator) {
    VBox vbox = (VBox) getContent();
    vbox.getChildren().add(createActuatorGui(actuator));
    actuatorActive.put(actuator.getId(), new SimpleBooleanProperty(actuator.isOn()));
    actuatorValue.put(actuator.getId(), new SimpleStringProperty(generateActuatorText(actuator)));
  }
}