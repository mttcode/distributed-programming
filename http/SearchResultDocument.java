package at.ac.tuwien.infosys.rnue.implementation.http;

import java.util.*;
import java.net.*;
import at.ac.tuwien.infosys.rnue.interfaces.*;
import at.ac.tuwien.infosys.rnue.helpers.*;
import at.ac.tuwien.infosys.rnue.helpers.http.*;
import java.util.Properties.*;
import java.io.*;
import java.lang.*;

public class SearchResultDocument implements IDocument {
	private String con;	
	
	public SearchResultDocument(ISearchResult searchRes) {
		con = new String("<html><head><title>Search result</title></head><body>");
		for (Enumeration enOwn = searchRes.owners(); enOwn.hasMoreElements();) {
			String owner = enOwn.nextElement().toString();
			con = con.concat("<font face='Comic Sans MS' size='3' color='000000'><b>"+owner+"</b></font><br><br>");
			//System.out.println(owner);
			IFileList list = searchRes.getFileList(owner);
			String downloadURLPrefix = list.getDownloadURLPrefix();
			//System.out.println(downloadURLPrefix);
			if (list.size() != 0) {
				con = con.concat("<table border='1'><tr><th align='left' width='700'>Najdene subory</th><th align='left' width='160'>Velkost</th></tr>");
			}
			for (Enumeration enFile = list.files(); enFile.hasMoreElements();) {
				IFile file = (IFile) enFile.nextElement();
				//System.out.println(file.getFilePathAndName());
				con = con.concat("<tr><td><a href='"+downloadURLPrefix+file.getFilePathAndName()+"'>"+file.getFileName()+"</a></td><td>"+file.getFileSize()+" bytes</td></tr>");
			}
			con = con.concat("</table><hr>");
		}
		con = con.concat("<br><a href='/docs/index.html'><font face='Comic Sans MS' size='2' color='000000'><b>-=home=-</b></font></a>");
		con = con.concat("</body></html>");
	}

	public java.util.Date getLastModified() {
		return new java.util.Date();
	}

	public java.lang.String getContentType() {
		return new String("text/html");
	}

	public long getContentLength() {
		return con.getBytes().length;
	}

	public java.lang.String getCachingInfo() {
		return null;
	}

	public java.io.InputStream getContent() {
		return new ByteArrayInputStream(con.getBytes());
	}	
}
