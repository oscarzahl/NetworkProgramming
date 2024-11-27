package no.ntnu.TCP;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.net.Socket;
import java.util.Map;

import no.ntnu.greenhouse.SensorActuatorNode;

public class TCPServer implements Runnable {

    private final int port;
    private final Map<Integer,SensorActuatorNode> nodes;
    
    public TCPServer(int port, Map<Integer,SensorActuatorNode> nodes){
        this.port = port;
        this.nodes = nodes;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);

            while (true) {
                // Accept an incoming connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Handle client in a separate thread
                new Thread(new ClientHandler(clientSocket, nodes)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }
}