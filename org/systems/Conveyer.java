package org.usfirst.frc.team4488.robot.systems;

import org.usfirst.frc.team4488.robot.RobotMap;

import com.ctre.CANTalon;

import JavaRoboticsLib.ControlSystems.SetPointProfile;
import JavaRoboticsLib.ControlSystems.SimPID;
import edu.wpi.first.wpilibj.Counter;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.Utility;

public class Conveyer {

	private CANTalon motor;

	private SimPID m_pid;
	private Preferences prefs;

	private double m_oldPosition;
	private double m_oldTime;
	private double m_rateBuffer;

	private double m_power;

	private Counter counter;
	private double m_tolerance;
	private double cycleCount;
	private double minCycleCount;

	public Conveyer() {
		prefs = Preferences.getInstance();
		motor = new CANTalon(RobotMap.ConveyorBeltMotor);
		motor.setInverted(true);
		counter = new Counter(RobotMap.ConveyerCounter);
		counter.setDistancePerPulse(1.0 / 1024.0);
		m_power = .5;
		m_tolerance = prefs.getDouble("ConveyerTolerance", 100);
		cycleCount = 0; // amount of debounce cycles
		minCycleCount = 20; // minimum amount of debounce cycles
		try {
			m_pid = new SimPID(prefs.getDouble("ConveyerP", 0), prefs.getDouble("ConveyerI", 0),
					prefs.getDouble("ConveyerD", 0), prefs.getDouble("ConveyerEps", 0));
			m_pid.setDoneRange(m_tolerance);
		} catch (Exception e) {
			System.out.println("Warning: Conveyer PID init failed");
		}

		m_oldTime = Utility.getFPGATime();
		m_pid.setDesiredValue(RobotMap.kConveyerRPM);
	}

	public void updateSpeed() {
		double distance = counter.getDistance();
		double time = (double) Utility.getFPGATime() / 1000000.0;
		double dx = distance - m_oldPosition;
		double dt = time - m_oldTime;
		m_oldTime = time;
		m_oldPosition = distance;
		double rate = dx / dt; // rotations per second
		m_rateBuffer = rate * 60;
	}

	/**
	 * Gets the set desired speed value.
	 * 
	 * @return The set desired speed, in RPM.
	 */
	public double getDesRPM() {
		return m_pid.getDesiredVal();
	}

	/**
	 * Gets the current speed.
	 * 
	 * @return The current calculated speed from updateSpeed(), in RPM.
	 */
	public double getspeed() {
		return m_rateBuffer;
	}

	/**
	 * Stops the flywheel.
	 */
	public void Stop() {
		motor.set(0.0);
	}

	public void Shoot() {
		updateSpeed();
		double power = m_pid.calcPID(getspeed());

		m_power = m_power + .015 * power;

		if (power > 1) {
			m_power = 1;
		}
		motor.set(m_power);
	}
}
