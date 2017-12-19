package org.usfirst.frc.team4488.robot.autonomous;

import org.usfirst.frc.team4488.robot.Robot;
import org.usfirst.frc.team4488.robot.components.Agitator;
import org.usfirst.frc.team4488.robot.components.LEDController;
import org.usfirst.frc.team4488.robot.operator.Logging;
import org.usfirst.frc.team4488.robot.operator.LoggingSystems;
import org.usfirst.frc.team4488.robot.systems.Conveyer;
import org.usfirst.frc.team4488.robot.systems.Drive;
import org.usfirst.frc.team4488.robot.systems.GearManipulator;
import org.usfirst.frc.team4488.robot.systems.Indexing_System;
import org.usfirst.frc.team4488.robot.systems.Intake;
import org.usfirst.frc.team4488.robot.systems.Shooter;
import org.usfirst.frc.team4488.robot.systems.SmartDrive;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class AutonomousManager {

	AutoChoices autoChoices;
	SendableChooser sendable1;
	SendableChooser sendable2;
	SendableChooser sendable3;
	private double sideMultiplier;
	private AutoPaths paths;
	private Thread autoThread;
	private Thread resetThread;
	private Drive drive;

	public AutonomousManager(Drive d1, SmartDrive d2, GearManipulator gear, Intake intake, Shooter shooter,
			Indexing_System indexer, Conveyer conveyer, LEDController led, Agitator agitator) {
		paths = new AutoPaths(d1, d2, gear, intake, shooter, indexer, conveyer, led, agitator);
		drive = d1;
		sideMultiplier = 1.0;

		sendable1 = new SendableChooser();

		sendable1.addObject("Hopper and Shoot", AutoChoices.HopperAndShoot);
		sendable1.addObject("Gear and Shoot", AutoChoices.GearAndShoot);
		sendable1.addObject("Shoot And Gear", AutoChoices.ShootAndGear);
		sendable1.addObject("Gear And Drive", AutoChoices.GearAndDrive);
		sendable1.addObject("Cross the Line", AutoChoices.CrossTheLine);
		sendable1.addObject("Gear Shoot And Drive", AutoChoices.GearShootAndDrive);
		sendable1.addDefault("None", AutoChoices.None);

		SmartDashboard.putData("Auto Path", sendable1);

		sendable2 = new SendableChooser();

		sendable2.addDefault("Boiler Side", AutoPositions.BoilerSide);
		sendable2.addObject("Center", AutoPositions.Center);
		sendable2.addObject("Retreival Side", AutoPositions.RetrievalSide);

		SmartDashboard.putData("Positions", sendable2);

		sendable3 = new SendableChooser();

		sendable3.addDefault("Red", Sides.Red);
		sendable3.addObject("Blue", Sides.Blue);

		SmartDashboard.putData("Side", sendable3);

	}

	public void start() {
		resetThread = new Thread(() -> {
			drive.resetAngle();
			drive.resetEncoders();
			wait(250);
		});
		resetThread.start();
		
		autoThread = new Thread(() -> {
			System.out.println("Thread Start");
			AutoChoices autoChoices = (AutoChoices) sendable1.getSelected();
			AutoPositions autoPositions = (AutoPositions) sendable2.getSelected();
			Sides sides = (Sides) sendable3.getSelected();

			if (sides == Sides.Blue) {
				sideMultiplier = 1.0;
			} else {
				sideMultiplier = -1.0;
			}

			switch (autoChoices) {
			case CrossTheLine:
				if (autoPositions == AutoPositions.BoilerSide) {
					paths.CrossTheLine();
				} else if (autoPositions == AutoPositions.Center) {
					paths.CenterCrossTheLine();
				}
				break;

			case HopperAndShoot:
				if (autoPositions == AutoPositions.BoilerSide) {
					paths.FarHopperAndShoot(sideMultiplier);
				} else if (autoPositions == AutoPositions.Center) {
					// Function
				}
				break;

			case GearAndShoot:
				if (autoPositions == AutoPositions.BoilerSide) {
					paths.gearAndShootBoilerSide(sideMultiplier);
				}

				else if (autoPositions == AutoPositions.Center) {
					paths.gearAndShootCenter(sideMultiplier);
				}
				break;
				
			case GearAndDrive:
				if (autoPositions == AutoPositions.BoilerSide){
					paths.gearAndDriveBoilerSide(sideMultiplier);
				}
				
				else if (autoPositions == AutoPositions.RetrievalSide){
					paths.gearAndDriveRetrievalSide(sideMultiplier);
				}
				break;
				
			case ShootAndGear:
				if(autoPositions == AutoPositions.BoilerSide){
					paths.ShootThenGearBoilerSide(sideMultiplier);
				}
				break;
				
			case GearShootAndDrive:
				if(autoPositions == AutoPositions.BoilerSide){
					paths.gearShootAndDriveBoilerSide(sideMultiplier);
				}
				break;
				
			case None:
				System.out.println("NONE SELECTED, YOU FOOOOL!!!");
				break;
			}
			
			Logging.logWriter(LoggingSystems.Auto, "Thread", "Reached The End");
		});
		autoThread.start();
	}

	public void check() {
		if (!(autoThread.isAlive() && DriverStation.getInstance().isAutonomous()
				&& DriverStation.getInstance().isEnabled())) {
			autoThread.interrupt();
		}
	}

	/**
	 * Kills the thread that is running the auto mode code
	 */
	public void kill() {
		if (autoThread != null && autoThread.isAlive()) {
			System.out.println("Thread Killed");
			autoThread.interrupt();
		}
	}

	public double[] negative(double[] angle) {
		for (int i = 0; i < angle.length; i++) {
			angle[i] = -angle[i];
		}

		return angle;

	}

	public double getSelectedSide() {
		Sides sides = (Sides) sendable3.getSelected();
		if (sides == Sides.Blue) {
			sideMultiplier = 1;
		} else {
			sideMultiplier = -1;
		}
		return sideMultiplier;
	}

	public boolean isAutoHopper() {
		if ((AutoChoices) sendable1.getSelected() == AutoChoices.HopperAndShoot) {
			return true;
		} else
			return false;
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
