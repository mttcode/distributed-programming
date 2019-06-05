package at.ac.tuwien.infosys.rnue.implementation.search;

import java.util.*;
import java.net.*;
import java.rmi.*;
import at.ac.tuwien.infosys.rnue.interfaces.*;
import at.ac.tuwien.infosys.rnue.helpers.*;
import at.ac.tuwien.infosys.rnue.helpers.search.*;
import at.ac.tuwien.infosys.rnue.implementation.multicast.*;
import java.util.Properties.*;
import java.rmi.server.*;
import java.security.*;

public class SearchEngineImpl extends UnicastRemoteObject implements ISearchEngine {
	private IFileSystemHelper sysHelp;
	private String downloadURLPrefix;
	private ISecurityHelper secHelp;
	
	public SearchEngineImpl(String fileBase, String downloadURLPrefix, ISecurityHelper secHelp) throws RemoteException {
		try {
			sysHelp = new FileSystemHelperImpl(fileBase);
		} catch (ShareMeException sme) {
			System.out.println(sme.getMessage());
		}
		this.downloadURLPrefix = downloadURLPrefix;
		this.secHelp = secHelp;
	}
	
	public ISearchResponse search(ISearchRequest request) throws ShareMeException {
		byte[] sign;
		PublicKey pubKey;
		byte[] signature;
		
		if (request != null) {
			pubKey = secHelp.getPublicKeyFromCA(request.getRequestOriginator());
			signature = request.getSignature();
			IFileList fileList = new FileListImpl(downloadURLPrefix, sysHelp.searchFor(request.getSearchString()));
			if (signature == null) {
				ISearchResponse response = new SearchResponseImpl(fileList, null);
				return response;
			} if (secHelp.verify(request.getSearchString(), signature, pubKey)) {
				sign = secHelp.sign(fileList);
				ISearchResponse response = new SearchResponseImpl(fileList, sign);
				return response;
			} else {
				throw new ShareMeException("<"+request.getRequestOriginator()+">\nSearchRequest's signature is not valid.\n");
			}
		} else {
			throw new ShareMeException("Null search request.");
		}
	}
}
