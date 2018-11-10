/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 *    Kai Hudalla (Bosch Software Innovations GmbH) - add endpoints for all IP addresses
 ******************************************************************************/
//package gpio;
package org.eclipse.californium.examples;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;

// temperature sensor
import com.pi4j.io.gpio.*;
import com.pi4j.component.temperature.TemperatureSensor;
import com.pi4j.io.w1.W1Master;
import com.pi4j.temperature.TemperatureScale;

// temp/hum sensor
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;


public class HelloWorldServer extends CoapServer {
	
	// GPIO variables
	public static GpioController gpio;

	// Sensor class
	public class DHT11 {
		private static final int MAXTIMINGS = 85;
		private final int[] dht11_dat = {0, 0, 0, 0, 0};
		
		public DHT11() {
			//setup wiringpi
			if (Gpio.wiringPiSetup() == -1) {
				System.out.println(" ==> GPIO SETUP FAILED");
				return;
			}
			GpioUtil.export(3, GpioUtil.DIRECTION_OUT);
		}
		
		public float getTemperature(final int pin, int sense) {
			int laststate = Gpio.HIGH;
			int j = 0;
			dht11_dat[0] = dht11_dat[1] = dht11_dat[2] = dht11_dat[3] = dht11_dat[4] = 0;
			
			Gpio.pinMode(pin, Gpio.OUTPUT);
			Gpio.digitalWrite(pin, Gpio.LOW);
			Gpio.delay(20);
			
			Gpio.digitalWrite(pin, Gpio.HIGH);
			Gpio.pinMode(pin, Gpio.INPUT);
			
			for (int i = 0; i < MAXTIMINGS; i++) {
				int counter = 0;
				while (Gpio.digitalRead(pin) == laststate) {
					counter++;
					Gpio.delayMicroseconds(1);
					if (counter == 255) {
						break;
					}
				}
				laststate = Gpio.digitalRead(pin);
				if(counter == 255) {
					break;
				}
				// ignore first 3 transitions
				if(i >= 4 && i % 2 == 0) {
					//shove each bit into the storage bytes
					dht11_dat[j / 8] <<= 1;
					if (counter > 16) {
						dht11_dat[j / 8] |= 1;
					}
					j++;
				}
			}
			//check we read 40 bits and verify checksum in the last byte
			if (j >= 40 && checkParity()) {
				float h = (float) ((dht11_dat[0] << 8) + dht11_dat[1]) / 10;
				if (h > 100) {
					h = dht11_dat[0]; // for DHT11
				}
				float c = (float) (((dht11_dat[2] & 0x7F) << 8) + dht11_dat[3]) / 10;
				if (c > 125) {
					c = dht11_dat[2]; // for DHT11
				}
				if ((dht11_dat[2] & 0x80) != 0) {
					c = -c;
				}
				final float f = c * 1.8f + 32;
				System.out.println("Humidity = " + h + " Temperature = " + c + "(" + f + "f)");
				if (sense == 0) {
					return h;
				}
				else if (sense == 1) {
					return f;
				}
				else {
					return 0;
				}
			}
			else {
				System.out.println("Data not good, skip");
				System.out.println("j: " + j);
				checkParity();
				return 0;
			}
		}
		
		private boolean checkParity() {
			System.out.println(dht11_dat[4]);
			System.out.println(dht11_dat[0] + dht11_dat[1] + dht11_dat[2] + dht11_dat[3] & 0xFF);
			return dht11_dat[4] == (dht11_dat[0] + dht11_dat[1] + dht11_dat[2] + dht11_dat[3] & 0xFF);
		}
	}

	// Sensor variables
	public static W1Master w1Master;
	public static TemperatureSensor tempSensor;
	public static DHT11 dhtSensor;
	
	private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
    /*
     * Application entry point.
     */
    public static void main(String[] args) {
        
        try {

            // create server
            HelloWorldServer server = new HelloWorldServer();
            // add endpoints on all IP addresses
            server.addEndpoints();
            server.start();

        } catch (SocketException e) {
            System.err.println("Failed to initialize server: " + e.getMessage());
        }
    }

    /**
     * Add individual endpoints listening on default CoAP port on all IPv4 addresses of all network interfaces.
     */
    private void addEndpoints() {
    	for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
    		// only binds to IPv4 addresses and localhost
			if (addr instanceof Inet4Address || addr.isLoopbackAddress()) {
				InetSocketAddress bindToAddress = new InetSocketAddress(addr, COAP_PORT);
				addEndpoint(new CoapEndpoint(bindToAddress));
			}
		}
    }

    /*
     * Constructor for a new Hello-World server. Here, the resources
     * of the server are initialized.
     */
    public HelloWorldServer() throws SocketException {
		
		// create gpio controller
		gpio = GpioFactory.getInstance();

		// for getting sensor device
		w1Master = new W1Master();
		dhtSensor = new DHT11();
		
		// provision temperature sensor
		for(TemperatureSensor device : w1Master.getDevices(TemperatureSensor.class)){
			if(device.getName().contains("28-0000075565ad")){
				tempSensor = device;
			}
		}
        
        // provide an instance of the Temperature1 resource
        add(new Temperature1Resource());
        
        // provide an instance of the Humidity resource
        add(new HumidityResource());
        
        // provide an instance of the Temperature2 resource
        add(new Temperature2Resource());
    }

    /*
     * Definition of the Temperature1 Resource
     */
    class Temperature1Resource extends CoapResource {
        
        public Temperature1Resource() {
            
            // set resource identifier
            super("temperature");
            
            // set display name
            getAttributes().setTitle("Temperature Resource");
        }

        @Override
        public void handleGET(CoapExchange exchange) {
			
			double temp = tempSensor.getTemperature(TemperatureScale.CELSIUS);
			String tempstr = String.valueOf(temp);
			System.out.println("Temperature1: " + tempstr);
			
            // respond to the request
            exchange.respond(tempstr);
        }
    }
    
    /*
     * Definition of the Second Resource
     */
    class HumidityResource extends CoapResource {
        
        public HumidityResource() {
            
            // set resource identifier
            super("humidity");
            
            // set display name
            getAttributes().setTitle("Humidity Resource");
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            
            float hum_data = dhtSensor.getTemperature(21, 0);
            
            String hum_dataStr = String.valueOf(hum_data);
            
            System.out.println("Humility: " + hum_dataStr);
            
            // respond to the request
            exchange.respond(hum_dataStr);
        }
    }
    
    /*
     * Definition of the Temperature2 Resource
     */
    class Temperature2Resource extends CoapResource {
        
        public Temperature2Resource() {
            
            // set resource identifier
            super("temperature2");
            
            // set display name
            getAttributes().setTitle("Temperature2 Resource");
        }

        @Override
        public void handleGET(CoapExchange exchange) {
			
			float temp_data = dhtSensor.getTemperature(21, 1);
			String temp_dataStr = String.valueOf(temp_data);
			System.out.println("Temperature2: " + temp_dataStr);
			
            // respond to the request
            exchange.respond(temp_dataStr);
        }
    }
}
