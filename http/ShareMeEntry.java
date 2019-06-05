package at.ac.tuwien.infosys.rnue.implementation.http;

import java.util.*;
import java.net.*;
import at.ac.tuwien.infosys.rnue.interfaces.*;
import at.ac.tuwien.infosys.rnue.helpers.*;
import at.ac.tuwien.infosys.rnue.helpers.http.*;
import java.util.Properties.*;
import java.io.*;
import java.lang.*;

public class ShareMeEntry implements IHTTPServerEntry {
	IShareMe share;
	ISearchResult searchRes;
	
	public ShareMeEntry(IShareMe share) {
		this.share = share;
	}

	public String getDescription() {
		return new String("Searching...");
	}

	public IDocument getDocument(String path) {
		String searchStr = null;
		
		System.out.println(this.getDescription());
		if (path.indexOf(IConstants.SHAREME_CGI) == 0) {
			String pars = path.substring(path.indexOf('?')+1);
			StringTokenizer st = new StringTokenizer(pars, "&");
			while (st.hasMoreTokens()) {
				String pair = st.nextToken();
				try {
					//System.out.println("pred: "+pair);
					pair = URLDecoder.decode(pair, "UTF-8");
					//System.out.println("po: "+pair);
					if (pair.indexOf(IConstants.SHAREME_PARAMNAME) == 0) {
						searchStr = pair.substring(pair.indexOf('=')+1);
					}
				} catch (UnsupportedEncodingException uee) {
					System.err.println(uee.getMessage());
				}
			}
			if (searchStr == null) {
				return (IDocument) new SimpleDocument("Bad parameters.<br>Missing search parameter.", true);
			} else {
				try {
					searchRes = share.search(searchStr);
				} catch (ShareMeException sme) {
					System.err.println(sme.getMessage());
				}
				return new SearchResultDocument(searchRes);
			}
		} else {
			return (IDocument) new SimpleDocument("Bad request format '"+path+"'.<br>Valid format: /cgi-bin/shareme?search=...", true);
		}
	}
}
