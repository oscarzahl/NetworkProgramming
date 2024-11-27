package no.ntnu.run;

import no.ntnu.gui.greenhouse.GreenhouseApplication;
import no.ntnu.tools.Logger;

/**
 * Starter for GUI version of the greenhouse simulator.
 */
public class GreenhouseGuiStarter {
  /**
   * Entrypoint gor the Greenhouse GUI application.
   *
   * @param args Command line arguments, only the first one of them used: when it
   *             is "fake",
   *             emulate fake events, when it is either something else or not
   *             present,
   *             use real socket communication.
   */
  public static void main(String[] args) {
    Logger.info("Starting Greenhouse GUI with real communication...");
    GreenhouseApplication.startApp(false); // Sett fake til false
  }
}
