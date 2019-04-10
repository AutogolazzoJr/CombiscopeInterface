package com.autogolazzojr.combiscope;

import java.io.IOException;

import com.fazecast.jSerialComm.SerialPort;

public class Combiscope {
	private SerialPort sp;
	private String ScopeID = "";
	private short[] previousWaveform = new short[32768];
	private static int frontPanelState = 0;

	/**
	 * Instantiates a Combiscope object used for interfacing with a Phillips/Fluke
	 * Combiscope. This is done using the jSerialComm library.
	 * 
	 * @param baudRate Serial baud rate (Often 9600)
	 * @param bits     serial bits (usually 8, sometimes 7)
	 * @param stopBits serial stop bits (usually 1)
	 * @param parity   serial parity (usually 0)
	 */
	public Combiscope(int baudRate, int bits, int stopBits, int parity) {
		sp = SerialPort.getCommPorts()[0];
		sp.openPort(1000, 4096, 80000);
		sp.setComPortParameters(baudRate, bits, stopBits, parity);
		sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
		this.updateInfo();
	}

	private boolean runCommand(byte[] command) {
		if (sp.bytesAvailable() > 0)
			sp.readBytes(new byte[sp.bytesAvailable()], sp.bytesAvailable());
		try {
			sp.getOutputStream().write(command);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		long start = System.nanoTime();
		while (sp.bytesAvailable() < 2) {
			if ((System.nanoTime() - start) / 1000000 >= 10000)
				return false;
		}
		return true;
	}

	private byte[] runReturningCommand(byte[] command) {
		if (sp.bytesAvailable() > 0)
			sp.readBytes(new byte[sp.bytesAvailable()], sp.bytesAvailable());
		try {
			sp.getOutputStream().write(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (sp.bytesAvailable() <= 3) {
		}
		int currentBytes;
		int lastBytes;
		do {
			lastBytes = sp.bytesAvailable();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			currentBytes = sp.bytesAvailable();
		} while (currentBytes > lastBytes);

		byte[] inputBuffer = new byte[sp.bytesAvailable()];
		sp.readBytes(inputBuffer, sp.bytesAvailable());// */
		return inputBuffer;
	}

	/**
	 * Uses the "ID" command to retrieve info about the oscilloscope.
	 * 
	 * @return A string consisting of info about the oscilloscope.
	 */
	public String toString() {
		return ScopeID;
	}

	/**
	 * Tells if the serial port is open or not.
	 *
	 * @return True if the port is open, and false if it is not.
	 */
	public boolean isOpen() {
		return sp.openPort(1000, 4096, 80000);
	}

	/**
	 * Updates oscilloscope identification information.
	 */
	public void updateInfo() {
		byte[] inputBytes = runReturningCommand(new byte[] { 'I', 'D', 0x0d });
		ScopeID = "";
		for (int i = 2; i < inputBytes.length; i++) {
			ScopeID += (char) inputBytes[i];
		}
	}

	/**
	 * Displays text on the oscilloscope screen.
	 * 
	 * @param input The string of text to be displayed.
	 */
	public void displayText(String input) {
		runCommand(new byte[] { 'P', 'T', 0x0d });
		byte[] text = new byte[input.length() + 1];
		for (int i = 0; i < input.length(); i++)
			text[i] = (byte) input.charAt(i);
		text[text.length - 1] = 0x0d;
		runCommand(text);
	}

	/**
	 * Clears any text on the screen.
	 */
	public void clearText() {
		runCommand(new byte[] { 'P', 'T', 0x0d });
		runCommand(new byte[] { 0x0d });
	}

	/**
	 * Runs the autoset function on the oscilloscope.
	 */
	public void autoSet() {
		runCommand(new byte[] { 'A', 'S', 0x0d });
	}

	/**
	 * Arms the oscilloscope trigger.
	 */
	public void armTrigger() {
		runCommand(new byte[] { 'A', 'T', 0x0d });
	}

	/**
	 * Returns the oscilloscope to the default setup.
	 */
	public void defaultSetup() {
		runCommand(new byte[] { 'D', 'S', 0x0d });
	}

	/**
	 * Resets all the software on the oscilloscope. Settings remain the same. The
	 * interface parameters aren't changed so the communication will stay alive.
	 * WARNING: I haven't tested this command. There is NO front panel equivalent
	 * for this command.
	 */
	public void resetInstrument() {
		runCommand(new byte[] { 'R', 'I', 0x0d });
	}

	/**
	 * Causes an acquisition or sweep to be started. If using single shot mode, use
	 * the armTrigger() method first.
	 */
	public void performSoftwareTrigger() {
		runCommand(new byte[] { 'T', 'A', 0x0d });
	}

	/**
	 * Changes the amount of control the user has at the front panel of the
	 * oscilloscope. Local mode allows full control of the scope from the front
	 * panel. Remote mode disables front panel control, but pressing STATUS/LOCAL
	 * will return the scope to local mode. Local Lockout disables STATUS/LOCAL,
	 * making it impossible to return the scope to local mode from the front panel
	 * besides powering off/on.
	 * 
	 * @param mode 0 - Local, 1 - Remote, 2 - Local Lockout.
	 */
	public void setFrontPanelMode(int mode) {
		switch (mode) {
		case 0:
			if (frontPanelState != 0) {
				runCommand(new byte[] { 'G', 'L', 0x0d });
				frontPanelState = 0;
			}
			break;
		case 1:
			if (frontPanelState == 0) {
				runCommand(new byte[] { 'G', 'R', 0x0d });
				frontPanelState = 1;
			}
			if (frontPanelState == 3) {
				runCommand(new byte[] { 'G', 'L', 0x0d });
				runCommand(new byte[] { 'G', 'R', 0x0d });
				frontPanelState = 1;
			}
			break;
		case 2:
			if (frontPanelState == 0) {
				runCommand(new byte[] { 'G', 'R', 0x0d });
				frontPanelState = 1;
			}
			if (frontPanelState == 1) {
				runCommand(new byte[] { 'L', 'L', 0x0d });
				frontPanelState = 2;
			}
			break;
		}
	}

	/**
	 * Returns an array of 16-bit signed integers representing a waveform. NOTE:
	 * Works on all acquisition lengths!
	 * 
	 * @param channel        Channel to read from.
	 * @param memoryRegister Memory register to read from. 0 will read from the
	 *                       acquisition memory.
	 * @return An array of 16-bit integers representing the waveform.
	 */
	public short[] getWaveform(int channel, int memoryRegister) {
		byte[] input = runReturningCommand(
				new byte[] { 'Q', 'W', (byte) (memoryRegister + 0x30), (byte) (channel + 0x30), 0x0d });
		int counter = 0;
		int startIndex = 0;
		int acquisitionLengthIndex = 0;
		for (int i = 0; i < input.length; i++) {
			if (input[i] == 0x2c) {
				counter++;
				if (counter == 16) {
					acquisitionLengthIndex = i;
				}
				if (counter == 17) {
					startIndex = i;
					break;
				}
			}
		}
		String acqString = "";
		for (int i = acquisitionLengthIndex + 1; i < startIndex; i++)
			acqString += (char) input[i];
		short[] samples = new short[(input.length - startIndex - 2) / 2];
		if (samples.length != Integer.parseInt(acqString)) {
			// System.out.println("Waveform acquisition failed. Displaying previous
			// acquisition.");
			return previousWaveform;
		}
		counter = 0;
		for (int i = 0; i < (input.length - startIndex - 2); i += 2) {
			samples[counter] = (short) ((short) (input[i + startIndex + 1] << 8) + input[i + 2 + startIndex]);
			counter++;
		}
		previousWaveform = samples;
		return samples;
	}

	/**
	 * Uses the QS command to retrieve the setup info of the oscilloscope. Note:
	 * User's manual states the purpose of the setup nodes: "Applications: The setup
	 * nodes for different timebase settings can be stored separately. They can be
	 * used afterwards as fixed ’templates’ to change only the oscilloscope timebase
	 * setup." Translation: "There's no way that we are going to write hundreds of
	 * pages on how to decode the setup info."
	 * 
	 * @return A byte array containing the setup info.
	 */
	public byte[] getSetup() {
		byte[] input = runReturningCommand(new byte[] { 'Q', 'S', 0x0d });
		byte[] setup = new byte[input.length - 2];
		for (int i = 0; i < input.length - 2; i++)
			setup[i] = input[i + 2];
		return setup;
	}

	/**
	 * Saves a setup to the oscilloscope.
	 * 
	 * @param input A byte array consisting of a setup. Get this array using the
	 *              getSetup() method.
	 */
	public void programSetup(byte[] input) {
		byte[] setup = new byte[input.length + 3];
		setup[0] = 'P';
		setup[1] = 'S';
		setup[2] = 0x2c;
		for (int i = 0; i < input.length; i++)
			setup[i + 3] = input[i];
		runCommand(setup);
	}
}