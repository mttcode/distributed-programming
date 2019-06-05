package at.ac.tuwien.infosys.rnue.implementation.multicast;

import at.ac.tuwien.infosys.rnue.interfaces.*;
import at.ac.tuwien.infosys.rnue.helpers.*;
import at.ac.tuwien.infosys.rnue.helpers.multicast.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class IsAliveReceiver implements IIsAliveReceiver {
	private InetAddress multAdd;
	private int multPort;
	private boolean alive;
	private MulticastSocket MSocket;
	private IHostList zoznamUzlov;
	
	public IsAliveReceiver(Properties properties, IHostList hostList) throws ShareMeException {
		try {
			multAdd = InetAddress.getByName(properties.getProperty(IConstants.MULTICAST_ADDRESS));
		} catch (UnknownHostException uhe) {
			System.out.println("IsAliveReceiver - InetAddress: "+uhe.getMessage());
		}
		multPort = Integer.parseInt(properties.getProperty(IConstants.MULTICAST_PORT));
		alive = true;
		
		try {
			MSocket = new MulticastSocket(multPort);
			MSocket.setSoTimeout(IConstants.IS_ALIVE_RECEIVER_TIMEOUT);
		} catch (IOException ioe) {
			System.out.println("IsAliveReceiver - MulticastSocket1: "+ioe.getMessage());	
		}

		zoznamUzlov = hostList;
	}
	
	public void run() {
		byte buf[] = new byte[IConstants.MAX_ISALIVE_PACKET_LENGTH];
				
		try {
			MSocket.joinGroup(multAdd);
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	
		while (alive) {
			DatagramPacket DPacket = new DatagramPacket(buf,buf.length);
			try {
				MSocket.receive(DPacket);
				Object obj = (new ObjectInputStream(new ByteArrayInputStream(DPacket.getData()))).readObject();
				if (obj instanceof IHostInfoMessage) {
					HostInfoImpl host = new HostInfoImpl();
					host.setHostInfoMessage((IHostInfoMessage) obj);
					host.setTimestamp((new Date()).getTime());
					zoznamUzlov.put(host);
				} 
			} catch (InterruptedIOException iioe) {
				//System.out.println("IsAliveReceiver - MulticastSocket: "+iioe.getMessage());
			} catch (IOException ioe) {
				System.out.println("IsAliveReceiver - DatagramPacket: "+ioe.getMessage());
			} catch (ClassNotFoundException cnfe) {
				System.out.println("IsAliveReceiver - Object: "+cnfe.getMessage());
			}
		}
		
		try {
			MSocket.leaveGroup(multAdd);
		} catch (IOException ioe) {
			System.out.println("IsAliveReceiver - MSocket: "+ioe.getMessage());
		}
	}
	
	public void stop() {
		alive = false;
	}
}
