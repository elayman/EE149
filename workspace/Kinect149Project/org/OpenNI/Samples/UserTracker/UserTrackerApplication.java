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
package org.OpenNI.Samples.UserTracker;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JFrame;

public class UserTrackerApplication {

    /**
	 * 
	 */
	public UserTracker viewer;
	private boolean shouldRun = true;
	private JFrame frame;

    public UserTrackerApplication (JFrame frame)
    {
    	this.frame = frame;
    	frame.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent arg0) {}
			@Override
			public void keyReleased(KeyEvent arg0) {}
			@Override
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					shouldRun = false;
				}
			}
		});
    }

    public static void main(String s[])
    {
    	
    	
        JFrame f = new JFrame("OpenNI User Tracker");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        UserTrackerApplication app = new UserTrackerApplication(f);
        
        app.viewer = new UserTracker();
        /*
        try{
    		//Create python process
	    	
	    	//String[] envprops = new String[] {"PROP1=VAL1", "PROP2=VAL2" };
        	//Process startRosCore = Runtime.getRuntime().exec( "roscore", null, new File("/home/evan"));
	    	Process pythonProc = Runtime.getRuntime().exec( 
	    			"/opt/ros/diamondback/ros/bin/rosrun nxtRobot first_test.py");
	    	
	    	app.viewer.setPythonProcess(pythonProc);    	
    	
    	 } catch (IOException e) {
             e.printStackTrace();
             System.exit(1);
    	 }
        */
        
        f.add("Center", app.viewer);
        f.pack();
        f.setVisible(true);
        app.run();
    }

    void run()
    {
    	long start = System.currentTimeMillis();
    	int count = 0;
    	int hertz = 10;
        while(shouldRun) {
        	count++;
        	if ((System.currentTimeMillis() - start) >= (1000 * (1.0/hertz))){
	            viewer.updateDepth();
	            viewer.repaint();
            }
        }
        frame.dispose();
    }
    
}
