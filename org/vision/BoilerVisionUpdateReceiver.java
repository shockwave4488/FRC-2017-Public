package org.usfirst.frc.team4488.robot.vision;

/**
 * A basic interface for classes that get VisionUpdates. Classes that implement
 * this interface specify what to do when VisionUpdates are received.
 * 
 * @see VisionUpdate.java
 */
public interface BoilerVisionUpdateReceiver {
	void gotBoilerUpdate(BoilerVisionUpdate update);
}