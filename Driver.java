package com.autogolazzojr.combiscope;

import java.awt.*;
import java.util.Scanner;

import javax.swing.JFrame;

public class Driver extends Canvas {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static Combiscope pm3394;
	static final int vertMag = 4;
	static final double horizMag = 4;
	static int acqLength;
	static int channel;
	static int register;

	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		System.out.println("What channel should I read from?");
		channel = input.nextInt();
		System.out.println("What memory register should I read from?");
		register = input.nextInt();
		System.out.println("What is the acquisition length?");
		acqLength = input.nextInt();
		input.close();
		pm3394 = new Combiscope(19200, 8, 1, 0);
		System.out.println(pm3394.isOpen() ? "The scope is online!" : "The scope is not online.");
		System.out.println("Scope ID: " + pm3394);
		pm3394.displayText(
				"AutogolazzoJr's Fluke CombiscopeJava-powered interface! " + (char) 25 + (char) 127 + (char) 19);

		// short[] points = pm3394.getWaveform(1, 0);
		// for (int i = 0; i < points.length; i++) {
		// int one = (points[i] >> 8) & 0xff;
		// if ((one & 0xf0) == 0)
		// System.out.print("0");
		// System.out.print(Integer.toHexString(one));
		// int two = points[i] & 0xff;
		// if ((two & 0xf0) == 0)
		// System.out.print("0");
		// System.out.print(Integer.toHexString(two));
		// }

		JFrame frame = new JFrame("Point Plotter");
		Canvas canvas = new Driver();
		canvas.setSize((int) Math.round(acqLength * horizMag), 255 * vertMag);
		frame.add(canvas);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	/*public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		while (true) {
			short[] points = pm3394.getWaveform(channel, register);
			g2d.setColor(Color.BLACK);
			g2d.fillRect(0, 0, (int) Math.round(acqLength * horizMag), 255 * vertMag);
			g2d.setColor(Color.GREEN);
			for (int i = 0; i < acqLength - 1; i++) {
				g2d.drawLine((int) Math.round(i * horizMag), -points[i] / (255 / vertMag) + 127 * vertMag,
						(int) Math.round((i + 1) * horizMag), -points[i + 1] / (255 / vertMag) + 127 * vertMag);
			}
			while (true) {
			}
		}
	}*/
	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		while (true) {
			short[] points = pm3394.getWaveform(channel, register);
			g2d.setColor(Color.BLACK);
			g2d.fillRect(0, 0, (int) Math.round(acqLength * horizMag), 255 * vertMag);
			g2d.setColor(Color.GREEN);
			for (int i = 0; i < acqLength - 1; i++) {
				g2d.drawLine((int) Math.round(i * horizMag), -points[i] / (255 / vertMag) + 127 * vertMag,
						(int) Math.round((i + 1) * horizMag), -points[i + 1] / (255 / vertMag) + 127 * vertMag);
			}
			/*points = pm3394.getWaveform(2, 3);
			g2d.setColor(Color.RED);
			for (int i = 0; i < acqLength - 1; i++) {
				g2d.drawLine((int) Math.round(i * horizMag), -points[i] / (255 / vertMag) + 127 * vertMag,
						(int) Math.round((i + 1) * horizMag), -points[i + 1] / (255 / vertMag) + 127 * vertMag);
			}*/
			//while (true) {
			//}
		}
	}
}
