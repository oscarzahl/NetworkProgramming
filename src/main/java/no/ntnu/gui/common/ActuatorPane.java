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
 * A GUI pane that displays a list of actuators. This pane allows for the
 * display
 * and control of actuators and is usable on both sensor/actuator nodes and
 * control panel nodes.
 */
public class ActuatorPane extends TitledPane {
  private final Map<Integer, SimpleStringProperty> actuatorValue = new HashMap<>(); // Stores actuator labels
  private final Map<Integer, SimpleBooleanProperty> actuatorActive = new HashMap<>(); // Stores actuator states
  private ActuatorListener actuatorListener; // Listener for actuator updates

  /**
   * Creates an ActuatorPane with a collection of actuators.
   *
   * @param actuators A collection of actuators to be displayed in the pane.
   */
  public ActuatorPane(ActuatorCollection actuators) {
    super();
    setText("Actuators"); // Title of the pane
    VBox vbox = new VBox();
    vbox.setSpacing(10); // Space between controls
    setContent(vbox); // Set the VBox as the content of the pane
    addActuatorControls(actuators, vbox); // Add actuator controls to the VBox
    GuiTools.stretchVertically(this); // Ensure the pane stretches vertically
  }

  /**
   * Sets a listener to handle actuator updates.
   *
   * @param listener The listener for actuator updates.
   */
  public void setActuatorListener(ActuatorListener listener) {
    this.actuatorListener = listener;
  }

  /**
   * Adds controls for a collection of actuators to a parent container.
   *
   * @param actuators The collection of actuators.
   * @param parent    The parent container.
   */
  private void addActuatorControls(ActuatorCollection actuators, Pane parent) {
    actuators.forEach(actuator -> parent.getChildren().add(createActuatorGui(actuator)));
  }

  /**
   * Creates the GUI representation for a single actuator.
   *
   * @param actuator The actuator to display.
   * @return A Node containing the actuator's GUI controls.
   */
  private Node createActuatorGui(Actuator actuator) {
    HBox actuatorGui = new HBox(createActuatorLabel(actuator), createActuatorCheckbox(actuator));
    actuatorGui.setSpacing(5); // Space between controls
    return actuatorGui;
  }

  /**
   * Creates a checkbox control for an actuator, allowing its state to be toggled.
   *
   * @param actuator The actuator to control.
   * @return The checkbox control.
   */
  private CheckBox createActuatorCheckbox(Actuator actuator) {
    CheckBox checkbox = new CheckBox();
    SimpleBooleanProperty isSelected = new SimpleBooleanProperty(actuator.isOn());
    actuatorActive.put(actuator.getId(), isSelected); // Store the actuator's state
    checkbox.selectedProperty().bindBidirectional(isSelected); // Bind the checkbox to the state

    // Add listener to update the actuator's state
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

  /**
   * Creates a label to display the current state of an actuator.
   *
   * @param actuator The actuator to display.
   * @return The label for the actuator.
   */
  private Label createActuatorLabel(Actuator actuator) {
    SimpleStringProperty props = new SimpleStringProperty(generateActuatorText(actuator));
    actuatorValue.put(actuator.getId(), props); // Store the actuator's label
    Label label = new Label();
    label.textProperty().bind(props); // Bind the label to the actuator's state
    return label;
  }

  /**
   * Generates the text representation of an actuator's state.
   *
   * @param actuator The actuator to describe.
   * @return A string representation of the actuator's state.
   */
  private String generateActuatorText(Actuator actuator) {
    String onOff = actuator.isOn() ? "ON" : "off";
    return actuator.getType() + ": " + onOff;
  }

  /**
   * Updates the GUI representation of an actuator when its state changes.
   *
   * @param actuator The actuator to update.
   */
  public void update(Actuator actuator) {
    SimpleStringProperty actuatorText = actuatorValue.get(actuator.getId());
    SimpleBooleanProperty actuatorSelected = actuatorActive.get(actuator.getId());
    if (actuatorText == null || actuatorSelected == null) {
      throw new IllegalStateException("Can't update GUI for an unknown actuator: " + actuator);
    }

    // Update the GUI on the JavaFX application thread
    Platform.runLater(() -> {
      actuatorText.set(generateActuatorText(actuator));
      actuatorSelected.set(actuator.isOn());
    });
  }

  /**
   * Adds a new actuator to the GUI pane.
   *
   * @param actuator The actuator to add.
   */
  public void addActuator(Actuator actuator) {
    VBox vbox = (VBox) getContent();
    vbox.getChildren().add(createActuatorGui(actuator)); // Add the actuator's GUI
    actuatorActive.put(actuator.getId(), new SimpleBooleanProperty(actuator.isOn())); // Store its state
    actuatorValue.put(actuator.getId(), new SimpleStringProperty(generateActuatorText(actuator))); // Store its label
  }
}
