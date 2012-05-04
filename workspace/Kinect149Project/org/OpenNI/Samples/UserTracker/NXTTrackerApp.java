package Kinxt;


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

	import java.awt.event.KeyEvent;
	import java.awt.event.KeyListener;
	import java.awt.event.WindowAdapter;
	import java.awt.event.WindowEvent;
	import java.io.File;
	import java.io.IOException;
	import java.io.OutputStream;

	import javax.swing.JFrame;

	public class NXTTrackerApp {

	    /**
		 * 
		 */
		public UserTracker viewer;
		private boolean shouldRun = true;
		private JFrame frame;
		private BluetoothConnection conn;
		
	    public NXTTrackerApp (JFrame frame)
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
	    	conn = new BluetoothConnection();
	    	try{
	    		conn.connect();
	    	} catch (IOException ioe){
	    		ioe.printStackTrace();
	    		System.exit(0);
	    	}
	    }

	    public static void main(String s[])
	    {
	    	
	    	
	        JFrame f = new JFrame("OpenNI User Tracker");
	        f.addWindowListener(new WindowAdapter() {
	            public void windowClosing(WindowEvent e) {System.exit(0);}
	        });
	        NXTTrackerApp app = new NXTTrackerApp(f);
	        
	        app.viewer = new UserTracker();
	        
	        f.add("Center", app.viewer);
	        f.pack();
	        f.setVisible(true);
	        app.run();
	    }

	    
	    void run()
	    {
	    	byte[] nxt_data = { 0,0, 0, 1 };
	    	long start = System.currentTimeMillis();
	    	int count = 0;
	    	int hertz = 200;
	        while(shouldRun) {
	        	count++;
	        	if ((System.currentTimeMillis() - start) >= (1000 * (1.0/hertz))){
		            viewer.updateDepth();
		            viewer.repaint();
		            viewer.update_speeds(nxt_data);
		            System.out.println("Left: " + nxt_data[0] + ", Right: " + nxt_data[1] + ", Claw: " + nxt_data[2]);
		            try{
		            	conn.send(nxt_data);
		            } catch (IOException ioe){
		            	System.out.println("Send to NXT failed...");
		            	ioe.printStackTrace();		            	
		            }
	            }
	        }
	        try{
	        	conn.sendStop();
	        } catch( IOException ioe ){
	        	System.out.println("clean shutdown of NXT failed...");
	        	ioe.printStackTrace();
	        }
	        frame.dispose();
	    }
	    
	}

