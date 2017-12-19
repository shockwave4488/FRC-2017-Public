package org.usfirst.frc.team4488.robot.components;

import org.usfirst.frc.team4488.robot.RobotMap;

import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.Timer;

public class Agitator {
	private Solenoid agitator;
	private Timer timer;
	private boolean button;
	private Thread thread;

	public Agitator() {
		agitator = new Solenoid(RobotMap.AgitatorSolenoid);
		timer = new Timer();
		button = false;
	}

	public void setAgitateButton(boolean val) {
		button = val;
	}

	public void agitate(double rateSec) {
		if (button && (thread == null || !thread.isAlive())) {
			thread = new Thread(() -> {
				timer.start();
				while (timer.get() < rateSec) {
					agitator.set(true);
				}
				timer.stop();
				timer.reset();
				timer.start();
				while (timer.get() < rateSec) {
					agitator.set(false);
				}
				timer.stop();
				timer.reset();
			});
			thread.start();
		}
	}
	
	public Solenoid getPiston(){
		return agitator;
	}

}
