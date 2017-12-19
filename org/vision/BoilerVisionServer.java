package org.usfirst.frc.team4488.robot.vision;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;

import org.usfirst.frc.team4488.robot.RobotMap;
import org.usfirst.frc.team4488.robot.vision.AdbBridge;

/**
 * This controls all vision actions, including vision updates, capture, and
 * interfacing with the Android phone with Android Debug Bridge. It also stores
 * all VisionUpdates (from the Android phone) and contains methods to add
 * to/prune the VisionUpdate list. Much like the subsystems, outside methods get
 * the VisionServer instance (there is only one VisionServer) instead of
 * creating new VisionServer instances.
 * 
 * @see VisionUpdate.java
 */

public class BoilerVisionServer extends CrashTrackingRunnable {

	private static BoilerVisionServer _instance = null;
	private ServerSocket m_server_socket;
	private boolean m_running = true;
	private int m_port;
	private ArrayList<BoilerVisionUpdateReceiver> receivers = new ArrayList<>();
	AdbBridge adb = new AdbBridge();
	double lastMessageReceivedTime = 0;
	private boolean m_use_java_time = false;

	private ArrayList<ServerThread> serverThreads = new ArrayList<>();
	private volatile boolean mWantsAppRestart = false;

	public static BoilerVisionServer getInstance() {
		if (_instance == null) {
			System.out.println("Vision Server");
			_instance = new BoilerVisionServer(4488, RobotMap.CameraSerial[RobotMap.cameraNumber]);
		}
		return _instance;
	}

	private boolean mIsConnect = false;

	public boolean isConnected() {
		return mIsConnect;
	}

	public void requestAppRestart() {
		mWantsAppRestart = true;
	}

	protected class ServerThread extends CrashTrackingRunnable {
		private Socket m_socket;

		public ServerThread(Socket socket) {
			m_socket = socket;
		}

		public void send(VisionMessage message) {
			String toSend = message.toJson() + "\n";
			if (m_socket != null && m_socket.isConnected()) {
				try {
					OutputStream os = m_socket.getOutputStream();
					os.write(toSend.getBytes());
				} catch (IOException e) {
					System.err.println("VisionServer: Could not send data to socket");
				}
			}
		}

		public void handleMessage(VisionMessage message, double timestamp) {
			if ("targets".equals(message.getType())) {
				BoilerVisionUpdate update = BoilerVisionUpdate.generateFromJsonString(timestamp, message.getMessage(),
						m_port);
				receivers.removeAll(Collections.singleton(null));
				if (update.isValid()) {
					for (BoilerVisionUpdateReceiver receiver : receivers) {
						receiver.gotBoilerUpdate(update);
					}
				}
			}
			if ("heartbeat".equals(message.getType())) {
				send(HeartbeatMessage.getInstance());
			}
		}

		public boolean isAlive() {
			return m_socket != null && m_socket.isConnected() && !m_socket.isClosed();
		}

		@Override
		public void runCrashTracked() {
			if (m_socket == null) {
				return;
			}
			try {
				InputStream is = m_socket.getInputStream();
				byte[] buffer = new byte[2048];
				int read;
				while (m_socket.isConnected() && (read = is.read(buffer)) != -1) {
					double timestamp = getTimestamp();
					lastMessageReceivedTime = timestamp;
					String messageRaw = new String(buffer, 0, read);
					String[] messages = messageRaw.split("\n");
					for (String message : messages) {
						OffWireMessage parsedMessage = new OffWireMessage(message);
						if (parsedMessage.isValid()) {
							handleMessage(parsedMessage, timestamp);
						}
					}
				}
				System.out.println("Socket disconnected");
			} catch (IOException e) {
				System.err.println("Could not talk to socket");
			}
			if (m_socket != null) {
				try {
					m_socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Instantializes the VisionServer and connects to ADB via the specified
	 * port.
	 * 
	 * @param Port
	 */
	private BoilerVisionServer(int port, String serial) {
		try {
			adb = new AdbBridge();
			m_port = port;
			m_server_socket = new ServerSocket(port);
			adb.start(serial);
			adb.reversePortForward(port, port, serial);
			try {
				String useJavaTime = System.getenv("USE_JAVA_TIME");
				m_use_java_time = "true".equals(useJavaTime);
			} catch (NullPointerException e) {
				m_use_java_time = false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		new Thread(this).start();
		new Thread(new AppMaintainanceThread()).start();
	}

	public void restartAdb() {
		System.out.println("If you see this function called - tell Dave...");
		// adb.restartAdb();
		// adb.reversePortForward(m_port, m_port);
	}

	public void startApp(String serial) {
		adb.startApp(serial);
	}

	public void stopApp(String serial) {
		adb.stopApp(serial);
	}

	public void startPhone() {
		adb.startPhone();
	}

	/**
	 * If a VisionUpdate object (i.e. a target) is not in the list, add it.
	 * 
	 * @see VisionUpdate
	 */
	public void addVisionUpdateReceiver(BoilerVisionUpdateReceiver receiver) {
		System.out.println("Heyo");
		if (!receivers.contains(receiver)) {
			System.out.println("Heyo2");
			receivers.add(receiver);
		}
	}

	public void removeVisionUpdateReceiver(BoilerVisionUpdateReceiver receiver) {
		if (receivers.contains(receiver)) {
			receivers.remove(receiver);
		}
	}

	@Override
	public void runCrashTracked() {
		while (m_running) {
			try {
				Socket p = m_server_socket.accept();
				ServerThread s = new ServerThread(p);
				new Thread(s).start();
				serverThreads.add(s);
			} catch (IOException e) {
				System.err.println("Issue accepting socket connection!");
			} finally {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class AppMaintainanceThread extends CrashTrackingRunnable {
		@Override
		public void runCrashTracked() {
			while (true) {
				if (getTimestamp() - lastMessageReceivedTime > .1) {
					// camera disconnected
					if (m_port == 4488)
						adb.reversePortForward(m_port, m_port, "02b5af2dd0217e58");
					else
						adb.reversePortForward(m_port, m_port, "06c6e3ec0ae53c99");
					mIsConnect = false;
				} else {
					mIsConnect = true;
				}
				if (mWantsAppRestart) {
					if (m_port == 4488) {
						adb.restartApp("02b5af2dd0217e58");
					} else {
						adb.restartApp("06c6e3ec0ae53c99");
					}
					mWantsAppRestart = false;
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private double getTimestamp() {
		if (m_use_java_time) {
			return System.currentTimeMillis();
		} else {
			return Timer.getFPGATimestamp();
		}
	}
}
