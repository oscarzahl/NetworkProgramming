# Communication protocol

This document describes the protocol used for communication between the different nodes in a greenhouse.
Our application simulates a greenhouse with monitoring and control system using TCP connections.
It consists of multiple nodes representing sensors and actuators that communicate with a central server.
The nodes send data to the server that are then sent to controlpanel nodes that visualize the data and offer
control off the actuators.

## Terminology

- Node - Represents a client in the system. Nodes include sensors, actuators, sensoractuator and controlpanel nodes.
- Server - The central entity that coordinates the communication and manages connections between nodes.
- Client - Any nodes connected to the server.
- Sensor - A device which senses the environment and describes it with a integer value. Examples: temperature sensor, humidity sensor.
- Actuator - A device which can influence the environment based on control signals, such as turning on/off a fan.
- Sensor and actuator node - a computer which has direct access to a set of sensors, a set of
  actuators and is connected to the Internet.
- Control-panel node - a device connected to the Internet which visualizes status of sensor and
  actuator nodes and sends control commands to them.
- Graphical User Interface (GUI) - A graphical interface where users of the system can interact with
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

### The Server

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

### Sensor/Actuator node

- On startup:
  - Gets created when the Greenhouse simulator is started.
  - Establish a connection to the Server.
  - Sends initial node data to the server.
- Every 5 seconds send updated Sensor readings to the server.
- If it recieves updated actuator state from the server it updates the actuator.

### Control-Panel node

- On Startup:
  - Establish a connection to the server.
- Recieves message from server with Sensor/Actuator data.
- Visualizes the Sensor Readings and Actuator State.
- Updates Sensor readings when recieved by server
- Sends message to server if the User changes the actuator state in the Control Panel

## Connection and state

The protocol uses TCP connection which is connection oriented, The clients need to be connected to the server to be able
to send or recieve data from the server, the communication is stateful.

## Types, constants

The "State" of an actuator is held in the boolean state, being true/false.

## Message format

### Messages from SensorActuator Nodes

SensorActuator nodes can send the following messages:

#### Sensor Data Message

This message is sent periodically. The sensor node reports the current values (readings) for all the sensors it has.

- **Format**: `SENSOR:<nodeId>:<sensorType>=<sensorValue><unit>,...`
- **Example**: `SENSOR:1:temperature=23.00°C,humidity=70.55%`

- `<nodeId>` is the ID of the sensorActuator node.
- Multiple sensor readings can be reported, separated by commas
- `<sensorType>` is the type of the sensor. Examples: temperature, humidity.
- `<sensorValue>` is the reading of the sensor, a number.
- `<unit>` is the unit for the sensor value. Examples: °C, %.

### Actuator State Message

This message is sent periodically. The actuator node sends its current states.

- **Format**: `ACTUATOR:<nodeId>:<actuatorId>:<actuatorType>=<state>,...`
- **Example**: `ACTUATOR:1:2:fan=true`

- `<nodeId>` is the ID of the sensorActuator node.
- `<actuatorId>` is the ID of the actuator in the sensorActuator node.
- Multiple actuator states can be reported, separated by commas
- `<actuatorType>` is the type of the actuator. Examples: window, heater.
- `<state>` is the state of the actuator, a boolean.

#### Actuator Command Message

This message is sent by a control panel to the server whenever the control panel wants to change the state of a actuator in one of the sensorActuator nodes.

- **Format**: `ACTUATOR:<nodeId>:<actuatorId>:state`
- **Example**: `ACTUATOR:1:2:true`

Where:

- `<nodeId>` is the ID of the sensorActuator node.
- `<actuatorId>` is the ID of the actuator in the sensorActuator node.
- `<state>` is the state of the actuator, a boolean.

### Messages from the Server

The server forwards the following received messages in their original format:

- **Sensor Data Message**: Forwarded to all clients.
- **Actuator State Message**: Forwarded to all clients.
- **Actuator Command Message**: Forwarded to the necessary sensorActuator nodes.

### Error messages

The following error messages describe issues that can occur within the greenhouse system. These are derived from explicit checks and logging statements in the provided code.

#### Connection and Socket Errors

**Socket closed, stopping sensor data thread**

- **Origin**: Node.
- **Description**: Occurs when the socket is closed, either intentionally or due to an error.
- **Code Reference**: `GreenhouseNode` start method.
- **Handling**: The thread responsible for sending sensor and actuator data stops execution.

**Socket closed, stopping server response thread**

- **Origin**: Node.
- **Description**: Raised when the socket is closed while listening for server responses.
- **Code Reference**: `GreenhouseNode` start method.
- **Handling**: The thread responsible for receiving server messages stops execution.

**Socket closed, stopping listener thread**

- **Origin**: Server or Node.
- **Description**: Triggered when a socket connection is closed during server or client communication.
- **Code Reference**: `GreenhouseServer` listenToServer and `ClientHandler` run methods.
- **Handling**: The associated thread or handler cleans up resources and terminates.

#### Message Format Errors

**Unknown message type: <message>**

- **Origin**: Server.
- **Description**: Logged when a message does not start with a recognized prefix (e.g., SENSOR: or ACTUATOR:).
- **Code Reference**: `ClientHandler` handleMessage method.
- **Handling**: The message is ignored, and no further processing occurs.

**Invalid SENSOR message format: <message>**

- **Origin**: Server.
- **Description**: Raised when a SENSOR message does not match the expected format.
- **Code Reference**: `ClientHandler` handleSensorMessage method.
- **Handling**: The message is logged and discarded.

**Invalid ACTUATOR message format: <message>**

- **Origin**: Server.
- **Description**: Logged when an ACTUATOR message is malformed or missing required components.
- **Code Reference**: `ClientHandler` handleActuatorMessage method.
- **Handling**: The message is logged and ignored.

#### Actuator Errors

**Actuator not found: <actuatorId>**

- **Origin**: Node.
- **Description**: Occurs when a server command references a non-existent actuator on a node.
- **Code Reference**: `GreenhouseNode` handleActuatorStateChange method.
- **Handling**: The error is logged, and the command is ignored.

**Error processing actuator state change message: <message>**

- **Origin**: Node.
- **Description**: Raised when there is an issue parsing or handling an actuator state change message.
- **Code Reference**: `GreenhouseNode` handleActuatorStateChange method.
- **Handling**: The error and stack trace are logged for debugging purposes.
  If there is an unexpected message format it logs the issue as "invalid sensor/actuator data format" the message is ingored and is not sent.

## An example scenario

Scenario: The temperature in the greenhouse rises above a safe threshold

1. Sensor: Sends sensor readings data about temperature/humidity to the server
2. Server: The server processes the data and sends it to the control panel.
3. Control Panel: The Control panel recieves server message and updates the sensor readings.
4. User: The user sees that the current temperature is too high for the plants in the greenhouse.
         The User uses the control panel to turn on the Fans.
6. Server: Processes the message and sends tchange state to the actuator node.
7. Actuator: Recieves the command and activates the fan, which causes the tempature reading to drop.
8. Server: Sends updated sensor readings to control panel.
9. Control Panel: Updates the UI with sensor readings and actuator states.

## Reliability and security

Using TCP ensures reliable delivery of data, preventing packet loss from nodes to server and back.
If a node loses connection to the server the client is removed from the list of clients.
