package org.usfirst.frc.team4488.robot.systems;

import org.usfirst.frc.team4488.robot.RobotMap;

import com.ctre.CANTalon;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Indexing_System {

	private CANTalon blender;

	public Indexing_System() {
		blender = new CANTalon(RobotMap.HopperBlenderMotor);
		blender.enableBrakeMode(false);
	}

	public void start() {
		blender.set(-1.0);
	}
	
	public void hopperStart(double side){
		blender.set(-1.0 * side);
	}

	public void stop() {
		blender.set(0);
	}

}
