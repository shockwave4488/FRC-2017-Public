package org.usfirst.frc.team4488.robot;

import org.usfirst.frc.team4488.robot.autonomous.AutonomousManager;
import org.usfirst.frc.team4488.robot.components.Agitator;
import org.usfirst.frc.team4488.robot.components.LEDController;
import org.usfirst.frc.team4488.robot.components.SolenoidLED;
import org.usfirst.frc.team4488.robot.operator.Controllers;
import org.usfirst.frc.team4488.robot.operator.Logging;
import org.usfirst.frc.team4488.robot.operator.LoggingSystems;
import org.usfirst.frc.team4488.robot.systems.Climber;
import org.usfirst.frc.team4488.robot.systems.Conveyer;
import org.usfirst.frc.team4488.robot.systems.Drive;
import org.usfirst.frc.team4488.robot.systems.GearManipulator;
import org.usfirst.frc.team4488.robot.systems.Indexing_System;
import org.usfirst.frc.team4488.robot.systems.Intake;
import org.usfirst.frc.team4488.robot.systems.Shooter;
import org.usfirst.frc.team4488.robot.systems.SmartDrive;
import org.usfirst.frc.team4488.robot.testing.TestingManager;
import org.usfirst.frc.team4488.robot.vision.BoilerRobotState;
import org.usfirst.frc.team4488.robot.vision.BoilerVisionProcessor;
import org.usfirst.frc.team4488.robot.vision.BoilerVisionServer;
import org.usfirst.frc.team4488.robot.vision.Looper;

import JavaRoboticsLib.Drive.DriveHelper;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {
	private GearManipulator gearmanip;
	private Drive drive;
	private SmartDrive smartdrive;
	private Controllers xbox;
	private DriveHelper drivehelp;
	private SmartDrive smartDrive;
	private PowerDistributionPanel pdp;
	private Climber climber;
	private TestingManager testing;
	private AutonomousManager auto;
	private Shooter shooter;
	private Conveyer conveyer;
	private Indexing_System indexer;
	private LEDController led;
	private Intake intake;
	private Agitator agitator;
	private SolenoidLED intakeLed;
	private boolean atSpeed;
	private boolean doneDriving;
	private boolean intakestate;
	private boolean doneAligning;
	private boolean shooting;

	public static Timer timer;

	BoilerVisionServer boilerVisionServer = BoilerVisionServer.getInstance();
	BoilerRobotState boilerRobotState = BoilerRobotState.getInstance();
	Looper mEnabledLooper = new Looper();

	/**
	 * This function is run when the robot is first started up and should be
	 * used for any initialization code.
	 */
	@Override
	public void robotInit() {
		boilerVisionServer.addVisionUpdateReceiver(BoilerVisionProcessor.getInstance());
		mEnabledLooper.register(BoilerVisionProcessor.getInstance());
		BoilerVisionServer.getInstance();
		led = new LEDController();
		led.initializeLEDs();
		drive = new Drive();
		drivehelp = new DriveHelper(drive, 0.1, 0.15); // Xbox 360
		smartDrive = new SmartDrive(drive);
		xbox = new Controllers();
		pdp = new PowerDistributionPanel();
		climber = new Climber(pdp);
		shooter = new Shooter();
		conveyer = new Conveyer();
		intake = new Intake(drive, led);
		indexer = new Indexing_System();
		gearmanip = new GearManipulator(drive, intake, led);
		agitator = new Agitator();
		intakeLed = new SolenoidLED();
		// SmartDashboard.putNumber("Set Shooter RPM", 3125.0);
		SmartDashboard.putNumber("Auto Shooting Time", 4.5);
		boilerVisionServer.startPhone();
		boilerVisionServer.startApp(RobotMap.CameraSerial[RobotMap.cameraNumber]);
		atSpeed = false;
		shooting = false;
		auto = new AutonomousManager(drive, smartDrive, gearmanip, intake, shooter, indexer, conveyer, led, agitator);
		Logging.init();
		drive.resetAngle();
		drive.resetEncoders();
		timer = new Timer();
	}

	@Override
	public void robotPeriodic() {
		double batteryVoltage;
		SmartDashboard.putString("Gear Manip State", gearmanip.getState().toString());
		SmartDashboard.putBoolean("isGear", gearmanip.isGear());
		SmartDashboard.putNumber("Shooter RPM", shooter.getspeed());
		SmartDashboard.putNumber("Conveyer RPM", conveyer.getspeed());
		SmartDashboard.putNumber("DriveL Speed", drive.getLeftSpeed());
		SmartDashboard.putNumber("DriveR Speed", drive.getRightSpeed());
		SmartDashboard.putNumber("Drive Speed", drive.getLinearSpeed());
		SmartDashboard.putNumber("DriveL Pos", drive.getLeftDistance());
		SmartDashboard.putNumber("DriveR Pos", drive.getRightDistance());
		SmartDashboard.putNumber("Drive Pos", drive.getLinearDistance());
		SmartDashboard.putNumber("Drive Angle", drive.getAngle());
		SmartDashboard.putNumber("Climber Motor Current", climber.getCurrent());
		SmartDashboard.putBoolean("Target Found?", smartDrive.TargetFound() && xbox.getLeftTrigger(xbox.m_primary));
		SmartDashboard.putBoolean("Done Aiming?", smartDrive.isTurnDone() && xbox.getLeftTrigger(xbox.m_secondary));
		SmartDashboard.putString("Drive Gear", drive.GearState().toString());
		SmartDashboard.putBoolean("At Optimized Shooter Range?", smartDrive.inShooterRange());
		SmartDashboard.putNumber("US Distance", (smartDrive.getUSRightFiltered() + smartDrive.getUSLeftFiltered()) / 2);
		SmartDashboard.putNumber("US Right Distance", smartDrive.getUSRightFiltered());
		SmartDashboard.putNumber("US Left Distance", smartDrive.getUSLeftFiltered());
		SmartDashboard.putBoolean("Done Aligning to Wall?", doneDriving);
		SmartDashboard.putString("Intake State", intake.getState().toString());
		SmartDashboard.putBoolean("Intake Has Gear?", intake.isGear() && intake.getArmState());
		SmartDashboard.putNumber("Temperature", pdp.getTemperature());
		SmartDashboard.putNumber("Current", pdp.getTotalCurrent());
		SmartDashboard.putNumber("Energy", pdp.getTotalEnergy());
		SmartDashboard.putNumber("Power", pdp.getTotalPower());
		batteryVoltage = pdp.getVoltage();
		SmartDashboard.putNumber("Voltage", batteryVoltage);
		if (batteryVoltage < 12) {
			SmartDashboard.putBoolean("Battery Level", false);
		} else {
			SmartDashboard.putBoolean("Battery Level", true);
		}
	}

	public void periodicButtonSets() {
		gearmanip.setReceiveButton(xbox.getRightBumper(xbox.m_secondary));
		gearmanip.setScoreButton(xbox.getRightTrigger(xbox.m_secondary));
		gearmanip.setDropGear(xbox.getX(xbox.m_secondary));
		gearmanip.setResetButton(xbox.getSelect(xbox.m_secondary));
		climber.setBypassButton(xbox.getStart(xbox.m_secondary));
		climber.setClimbButton(xbox.getLeftBumper(xbox.m_secondary));
		intake.setArmButton(xbox.getA(xbox.m_secondary));
		intake.setRollerButton(xbox.getB(xbox.m_secondary));
		intake.setDeJamButton(xbox.getY(xbox.m_secondary));
		intake.setScoreButton(xbox.getX(xbox.m_secondary));
		intake.setResetButton(xbox.getStart(xbox.m_secondary));
		drive.setDriveShiftButton(xbox.getLeftBumper(xbox.m_primary));
		agitator.setAgitateButton(xbox.getDPadPressed(xbox.m_secondary));
	}

	/**
	 * This autonomous (along with the chooser code above) shows how to select
	 * between different autonomous modes using the dashboard. The sendable
	 * chooser code works with the Java SmartDashboard. If you prefer the
	 * LabVIEW Dashboard, remove all of the chooser code and uncomment the
	 * getString line to get the auto name from the text box below the Gyro
	 *
	 * You can add additional auto modes by adding additional comparisons to the
	 * switch structure below with additional strings. If using the
	 * SendableChooser make sure to add them to the chooser code above as well.
	 */
	@Override
	public void autonomousInit() {
		gearmanip.setScoreButton(true);
		gearmanip.initStateSet(auto.isAutoHopper());
		mEnabledLooper.start();
		auto.start();
	}

	@Override
	public void teleopInit() {
		auto.kill();
		gearmanip.initStateSet(auto.isAutoHopper());
		mEnabledLooper.start();
		smartDrive.setTurnDoneRange(Preferences.getInstance().getDouble("DriveTurnDoneRange", 1.0));
		smartDrive.setTurnMinDoneCycles(5);
		// shooter.setRPM(SmartDashboard.getNumber("Set Shooter RPM", 3150));
		SmartDashboard.putNumber("LED Brightness %", 75);
		drive.resetEncoders();
		drive.UnBreakModeAll();
	}

	/**
	 * This function is called periodically during autonomous
	 */
	@Override
	public void autonomousPeriodic() {
		drive.gearShiftUpdate();
		gearmanip.update(doneDriving);
		intake.update();
		auto.check();
		Logging.logWriter(LoggingSystems.Drive, "Linear distance: ", Double.toString(drive.getLinearDistance()));
	}

	/**
	 * This function is called periodically during operator control
	 */
	@Override
	public void teleopPeriodic() {
		// Shoot Button : RT Primary
		// Charge Button : LT Secondary
		// Align To Boiler Target Button : LT Primary
		// Manual Gear Shift : LB Primary
		// Align To Feeding Station : A Primary
		auto.kill();
		periodicButtonSets();

		gearmanip.update(doneAligning);

		intake.update();

		climber.update();
		drive.gearShiftUpdate();
		agitator.agitate(.25);

		if (xbox.getLeftTrigger(xbox.m_secondary) || xbox.getLeftTrigger(xbox.m_primary)
				|| xbox.getRightBumper(xbox.m_primary)) {
			shooter.setDistance();
			led.setBrightness(SmartDashboard.getNumber("LED Brightness %", 75));
			shooter.Spin();

			Logging.logWriter(LoggingSystems.Camera, "Shooter Range: ",
					Double.toString(SmartDashboard.getNumber("BoilerRange", 0)));
			Logging.logWriter(LoggingSystems.Camera, "Shooter RPM: ", Double.toString(shooter.getspeed()));
			if (smartDrive.inShooterRange()) {
				xbox.m_primary.setRumble(RumbleType.kLeftRumble, .3);
				xbox.m_primary.setRumble(RumbleType.kRightRumble, .3);
			} else {
				xbox.m_primary.setRumble(RumbleType.kLeftRumble, 0);
				xbox.m_primary.setRumble(RumbleType.kRightRumble, 0);
			}
		} else {
			led.setBrightness(0);
			shooter.Stop();
			xbox.m_primary.setRumble(RumbleType.kLeftRumble, 0);
			xbox.m_primary.setRumble(RumbleType.kRightRumble, 0);
		}

		if ((xbox.getLeftTrigger(xbox.m_secondary) || xbox.getLeftTrigger(xbox.m_primary)
				|| xbox.getRightBumper(xbox.m_primary)) && xbox.getRightTrigger(xbox.m_primary)
				&& (shooter.atSpeed() || atSpeed) && smartDrive.inShooterRange())
		// &&
		// smartDrive.TargetFound())
		{
			// Charge Button && Shoot Button && Wheels At Speed && Drive Turn
			// Done && Target Found
			conveyer.Shoot();
			indexer.start();
			atSpeed = true;
			shooting = true;
		} else {
			conveyer.Stop();
			indexer.stop();
			atSpeed = false;
			shooting = false;
		}

		if (xbox.getLeftTrigger(xbox.m_primary) && smartDrive.TargetFound()) {
			// if boiler align button && target found
			drive.BreakModeAll();
			smartDrive.turnToCamera();
			doneDriving = false;
			doneAligning = false;
			intakeLed.off();
		} else if (xbox.getRightBumper(xbox.m_primary)) {
			drive.BreakModeAll();
			smartDrive.turnToCameraCrawl(shooting);
			doneDriving = false;
			doneAligning = false;
			intakeLed.off();
		} else if (xbox.getA(xbox.m_primary) && !doneAligning) {
			drive.BreakModeAll();
			smartDrive.centerToWall(auto.getSelectedSide(), auto.isAutoHopper());
			doneDriving = false;
			doneAligning = smartDrive.isTurnDone();
			intakeLed.off();

		} else if (xbox.getA(xbox.m_primary) && doneAligning) {
			smartDrive.driveFromWall(auto.getSelectedSide());
			doneDriving = smartDrive.doneAligningToWall();
			if (doneDriving) {
				intakeLed.blink();
			} else if (!doneDriving) {
				intakeLed.off();
			}
		} else {
			doneAligning = false;
			doneDriving = false;
			drive.UnBreakModeAll();
			intakeLed.off();
			drivehelp.Drive(Math.pow(xbox.getLeftStickY(xbox.m_primary), 3), xbox.getRightStickX(xbox.m_primary));
		}
	}

	@Override
	public void testInit() {
		testing = new TestingManager(xbox, drive, smartDrive, pdp, climber, conveyer, shooter, intake, gearmanip,
				indexer);
	}

	/**
	 * This function is called periodically during test mode
	 */
	@Override
	public void testPeriodic() {
	}

	@Override
	public void disabledInit() {
		drive.resetEncoders();
		drive.UnBreakModeAll();
		Logging.disabled();
	}

	@Override
	public void disabledPeriodic() {
		led.setBrightness(0);
		auto.kill();
		xbox.m_primary.setRumble(RumbleType.kLeftRumble, 0);
		xbox.m_primary.setRumble(RumbleType.kRightRumble, 0);
	}

	private void wait(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
