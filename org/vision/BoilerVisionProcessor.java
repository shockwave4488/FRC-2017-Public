package org.usfirst.frc.team4488.robot.vision;

/**
 * This function adds vision updates (from the Nexus smartphone) to a list in
 * RobotState. This helps keep track of goals detected by the vision system. The
 * code to determine the best goal to shoot at and prune old Goal tracks is in
 * GoalTracker.java
 * 
 * @see GoalTracker.java
 */
public class BoilerVisionProcessor implements Loop, BoilerVisionUpdateReceiver {
	static BoilerVisionProcessor instance_ = new BoilerVisionProcessor();
	BoilerVisionUpdate update_ = null;
	BoilerRobotState robot_state_ = BoilerRobotState.getInstance();

	public static BoilerVisionProcessor getInstance() {
		return instance_;
	}

	BoilerVisionProcessor() {
	}

	@Override
	public void onStart() {
	}

	@Override
	public void onLoop() {
		BoilerVisionUpdate update;
		synchronized (this) {
			if (update_ == null) {
				return;
			}
			update = update_;
			update_ = null;
		}
		robot_state_.addVisionUpdate(update.getCapturedAtTimestamp(), update.getTargets());
	}

	@Override
	public void onStop() {
		// no-op
	}

	@Override
	public void gotBoilerUpdate(BoilerVisionUpdate update) {
		update_ = update;

	}

}
