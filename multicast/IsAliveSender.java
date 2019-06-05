package at.ac.tuwien.infosys.rnue.implementation.multicast;

import at.ac.tuwien.infosys.rnue.interfaces.*;
import at.ac.tuwien.infosys.rnue.helpers.*;
import at.ac.tuwien.infosys.rnue.helpers.multicast.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class IsAliveSender extends TimerTask implements IIsAliveSender {
	private InetAddress multAdd;
	private int multPort, RMIPort;
	private String RMIAdd, serviceName, humanName;
	private MulticastSocket MSocket;
	private DatagramPacket DPacket;
	private HostInfoMessageImpl hostInfo;
	
	public IsAliveSender(Properties properties) {
		try {   
			multAdd = InetAddress.getByName(properties.getProperty(IConstants.MULTICAST_ADDRESS));
		} catch (UnknownHostException uhe) {
			System.out.println("IsAliveReceiver - InetAddress: "+uhe.getMessage());
		}
		RMIAdd = properties.getProperty(IConstants.RMI_REGISTRY_HOST);
		serviceName = properties.getProperty(IConstants.RMI_SERVICE_NAME);
		humanName = properties.getProperty(IConstants.HUMAN_READABLE_NAME);
		multPort = Integer.parseInt(properties.getProperty(IConstants.MULTICAST_PORT));
		RMIPort = Integer.parseInt(properties.getProperty(IConstants.RMI_REGISTRY_PORT));

		try {   
			MSocket = new MulticastSocket(multPort);
			MSocket.joinGroup(multAdd);
		} catch (IOException ioe) {
			System.out.println("IsAliveSender - MulticastSocket: "+ioe.getMessage());
		}

		hostInfo = new HostInfoMessageImpl();
		hostInfo.setHumanReadableName(humanName);
		hostInfo.setRegistryHost(RMIAdd);
		hostInfo.setRegistryPort(RMIPort);
		hostInfo.setServiceName(serviceName);
		
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		try {
			ObjectOutputStream output = new ObjectOutputStream(byteArray);
			output.writeObject(hostInfo);
		} catch (InvalidClassException ice) {
			System.out.println("IsAliveSender - Object: "+ice.getMessage());
		} catch (NotSerializableException nse) {
			System.out.println("IsAliveSender - Object: "+nse.getMessage());
		} catch (IOException ioe){
			System.out.println("IsAliveSender - Object: "+ioe.getMessage());
		}
		DPacket = new DatagramPacket(byteArray.toByteArray(), byteArray.toByteArray().length, multAdd, multPort);
	}

	public void run() {
		try {
			MSocket.send(DPacket);
		} catch (IOException ioe) {
			System.out.println("IsAliveSender - DatagramPacket: "+ioe.getMessage());
		}
	}

	public void stop() {
		try {
			MSocket.leaveGroup(multAdd);
		} catch (IOException ioe) {
			System.out.println("IsAliveSender - MulticastSocket: "+ioe.getMessage());
		}
		this.cancel();
	}
}
