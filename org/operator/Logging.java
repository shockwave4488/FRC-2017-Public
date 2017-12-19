package org.usfirst.frc.team4488.robot.operator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.simple.JSONObject;

public class Logging {

	private static String startTime;
	private static String FILENAME;
	private static String prevMessage;
	private static int numberOfSystems = 10;
	private static int cacheSize = 12;
	private static boolean disabled = false;

	private static boolean[] firstEntry = new boolean[numberOfSystems];
	private static boolean[] firstEntry2 = new boolean[numberOfSystems];

	private static JSONObject[] publicObj = new JSONObject[numberOfSystems];
	private static JSONObject[] prevObj = new JSONObject[numberOfSystems];
	// private static JSONObject[] writingObj = new JSONObject[numberOfSystems];
	private static String[] writingObj = new String[numberOfSystems];

	private static String[][] cacheObj = new String[numberOfSystems][cacheSize + 1];

	private static int[] cacheCounter = new int[numberOfSystems];
	private static int[] condenserNum = new int[numberOfSystems];
	private static int[] disabledNum = new int[numberOfSystems];

	// run in robot init
	public static void init() {
		startTime = timeStamp();
		prevMessage = "first";
		disabled = false;
		for (int i = 0; i < numberOfSystems; i++) {
			firstEntry[i] = true;
			firstEntry2[i] = true;
			prevObj[i] = new JSONObject();
			publicObj[i] = new JSONObject();
			condenserNum[i] = 1;
			cacheCounter[i] = 0;
			disabledNum[i] = 0;
		}
	}

	// run in disabled init
	public static void disabled() {
		disabled = true;
		for (int i = 0; i < 9; i++) {
			Logging.logWriter(LoggingSystems.Auto, "end", "end");
			Logging.logWriter(LoggingSystems.Camera, "end", "end");
			Logging.logWriter(LoggingSystems.Climber, "end", "end");
			Logging.logWriter(LoggingSystems.Drive, "end", "end");
			Logging.logWriter(LoggingSystems.Gear, "end", "end");
			Logging.logWriter(LoggingSystems.Blender, "end", "end");
			Logging.logWriter(LoggingSystems.Compressor, "end", "end");
			Logging.logWriter(LoggingSystems.Conveyor, "end", "end");
			Logging.logWriter(LoggingSystems.Intake, "end", "end");
			Logging.logWriter(LoggingSystems.Shooter, "end", "end");
		}
	}

	/*
	 * Call this function to log. Redundancy check using the previous
	 * publicObject. If same, redundancy number++; if different, use
	 * writeObject() to cache and write.
	 */
	public static void logWriter(LoggingSystems system, String message, String messageValue) {
		/*
		 * JSONObject mediator = new JSONObject(); mediator.put(message,
		 * messageValue); publicObj[system.getValue()] = mediator;
		 * 
		 * if (firstEntry2[system.getValue()]) { JSONObject objectMediator = new
		 * JSONObject(); objectMediator.put(message, messageValue);
		 * prevObj[system.getValue()] = objectMediator; prevMessage = message;
		 * firstEntry2[system.getValue()] = false; }
		 * 
		 * if (prevObj[system.getValue()].toString().equals(publicObj[system.
		 * getValue()].toString())) { JSONObject objectMediator = new
		 * JSONObject(); objectMediator.put(message, messageValue);
		 * prevObj[system.getValue()] = objectMediator; prevMessage = message;
		 * condenserNum[system.getValue()]++; } else { /* JSONObject
		 * objectMediator = new JSONObject(); objectMediator.put(prevMessage,
		 * prevObj[system.getValue()].get(prevMessage) + "  (X" +
		 * condenserNum[system.getValue()] + ")"); writingObj[system.getValue()]
		 * = objectMediator;
		 */
		// writingObj[system.getValue()] = prevMessage + ":" +
		// prevObj[system.getValue()].get(prevMessage).toString()
		// + " (X" + condenserNum[system.getValue()] + ")";
		// condenserNum[system.getValue()] = 0;
		writeObject(system, message, messageValue);
		// JSONObject objectMediator = new JSONObject();
		// objectMediator.put(message, messageValue);
		// prevObj[system.getValue()] = objectMediator;
		// prevMessage = message;
		// }
	}

	/*
	 * Private function used to write the publicObjects (located in the
	 * cacheObject) out to the RoboRio. Logs are written in JSON format to a
	 * file specific to date, system, and time the robot was powered on. The
	 * bw.write prints out the time in milliseconds epoc; the time in hours,
	 * min, sec, millisec; and 10 (or cacheSize - 3) messages specific to
	 * system.
	 */
	// private static void writeObject(LoggingSystems system) {
	private static void writeObject(LoggingSystems system, String message, String messageVal) {

		FILENAME = "/home/lvuser/logs/" + dateStamp() + "/" + system + "/" + startTime + ".json";
		File file = new File(FILENAME);
		file.getParentFile().mkdirs();

		try (FileWriter bw = new FileWriter(FILENAME, true)) {
			// if (cache(system.getValue())) {
			/*
			 * if (disabled) { for (int i = 0; i <
			 * disabledNum[system.getValue()]; i++) { if
			 * (!firstEntry[system.getValue()]) { bw.write(","); } else {
			 * bw.write("["); firstEntry[system.getValue()] = false; }
			 * bw.write("\n"); bw.write(cacheObj[system.getValue()][i]); }
			 * 
			 * bw.flush(); bw.close(); } else {
			 */
			// for (int i = 0; i < cacheSize; i++) {
			if (!firstEntry[system.getValue()]) {
				bw.write(",");
			} else {
				bw.write("[");
				firstEntry[system.getValue()] = false;
			}
			bw.write("\n");
			bw.write(message + ":" + messageVal);// writingObj[system.getValue()]);//
													// cacheObj[system.getValue()][i]);
			// }
			bw.flush();
			bw.close();
			// }

			// }
		} catch (

		IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Private boolean function used to put only [int cacheSize] publicObjects
	 * into the cacheObject. Writes both time in milliseconds epoc and the time
	 * in hours, min, sec, millisec at the end of the cache.
	 * 
	 */
	/*
	 * private static boolean cache(int systemInt) {
	 * cachingWritingObj(systemInt); if (cacheCounter[systemInt] < (cacheSize -
	 * 1)) { // -2 return false; } else if (false) { disabledNum[systemInt] =
	 * cacheCounter[systemInt]; return true; } else { addTime(systemInt);
	 * cacheCounter[systemInt] = 0; return true; } }
	 * 
	 * /* Private function that puts the current publicObject into the cache
	 * object and increases the cacheCounter by 1.
	 */
	/*
	 * private static void cachingWritingObj(int systemInt) {
	 * cacheObj[systemInt][cacheCounter[systemInt]] =
	 * writingObj[systemInt].toString(); cacheCounter[systemInt]++; }
	 * 
	 * // Adds both time in milliseconds epoc and time in hours, min, sec, //
	 * millisec. private static void addTime(int systemInt) { /* JSONObject
	 * mediator = new JSONObject(); mediator.put("MillisecondStamp",
	 * millisecondStamp()); writingObj[systemInt] = mediator;
	 * cachingWritingObj(systemInt);
	 */
	/*
	 * JSONObject mediator2 = new JSONObject(); mediator2.put("TimeStamp",
	 * timeStamp()); writingObj[systemInt] = mediator2;
	 * cachingWritingObj(systemInt); }
	 */
	private static String millisecondStamp() {
		Date date = new Date();
		return Long.toString(date.getTime());
	}

	private static String timeStamp() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("HH-mm-ss-SSSS");
		String dateString = sdf.format(date);
		dateString.replaceAll(":", "-");
		return dateString;
	}

	private static String dateStamp() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = sdf.format(date);
		dateString.replaceAll(":", "-");
		return dateString;
	}
}
