package org.usfirst.frc.team4488.robot.systems;

import edu.wpi.first.wpilibj.Counter;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.Utility;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import org.usfirst.frc.team4488.robot.RobotMap;

import com.ctre.CANTalon;
import com.ctre.CANTalon.TalonControlMode;

import JavaRoboticsLib.ControlSystems.SetPointProfile;
import JavaRoboticsLib.ControlSystems.SimPID;

public class Shooter {
	private CANTalon m_flyWheelMotor;
	private CANTalon m_flyWheelSlave;

	private SimPID m_pid;
	private Preferences prefs;

	private double m_oldPosition;
	private double m_oldTime;
	private double m_rateBuffer;

	private double m_power;

	private SetPointProfile rpmsInterpolTable;
	private Counter counter;
	private double m_rpm;
	private double m_tolerance;
	private double cycleCount;
	private double minCycleCount;

	/**
	 * Initializes the motor, encoder, PID, and Set Point Profile.
	 */
	public Shooter() {
		prefs = Preferences.getInstance();
		m_flyWheelMotor = new CANTalon(RobotMap.FlyWheelMotor);
		m_flyWheelMotor.setInverted(true);
		m_flyWheelMotor.enableBrakeMode(false);
		m_flyWheelSlave = new CANTalon(RobotMap.FlyWheelSlaveMotor);
		m_flyWheelSlave.enableBrakeMode(false);
		m_flyWheelSlave.changeControlMode(TalonControlMode.Follower);
		m_flyWheelSlave.set(RobotMap.FlyWheelMotor);
		counter = new Counter(RobotMap.FlyWheelCounter);
		counter.setDistancePerPulse(1.0 / 256.0);
		m_power = .5;
		m_tolerance = prefs.getDouble("ShooterTolerance", 100);
		cycleCount = 0; // amount of debounce cycles
		minCycleCount = 20; // minimum amount of debounce cycles

		try {
			m_pid = new SimPID(prefs.getDouble("ShooterP", 0), prefs.getDouble("ShooterI", 0),
					prefs.getDouble("ShooterD", 0), prefs.getDouble("ShooterEps", 0));
			m_pid.setDoneRange(m_tolerance);
		} catch (Exception e) {
			System.out.println("Warning: ShooterWheel PID init failed");
		}

		m_oldTime = Utility.getFPGATime();

		rpmsInterpolTable = new SetPointProfile();
		// Range(inches), RPM
		/*
		 * rpmsInterpolTable.add(90, 3060.0); rpmsInterpolTable.add(100.88,
		 * 3135.0); rpmsInterpolTable.add(120, 3320.0);
		 */
		rpmsInterpolTable.add(70.0, 2825.0);
		rpmsInterpolTable.add(75.0, 2885.0);
		rpmsInterpolTable.add(80.0, 2950.0);
		rpmsInterpolTable.add(86.5, 3075.0);
		rpmsInterpolTable.add(90.0, 3125.0);
		rpmsInterpolTable.add(96.0, 3160.0);
		rpmsInterpolTable.add(100.0, 3160.0);
		rpmsInterpolTable.add(110.0, 3275.0);
		rpmsInterpolTable.add(115.0, 3300.0);
		rpmsInterpolTable.add(120.0, 3320.0);

	}

	/**
	 * Calculates the current speed of the flywheel, in RPM.
	 */
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

	public double getTicks() {
		return counter.getDistance();
	}

	/**
	 * Sets the desired RPM for the system to go to.
	 * 
	 * @param rpm
	 *            The desired speed, in RPM.
	 */
	public void setRPM(double rpm) {
		m_rpm = rpm;
		m_pid.setDesiredValue(rpm);
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
	 * Gets the curret speed.
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
		m_flyWheelMotor.set(0.0);
	}

	/**
	 * Runs the current speed though the PID and increments the power according
	 * to the desired PID value.
	 */
	public void Spin() {
		updateSpeed();
		double power = m_pid.calcPID(getspeed());

		m_power = m_power + .015 * power;

		if (power > 1) {
			m_power = 1;
		}
		m_flyWheelMotor.set(-m_power);

	}

	/**
	 * Takes the range variable from the camera code and runs it through a
	 * user-defined setpoint profile to get an appropriate RPM, and then sets
	 * the wheels to fire at that RPM.
	 */
	public void setDistance() {
		double range = SmartDashboard.getNumber("BoilerRange", 0);
		if (range >= 66) {
			double rpm = Math.abs(rpmsInterpolTable.get(range));
			setRPM(rpm);
		} else
			setRPM(3000);
	}

	public void ShootByPower(double power) {
		m_flyWheelMotor.set(-power);
	}

	public boolean atSpeed() {
		double currSpeed = getspeed();

		// check if close enough to target
		if ((currSpeed <= this.m_rpm + this.m_tolerance) && (currSpeed >= this.m_rpm - this.m_tolerance)) {
			if (this.cycleCount <= this.minCycleCount) {
				this.cycleCount++;
			}
		}
		// not close enough to target
		else {
			this.cycleCount = 0;
		}

		return this.cycleCount > this.minCycleCount;
	}
}