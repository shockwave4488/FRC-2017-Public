package org.usfirst.frc.team4488.robot.vision;

/**
 * A container class to specify the shooter angle. It contains the desired
 * range, the turret angle, and the computer vision's track's ID.
 */
public class ShooterAimingParameters {
	double range;
	Rotation2d turret_angle;
	int track_id;

	public ShooterAimingParameters(double range, Rotation2d turret_angle, int track_id) {
		this.range = range;
		this.turret_angle = turret_angle;
		this.track_id = track_id;
	}

	public double getRange() {
		System.out.println(range);
		System.out.println("Hi There!");
		return range;
	}

	public Rotation2d getTurretAngle() {
		return turret_angle;
	}

	public int getTrackid() {
		return track_id;
	}
}
