package org.usfirst.frc.team4488.robot.systems;

import org.usfirst.frc.team4488.robot.RobotMap;
import org.usfirst.frc.team4488.robot.components.LEDController;

import com.ctre.CANTalon;

import JavaRoboticsLib.FlowControl.Toggle;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.Timer;

public class Intake {

	private CANTalon roller;
	private Solenoid arm;
	private DigitalInput bb;

	private boolean armbool;
	private boolean rollerbool;
	private boolean dejam;
	private boolean armForceUp;
	private boolean rollerForce;
	private boolean score;
	private boolean armForceDown;
	private boolean armState;
	private boolean reset;
	private double driveCount;
	private boolean rollerState;
	private boolean threadbool;

	private IntakeState state;

	private Timer timer;

	private Drive drive;

	private Thread thread;
	
	private LEDController led;
	private boolean ledbool;

	public Intake(Drive driveInstance, LEDController ledInstance) {
		roller = new CANTalon(RobotMap.IntakeMotor);
		arm = new Solenoid(RobotMap.IntakeArmSolenoid);
		bb = new DigitalInput(RobotMap.FloorGearBB);

		armbool = false;
		rollerbool = false;
		dejam = false;
		armForceUp = false;
		rollerForce = false;
		score = false;
		armForceDown = false;
		armState = false;
		reset = false;
		rollerState = false;
		threadbool = false;

		state = IntakeState.Manual;

		timer = new Timer();

		drive = driveInstance;
		led = ledInstance;
		ledbool = false;
	}

	public boolean isGear() {
		return !bb.get();
	}

	public boolean getArmState() {
		return armState;
	}

	public IntakeState getState() {
		return state;
	}

	public void setArmButton(boolean val) {
		armbool = val;
	}

	public void setRollerButton(boolean val) {
		rollerbool = val;
	}

	public void setDeJamButton(boolean val) {
		dejam = val;
	}

	public void setScoreButton(boolean val) {
		score = val;
	}

	public void setResetButton(boolean val) {
		reset = val;
	}

	public void forceArmUp(boolean val) {
		armForceUp = val;
	}

	public void forceArmDown(boolean val) {
		armForceDown = val;
	}

	public void forceRollerOn(boolean val) {
		rollerForce = val;
	}

	private void armDown() {
		arm.set(true);
	}

	private void armUp() {
		arm.set(false);
	}

	private void rollerIntake_helpGear() {
		roller.set(0.75);
	}

	private void rollerOff() {
		roller.set(0.0);
	}

	private void rollerDeJam() {
		roller.set(-0.7);
	}

	private void rollerScore() {
		roller.set(-1.0);
	}
	
	private void rollerGear() {
		roller.set(.4);
	}

	public void update() {
		if (reset) {
			state = IntakeState.Manual;
		}

		switch (state) {

		case Manual:
			if (dejam) {
				rollerDeJam();
				rollerState = false;
			} else if ((rollerbool || rollerForce) && !armState) {
				rollerIntake_helpGear();
				rollerState = true;
			} else if ((rollerbool || rollerForce) && armState) {
				rollerGear();
				rollerState = true;
			} else {
				rollerOff();
				rollerState = false;
			}

			if (armForceUp) {
				armUp();
				armState = false;
			} else if (armbool || armForceDown) {
				armDown();
				armState = true;
			} else {
				armUp();
				armState = false;
			}
			
			threadbool = false;

			if (isGear() && armState && rollerState) {
				timer.start();
				while (timer.get() < .2) {
					armDown();
					rollerIntake_helpGear();
				}
				if (!ledbool) {
					led.blinkLight(75);
					ledbool = true;
				}
				timer.stop();
				timer.reset();
				state = IntakeState.HasGear;
			}
			break;

		case HasGear:
			armState = true;
			ledbool = false;
			armUp();
			rollerOff();
			threadbool = false;
			if (score) {
				state = IntakeState.Score;
			}
			break;

		case Score:
			driveCount = drive.getLinearDistance();
			ledbool = false;
			if (!threadbool) {
				Score();
				threadbool = true;
			}
			break;

		case Scored:
			armState = true;
			ledbool = false;
			rollerOff();
			armUp();
			threadbool = false;
			if (drive.getLinearDistance() <= driveCount - 6) {
				threadbool = false;
				rollerState = false;
				armState = false;
				ledbool = false;
				state = IntakeState.Manual;
			}
		}
	}

	private void Score() {
		if (thread == null || !thread.isAlive()) {
			thread = new Thread(() -> {
				armState = true;
				timer.start();
				armDown();
				while (timer.get() < .15) {
					armDown();
				}
				armDown();
				timer.stop();
				timer.reset();
				rollerScore();
				timer.start();
				while (timer.get() < .45) {
					rollerScore();
				}
				rollerScore();
				timer.stop();
				timer.reset();
				armUp();
				timer.start();
				while (timer.get() < .75) {
					armUp();
				}
				armUp();
				state = IntakeState.Scored;
			});
			thread.start();
		}
	}
}
