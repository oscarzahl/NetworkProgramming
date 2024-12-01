# Communication protocol

This document describes the protocol used for communication between the different nodes in a greenhouse. 
Our application simulates a greenhouse with monitoring and control system using TCP connections.
It consists of multiple nodes representing sensors and actuators that communicate with a central server. 
The nodes send data to the server that are then sent to controlpanel nodes that visualize the data and offer
control off the actuators.

## Terminology
* Node - Represents a client in the system. Nodes include sensors, actuators, sensoractuator and controlpanel nodes.
* Server - The central entity that coordinates the communication and manages connections between nodes.
* Client - Any nodes connected to the server.
* Sensor - A device which senses the environment and describes it with a integer value. Examples: temperature sensor, humidity sensor.
* Actuator - A device which can influence the environment based on control signals, such as turning on/off a fan.
* Sensor and actuator node - a computer which has direct access to a set of sensors, a set of
  actuators and is connected to the Internet.
* Control-panel node - a device connected to the Internet which visualizes status of sensor and
  actuator nodes and sends control commands to them.
* Graphical User Interface (GUI) - A graphical interface where users of the system can interact with
  it.

## The underlying transport protocol

We use TCP(Transsmission Control Protocol) as the transport layer protocol because in a greenhouse we want
to ensure reliable and ordered delivery of data between nodes and the server.
We used port number 12345; This ensures that all nodes and the server communicate consistently on the same port.

## The architecture

The network architecture is as follows:

- Server: One server that accepts incoming connections. The server parses the message sent from sensor/actuator, and sends the data to the Control panel. The control panel sends a message to the server when an actuator state has changed, The server sends the actuator state to the Sensor/Actuator node.
- Sensor/Actuator: Nodes that establish a connection to the server. Sends the server sensor data and actuator status. It recieves control commands from the server.
- Control panel nodes: Nodes that establish a connection to the server. Visualizes the sensor data and actuator state sent from the server. Sends the server actuator changed state from the User.


## The flow of information and events

These events happen in the system:

#The Server
 - On startup: Listening to incoming tcp connections.
 - On a new connection: mark the connection as a Client.
   - Control Panel node Sensor/Actuator node are Clients.
   - When recieving message from the Clients:
     - If the message is a certain format, parses the message and broadcast it to all Clients.
     - If the message is from Sensor data it gets broadcasted to Control Panel.
     - If the message is from Control panel it sends the updated state to Sensor/Actuator nodes.
   - On closed connection:
     - If the connection is closed the client is removed from the server.
    - When incorrect message format is sent:
      - Unknown message type error message is sent.
     
#Sensor/Actuator node
  - On startup:
    - Gets created when the Greenhouse simulator is started.
    - Establish a connection to the Server.
    - Sends initial node data to the server.
  - Every 5 seconds send updated Sensor readings to the server.
  - If it recieves updated actuator state from the server it updates the actuator.

#Control-Panel node
  - On Startup:
    - Establish a connection to the server.
  - Recieves message from server with Sensor/Actuator data.
  - Visualizes the Sensor Readings and Actuator State.
  - Updates Sensor readings when recieved by server
  - Sends message to server if the User changes the actuator state in the Control Panel

## Connection and state

TODO - is your communication protocol connection-oriented or connection-less? Is it stateful or 
stateless? 

The protocol uses TCP connection which is connection oriented, The clients need to be connected to the server to be able 
to send or recieve data from the server, the communication is stateful. 


## Types, constants

TODO - Do you have some specific value types you use in several messages? They you can describe 
them here.

The "State" of an actuator is held in the boolean state, being true/false.

## Message format

TODO - describe the general format of all messages. Then describe specific format for each 
message type in your protocol.

Message format:
- Sensor Messages: SENSOR:<nodeId>:<sensorType>=<currentReading unit>
   Example: SENSOR:1:temperature=27.5°C,humidity=50%
- Actuator Messages: ACTUATOR:<nodeId>:<actuatorId>:<actuatorType>=<state>
   Example: ACTUATOR:2:3:fan=true,4:heater=false

  These messages are sent and parsed on the server where they are broadcasted to and visualized in the control panel. 

### Error messages

TODO - describe the possible error messages that nodes can send in your system.

If there is an unexpected message format it logs the issue as "invalid sensor/actuator data format" the message is ingored and is not sent.

## An example scenario

TODO - describe a typical scenario. How would it look like from communication perspective? When 
are connections established? Which packets are sent? How do nodes react on the packets? An 
example scenario could be as follows:
1. A sensor node with ID=1 is started. It has a temperature sensor, two humidity sensors. It can
   also open a window.
2. A sensor node with ID=2 is started. It has a single temperature sensor and can control two fans
   and a heater.
3. A control panel node is started.
4. Another control panel node is started.
5. A sensor node with ID=3 is started. It has a two temperature sensors and no actuators.
6. After 5 seconds all three sensor/actuator nodes broadcast their sensor data.
7. The user of the first-control panel presses on the button "ON" for the first fan of
   sensor/actuator node with ID=2.
8. The user of the second control-panel node presses on the button "turn off all actuators".


Scenario: The temperature in the greenhouse rises above a safe threshold
1. Sensor Node: Sends data temperature and other data to the server
2. Server: The server processes the data and sends it to the control panel.
3. User: The user sees the current temperature off the greenhouse and uses the control panel to turn on Fans
4. Control Panel: Sends which actuator on which node that has a changed state to the server.
5. Server: Processes the message and sends tchange state to the actuator node.
6. Actuator Node: Recieves the command and activates the fan.
7. Control Panel: Updates the UI with sensor readings and actuator states.

## Reliability and security

TODO - describe the reliability and security mechanisms your solution supports.

Using TCP ensures reliable delivery of data, preventing packet loss from nodes to server and back.
Nodes and Server 
