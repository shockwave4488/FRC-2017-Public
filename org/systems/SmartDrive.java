package org.usfirst.frc.team4488.robot.systems;

import org.usfirst.frc.team4488.robot.RobotMap;
import org.usfirst.frc.team4488.robot.autonomous.Sides;
import org.usfirst.frc.team4488.robot.operator.Logging;
import org.usfirst.frc.team4488.robot.operator.LoggingSystems;

import com.kauailabs.navx.frc.AHRS;

import JavaRoboticsLib.ControlSystems.SimPID;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class SmartDrive {

	private Drive m_drive;
	private SimPID m_turnController;
	private SimPID m_driveController;
	private SimPID m_driveSpeedController;
	private SimPID m_straightController;
	private SimPID uSTurnController;
	private SimPID uSDriveController;
	private SimPID singleSideController;
	private Preferences prefs;
	private AHRS m_navx;

	private AnalogInput USRight;
	private AnalogInput USLeft;

	private final int USLeftSampleLength = 5;
	private double[] USLeftSamples;
	private int USLeftSampleIndex;

	private final int USRightSampleLength = 5;
	private double[] USRightSamples;
	private int USRightSampleIndex;

	private boolean doneGear;

	private double m_driveTurnCrawlRangeMin;
	private double m_driveTurnCrawlPower; // Used to break static friction and
	private double rangeMax; // help PID converge on angle

	private double stationAngle;

	public SmartDrive(Drive drive) {
		m_drive = drive;
		m_navx = drive.getGyroscope();
		USRight = new AnalogInput(RobotMap.kUltraSonicRight);
		USLeft = new AnalogInput(RobotMap.kUltraSonicLeft);
		doneGear = false;
		stationAngle = 63.3;
		try {
			prefs = Preferences.getInstance();
			uSTurnController = new SimPID(prefs.getDouble("DriveUSP", 0), prefs.getDouble("DriveUSI", 0),
					prefs.getDouble("DriveUSD", 0), prefs.getDouble("DriveUSEps", 0));
			uSTurnController.setMaxOutput(0.6);
			uSTurnController.setDoneRange(.5);
			uSTurnController.setMinDoneCycles(5);
			uSDriveController = new SimPID(prefs.getDouble("DriveBackUSP", 0), prefs.getDouble("DriveBackUSI", 0),
					prefs.getDouble("DriveBackD", 0), prefs.getDouble("DriveBackUSEps", 0));
			uSDriveController.setMaxOutput(0.5);
			uSDriveController.setDoneRange(.25);
			uSDriveController.setMinDoneCycles(5);
			uSDriveController.setDesiredValue(13.25);
			m_turnController = new SimPID(prefs.getDouble("DriveTurnP", 0), prefs.getDouble("DriveTurnI", 0),
					prefs.getDouble("DriveTurnD", 0), prefs.getDouble("DriveTurnEps", 0));
			m_turnController.setMaxOutput(1.0);
			m_turnController.setDoneRange(prefs.getDouble("DriveTurnDoneRange", 0));
			m_turnController.setMinDoneCycles(1);
			m_driveTurnCrawlRangeMin = 99;
			rangeMax = 101;
			m_driveController = new SimPID(prefs.getDouble("DriveP", 0), prefs.getDouble("DriveI", 0),
					prefs.getDouble("DriveD", 0), prefs.getDouble("DriveEps", 0));
			m_driveController.setMaxOutput(prefs.getDouble("Drive Max Output", .5));
			m_driveController.setDoneRange(2);
			m_driveController.setMinDoneCycles(5);
			m_driveSpeedController = new SimPID(prefs.getDouble("DriveSpeedP", 0), prefs.getDouble("DriveSpeedI", 0),
					prefs.getDouble("DriveSpeedD", 0), prefs.getDouble("DriveSpeedEps", 0));
			m_driveSpeedController.setMaxOutput(1);
			m_straightController = new SimPID(prefs.getDouble("DriveStraightP", 0),
					prefs.getDouble("DriveStraightI", 0), prefs.getDouble("DriveStraightD", 0),
					prefs.getDouble("DriveStraightEps", 0));
			m_straightController.setMaxOutput(0.20);
			m_driveTurnCrawlPower = .2;
			singleSideController = new SimPID(.11, .01, .1, 0);
			singleSideController.setMaxOutput(.67);
			singleSideController.setDoneRange(1.5);
			singleSideController.setMinDoneCycles(5);

			USLeftSamples = new double[USLeftSampleLength];
			for (int i = 0; i < USLeftSampleLength; i++) {
				USLeftSamples[i] = 0;
			}
			USLeftSampleIndex = 0;

			USRightSamples = new double[USRightSampleLength];
			for (int i = 0; i < USRightSampleLength; i++) {
				USRightSamples[i] = 0;
			}
			USRightSampleIndex = 0;
		} catch (Exception e) {
			System.out.println("Oops");
			e.printStackTrace();
		}
	}

	public double getUSLeftFiltered() {
		USLeftSamples[USLeftSampleIndex] = getUSLeftDistance();
		USLeftSampleIndex = (USLeftSampleIndex + 1) % USLeftSampleLength;
		double m_tempSensorSum = 0;
		for (int i = 0; i < USLeftSampleLength; i++) {
			m_tempSensorSum += USLeftSamples[i];
		}

		return (m_tempSensorSum / USLeftSampleLength) + 1.75;
	}

	public double getUSRightFiltered() {
		USRightSamples[USRightSampleIndex] = getUSRightDistance();
		USRightSampleIndex = (USRightSampleIndex + 1) % USRightSampleLength;
		double m_tempSensorSum = 0;
		for (int i = 0; i < USRightSampleLength; i++) {
			m_tempSensorSum += USRightSamples[i];
		}

		return (m_tempSensorSum / USRightSampleLength) + 1.75;
	}

	private double getUSLeftDistance() {
		return (USLeft.getVoltage() * 1000.0) / 9.8;
	}

	private double getUSRightDistance() {
		return (USRight.getVoltage() * 1000.0) / 9.8;
	}
	
	public double getUSDistAverage(){
		return (getUSLeftFiltered() + getUSRightFiltered())/2;
	}

	public double getUSLeftVoltage() {
		return USLeft.getVoltage();
	}

	public double getUSRightVoltage() {
		return USRight.getVoltage();
	}

	public Drive getDrive() {
		return m_drive;
	}

	public void centerToWall(double side, boolean hopper) {
		m_turnController.setDesiredValue(BoundAngleNeg180To180Degrees(stationAngle * side));
		double power = m_turnController.calcPID(m_drive.getAngle());
		if (m_turnController.isDone())
			power = 0;
		m_drive.setPowers(power, -power);

	}

	public boolean isUSCenteringDone() {
		return uSTurnController.isDone();
	}

	public void driveFromWall(double side) {
		double right = getUSRightFiltered();
		double left = getUSLeftFiltered();
		double dist = (right + left) / 2;
		double mindist = 20.0;
		double angle = BoundAngleNeg180To180Degrees(stationAngle * side);

		if (dist > mindist) {
			m_drive.setPowers(.4, .4);
		} else if (right > left) {
			m_straightController.setDesiredValue(angle);
			double power = -uSDriveController.calcPID(right);
			double angleCorrection = m_straightController.calcPID(m_drive.getAngle());
			m_drive.setPowers((power) + (angleCorrection), (power) - (angleCorrection));
		} else if (left > right) {
			m_straightController.setDesiredValue(angle);
			double power = -uSDriveController.calcPID(left);
			double angleCorrection = m_straightController.calcPID(m_drive.getAngle());
			m_drive.setPowers((power) + (angleCorrection), (power) - (angleCorrection));
		}
	}

	public boolean doneAligningToWall() {
		return uSDriveController.isDone();
	}

	public void turnToCameraCrawl(boolean shooting) {
		turnToCameraCrawl(0, shooting);
	}

	public void turnToCameraCrawl(double linearPower, boolean shooting) {
		double azimuthX = BoundAngleNeg180To180Degrees(
				m_drive.getAngle() - SmartDashboard.getNumber("BoilerDegToCenterOfTarget", 0) - 1.5);

		Logging.logWriter(LoggingSystems.Drive, "AzimuthX: ", Double.toString(azimuthX));

		m_turnController.setDesiredValue(azimuthX);

		double power = m_turnController.calcPID(m_drive.getAngle());

		if (m_turnController.isDone()) {
			power = 0;
		}

		if (Math.abs(linearPower) < m_driveTurnCrawlPower && !m_turnController.isDone() && !shooting) {
			double range = SmartDashboard.getNumber("BoilerRange", 0);
			m_driveController.setDesiredValue(90.0);
			if (range < 84.0 || range > 96) {
				linearPower = m_driveController.calcPID(range) * .6;
			} else if (range < 88 || range > 92) {
				linearPower = m_driveController.calcPID(range) * .05;
			} else
				linearPower = 0;
		} else
			linearPower = 0;

		m_drive.setPowers(linearPower + (power * .8), linearPower + (-power * .8));
	}

	public void turnToCamera() {
		double azimuthX = BoundAngleNeg180To180Degrees(
				m_drive.getAngle() - SmartDashboard.getNumber("BoilerDegToCenterOfTarget", 0) - 1.5);

		Logging.logWriter(LoggingSystems.Drive, "AzimuthX: ", Double.toString(azimuthX));

		m_turnController.setDesiredValue(azimuthX);

		double power = m_turnController.calcPID(m_drive.getAngle());

		if (m_turnController.isDone()) {
			power = 0;
		}

		m_drive.setPowers((power * .8), -power * .8);
	}

	public double getAzimuthX() {
		return BoundAngleNeg180To180Degrees(
				m_drive.getAngle() - SmartDashboard.getNumber("BoilerDegToCenterOfTarget", 0));
	}
	
	public double getTurnSetpoint() {
		return m_turnController.getDesiredVal();
	}

	public void arcDrive(double heading, boolean reverse) {
		double power = 0.0;
		m_straightController.resetErrorSum();
		m_straightController.resetPreviousVal();

		m_straightController.setDesiredValue(BoundAngleNeg180To180Degrees(heading));
		double angleCorrection = m_straightController.calcPID(m_drive.getAngle());
		if (reverse) {
			power = -0.3;
		} else {
			power = 0.3;
		}
		final double angleCorrectionMultiplier = 3;
		angleCorrection = angleCorrection * angleCorrectionMultiplier;
		if ((angleCorrection + power) > 1) {
			m_drive.setPowers(1 / 1.5, (power - angleCorrection) / 1.5);
		} else if ((angleCorrection - power) < -1) {
			m_drive.setPowers((power + angleCorrection) / 1.5, -1 / 1.5);
		} else {
			m_drive.setPowers((power + angleCorrection) / 1.5, (power - angleCorrection) / 1.5);
		}
	}

	public void driveToDistance(double distance) {
		m_driveController.setDesiredValue(distance);
		double power = m_driveController.calcPID(m_drive.getLinearDistance());
		System.out.println(power);
		m_drive.setPowers(power, power);
	}

	public void driveToDistance(double distance, double heading) {
		m_driveController.setDesiredValue(distance);
		m_straightController.setDesiredValue(heading);
		double power = m_driveController.calcPID(m_drive.getLinearDistance());
		double angleCorrection = m_straightController.calcPID(m_drive.getAngle());
		m_drive.setPowers(power + angleCorrection, power - angleCorrection);
	}

	public void driveToSpeed(double speed, double heading) {
		m_driveSpeedController.setDesiredValue(speed);
		m_straightController.setDesiredValue(heading);
		double power = m_driveSpeedController.calcPID(m_drive.getLinearSpeed());
		double angleCorrection = m_straightController.calcPID(m_drive.getAngle());
		m_drive.setPowers(power + angleCorrection, power - angleCorrection);
	}

	public void turnToAngle(double angle) {
		m_turnController.setDesiredValue(angle);
		double power = m_turnController.calcPID(m_drive.getAngle());
		m_drive.setPowers((power), (-power));
	}

	public void turnToAngleLeftSide(double angle) {
		singleSideController.setDesiredValue(angle);
		double power = singleSideController.calcPID(m_drive.getAngle());
		m_drive.setPowers((power + m_driveTurnCrawlPower), 0);
	}

	public void turnToAngleRightSide(double angle) {
		singleSideController.setDesiredValue(angle);
		double power = singleSideController.calcPID(m_drive.getAngle());
		m_drive.setPowers(0, (-power - m_driveTurnCrawlPower));
	}

	public void stop() {
		m_drive.setPowers(0, 0);
	}

	public void resetAll() {
		m_drive.resetAngle();
		m_drive.resetEncoders();
	}

	public boolean TargetFound() {
		if (Math.abs(SmartDashboard.getNumber("BoilerDegToCenterOfTarget", 0)) < 29) {
			return true;
		} else
			return false;
	}

	public boolean isDriveDistanceDone() {
		return m_driveController.isDone();
	}

	public boolean isDriveTurnDone(double heading) {
		if (heading > 0) {
			if (m_navx.getYaw() >= heading) {
				return true;
			} else {
				return false;
			}
		} else {
			if (m_navx.getYaw() <= heading) {
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean isTurnDone() {
		return m_turnController.isDone();
	}

	public boolean isSingleSideTurnDone() {
		return singleSideController.isDone();
	}

	public void setDriveMaxOutput(double max) {
		m_driveController.setMaxOutput(max);
	}

	public double getDriveMaxOutput() {
		return m_driveController.getMaxOutputVal();
	}

	public void setTurnDoneRange(double range) {
		m_turnController.setDoneRange(range);
	}

	public double getTurnDoneRange() {
		return m_turnController.getDoneRangeVal();
	}

	public void setDriveDoneRange(double range) {
		m_driveController.setDoneRange(range);
	}

	public double getDriveDoneRange() {
		return m_driveController.getDoneRangeVal();
	}

	public void setTurnMinDoneCycles(int cycles) {
		m_turnController.setMinDoneCycles(cycles);
	}

	public int getTurnMinDoneCycles() {
		return m_turnController.getMinDoneCycles();
	}
	
	public double getTurnDesValue(){
		return m_turnController.getDesiredVal();
	}

	public void setDriveMinDoneCycles(int cycles) {
		m_driveController.setMinDoneCycles(cycles);
	}

	public int getDriveMinDoneCycles() {
		return m_driveController.getMinDoneCycles();
	}

	private double BoundAngleNeg180To180Degrees(double angle) {
		while (angle <= -180)
			angle += 360;
		while (angle > 180)
			angle -= 360;
		return angle;
	}

	public boolean inShooterRange() {
		if (SmartDashboard.getNumber("BoilerRange", 0) >= 80 && SmartDashboard.getNumber("BoilerRange", 0) <= 120) {
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