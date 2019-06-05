package at.ac.tuwien.infosys.rnue.implementation.multicast;

import at.ac.tuwien.infosys.rnue.helpers.*;
import at.ac.tuwien.infosys.rnue.interfaces.*;
import java.net.*;
import java.lang.*;
import java.io.*;

public class ShutdownListener implements Runnable {
	private int port;
	private IShareMe shareMe;
	private String end;

	public ShutdownListener(IShareMe shareMe, int port, String passwd) throws ShareMeException {
		this.shareMe = shareMe;
		this.port = port;
		this.end = IConstants.EXIT_FLAG + " " + passwd;
	}
	
	public void run() {
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException se) {
			System.out.println("\n"+se.getMessage());
		}
		
		byte buffer[] = new byte[IConstants.MAX_UDP_PACKET_LENGTH];
		String rec_end; 
		
		do {
			DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
			try {
				socket.receive(packet);
			} catch (IOException ioe) {
				System.out.println("\n"+ioe.getMessage());
			}
			rec_end = new String(packet.getData(), packet.getOffset(), packet.getLength());
		} while(!end.equals(rec_end));
		try {
			shareMe.stop();
		} catch (ShareMeException sme) {
			System.out.println("\n"+sme.getMessage());
		}
	}
}
