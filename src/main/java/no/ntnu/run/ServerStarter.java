package no.ntnu.run;

import no.ntnu.tcp.GreenhouseServer;

public class ServerStarter {
    public static void main(String[] args) {
        int port = 12345; // Velg port for serveren
        GreenhouseServer server = new GreenhouseServer(port);
        server.start();
    }
}
