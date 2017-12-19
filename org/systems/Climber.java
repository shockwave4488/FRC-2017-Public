package org.usfirst.frc.team4488.robot.systems;

import org.usfirst.frc.team4488.robot.RobotMap;
import org.usfirst.frc.team4488.robot.operator.Logging;
import org.usfirst.frc.team4488.robot.operator.LoggingSystems;

import com.ctre.CANTalon;

import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.Timer;

public class Climber {

	private CANTalon climber;
	private PowerDistributionPanel pdp;
	private Preferences prefs;

	private boolean climb;
	private boolean softbypass;
	private boolean hardLimTripped;

	// @TODO Change to static final double after testing to find appropriate
	// value.
	private double softCurrentLimit;
	private static final double hardCurrentLimit = RobotMap.kClimberHardCurrentLimit;

	public Climber(PowerDistributionPanel pdpInstance) {
		climber = new CANTalon(RobotMap.ClimberMotor);
		pdp = pdpInstance;

		prefs = Preferences.getInstance();

		climb = false;
		softbypass = false;
		hardLimTripped = false;

		softCurrentLimit = prefs.getDouble("Climber Soft Current Limit", RobotMap.kClimberSoftCurrentLimit);

		climber.enableBrakeMode(true);
	}

	public void setClimbButton(boolean val) {
		climb = val;
	}

	public void setBypassButton(boolean val) {
		softbypass = val;
	}

	private void climberOn() {
		climber.set(-1.0);
	}

	private void climberOff() {
		climber.set(0.0);
	}

	public double getCurrent() {
		return pdp.getCurrent(RobotMap.ClimberMotorPDPChannel);
	}

	public void update() {

		if (getCurrent() >= hardCurrentLimit || hardLimTripped) {
			climberOff();
			climb = false;
			hardLimTripped = true;
		} else if (getCurrent() >= softCurrentLimit) {
			if (softbypass) {
				climberOn();
			} else {
				climberOff();
			}
		} else if (climb) {
			climberOn();
			Logging.logWriter(LoggingSystems.Climber, "Current: ", Double.toString(getCurrent()));

		} else if (!climb) {
			climberOff();
		}

	}
}
