package org.usfirst.frc.team4488.robot.systems;

import org.usfirst.frc.team4488.robot.RobotMap;
import org.usfirst.frc.team4488.robot.components.LEDController;

import JavaRoboticsLib.FlowControl.Toggle;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.Timer;

public class GearManipulator {

	private Solenoid clamp;
	private Solenoid wrist;
	private Solenoid arm;

	private DigitalInput gearBB;

	private Toggle recieveSwitch;
	private Toggle scoreSwitch;

	private boolean receive;
	private boolean score;
	private boolean drop;
	private boolean reset;

	private GearManipState state;

	private Timer releaseTimer;
	private Timer gearTimer;
	private Timer ledTimer;
	private Timer autoTimer;

	private Drive drive;

	private Intake intake;

	private double driveCount = 0;

	private boolean doneAligning;
	private LEDController led;
	private boolean ledbool;
	
	private Thread autoThread;
	private boolean autobool;
	private boolean autopop;
	private boolean hopperbool;

	public GearManipulator(Drive driveInstance, Intake intakeInstance, LEDController LEDInstance) {
		clamp = new Solenoid(RobotMap.GearClampSolenoid);
		wrist = new Solenoid(RobotMap.GearWristSolenoid);
		arm = new Solenoid(RobotMap.GearArmSolenoid);

		gearBB = new DigitalInput(RobotMap.GearTankBB);

		recieveSwitch = new Toggle();
		scoreSwitch = new Toggle();

		receive = false;
		score = false;
		drop = false;
		reset = false;

		state = GearManipState.Stored;

		releaseTimer = new Timer();
		gearTimer = new Timer();
		ledTimer = new Timer();
		autoTimer = new Timer();

		drive = driveInstance;
		intake = intakeInstance;
		doneAligning = false;
		led = LEDInstance;
		ledbool = false;
		autobool = false;
		autopop = false;
		hopperbool = false;
	}

	public void initStateSet(boolean hopper) {
		if (DriverStation.getInstance().isOperatorControl()) {
			if (hopper) {
				state = GearManipState.Stored;
			} else {
				state = GearManipState.Empty;
			}
		} else if (DriverStation.getInstance().isAutonomous()) {
			if (hopper) {
				state = GearManipState.HopperStart;
			} else
				state = GearManipState.Stored;
		} else
			state = GearManipState.Stored;
	}

	public void setDropGear(boolean val) {
		drop = val;
	}

	public void setReceiveButton(boolean val) {
		receive = val;
	}

	public void setScoreButton(boolean val) {
		score = val;
	}

	public void setResetButton(boolean val) {
		reset = val;
	}

	public GearManipState getState() {
		return state;
	}

	public boolean isGear() {
		return !gearBB.get();
	}

	private void openClamp() {
		clamp.set(true);
	}

	private void closeClamp() {
		clamp.set(false);
	}

	private void armUp() {
		arm.set(false);
	}

	private void armDown() {
		arm.set(true);
	}

	private void wristUp() {
		wrist.set(true);
	}

	private void wristDown() {
		wrist.set(false);
	}

	public void update(boolean alignment) {
		recieveSwitch.setState(receive);
		scoreSwitch.setState(score);

		if (reset) {
			state = GearManipState.Empty;
			doneAligning = false;
		}

		switch (state) {
		
		case HopperStart:
			if(!hopperbool){
				autoStart();
				hopperbool = true;
			}

		case AutoPop:
			if(!autobool){
				autoPop();
				autobool = true;
			}
			break;

		case Empty:
			empty();
			doneAligning = alignment;
			gearTimer.reset();
			scoreSwitch.force(false);
			driveCount = 0;
			intake.forceArmUp(false);
			if (recieveSwitch.getState() || doneAligning) {
				recieveSwitch.force(true);
				state = GearManipState.Receiving;
			}
			if (intake.getArmState()) {
				state = GearManipState.FloorGear;
			}
			if(DriverStation.getInstance().isAutonomous() && !autopop){
				state = GearManipState.AutoPop;
			}
			break;

		case Receiving:
			Receiving();
			intake.forceArmUp(false);
			driveCount = drive.getLinearDistance();
			doneAligning = false;
			if (!recieveSwitch.getState()) {
				doneAligning = false;
				state = GearManipState.Empty;
			}
			if (intake.getArmState()) {
				state = GearManipState.FloorGear;
			}
			if (isGear()) {
				gearTimer.start();
				while (gearTimer.get() <= .2) {
				}
				state = GearManipState.Received;
			}
			break;

		case Received:
			Received();
			if (!ledbool) {
				led.blinkLight(75);
				ledbool = true;
			}
			intake.forceArmUp(true);
			if (drive.getLinearDistance() <= driveCount - 2) {
				state = GearManipState.Stored;
			}

			if (!isGear()) {
				state = GearManipState.Receiving;
			}
			break;

		case Stored:
			Stored();
			ledbool = false;
			intake.forceArmUp(true);
			recieveSwitch.force(false);
			driveCount = 0;
			if (scoreSwitch.getState()) {
				state = GearManipState.Score;
			}
			break;

		case Score:
			Score();
			releaseTimer.reset();
			intake.forceArmUp(true);
			if (!scoreSwitch.getState()) {
				state = GearManipState.Stored;
			} else if (drop) {
				state = GearManipState.DropGear;
			}
			break;

		case DropGear:
			DropGear();
			releaseTimer.start();
			scoreSwitch.force(false);
			intake.forceArmUp(true);
			driveCount = drive.getLinearDistance();
			while (releaseTimer.get() < .5) {
			}
			state = GearManipState.FlipGear;
			releaseTimer.reset();
			break;

		case FlipGear:
			FlipGear();
			intake.forceArmUp(true);
			if (drive.getLinearDistance() <= driveCount - 6) {
				intake.forceArmUp(false);
				state = GearManipState.Empty;
			}
			break;

		case FloorGear:
			Received();
			if ((!intake.getArmState())) {
				state = GearManipState.Empty;
			}
			break;

		default:
			state = GearManipState.Stored;
		}
	}

	public void empty() {
		closeClamp();
		armUp();
		wristDown();
	}

	public void Receiving() {
		openClamp();
		armUp();
		wristUp();
	}

	public void Received() {
		closeClamp();
		armUp();
		wristUp();
	}

	public void Stored() {
		closeClamp();
		armUp();
		wristDown();
	}

	public void Score() {
		closeClamp();
		armDown();
		wristDown();
	}

	public void DropGear() {
		openClamp();
		armDown();
		wristDown();
	}

	public void FlipGear() {
		openClamp();
		armDown();
		wristUp();
	}

	public void TestingClampOpen() {
		openClamp();
		armUp();
		wristDown();
	}
	
	private void autoPop(){
	if(autoThread == null || !autoThread.isAlive()){
		autoThread = new Thread(() ->{
			intake.forceArmDown(true);
			autoTimer.start();
			while (autoTimer.get() < .2) {
			}
			autoTimer.stop();
			autoTimer.reset();
			intake.forceArmDown(false);
			autopop = true;
			state = GearManipState.Empty;
		});
		autoThread.start();
	}
	}
	
	private void autoStart(){
	if(autoThread == null || !autoThread.isAlive()){
		autoThread = new Thread(() ->{
			autoTimer.reset();
			Received();
			autoTimer.start();
			while (autoTimer.get() < .2) {
			}
			autoTimer.stop();
			autoTimer.reset();
			intake.forceArmDown(true);
			autoTimer.start();
			while (autoTimer.get() < .4) {
			}
			autoTimer.stop();
			autoTimer.reset();
			intake.forceArmDown(false);
			autoTimer.start();
			while (autoTimer.get() < .4) {
			}
			autoTimer.stop();
			Stored();
			state = GearManipState.Stored;
		});
		autoThread.start();
	}
	}
}
