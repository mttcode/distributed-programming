package at.ac.tuwien.infosys.rnue.implementation;

import java.util.*;
import java.net.*;
import java.rmi.*;
import at.ac.tuwien.infosys.rnue.interfaces.*;
import at.ac.tuwien.infosys.rnue.helpers.*;
import at.ac.tuwien.infosys.rnue.helpers.search.*;
import at.ac.tuwien.infosys.rnue.helpers.http.*;
import at.ac.tuwien.infosys.rnue.implementation.multicast.*;
import at.ac.tuwien.infosys.rnue.implementation.search.*;
import at.ac.tuwien.infosys.rnue.implementation.security.*;
import at.ac.tuwien.infosys.rnue.implementation.http.*;
import java.util.Properties.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.security.*;

public class ShareMeImpl implements IShareMe {
	private Properties myProps;
	private Thread threadForSL;
	private Thread threadForIAR;
	private Thread threadForHTTPS;
	private HostListImpl zoznamUzlov;
	private IsAliveSender alSender;
	private Timer timer;
	private SearchEngineImpl searcher;
	private SecurityHelperImpl secHelp;
	
	public ShareMeImpl(Properties properties) throws ShareMeException {
		myProps = properties;
		
		try {
			InetAddress ia = InetAddress.getLocalHost();
			myProps.setProperty(IConstants.RMI_REGISTRY_HOST, ia.getHostName());
		}
		catch (java.net.UnknownHostException e) {
			System.out.println(e.getMessage());
		}
		zoznamUzlov = new HostListImpl();
		secHelp = new SecurityHelperImpl(properties);
	}

	public void start() throws ShareMeException {
		timer = new Timer();
		alSender= new IsAliveSender(myProps);
		
		IsAliveReceiver aliveRec = new IsAliveReceiver(myProps, zoznamUzlov);
		threadForIAR = new Thread(aliveRec);
		threadForIAR.start();
		
		timer.scheduleAtFixedRate(alSender, 0, IConstants.IS_ALIVE_INTERVAL);
		
		ShutdownListener myListener = new ShutdownListener(this, Integer.parseInt(myProps.getProperty(IConstants.SHUTDOWN_PORT)), myProps.getProperty(IConstants.SHUTDOWN_PASSWORD));
		threadForSL = new Thread(myListener);
		threadForSL.start();

		try {	
			LocateRegistry.createRegistry(Integer.parseInt(myProps.getProperty(IConstants.RMI_REGISTRY_PORT)));
			searcher = new SearchEngineImpl(myProps.getProperty(IConstants.FILE_BASE), new String("http://" + myProps.getProperty(IConstants.RMI_REGISTRY_HOST) +":"+ myProps.getProperty(IConstants.HTTP_SERVER_PORT) + IConstants.DOWNLOAD_PREFIX), secHelp);
			
			String rmi_url = new String("rmi://"+myProps.getProperty(IConstants.RMI_REGISTRY_HOST)+":"+myProps.getProperty(IConstants.RMI_REGISTRY_PORT)+"/"+myProps.getProperty(IConstants.RMI_SERVICE_NAME));
			
			Naming.rebind(rmi_url, (Remote) searcher);
		} catch (RemoteException re) {
			System.out.println(re.getMessage());
		} catch (MalformedURLException murle) {
			System.out.println(murle.getMessage());
		}
		
		HTTPServerImpl server = new HTTPServerImpl(new Integer(myProps.getProperty(IConstants.HTTP_SERVER_PORT)));
		server.register(IConstants.DOC_PREFIX, new DocumentEntry(myProps.getProperty(IConstants.DOCUMENT_BASE)));
                server.register(IConstants.DOWNLOAD_PREFIX, new DocumentEntry(myProps.getProperty(IConstants.FILE_BASE)));
                server.register(IConstants.CGIBIN_PREFIX, new ShareMeEntry(this));
		threadForHTTPS = new Thread(server);
		threadForHTTPS.start();

		System.out.println("ShareMeImpl: start()");
	}

	public void stop() throws ShareMeException {	
		timer.cancel();
		threadForIAR.stop();
		alSender.stop();
		zoznamUzlov.stopGarbageCollector();
		threadForHTTPS.stop();
		
		try {
			String rmi_url = new String("rmi://"+myProps.getProperty(IConstants.RMI_REGISTRY_HOST)+":"+myProps.getProperty(IConstants.RMI_REGISTRY_PORT)+"/"+myProps.getProperty(IConstants.RMI_SERVICE_NAME));
			Naming.unbind(rmi_url);
			UnicastRemoteObject.unexportObject(searcher, true);
		} catch (RemoteException re) {
			System.out.println(re.getMessage());
		} catch (MalformedURLException murle) {
			System.out.println(murle.getMessage());
		} catch (NotBoundException nbe) {
			System.out.println(nbe.getMessage());
		}	

		System.out.println("ShareMeImpl: stop()");
	}

	public ISearchResult search(String searchStr) throws ShareMeException {
		ISearchRequest searchRequest;
		ISearchResult searchResult = new SearchResultImpl();
		byte[] sign;
		byte[] signature;
		IFileList fileList;
		PublicKey pubKey;
		
		sign = secHelp.sign(searchStr);
		searchRequest = new SearchRequestImpl(searchStr, sign, myProps.getProperty(IConstants.HUMAN_READABLE_NAME));
		if (searchRequest == null) {
			throw new ShareMeException("Null search request.");
		}
		for (Enumeration en = zoznamUzlov.elements(); en.hasMoreElements();) {
			IHostInfo info = (IHostInfo) en.nextElement();
			ISearchResponse searchResponse;
			String rmi_url = new String("rmi://"+info.getHostInfoMessage().getRegistryHost()+":"+info.getHostInfoMessage().getRegistryPort()+"/"+info.getHostInfoMessage().getServiceName());
			try {
				pubKey = secHelp.getPublicKeyFromCA(info.getHostInfoMessage().getHumanReadableName());
				ISearchEngine searching = (ISearchEngine) Naming.lookup(rmi_url);
				searchResponse = searching.search(searchRequest);
				signature = searchResponse.getSignature();
				fileList = searchResponse.getFileList();
				if (secHelp.verify(fileList, signature, pubKey)) {
					searchResult.putFileList(info.getHostInfoMessage().getHumanReadableName(), fileList);
				}
			} catch (ShareMeException sme) {
				System.out.println(sme.getMessage());
			} catch (RemoteException re) {
				System.out.println(re.getMessage()+"\t"+info.getHostInfoMessage().getHumanReadableName());
			} catch (MalformedURLException murle) {
				System.out.println(murle.getMessage());
			} catch (NotBoundException nbe) {
				System.out.println(nbe.getMessage());
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
		return searchResult;
	}
}
