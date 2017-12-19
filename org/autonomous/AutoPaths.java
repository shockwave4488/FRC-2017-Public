package org.usfirst.frc.team4488.robot.autonomous;

import java.util.function.Supplier;

import org.usfirst.frc.team4488.robot.Robot;
import org.usfirst.frc.team4488.robot.components.Agitator;
import org.usfirst.frc.team4488.robot.components.LEDController;
import org.usfirst.frc.team4488.robot.operator.Logging;
import org.usfirst.frc.team4488.robot.operator.LoggingSystems;
import org.usfirst.frc.team4488.robot.systems.Conveyer;
import org.usfirst.frc.team4488.robot.systems.Drive;
import org.usfirst.frc.team4488.robot.systems.GearManipState;
import org.usfirst.frc.team4488.robot.systems.GearManipulator;
import org.usfirst.frc.team4488.robot.systems.Indexing_System;
import org.usfirst.frc.team4488.robot.systems.Intake;
import org.usfirst.frc.team4488.robot.systems.Shooter;
import org.usfirst.frc.team4488.robot.systems.SmartDrive;

import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class AutoPaths {
	private GearManipulator gearmanip;
	private Shooter shooter;
	private Conveyer conveyer;
	private Indexing_System indexer;
	private SmartDrive drive;
	private Drive pDrive;
	private Intake intake;
	private LEDController led;
	private Agitator agitator;
	private Timer timer;

	AutoChoices autoChoices;
	private int linearDistance;
	Preferences prefs = Preferences.getInstance();

	double[] hopperAndShootDistances = new double[1];
	double[] hopperAndShootAngles = new double[4];

	public AutoPaths(Drive d1, SmartDrive d2, GearManipulator gear, Intake intakeInstance, Shooter shooterInstance,
			Indexing_System indexerInstance, Conveyer conveyerInstance, LEDController ledInstance,
			Agitator agitatorInstance) {
		pDrive = d1;
		drive = d2;
		gearmanip = gear;
		intake = intakeInstance;
		shooter = shooterInstance;
		indexer = indexerInstance;
		conveyer = conveyerInstance;
		led = ledInstance;
		agitator = agitatorInstance;

		linearDistance = 0;

		hopperAndShootDistances[0] = 74;
		hopperAndShootAngles[0] = -24;
		hopperAndShootAngles[1] = 15;
		hopperAndShootAngles[2] = -10;
		hopperAndShootAngles[3] = 5;
		timer = new Timer();
	}

	public void CrossTheLine() {
		pDrive.UnBreakModeAll();
		drive(0, 105, false); // drives out 105 inches
		pDrive.BreakModeAll();
	}

	public void CenterCrossTheLine() {
		pDrive.UnBreakModeAll();

		drive(-81, 0, false);
		drive(0, 48, false);
		drive(60, 0, false);
		drive(0, 12, false);

		pDrive.BreakModeAll();
	}

	public void gearAndShootBoilerSide(double sideSwitch) {
		pDrive.setDriveShiftButton(false);
		drive.setTurnMinDoneCycles(5);
		pDrive.BreakModeAll();
		drive.setDriveDoneRange(1.0);
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(100.5, 0));
		drive.stop();

		wait(() -> drive.isTurnDone(), () -> drive.turnToAngle(59 * sideSwitch));
		drive.stop();
		
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(128.5, 59 * sideSwitch));
		drive.stop(); // drives up to gear station
		
		wait(() -> gearmanip.getState() == GearManipState.FlipGear, () -> gearmanip.setDropGear(true));
		drive.setDriveMinDoneCycles(1);
		wait(() -> gearmanip.getState() == GearManipState.Empty && drive.isDriveDistanceDone(),
				() -> drive.driveToDistance(73));
		drive.stop();
		led.setBrightness(75);
		drive.setTurnDoneRange(2.0);
		drive.setTurnMinDoneCycles(1);		
		drive.setDriveDoneRange(2.0);
		wait(() -> drive.isTurnDone() && shooter.atSpeed(), () -> {
			shooter.setDistance();
			drive.turnToCamera();
			shooter.Spin();
		});
		Logging.logWriter(LoggingSystems.Auto, "GearAndShootCenter: ", "Done Turning");
		shooter.Spin();
		Logging.logWriter(LoggingSystems.Auto, "GearAndShootCenter: ", "Pre Shooting");
		wait(() -> false, () -> {
			indexer.start();
			conveyer.Shoot();
			drive.turnToCamera();
			shooter.Spin();
			intake.setRollerButton(true);

		});
		pDrive.UnBreakModeAll();
	}
	
	public void gearShootAndDriveBoilerSide(double sideSwitch) {
		pDrive.setDriveShiftButton(false);
		drive.setTurnMinDoneCycles(5);
		pDrive.BreakModeAll();
		drive.setDriveDoneRange(1.0);
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(100.5, 0));
		drive.stop();

		wait(() -> drive.isTurnDone(), () -> drive.turnToAngle(59 * sideSwitch));
		drive.stop();
		
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(128.5, 59 * sideSwitch));
		drive.stop(); // drives up to gear station
		
		wait(() -> gearmanip.getState() == GearManipState.FlipGear, () -> gearmanip.setDropGear(true));
		drive.setDriveMinDoneCycles(1);
		wait(() -> gearmanip.getState() == GearManipState.Empty && drive.isDriveDistanceDone(),
				() -> drive.driveToDistance(73));
		drive.stop();
		led.setBrightness(75);
		drive.setTurnDoneRange(2.0);
		drive.setTurnMinDoneCycles(1);		
		drive.setDriveDoneRange(2.0);
		wait(() -> drive.isTurnDone() , () -> {
			drive.turnToCamera();
			shooter.setDistance();
			shooter.Spin();
		});
		Logging.logWriter(LoggingSystems.Auto, "GearAndShootCenter: ", "Done Turning");
		shooter.Spin();
		Logging.logWriter(LoggingSystems.Auto, "GearAndShootCenter: ", "Pre Shooting");
		timer.start();
		wait(() -> timer.get() > SmartDashboard.getNumber("Auto Shooting Time", 4.5), () -> {
			indexer.start();
			conveyer.Shoot();
			drive.turnToCamera();
			shooter.Spin();
			intake.setRollerButton(true);
		});
		indexer.stop();
		conveyer.Stop();
		shooter.Stop();
		intake.setRollerButton(false);
		
		wait(() -> drive.isTurnDone(), () -> drive.turnToAngle(0));
		drive.stop();
		wait(() -> drive.isDriveDistanceDone() || (drive.getUSLeftFiltered() < 15.0 && pDrive.getLinearDistance()>150.0) || (drive.getUSRightFiltered() < 15.0 && pDrive.getLinearDistance()>150.0), () -> {
			drive.driveToDistance(400.0, 30 * sideSwitch);
			pDrive.setDriveShiftButton(true);
		});
		drive.stop();
		pDrive.BreakModeAll();
	}
	
	public void ShootThenGearBoilerSide(double sideSwitch) {
		pDrive.setDriveShiftButton(false);
		drive.setTurnMinDoneCycles(5);
		pDrive.BreakModeAll();
		drive.setDriveDoneRange(1.0);
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(100.5, 0));
		drive.stop();

		wait(() -> drive.isTurnDone(), () -> drive.turnToAngle(40 * sideSwitch));
		drive.stop();
		
		drive.setTurnDoneRange(2.0);
		drive.setTurnMinDoneCycles(1);		
		drive.setDriveDoneRange(2.0);
		led.setBrightness(75);
		wait(() -> drive.isTurnDone() && shooter.atSpeed(), () -> {
			shooter.setDistance();
			drive.turnToCamera();
			shooter.Spin();
		});
		Logging.logWriter(LoggingSystems.Auto, "ShootThenGear: ", "Done Turning");
		shooter.Spin();
		Logging.logWriter(LoggingSystems.Auto, "ShootThenGear: ", "Pre Shooting");
		timer.start();
		wait(() -> timer.get() > SmartDashboard.getNumber("Auto Shooting Time", 4.5), () -> {
			indexer.start();
			conveyer.Shoot();
			drive.turnToCamera();
			shooter.Spin();
		});
		indexer.stop();
		conveyer.Stop();
		shooter.Stop();
		intake.setRollerButton(false);
		led.setBrightness(0);
		drive.setTurnMinDoneCycles(5);
		drive.setDriveDoneRange(1.0);
		drive.setTurnDoneRange(1.0);
		
		wait(() -> drive.isTurnDone(), () -> drive.turnToAngle(59 * sideSwitch));
		drive.stop();
		
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(128.5, 59 * sideSwitch));
		drive.stop(); // drives up to gear station
		
		wait(() -> gearmanip.getState() == GearManipState.FlipGear, () -> gearmanip.setDropGear(true));
		wait(() -> gearmanip.getState() == GearManipState.Empty && drive.isDriveDistanceDone(),
				() -> drive.driveToDistance(100.0));
		drive.stop();
		
		pDrive.UnBreakModeAll();
	}
	
	
	public void gearAndDriveBoilerSide(double sideSwitch) {
		pDrive.setDriveShiftButton(false);
		drive.setTurnMinDoneCycles(5);
		pDrive.BreakModeAll();
		drive.setDriveDoneRange(1.0);
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(100.5, 0));
		drive.stop();

		wait(() -> drive.isTurnDone(), () -> drive.turnToAngle(59 * sideSwitch));
		drive.stop();

		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(128.5, 59 * sideSwitch));
		drive.stop(); // drives up to gear station

		wait(() -> gearmanip.getState() == GearManipState.FlipGear, () -> gearmanip.setDropGear(true));
		drive.setDriveMinDoneCycles(1);
		wait(() -> gearmanip.getState() == GearManipState.Empty && drive.isDriveDistanceDone(),
				() -> drive.driveToDistance(100.0));
		drive.stop();
		drive.setTurnDoneRange(2.0);
		drive.setTurnMinDoneCycles(1);
		drive.setDriveDoneRange(2.0);
		wait(() -> drive.isTurnDone(), () -> drive.turnToAngle(0));
		drive.stop();
		wait(() -> drive.isDriveDistanceDone() || (drive.getUSLeftFiltered() < 15.0 && pDrive.getLinearDistance()>150.0) || (drive.getUSRightFiltered() < 15.0 && pDrive.getLinearDistance()>150.0), () -> {
			drive.driveToDistance(400.0, 30 * sideSwitch);
			pDrive.setDriveShiftButton(true);
		});
		drive.stop();
		pDrive.BreakModeAll();
	}
	
	public void gearAndDriveRetrievalSide(double sideSwitch) {
		pDrive.setDriveShiftButton(false);
		drive.setTurnMinDoneCycles(5);
		pDrive.BreakModeAll();
		drive.setDriveDoneRange(1.0);
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(100.5, 0));
		drive.stop();

		wait(() -> drive.isTurnDone(), () -> drive.turnToAngle(-59 * sideSwitch));
		drive.stop();
		
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(128.5, -59 * sideSwitch));
		//drives up to gear station

		wait(() -> gearmanip.getState() == GearManipState.FlipGear, () -> gearmanip.setDropGear(true));
		drive.setDriveMinDoneCycles(1);
		wait(() -> gearmanip.getState() == GearManipState.Empty && drive.isDriveDistanceDone(),
				() -> drive.driveToDistance(100.0));
		drive.stop();
		drive.setTurnDoneRange(2.0);
		drive.setTurnMinDoneCycles(1);
		drive.setDriveDoneRange(2.0);
		wait(() -> drive.isTurnDone(), () -> drive.turnToAngle(0));
		drive.stop();
		wait(() -> drive.isDriveDistanceDone() || (drive.getUSLeftFiltered() < 15.0 && pDrive.getLinearDistance()>150.0) || (drive.getUSRightFiltered() < 15.0 && pDrive.getLinearDistance()>150.0), () -> {
		drive.driveToDistance(400.0,0);
		pDrive.setDriveShiftButton(true);
		});
		drive.stop();
		pDrive.BreakModeAll();
	}

	public void gearAndShootCenter(double sideSwitch) {
		pDrive.setDriveShiftButton(false);
		pDrive.BreakModeAll();
		Logging.logWriter(LoggingSystems.Auto, "GearAndShootCenter: ", "Begin");

		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(82, 0));
		drive.stop();

		Logging.logWriter(LoggingSystems.Auto, "GearAndShootCenter: ", "Drive To On Peg");

		wait(() -> gearmanip.getState() == GearManipState.FlipGear, () -> gearmanip.setDropGear(true));

		wait(() -> gearmanip.getState() == GearManipState.FlipGear, () -> {
			gearmanip.setDropGear(true);
		});

		drive.setDriveMinDoneCycles(1);

		wait(() -> gearmanip.getState() == GearManipState.Empty && drive.isDriveDistanceDone(),
				() -> drive.driveToDistance(38));
		drive.stop();
		Logging.logWriter(LoggingSystems.Auto, "GearAndShootCenter: ", "Drop Gear");

		drive.setTurnDoneRange(2.0);

		wait(() -> drive.isTurnDone(), () -> drive.turnToAngle(76 * sideSwitch));
		drive.stop();

		Logging.logWriter(LoggingSystems.Auto, "GearAndShootCenter: ", "Turn To Boiler");

		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(-20));
		drive.stop();

		Logging.logWriter(LoggingSystems.Auto, "GearAndShootCenter: ", "Drive To Boiler");

		// Aim to Target
		led.setBrightness(75);
		drive.setDriveMinDoneCycles(5);
		wait(() -> drive.isTurnDone() && shooter.atSpeed(), () -> {
			shooter.setDistance();
			drive.turnToCamera();
			shooter.Spin();
		});
		Logging.logWriter(LoggingSystems.Auto, "GearAndShootCenter: ", "Done Turning");
		shooter.Spin();
		Logging.logWriter(LoggingSystems.Auto, "GearAndShootCenter: ", "Pre Shooting");
		wait(() -> false, () -> {
			indexer.start();
			conveyer.Shoot();
			drive.turnToCamera();
			shooter.Spin();
			intake.setRollerButton(true);

		});

		pDrive.UnBreakModeAll();

	}

	public void hopperAndShoot(double sideSwitch) {
		gearmanip.setScoreButton(false);
		pDrive.setDriveShiftButton(false);
		pDrive.BreakModeAll();
		

		if (sideSwitch == -1.0) {
			wait(() -> drive.isSingleSideTurnDone(), () -> drive.turnToAngleLeftSide(15.0 * sideSwitch));
		} else if (sideSwitch == 1.0) {
			wait(() -> drive.isSingleSideTurnDone(), () -> drive.turnToAngleRightSide(15.0 * sideSwitch));
		}
		drive.setDriveMinDoneCycles(1);
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(22.25, 15.0 * sideSwitch));
		drive.setDriveMinDoneCycles(5);
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(90.0, 46.0 * sideSwitch));

		agitator.setAgitateButton(true);
		agitator.agitate(.4);
		drive.setTurnDoneRange(2.0);
		led.setBrightness(75);
		timer.start();
		wait(() -> timer.get() > .3, () -> {
			pDrive.setPowers(-.7 * sideSwitch, .7 * sideSwitch);
			shooter.setDistance();
			shooter.Spin();
		});
		timer.stop();
		shooter.Spin();
		timer.start();
		wait(() -> timer.get() > .3, () -> {
			pDrive.setPowers(.7 * sideSwitch, -.7 * sideSwitch);
			shooter.setDistance();
			shooter.Spin();
		});
		timer.stop();
		double ticks = pDrive.getLinearDistance();
		shooter.Spin();
		Logging.logWriter(LoggingSystems.Auto, "Drive Ticks after power kicks", Double.toString(pDrive.getLinearDistance()));
		wait(() -> drive.isDriveDistanceDone(), () -> {
			drive.driveToDistance(ticks - 2.1, 51.0 * sideSwitch);
			shooter.setDistance();
			shooter.Spin();
		});
		shooter.setDistance();
		shooter.Spin();
		shooter.setDistance();
		wait(() -> false, () -> {
			drive.turnToCamera();
			indexer.hopperStart(sideSwitch);
			conveyer.Shoot();
			shooter.Spin();
			intake.setRollerButton(true);
			agitator.setAgitateButton(true);
			agitator.agitate(.4);
		});
		pDrive.UnBreakModeAll();
	}
	
	public void FarHopperAndShoot(double sideSwitch){
		gearmanip.setScoreButton(false);
		pDrive.setDriveShiftButton(false);
		pDrive.BreakModeAll();
		drive.setDriveDoneRange(4.0);
		drive.setDriveMinDoneCycles(1);
		
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(98.0, -21.0*sideSwitch));
		agitator.setAgitateButton(true);
		agitator.agitate(.4);
		drive.setTurnDoneRange(2.0);
		drive.setDriveMinDoneCycles(5);
		led.setBrightness(75);
		
		
		
		wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(155.0,-5.0*sideSwitch));
		
		drive.stop();
		
		System.out.println("Done with Drive Path");
		
		wait(() -> drive.isTurnDone() && shooter.atSpeed(), () -> {
			shooter.setDistance();
			drive.turnToCamera();
			shooter.Spin();
		});
		
		shooter.setDistance();
		shooter.Spin();
		
		shooter.setDistance();
		wait(() -> false, () -> {
			drive.turnToCamera();
			indexer.hopperStart(sideSwitch);
			conveyer.Shoot();
			shooter.Spin();
			intake.setRollerButton(true);
			agitator.setAgitateButton(true);
			agitator.agitate(.4);
		});
		pDrive.UnBreakModeAll();
	}
	
	private void wait(Supplier<Boolean> expression, Runnable periodic) {

		periodic.run();

		while (!expression.get()) {
			try {
				Thread.sleep(20);
				Logging.logWriter(LoggingSystems.Auto, "Waiting for Runnable: ", "20 second sleep");
			} catch (InterruptedException e) {
				Logging.logWriter(LoggingSystems.Auto, "Caught Exception--Waiting for Runnable", "20 second sleep");
				e.printStackTrace();
				return;
			}
			periodic.run();
		}
	}

	private void linearDistanceRestart() {
		linearDistance = 0;
	}

	private void drive(double angle, double distance, boolean backwards) {
		drive.resetAll();
		if (angle == 0) {
			wait(() -> drive.isDriveDistanceDone(), () -> drive.driveToDistance(distance));
		} else {
			wait(() -> drive.isDriveTurnDone(angle), () -> drive.arcDrive(angle, backwards));
		}
		linearDistance += pDrive.getLinearDistance();
		drive.stop();
	}

	private void wait(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
			Logging.logWriter(LoggingSystems.Auto, "Waiting for time: ", milliseconds + " seconds");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			Logging.logWriter(LoggingSystems.Auto, "Caught Exception--Waiting for time: ", milliseconds + " seconds");
			e.printStackTrace();
			return;
		}
	}

}
