package no.ntnu.TCP;

import java.io.*;
import java.net.*;
import java.util.Map;
import no.ntnu.greenhouse.SensorActuatorNode;

class ClientHandler implements Runnable {
  private final Socket clientSocket;
  private final Map<Integer, SensorActuatorNode> nodes;

  public ClientHandler(Socket clientSocket, Map<Integer, SensorActuatorNode> nodes) {
      this.clientSocket = clientSocket;
      this.nodes = nodes;
  }

  @Override
  public void run() {
      try (
          BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
          PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
      ) {
          String inputLine;

          // Process client messages
          while ((inputLine = in.readLine()) != null) {
              System.out.println("Received: " + inputLine);
              String response = processMessage(inputLine);
              out.println(response);
          }
      } catch (IOException e) {
          System.err.println("Client communication error: " + e.getMessage());
      } finally {
          try {
              clientSocket.close();
          } catch (IOException e) {
              System.err.println("Error closing client socket: " + e.getMessage());
          }
      }
  }

  private String processMessage(String message) {
      // TODO: Parse the message and update nodes or send responses
      // Example: {"type":"set","nodeId":1,"command":"open_window"}
      // Implement your protocol here
      return "Acknowledged: " + message;
  }
}