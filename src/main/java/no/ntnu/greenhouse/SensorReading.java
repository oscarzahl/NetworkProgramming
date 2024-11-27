package no.ntnu.greenhouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents one sensor reading (value).
 */
public class SensorReading {
  private final String type;
  private double value;
  private final String unit;

  /**
   * Create a new sensor reading.
   *
   * @param type  The type of sensor being red
   * @param value The current value of the sensor
   * @param unit  The unit, for example: %, lux
   */
  public SensorReading(String type, double value, String unit) {
    this.type = type;
    this.value = value;
    this.unit = unit;
  }

  public String getType() {
    return type;
  }

  public double getValue() {
    return value;
  }

  public String getUnit() {
    return unit;
  }

  public void setValue(double newValue) {
    this.value = newValue;
  }

  @Override
  public String toString() {
    return "{ type=" + type + ", value=" + value + ", unit=" + unit + " }";
  }

  /**
   * Get a human-readable (formatted) version of the current reading, including the unit.
   *
   * @return The sensor reading and the unit
   */
  public String getFormatted() {
    return value + unit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SensorReading that = (SensorReading) o;
    return Double.compare(value, that.value) == 0
        && Objects.equals(type, that.type)
        && Objects.equals(unit, that.unit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, value, unit);
  }

    /**
   * Parse a string containing sensor readings into a list of SensorReading objects.
   * The input string should be in the format: "type=value unit,type=value unit,..."
   * Example: "temperature=25.4 °C,humidity=67 %"
   *
   * @param data The formatted string containing sensor readings
   * @return A list of SensorReading objects
   */
  public static List<SensorReading> parse(String data) {
    List<SensorReading> readings = new ArrayList<>();
    String[] entries = data.split(",");

    for (String entry : entries) {
      String[] parts = entry.split("=");
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid sensor reading format: " + entry);
      }

      String type = parts[0].trim();
      String[] valueAndUnit = parts[1].trim().split(" ");
      if (valueAndUnit.length != 2) {
        throw new IllegalArgumentException("Invalid sensor reading format: " + parts[1]);
      }

      double value = Double.parseDouble(valueAndUnit[0].trim());
      String unit = valueAndUnit[1].trim();

      readings.add(new SensorReading(type, value, unit));
    }
    return readings;
  }

}
