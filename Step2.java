import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.util.CommandArgumentParser;

import com.pi4j.component.temperature.TemperatureSensor;
import com.pi4j.io.w1.W1Master;
import com.pi4j.temperature.TemperatureScale;

public class Step3 {

	// GPIO variables
	public static GpioController gpio;

	// Temperature Sensor Variables
	public static W1Master w1Master;
	public static TemperatureSensor sensor;
	public static double tempCelsius;

	public Step3() {
		// create gpio controller
		gpio = GpioFactory.getInstance();

		// for getting sensor device
		w1Master = new W1Master();

		// provision temperature sensor
		for(TemperatureSensor device : w1Master.getDevices(TemperatureSensor.class)){
			if(device.getName().contains("28-0000075565ad")){
				sensor = device;
			}
		}
	}

	// updates tempCelsius from temperature sensor
	public static void updateTemp(){
		tempCelsius = sensor.getTemperature(TemperatureScale.CELSIUS);
		System.out.printf("%-20s %3.1f°C\n", sensor.getName(), sensor.getTemperature(TemperatureScale.CELSIUS));
	}

	public static void main(String args[]) throws InterruptedException {

		new Step3();

		// keep program running until user aborts (CTRL-C)
		while(true) {
			updateTemp();
			Thread.sleep(100);
		}

		// stop all GPIO activity/threads by shutting down the GPIO controller
		// (this method will forcefully shutdown all GPIO monitoring threads and scheduled tasks)
		// gpio.shutdown();   <--- implem
