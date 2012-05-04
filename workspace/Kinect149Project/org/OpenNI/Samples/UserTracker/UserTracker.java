/****************************************************************************
 *                                                                           *
 *  OpenNI 1.x Alpha                                                         *
 *  Copyright (C) 2011 PrimeSense Ltd.                                       *
 *                                                                           *
 *  This file is part of OpenNI.                                             *
 *                                                                           *
 *  OpenNI is free software: you can redistribute it and/or modify           *
 *  it under the terms of the GNU Lesser General Public License as published *
 *  by the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                      *
 *                                                                           *
 *  OpenNI is distributed in the hope that it will be useful,                *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the             *
 *  GNU Lesser General Public License for more details.                      *
 *                                                                           *
 *  You should have received a copy of the GNU Lesser General Public License *
 *  along with OpenNI. If not, see <http://www.gnu.org/licenses/>.           *
 *                                                                           *
 ****************************************************************************/
package Kinxt;

import org.OpenNI.*;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;

;

public class UserTracker extends Component {
	class NewUserObserver implements IObserver<UserEventArgs> {
		@Override
		public void update(IObservable<UserEventArgs> observable,
				UserEventArgs args) {
			System.out.println("New user " + args.getId());
			try {
				if (skeletonCap.needPoseForCalibration()) {
					poseDetectionCap
							.startPoseDetection(calibPose, args.getId());
					System.out.println("need pose for calibration");
				} else {
					skeletonCap.requestSkeletonCalibration(args.getId(), true);
				}
			} catch (StatusException e) {
				e.printStackTrace();
			}
		}
	}

	class LostUserObserver implements IObserver<UserEventArgs> {
		@Override
		public void update(IObservable<UserEventArgs> observable,
				UserEventArgs args) {
			System.out.println("Lost user " + args.getId());
			joints.remove(args.getId());

			// Reset calibrated arm
			calibratedArm = false;
			calibrateArmCounter = 0;
		}
	}

	class CalibrationCompleteObserver implements
			IObserver<CalibrationProgressEventArgs> {
		@Override
		public void update(
				IObservable<CalibrationProgressEventArgs> observable,
				CalibrationProgressEventArgs args) {
			System.out.println("Calibraion complete: " + args.getStatus());
			try {
				if (args.getStatus() == CalibrationProgressStatus.OK) {
					System.out.println("starting tracking " + args.getUser());
					skeletonCap.startTracking(args.getUser());
					joints
							.put(
									new Integer(args.getUser()),
									new HashMap<SkeletonJoint, SkeletonJointPosition>());
				} else if (args.getStatus() != CalibrationProgressStatus.MANUAL_ABORT) {
					if (skeletonCap.needPoseForCalibration()) {
						poseDetectionCap.startPoseDetection(calibPose, args
								.getUser());
					} else {
						skeletonCap.requestSkeletonCalibration(args.getUser(),
								true);
					}
				}
			} catch (StatusException e) {
				e.printStackTrace();
			}
		}
	}

	class PoseDetectedObserver implements IObserver<PoseDetectionEventArgs> {
		@Override
		public void update(IObservable<PoseDetectionEventArgs> observable,
				PoseDetectionEventArgs args) {
			System.out.println("Pose " + args.getPose() + " detected for "
					+ args.getUser());
			try {
				poseDetectionCap.stopPoseDetection(args.getUser());
				skeletonCap.requestSkeletonCalibration(args.getUser(), true);
			} catch (StatusException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	private GestureGenerator gestureGen;
	private HashMap<Integer, ArrayList<Point3D>> history;

	private boolean calibratedArm = false;
	private int calibrateArmCounter = 0;
	private int movingCount = 0;
	private double armLength = 0;
	private double leftSpeed = 0;
	private double rightSpeed = 0;
	private double leftFilter = 0;
	private double rightFilter = 0;
	private LinkedList<Double> averageLeft = new LinkedList<Double>(); 
	private LinkedList<Double> averageRight = new LinkedList<Double>();

	private int averagedLength = 4;

	//0-255
	private double clawVal;
	private double clawFilter;
	private LinkedList<Double> averageClaw = new LinkedList<Double>();
	
	private static final long serialVersionUID = 1L;
	private OutArg<ScriptNode> scriptNode;
	private Context context;
	private DepthGenerator depthGen;
	private UserGenerator userGen;
	private SkeletonCapability skeletonCap;
	private PoseDetectionCapability poseDetectionCap;
	private byte[] imgbytes;
	private float histogram[];
	String calibPose = null;
	HashMap<Integer, HashMap<SkeletonJoint, SkeletonJointPosition>> joints;

	private boolean drawBackground = true;
	private boolean drawPixels = true;
	private boolean drawSkeleton = true;
	private boolean printID = true;
	private boolean printState = true;

	private BufferedImage bimg;
	int width, height;

	private final String SAMPLE_XML_FILE = "C:/Users/David/Dropbox/149project/OpenNI/Data/SamplesConfig.XML";


	public UserTracker() {

		try {

			scriptNode = new OutArg<ScriptNode>();
			context = Context.createFromXmlFile(SAMPLE_XML_FILE, scriptNode);

			depthGen = DepthGenerator.create(context);
			DepthMetaData depthMD = depthGen.getMetaData();

			histogram = new float[10000];
			width = depthMD.getFullXRes();
			height = depthMD.getFullYRes();

			imgbytes = new byte[width * height * 3];

			userGen = UserGenerator.create(context);
			skeletonCap = userGen.getSkeletonCapability();
			poseDetectionCap = userGen.getPoseDetectionCapability();

			userGen.getNewUserEvent().addObserver(new NewUserObserver());
			userGen.getLostUserEvent().addObserver(new LostUserObserver());
			skeletonCap.getCalibrationCompleteEvent().addObserver(
					new CalibrationCompleteObserver());
			poseDetectionCap.getPoseDetectedEvent().addObserver(
					new PoseDetectedObserver());

			calibPose = skeletonCap.getSkeletonCalibrationPose();
			joints = new HashMap<Integer, HashMap<SkeletonJoint, SkeletonJointPosition>>();

			skeletonCap.setSkeletonProfile(SkeletonProfile.ALL);

			context.startGeneratingAll();
		} catch (GeneralException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void calcHist(ShortBuffer depth) {
		// reset
		for (int i = 0; i < histogram.length; ++i)
			histogram[i] = 0;

		depth.rewind();

		int points = 0;
		
		while (depth.remaining() > 0) {
			short depthVal = depth.get();
			if (depthVal != 0) {
				histogram[depthVal]++;
				points++;
			}
		}

		for (int i = 1; i < histogram.length; i++) {
			histogram[i] += histogram[i - 1];
		}

		if (points > 0) {
			for (int i = 1; i < histogram.length; i++) {
				histogram[i] = 1.0f - (histogram[i] / (float) points);
			}
		}
	}

	void updateDepth() {
		try {

			context.waitAnyUpdateAll();

			DepthMetaData depthMD = depthGen.getMetaData();
			SceneMetaData sceneMD = userGen.getUserPixels(0);

			ShortBuffer scene = sceneMD.getData().createShortBuffer();
			ShortBuffer depth = depthMD.getData().createShortBuffer();
			calcHist(depth);
			depth.rewind();

			while (depth.remaining() > 0) {
				int pos = depth.position();
				short pixel = depth.get();
				short user = scene.get();

				imgbytes[3 * pos] = 0;
				imgbytes[3 * pos + 1] = 0;
				imgbytes[3 * pos + 2] = 0;

				if (drawBackground || pixel != 0) {
					int colorID = user % (colors.length - 1);
					if (user == 0) {
						colorID = colors.length - 1;
					}
					if (pixel != 0) {
						float histValue = histogram[pixel];
						imgbytes[3 * pos] = (byte) (histValue * colors[colorID]
								.getRed());
						imgbytes[3 * pos + 1] = (byte) (histValue * colors[colorID]
								.getGreen());
						imgbytes[3 * pos + 2] = (byte) (histValue * colors[colorID]
								.getBlue());
					}
				}
			}
		} catch (GeneralException e) {
			e.printStackTrace();
		}
	}

	public Dimension getPreferredSize() {
		return new Dimension(width, height);
	}

	Color colors[] = { Color.RED, Color.BLUE, Color.CYAN, Color.GREEN,
			Color.MAGENTA, Color.PINK, Color.YELLOW, Color.WHITE };

	public void getJoint(int user, SkeletonJoint joint) throws StatusException {
		SkeletonJointPosition pos = skeletonCap.getSkeletonJointPosition(user,
				joint);
		if (pos.getPosition().getZ() != 0) {
			joints.get(user).put(
					joint,
					new SkeletonJointPosition(depthGen
							.convertRealWorldToProjective(pos.getPosition()),
							pos.getConfidence()));
		} else {
			joints.get(user).put(joint,
					new SkeletonJointPosition(new Point3D(), 0));
		}
	}

	public void getJoints(int user) throws StatusException {
		
		 getJoint(user, SkeletonJoint.HEAD); getJoint(user,SkeletonJoint.NECK);

		getJoint(user, SkeletonJoint.LEFT_SHOULDER);
		getJoint(user, SkeletonJoint.LEFT_ELBOW);
		getJoint(user, SkeletonJoint.LEFT_HAND);

		getJoint(user, SkeletonJoint.RIGHT_SHOULDER);
		getJoint(user, SkeletonJoint.RIGHT_ELBOW);
		getJoint(user, SkeletonJoint.RIGHT_HAND);

		getJoint(user, SkeletonJoint.TORSO);

		getJoint(user, SkeletonJoint.LEFT_HIP);
		// getJoint(user, SkeletonJoint.LEFT_KNEE);
		// getJoint(user, SkeletonJoint.LEFT_FOOT);

		getJoint(user, SkeletonJoint.RIGHT_HIP);
		// getJoint(user, SkeletonJoint.RIGHT_KNEE);
		// getJoint(user, SkeletonJoint.RIGHT_FOOT);*/

	}

	void drawLine(Graphics g,
			HashMap<SkeletonJoint, SkeletonJointPosition> jointHash,
			SkeletonJoint joint1, SkeletonJoint joint2) {
		Point3D pos1 = jointHash.get(joint1).getPosition();
		Point3D pos2 = jointHash.get(joint2).getPosition();

		if (jointHash.get(joint1).getConfidence() == 0
				|| jointHash.get(joint2).getConfidence() == 0)
			return;

		g.drawLine((int) pos1.getX(), (int) pos1.getY(), (int) pos2.getX(),
				(int) pos2.getY());

	}

	// Prints 1-5 for gestures
	double[] detectGesture(int user,
			HashMap<SkeletonJoint, SkeletonJointPosition> jointHash) {
		if (jointHash.get(SkeletonJoint.LEFT_SHOULDER).getConfidence() == 0
				|| jointHash.get(SkeletonJoint.LEFT_HIP).getConfidence() == 0
				|| jointHash.get(SkeletonJoint.LEFT_HAND).getConfidence() == 0
				|| jointHash.get(SkeletonJoint.RIGHT_SHOULDER).getConfidence() == 0
				|| jointHash.get(SkeletonJoint.RIGHT_HIP).getConfidence() == 0
				|| jointHash.get(SkeletonJoint.RIGHT_HAND).getConfidence() == 0)
			return new double[3];
		

		// Every 15 time steps, re-calibrate the arm
		if (calibrateArmCounter == 15) {
			armLength = armLength / 15;
			calibrateArmCounter++;
			calibratedArm = true;
		}

		Point3D L_shoulder_pos = jointHash.get(SkeletonJoint.LEFT_SHOULDER)
				.getPosition();
		Point3D L_hand_pos = jointHash.get(SkeletonJoint.LEFT_HAND)
				.getPosition();
		Point3D L_elbow_pos = jointHash.get(SkeletonJoint.LEFT_ELBOW)
				.getPosition();

		Point3D R_shoulder_pos = jointHash.get(SkeletonJoint.RIGHT_SHOULDER)
				.getPosition();
		Point3D R_hand_pos = jointHash.get(SkeletonJoint.RIGHT_HAND)
				.getPosition();
		Point3D R_elbow_pos = jointHash.get(SkeletonJoint.RIGHT_ELBOW)
				.getPosition();
		Point3D head_pos = jointHash.get(SkeletonJoint.HEAD).getPosition();
		
		
		if (!calibratedArm) {
			// Calculate forearm length and upper arm length, add together
			double forearmLeftLength = Math.sqrt(Math.pow(L_elbow_pos.getX()
					- L_hand_pos.getX(), 2)
					+ Math.pow(L_elbow_pos.getY() - L_hand_pos.getY(), 2)
					+ Math.pow(L_elbow_pos.getZ() - L_hand_pos.getZ(), 2));
			double upperLeftLength = Math.sqrt(Math.pow(L_elbow_pos.getX()
					- L_shoulder_pos.getX(), 2)
					+ Math.pow(L_elbow_pos.getY() - L_shoulder_pos.getY(), 2)
					+ Math.pow(L_elbow_pos.getZ() - L_shoulder_pos.getZ(), 2));
			double armLeftLength = forearmLeftLength + upperLeftLength;

			double forearmRightLength = Math.sqrt(Math.pow(R_elbow_pos.getX()
					- R_hand_pos.getX(), 2)
					+ Math.pow(R_elbow_pos.getY() - R_hand_pos.getY(), 2)
					+ Math.pow(R_elbow_pos.getZ() - R_hand_pos.getZ(), 2));
			double upperRightLength = Math.sqrt(Math.pow(R_elbow_pos.getX()
					- R_shoulder_pos.getX(), 2)
					+ Math.pow(R_elbow_pos.getY() - R_shoulder_pos.getY(), 2)
					+ Math.pow(R_elbow_pos.getZ() - R_shoulder_pos.getZ(), 2));
			double armRightLength = forearmRightLength + upperRightLength;

			// Average left and right arm length for more accurate result and
			// average over previous calculations
			armLength += ((armLeftLength + armRightLength) / 2);

			// armLength will be max wheel speed
			// calibratedArm = true;
			calibrateArmCounter++;
		}

		/*
		 * Left Hand - Speed double a =
		 * Math.sqrt(Math.pow(((int)L_shoulder_pos.getX() -
		 * (int)L_elbow_pos.getX()), 2) + Math.pow(((int)L_shoulder_pos.getY() -
		 * (int)L_elbow_pos.getY()), 2)); double b =
		 * Math.sqrt(Math.pow(((int)L_elbow_pos.getX() -
		 * (int)L_hand_pos.getX()), 2) + Math.pow(((int)L_elbow_pos.getY() -
		 * (int)L_hand_pos.getY()), 2)); double c =
		 * Math.sqrt(Math.pow(((int)L_shoulder_pos.getX() -
		 * (int)L_hand_pos.getX()), 2) + Math.pow(((int)L_shoulder_pos.getY() -
		 * (int)L_hand_pos.getY()), 2));
		 */
		// double angle = Math.acos((Math.pow(a,2) + Math.pow(b,2) -
		// Math.pow(c,2)) / (2 * a * b));

		// Normalize over armLength with maxSpeed of 100
		double depthLeft = (L_shoulder_pos.getZ() - L_hand_pos.getZ());
		double depthRight = (R_shoulder_pos.getZ() - R_hand_pos.getZ());
		// System.out.println("armLength = " + armLength);
		// System.out.println("original depthLeft = " + depthLeft);
		// System.out.println("original depthRight = " + depthRight);

		depthLeft = ((200 / (armLength - 150)) * depthLeft)
				+ (100 - ((200 * armLength) / (armLength - 150)));
		// ((depthLeft - (armLength / 1.5)) / ((armLength / 2) / 100));
		depthRight = ((200 / (armLength - 150)) * depthRight)
				+ (100 - ((200 * armLength) / (armLength - 150)));
		// ((depthRight - (armLength / 1.5)) / ((armLength / 2) / 100));
		

		if (depthLeft > 100) {
			depthLeft = 100;
		} else if ( Math.abs(depthLeft) < 16 ){
			depthLeft = 0;
		} else if (depthLeft < -100) {
			depthLeft = -100;
		}
		if (depthRight > 100) {
			depthRight = 100;
		} else if ( Math.abs(depthLeft) < 16 ){
			depthRight = 0;
		} else if (depthRight < -100) {
			depthRight = -100;
		}

		
		//claw calculation
		double maxHandHeight = Math.max(R_hand_pos.getY(), L_hand_pos.getY());
		double shoulderHeight = (R_shoulder_pos.getY() + L_shoulder_pos.getY())/2;
		double headHeight = head_pos.getY();
		double range = (headHeight - shoulderHeight)*1.5;
		double a = 255/range;
		double b = -(255*shoulderHeight)/range;
		double clawHeight = a*maxHandHeight + b;
		
		if(clawHeight<0) clawHeight = 0;
		else if (clawHeight>255) clawHeight = 255;
		
		double[] depths = new double[3];
		depths[1] = depthRight;
		depths[0] = depthLeft;
		depths[2] = clawHeight;
		
		
		
		// System.out.println("armLength = " + armLength);

		// System.out.println("depthLeft = " + depthLeft);
		// System.out.println("depthRight = " + depthRight);
		return depths;
	}

	public void calibrate(){
		//do things
	}
	public void drawSkeleton(Graphics g, int user) throws StatusException {
		getJoints(user);
		HashMap<SkeletonJoint, SkeletonJointPosition> dict = joints
				.get(new Integer(user));

		if (user == 1) {
			// Detect Gesture
			/*
			 * double angle = 0; angle = (angle*.5) + (.5 * detectGesture(user,
			 * dict)[0]); System.out.println("left angle is: " + angle);
			 */

			
				double[] speeds = detectGesture(user,dict);
				leftSpeed = speeds[1];
				rightSpeed = speeds[0];
				clawVal = speeds[2];
				
			
			if(movingCount<averagedLength-1){
				averageLeft.add(leftSpeed);
				averageRight.add(rightSpeed);
				averageClaw.add(clawVal);
				movingCount++;
			} else if( movingCount == averagedLength-1){
				averageLeft.add(leftSpeed);
				averageRight.add(rightSpeed);
				averageClaw.add(clawVal);
				movingCount++;
				for(int i =0;i<averageLeft.size();i++){
					rightFilter += averageRight.get(i);
					leftFilter += averageLeft.get(i);
					clawFilter += averageClaw.get(i);
				}
				rightFilter /= averagedLength;
				leftFilter /= averagedLength;
				clawFilter /= averagedLength;
			}else{
				averageLeft.add(leftSpeed);
				averageRight.add(rightSpeed);
				averageClaw.add(clawVal);
				leftFilter -= averageLeft.removeFirst()/averagedLength;
				rightFilter -= averageRight.removeFirst()/averagedLength;
				clawFilter -= averageClaw.removeFirst()/averagedLength;
				leftFilter += leftSpeed/averagedLength;
				rightFilter += rightSpeed/averagedLength;
				clawFilter += clawVal/averagedLength;
			}
			
			
			
			/*

			if (movingCount == 6) {
				if (leftFilter == 0) {
					leftFilter = leftSpeed / 6;
					rightFilter = rightSpeed / 6;
				} else {

					leftSpeed = leftSpeed / 6;
					rightSpeed = rightSpeed / 6;
					leftFilter = .4*leftFilter + .6*leftSpeed;
					rightFilter = .4*rightFilter + .6*rightSpeed;
					
				}
				
				if ((leftFilter <20) && (leftFilter>-20)) leftFilter = 0;
				if ((rightFilter <20) && (rightFilter>-20)) rightFilter = 0;
				if(Math.abs(leftFilter-rightFilter) < 30) leftFilter = rightFilter = (leftFilter+rightFilter)/2;
				
				byte[] data = { (byte) leftFilter, (byte) rightFilter };
				
				System.out.println("leftFilter is " + leftFilter);
				System.out.println("rightFilter is " + rightFilter);
				leftSpeed = 0;
				rightSpeed = 0;
				movingCount = 0;
			}

*/

			/*
			 * try { InputStream inputStream = pythonProcess.getInputStream();
			 * OutputStream outputStream = pythonProcess.getOutputStream(); if
			 * (inputStream.read() == 1){ outputStream.write(data);
			 * System.out.println("sent " + data + " to python"); } } catch
			 * (IOException e) {
			 * System.out.println("Failed to communicate to ROS"); }
			 */

			// System.out.println("left hand relative depth is: " + depth);
		}

		/*
		 * drawLine(g, dict, SkeletonJoint.HEAD, SkeletonJoint.NECK);
		 * 
		 * drawLine(g, dict, SkeletonJoint.LEFT_SHOULDER, SkeletonJoint.TORSO);
		 * drawLine(g, dict, SkeletonJoint.RIGHT_SHOULDER, SkeletonJoint.TORSO);
		 */

		// drawLine(g, dict, SkeletonJoint.NECK, SkeletonJoint.LEFT_SHOULDER);
		drawLine(g, dict, SkeletonJoint.LEFT_SHOULDER, SkeletonJoint.LEFT_ELBOW);
		drawLine(g, dict, SkeletonJoint.LEFT_ELBOW, SkeletonJoint.LEFT_HAND);

		// drawLine(g, dict, SkeletonJoint.NECK, SkeletonJoint.RIGHT_SHOULDER);
		drawLine(g, dict, SkeletonJoint.RIGHT_SHOULDER,
				SkeletonJoint.RIGHT_ELBOW);
		drawLine(g, dict, SkeletonJoint.RIGHT_ELBOW, SkeletonJoint.RIGHT_HAND);

		/*
		 * drawLine(g, dict, SkeletonJoint.LEFT_HIP, SkeletonJoint.TORSO);
		 * drawLine(g, dict, SkeletonJoint.RIGHT_HIP, SkeletonJoint.TORSO);
		 * drawLine(g, dict, SkeletonJoint.LEFT_HIP, SkeletonJoint.RIGHT_HIP);
		 * 
		 * drawLine(g, dict, SkeletonJoint.LEFT_HIP, SkeletonJoint.LEFT_KNEE);
		 * drawLine(g, dict, SkeletonJoint.LEFT_KNEE, SkeletonJoint.LEFT_FOOT);
		 * 
		 * drawLine(g, dict, SkeletonJoint.RIGHT_HIP, SkeletonJoint.RIGHT_KNEE);
		 * drawLine(g, dict, SkeletonJoint.RIGHT_KNEE,
		 * SkeletonJoint.RIGHT_FOOT);
		 */
	}

	public void paint(Graphics g) {
		if (drawPixels) {
			DataBufferByte dataBuffer = new DataBufferByte(imgbytes, width
					* height * 3);

			WritableRaster raster = Raster.createInterleavedRaster(dataBuffer,
					width, height, width * 3, 3, new int[] { 0, 1, 2 }, null);

			ColorModel colorModel = new ComponentColorModel(ColorSpace
					.getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8 },
					false, false, ComponentColorModel.OPAQUE,
					DataBuffer.TYPE_BYTE);

			bimg = new BufferedImage(colorModel, raster, false, null);

			g.drawImage(bimg, 0, 0, null);
		}
		try {
			int[] users = userGen.getUsers();
			for (int i = 0; i < users.length; ++i) {
				Color c = colors[users[i] % colors.length];
				c = new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c
						.getBlue());

				g.setColor(c);
				if (drawSkeleton && skeletonCap.isSkeletonTracking(users[i])) {
					drawSkeleton(g, users[i]);
				}

				if (printID) {
					Point3D com = depthGen.convertRealWorldToProjective(userGen
							.getUserCoM(users[i]));
					String label = null;
					if (!printState) {
						label = new String("" + users[i]);
					} else if (skeletonCap.isSkeletonTracking(users[i])) {
						// Tracking
						label = new String(users[i] + " - Tracking");
					} else if (skeletonCap.isSkeletonCalibrating(users[i])) {
						// Calibrating
						label = new String(users[i] + " - Calibrating");
					} else {
						// Nothing
						label = new String(users[i] + " - Looking for pose ("
								+ calibPose + ")");
					}

					g.drawString(label, (int) com.getX(), (int) com.getY());
				}
			}
		} catch (StatusException e) {
			e.printStackTrace();
		}
	}

	public void update_speeds(byte[] data){
		data[0] = (byte) leftFilter;
		data[1] = (byte) rightFilter;
		data[2] = (byte) clawFilter;
	}
}
