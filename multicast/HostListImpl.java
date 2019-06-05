package at.ac.tuwien.infosys.rnue.implementation.multicast;

import at.ac.tuwien.infosys.rnue.interfaces.*;
import at.ac.tuwien.infosys.rnue.helpers.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

public class HostListImpl implements IHostList {
	private Hashtable hashList;
	private Thread threadForGC;
	
	public HostListImpl() {
		hashList = new Hashtable();
		GarbageCollector garbCol = new GarbageCollector(this);
		threadForGC = new Thread(garbCol);
		threadForGC.start();
	}
	
	class GarbageCollector implements Runnable {
		private IHostList hostList;
		private boolean logicVar;
		
		public GarbageCollector(IHostList list) {
			logicVar = true;
			hostList = list;
		}

		public void run() {
			while (logicVar) {
				for (Enumeration en = hostList.elements(); en.hasMoreElements();) {
					IHostInfo hostInfo = (IHostInfo) en.nextElement();
					if (((new Date()).getTime() - hostInfo.getTimestamp()) > IConstants.LIFETIME_OF_HOSTINFOS) {
						hostList.remove(new String(hostInfo.getHostInfoMessage().getRegistryHost()+":"+hostInfo.getHostInfoMessage().getRegistryPort()));
					} 
					//System.out.println(hostInfo.getHostInfoMessage().getHumanReadableName()); 
					
				}
				synchronized(this) {
					try {
						((Object)this).wait(IConstants.GC_INTERVAL);
					} catch (InterruptedException ie) {
						//nerob nic, len uplynul casovy interval
					}
				}
			}
		}

		public void stop() {
			logicVar = false;
			this.notify();
		}
	}
	
	public boolean contains(IHostInfo info) {
		synchronized(hashList) {
			return hashList.contains(info);
		}
	}

	public Enumeration elements() {
		synchronized(hashList) {
			return hashList.elements();
		}
	}

	public IHostInfo get(String hostID) {
		synchronized(hashList) {
			return (IHostInfo) hashList.get(hostID);
		}
	}

	public Enumeration keys() {
		synchronized(hashList) {
			return hashList.keys();
		}
	}

	public IHostInfo put(IHostInfo info) {
		synchronized(hashList) {
			StringBuffer host = new StringBuffer("                    "); 
			host.replace(0, info.getHostInfoMessage().getHumanReadableName().length()-1, info.getHostInfoMessage().getHumanReadableName());
			//System.out.println("Message from host: " + host + " at: " + (new Date(info.getTimestamp())).toString());
			return (IHostInfo) hashList.put(new String(info.getHostInfoMessage().getRegistryHost()+":"+info.getHostInfoMessage().getRegistryPort()), info);
		}
	}

	public IHostInfo remove(String hostID) {
		synchronized(hashList) {
			StringBuffer host = new StringBuffer("                    ");
			host.replace(0, ((IHostInfo) hashList.get(hostID)).getHostInfoMessage().getHumanReadableName().length()-1, ((IHostInfo) hashList.get(hostID)).getHostInfoMessage().getHumanReadableName());
			//System.out.println("Host removed: " + host + "\t reason: Timeout exceed!");
			return (IHostInfo) hashList.remove(hostID);
		}
	}

	public void stopGarbageCollector() {
		threadForGC.stop();
	}
}
