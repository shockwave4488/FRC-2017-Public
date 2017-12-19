package org.usfirst.frc.team4488.robot.operator;

public enum LoggingSystems {
	Drive(0), Auto(1), Camera(2), Climber(3), Shooter(4), Blender(5), Gear(6), Intake(7), Compressor(8), Conveyor(9);

	private int value;

	LoggingSystems(int Value) {
		this.value = Value;
	}

	public int getValue() {
		return value;
	}
}