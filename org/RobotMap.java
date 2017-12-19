package org.usfirst.frc.team4488.robot;

public class RobotMap {
	public static final int DriveMotorRightM = 6;
	public static final int DriveMotorRight2 = 7;
	public static final int DriveMotorRight3 = 8;
	public static final int DriveMotorLeftM = 5;
	public static final int DriveMotorLeft2 = 4;
	public static final int DriveMotorLeft3 = 3;

	public static final int FlyWheelMotor = 0;
	public static final int FlyWheelSlaveMotor = 1;
	public static final int IntakeMotor = 2;
	public static final int HopperBlenderMotor = 11;
	public static final int ConveyorBeltMotor = 10;
	public static final int ClimberMotor = 9;

	public static final int FlyWheelCounter = 0;
	public static final int ConveyerCounter = 1;
	public static final int GearTankBB = 8;
	public static final int kUltraSonicLeft = 0;
	public static final int kUltraSonicRight = 1;
	public static final int FloorGearBB = 7;

	public static final int DriveGearShiftSolenoid = 0;
	public static final int GearClampSolenoid = 1;
	public static final int GearWristSolenoid = 2;
	public static final int GearArmSolenoid = 3;
	public static final int IntakeArmSolenoid = 4;
	public static final int AgitatorSolenoid = 5;
	public static final int SolenoidLED = 7;

	public static final int ClimberMotorPDPChannel = 0;

	public static final double kClimberSoftCurrentLimit = 45;
	public static final double kClimberHardCurrentLimit = 85;

	public static final double kConveyerRPM = 2500;

	public static final double wheelDiameter = 4;
	public static final double encoderTicks = 4096;

	public static final double driftCorrection = 1.0;

	// NEVER CALL PHONE ZER0!!!
	public static final String[] CameraSerial = { "0000", "03916220215f3949", "032b2a1a0931755a", "02b5af2dd0217e58",
			"06c6e3ec0ae53c99", "030a3a6309317488", "032b40b5215ea1bd" };
	// 1 & 2 are for the competition bot.
	// Odds are for boiler, evens for gear.
	// 3 & 4 are for the practice bot.
	// 5 & 6 are backups.

	// DEFINE WHEN VALUES ARE KNOWN

	/*
	 * public static final double BoilerCamXOffset; public static final double
	 * BoilerCamYOffset; public static final double BoilerCamZOffset; public
	 * static final double BoilerCamPitchAngleDegrees; public static final
	 * double BoilerCamYawAngleDegrees; public static final double
	 * BoilerCenterofTargetHeight;
	 * 
	 * public static final double GearCamXOffset; public static final double
	 * GearCamYOffset; public static final double GearCamZOffset; public static
	 * final double GearCamPitchAngleDegrees; public static final double
	 * GearCamYawAngleDegrees; public static final double
	 * GearCenterofTargetHeight;
	 */
	public static final int cameraNumber = 4;

}