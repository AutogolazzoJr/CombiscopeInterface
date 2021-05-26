import java.io.IOException;
import com.fazecast.jSerialComm.SerialPort;

public class Combiscope {
	private SerialPort sp;
	private String ScopeID = "";
	private short[] previousWaveform = new short[32768];
	private int frontPanelState = 0;
	private int baudRate;
	private int bits;
	private int stopBits;
	private int parity;
	public byte[] traceName;
	public byte[] yUnit;
	public byte[] xUnit;
	public byte[] yZero;
	public byte[] xZero;
	public byte[] yResolution;
	public byte[] xResolution;
	public byte[] yRange;
	public byte[] date;
	public byte[] time;
	public byte[] dtCorr;
	public byte[] minMax;
	public byte[] multiShotTot;
	public byte[] multiShotNr;
	//public byte[] r1;
	//public byte[] r2;

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
		this.baudRate = baudRate;
		this.bits = bits;
		this.stopBits = stopBits;
		this.parity = parity;
		/*-
		SerialPort[] ports = SerialPort.getCommPorts();
		sp = ports[ports.length - 1];
		sp.openPort(1000, 4096, 80000);
		sp.setComPortParameters(baudRate, bits, stopBits, parity);
		sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
		this.updateInfo();
		*/
	}

	/**
	 * Returns the available COM ports. Available ports may change, so it is
	 * recommended that this method is called right before opening a port.
	 * 
	 * @return a SerialPort array consisting of available COM ports.
	 */
	public static SerialPort[] getPorts() {
		return SerialPort.getCommPorts();
	}

	/**
	 * Connects to the Combiscope via the port at the specified index. The index can
	 * and should be found with getPorts().
	 * 
	 * @param index the index of the COM port.
	 */
	public void openPort(int index) {
		sp = getPorts()[index];
		sp.openPort(1000, 4096, 80000);
		sp.setComPortParameters(baudRate, bits, stopBits, parity);
		sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
		this.updateInfo();
	}

	/**
	 * Disconnects from the Combiscope, and closes the serial port.
	 */
	public void closePort() {
		if (sp == null)
			return;
		sp.closePort();
	}

	/**
	 * Instantiates a Combiscope object used for interfacing with a Phillips/Fluke
	 * Combiscope. This is done using the jSerialComm library. The Combiscope is set
	 * up with a baud rate of 9600, 8 serial bits, 1 stop bit, and a parity of 0.
	 */
	public Combiscope() {
		this(9600);
	}

	/**
	 * Instantiates a Combiscope object used for interfacing with a Phillips/Fluke
	 * Combiscope. This is done using the jSerialComm library. The Combiscope is set
	 * up with 8 serial bits, 1 stop bit, and a parity of 0.
	 * 
	 * @param baudRate Serial baud rate (Often 9600)
	 */
	public Combiscope(int baudRate) {
		this(baudRate, 8, 1, 0);
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
			if ((System.nanoTime() - start) / 1000000 >= 5000)
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
		long start = System.nanoTime();
		while (sp.bytesAvailable() <= 3) {
			if ((System.nanoTime() - start) / 1000000 >= 5000)
				return new byte[] {};
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
		sp.readBytes(inputBuffer, sp.bytesAvailable());
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
		if (sp == null)
			return false;
		return sp.isOpen();
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
	 * WARNING: I haven't tested this. There is NO front panel equivalent
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
	 * Returns an array of 16-bit signed integers representing a waveform. Also
	 * updates information about the waveform. NOTE: Works on all acquisition
	 * lengths!
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
		int lastComma = -1;
		for (int i = 0; i < input.length; i++) {
			if (input[i] == 0x2c) {
				counter++;
				switch (counter) {
				case 1:
					traceName = new byte[i - lastComma - 1 - 2];
					for (int k = 0; k < traceName.length; k++) {
						traceName[k] = input[k + lastComma + 1 + 2];
					}
					lastComma = i;
					break;
				case 2:
					yUnit = new byte[i - lastComma - 1];
					for (int k = 0; k < yUnit.length; k++) {
						yUnit[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 3:
					xUnit = new byte[i - lastComma - 1];
					for (int k = 0; k < xUnit.length; k++) {
						xUnit[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 4:
					yZero = new byte[i - lastComma - 1];
					for (int k = 0; k < yZero.length; k++) {
						yZero[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 5:
					xZero = new byte[i - lastComma - 1];
					for (int k = 0; k < xZero.length; k++) {
						xZero[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 6:
					yResolution = new byte[i - lastComma - 1];
					for (int k = 0; k < yResolution.length; k++) {
						yResolution[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 7:
					xResolution = new byte[i - lastComma - 1];
					for (int k = 0; k < xResolution.length; k++) {
						xResolution[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 8:
					yRange = new byte[i - lastComma - 1];
					for (int k = 0; k < yRange.length; k++) {
						yRange[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 9:
					date = new byte[i - lastComma - 1];
					for (int k = 0; k < date.length; k++) {
						date[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 10:
					time = new byte[i - lastComma - 1];
					for (int k = 0; k < time.length; k++) {
						time[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 11:
					dtCorr = new byte[i - lastComma - 1];
					for (int k = 0; k < dtCorr.length; k++) {
						dtCorr[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 12:
					minMax = new byte[i - lastComma - 1];
					for (int k = 0; k < minMax.length; k++) {
						minMax[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 13:
					multiShotTot = new byte[i - lastComma - 1];
					for (int k = 0; k < multiShotTot.length; k++) {
						multiShotTot[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 14:
					multiShotNr = new byte[i - lastComma - 1];
					for (int k = 0; k < multiShotNr.length; k++) {
						multiShotNr[k] = input[k + lastComma + 1];
					}
					lastComma = i;
					break;
				case 16:
					acquisitionLengthIndex = i;
					break;
				case 17:
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

	public String getTraceName() {
		String output = "";
		for (int i = 0; i < traceName.length; i++)
			output += (char) traceName[i];
		return output;
	}

	public String getYUnit() {
		String output = "";
		for (int i = 0; i < yUnit.length; i++)
			output += (char) yUnit[i];
		return output;
	}
}