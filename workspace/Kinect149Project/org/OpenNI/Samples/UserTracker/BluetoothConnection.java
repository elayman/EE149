package Kinxt;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

public class BluetoothConnection implements DiscoveryListener{
    
    private static Object lock=new Object();
    public ArrayList<RemoteDevice> devices;
    private StreamConnection sock;
    private String URL = "";
    private OutputStream os = null;
    
    public BluetoothConnection() {
        devices = new ArrayList<RemoteDevice>();
    }
    
    public boolean connect() throws IOException{
    	LocalDevice localdev = LocalDevice.getLocalDevice();
    	DiscoveryAgent agent = localdev.getDiscoveryAgent();
        agent.startInquiry(DiscoveryAgent.GIAC, this);
        
        try {
            synchronized(lock){
                lock.wait();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        
        UUID[] uuidSet = new UUID[1];
        uuidSet[0]=new UUID("0003", true); //SPP
        
        int[] attrIDs =  new int[] {
                0x0100 // Service name
        };
        
        for (RemoteDevice device : devices) {
        	if(!device.getFriendlyName(true).equals("NXT"))
        		continue;
        	agent.searchServices(
                    attrIDs,uuidSet,device,this);
            
            
            try {
                synchronized(lock){
                    lock.wait();
                }
               
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
            
            
            System.out.println("Service search finished.");
        }
        return !URL.isEmpty() && (sock != null);
    }
        
    public static void main(String[] args) {
        
        BluetoothConnection listener =  new BluetoothConnection();
        
        try{
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            DiscoveryAgent agent = localDevice.getDiscoveryAgent();
            agent.startInquiry(DiscoveryAgent.GIAC, listener);
            
            try {
                synchronized(lock){
                    lock.wait();
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            
            
            System.out.println("Device Inquiry Completed. ");
            
       
            UUID[] uuidSet = new UUID[1];
            uuidSet[0]=new UUID(0x0003); //SPP
            
            int[] attrIDs =  new int[] {
                    0x0100 // Service name
            };
            
            for (RemoteDevice device : listener.devices) {
                agent.searchServices(
                        attrIDs,uuidSet,device,listener);
                
                
                try {
                    synchronized(lock){
                        lock.wait();
                    }
                   
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
                
                
                System.out.println("Service search finished.");
                if(!listener.URL.isEmpty())
                	break;
            }
            //tell the program to stop
            byte[] payload = {97, 97, (byte)0x01};
            
            if( listener.sock != null && listener.send(payload)){
            	System.out.println("Sent to the terminator");
            } else {
            	System.out.println("Failed to send to the terminator...");
            }
            Thread.sleep(5000);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

  

    @Override
    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass arg1) {
        String name;
        try {
            name = btDevice.getFriendlyName(false);
        } catch (Exception e) {
            name = btDevice.getBluetoothAddress();
        }
        
        devices.add(btDevice);
        System.out.println("device found: " + name);
        
    }

    @Override
    public void inquiryCompleted(int arg0) {
        synchronized(lock){
            lock.notify();
        }
    }

    @Override
    public void serviceSearchCompleted(int arg0, int arg1) {
        synchronized (lock) {
            lock.notify();
        }
    }

    @Override
    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
    	if(servRecord.length == 0)
    		return;
    	try{
    	String name = servRecord[0].getHostDevice().getFriendlyName(true);
    	if(!name.equals("NXT")){
    		System.out.println("Not looking for " + name);
    		return;
    	}
        for (int i = 0; i < servRecord.length; i++) {
            String url = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
            System.out.println("url is: " + url);
            if (url == null) {
                continue;
            }
            
            URL = url.split(";")[0];
            sock = (StreamConnection)Connector.open(URL);
		}
			
//            if (serviceName != null) {
//                System.out.println("service " + serviceName.getValue() + " found " + url);
//                
//                if(serviceName.getValue().equals("OBEX Object Push")){
//                    sendMessageToDevice(url);                
//                }
//            } else {
//                System.out.println("service found " + url);
//            }
            
    	} catch( IOException ioe){
    		ioe.printStackTrace();
    		return;
        }
    }
    
    /**
     * Sends a telegram to the NXT to stop current program.
     * @return true
     * @throws IOException if the send fails
     */
    public boolean sendStop() throws IOException{
    	if( os == null )
			os = sock.openOutputStream();
    	byte[] msg = {2,0, (byte)0x80, 1 };
    	os.write(msg);
    	os.flush();
    	return true;
    }
    public boolean send(byte[] payload) throws IOException{

    		if( os == null )
    			os = sock.openOutputStream();
			int length = 7 + payload.length;
			//Length - 2 because the length bites are not included.
			byte LSB = (byte)( (length-2) & 0xFF );
			byte MSB = (byte)( ((length-2) >> 8) & 0xFF);
			byte[] msg = new byte[length];
//			//message length LSB
			msg[0] = LSB;
//			//message length MSB
			msg[1] = MSB;
			//no-reply
			msg[2] = (byte) 0x80;
			//message op code
			msg[3] = (byte) 0x09;
			//inbox number
			msg[4] = 1;
			//payload length
			msg[5] = (byte)( payload.length + 1);
			//payload
			for(int i = 0; i<payload.length;i++){
				msg[i+6] = payload[i];
			}
			//Null terminate!
			msg[length-1] = 0;
			os.write(msg);
			os.flush();
			return true;
    }

}