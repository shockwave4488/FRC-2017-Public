package org.usfirst.frc.team4488.robot.components;

import org.usfirst.frc.team4488.robot.RobotMap;

import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.Timer;

public class SolenoidLED {
	private Solenoid led;
	private Timer timer;
	private Thread thread;

	public SolenoidLED() {
		led = new Solenoid(RobotMap.SolenoidLED);
		timer = new Timer();
	}

	public void off() {
		led.set(false);
	}

	public void blink() {
		if (thread == null || !thread.isAlive()) {
			thread = new Thread(() -> {
				timer.start();
				while (timer.get() < .275) {
					led.set(true);
				}
				timer.stop();
				timer.reset();
				timer.start();
				while (timer.get() < .275) {
					led.set(false);
				}
				timer.start();
				timer.reset();
			});
			thread.start();
		}
	}
}
