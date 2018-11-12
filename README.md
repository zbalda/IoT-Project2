# IoT Project 2
CSCI 43300

Introduction to Internet of Things

Zachary Balda

Corey Stockton

## Project Goal
To learn how to connect a smart device to the Internet, to learn the principles of CoAP, and to learn how to use CoAP to develop IoT applications.

## Step 1: Setup

#### Installing Maven ####
`sudo apt install maven`

#### Downloading CoAP ####
`git clone https://github.com/eclipse/californium.git myCoAP`

#### Running CoAP Hello World Server ####
`cd myCoAP/demo-apps/cf-helloworld-server`

`sudo mvn clean install`

`cd myCoAP/demo-apps/cf-helloworld-server`

`java -jar cf-helloworld-server-1.0.0-SNAPSHOT.jar`

#### CoAP Client
For our CoAP Client we used [Copper for Chrome](https://github.com/mkovatsc/Copper4Cr). To install it we followed the steps listed in the repositories readme. Once it was installed, we tested it on the helloworld-server program:

## Step 2: Buttons and LEDs

#### Goal
The goal of Step 2 is to take sensors for things like temperature, humidity, etc. and connect them to the Raspberry Pi as “resources” of the CoAP server. This way a CoAP client can get the sensor readings from the server through a GET request.

#### Implementation
For implementing our CoAP server we simply modified the code in the myCoAP/demo-apps/cf-helloworld-server to include our sensor readings. For implementing the temperature sensor we used identical code to Project 1 Step 3 (excluding the code for LEDs). We used the pi4j.io.w1.W1Master class to get the temperature sensor device and the pi4j.component.temperature.TemperatureSensor class to get the temperature reading from the device. We provide temperature readings in Celsius at the /temperature endpoint. For the second sensor, we use the Humidity and Temperature sensor. We used the pi4j.wiringpi.Gpio class to get streaming data from the humidity and temperature sensor.

![Step2 Diagram](https://github.com/zbalda/IoT-Project2/blob/master/proj2_wiring_diagram.png)
 
#### Challenges
Since Maven handles its own dependencies we were unable to compile with Pi4J the way we did for Project 1. The Raspberry_Pi_Coap_tutorial.pdf showed how to include the pi4j-core 1.0 library but our code to did not compile even after adding this dependency. After trying to figure out why our program was not compiling for some time we figured out the M1Master and TemperatureSensor classes require pi4j-core, pi4j-device, and pi4j-gpio-extension, all at version 1.2.M1. We found these dependencies at https://search.maven.org/search?q=pi4j. Furthermore, sometimes the Humidity and Temperature sensor will not retrieve valid data. This might be because of a clock not syncing correctly every time.  

#### Outcome
After getting the program to compile with the correct dependencies our CoAP server ran and provided sensor reading. The /temperature endpoint provided the temperature reading in celsius and the humidity/temperature sensor provided their sensor readings a majority of the time. If the humidity/temperature sensor fail, it will output 0.0 as its sensor value. 

## Step 3: Wireshark

#### Goal
The goal for this section is to use the Wireshark software to view the CoAP packets that are being transferred between the client and the Raspberry Pi.

#### Analysis
Below is a snapshot of the packets to and from the Raspberry Pi. We observed that the CoAP packets are significantly smaller on average. Additionally, we observed the ACK, which contained the data after every request for data.

![Step2 Diagram](https://github.com/zbalda/IoT-Project2/blob/master/WireShark_Capture.PNG)

## Conclusion
In completing this project we learned how to create a CoAP server on the Raspberry Pi B+ model. Additionally, we learned how to use our personal computers as CoAP clients and retrieve data or sensor values via get requests as well as observe the requests being sent with Wireshark.
