package at.ac.tuwien.infosys.rnue.implementation.http;

import java.util.*;
import java.net.*;
import at.ac.tuwien.infosys.rnue.interfaces.*;
import at.ac.tuwien.infosys.rnue.helpers.*;
import at.ac.tuwien.infosys.rnue.helpers.http.*;
import java.util.Properties.*;
import java.io.*;
import java.lang.*;

public class HTTPServerImpl implements IHTTPServer {
	private ServerSocket sSocket;
	private Socket socket;
	private boolean logicVar;
	private Hashtable hash;

	public HTTPServerImpl(Integer port) {
		try {
			sSocket = new ServerSocket(port.intValue());
			sSocket.setSoTimeout(IConstants.HTTP_SERVER_TIMEOUT);
		} catch (SocketException se) {
			System.out.println(se.getMessage());
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
		}
		hash = new Hashtable();
		logicVar = true;
	}

	class RequestHandlerThread extends Thread {
		private Socket socket;
		private BufferedReader in;
		//private PrintWriter out;
		private DataOutputStream out;
		private String method;
		private String path;
		private String protocol;
		private String host;
		private IDocument response;
		
		public RequestHandlerThread(Socket sock) {
			socket = sock;
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				//out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
				out = new DataOutputStream(socket.getOutputStream());
			} catch (IOException ioe) {
				System.err.println(ioe.getMessage());
			}
		}

		public void run() {
			String input1 = null; 
			String input2 = null;
			try { 
				input1 = in.readLine();
			} catch (IOException ioe) {
				System.err.println(ioe.getMessage());
			}
			StringTokenizer st = new StringTokenizer(input1);
			if (st.countTokens() != 3) this.throwException(400, "Bad request format.");
			else {
				// nacitanie metody
				if (st.hasMoreTokens()) {
					method = st.nextToken();
					//System.out.println("method: "+method);
					// overenie metody
					if ((method.compareToIgnoreCase("GET")) != 0) this.throwException(501, "Method '"+method+"' not implemented.");
                	        	// nacitanie cesty
					if (st.hasMoreTokens()) {
						path = st.nextToken();
						try {
							path = URLDecoder.decode(path, "UTF-8");
						} catch (UnsupportedEncodingException uee) {
							System.err.println(uee.getMessage());
						}
						//System.out.println("path: "+path);
						// nacitanie protokolu
			                        if (st.hasMoreTokens()) {
							protocol = st.nextToken();
							//System.out.println("protocol: "+protocol);
							// ak je protocol http/1.1, je potrebne nacitat host
							if ((protocol.compareToIgnoreCase("HTTP/1.0")) != 0)
								if ((protocol.compareToIgnoreCase("HTTP/1.1")) != 0)
									this.throwException(400, "Bad request format.<br>Unknown protocol '"+protocol+"'");
							if ((protocol.compareToIgnoreCase("HTTP/1.1")) == 0) {
								try {
									while ((input2 = in.readLine()).indexOf("Host:") == -1) ;
								} catch (IOException ioe) {
									System.err.println(ioe.getMessage());
								}
								st = new StringTokenizer(input2);
								while (st.hasMoreTokens()) {
									st.nextToken();
									host = st.nextToken();
									//System.out.println("host: "+host);
								}
								if (host == null) this.throwException(400, "Bad request format.<br>Missing host.");
							}
							for (Enumeration en = hash.keys(); en.hasMoreElements();) {
								String key = en.nextElement().toString();
								if (path.indexOf(key) == 0) {
									try {
										IHTTPServerEntry entry = (IHTTPServerEntry) hash.get(key);
										response = entry.getDocument(path.replaceFirst(key, ""));
										this.buildHead(response);
										socket.close();
									} catch (HTTPException he) {
										this.throwException(404, "File not found '"+path+"'.");
									} catch (ShareMeException sme) {
										System.err.println(sme.getMessage());
									} catch (IOException ioe) {
										System.err.println(ioe.getMessage());
									}
									break;
								}
							}
							this.throwException(400, "Bad request format.<br>Unknown prefix in path '"+path+"'.");
						} else this.throwException(400, "Bad request format.");
					} else this.throwException(400, "Bad request format.");
				} else this.throwException(400, "Bad request format.");
			}
		}
/*		
		private void buildHead(IDocument resp) {
			String line;
			in = new BufferedReader(new InputStreamReader(resp.getContent()));
			
			out.println("HTTP/1.1 200 OK");
			out.println("Date: "+RFC1123DateFormatter.format(new java.util.Date()));
			out.println("Server: ShareMe server Krizova Ves");
			out.println("Host: ruzin.fei.tuke.sk:7217");
			out.println("Connection: Close");
			out.println("Last-Modified: "+RFC1123DateFormatter.format(resp.getLastModified()));
			out.println("Content-Length: "+resp.getContentLength());
			out.println("Content-Type: "+resp.getContentType());
			out.println("Cache-Control: no-cache");
			out.println("Expires: "+RFC1123DateFormatter.format(new java.util.Date())+"\n");
			try {
				while ((line = in.readLine()) != null) out.println(line);
			} catch (IOException ioe) {
				System.err.println(ioe.getMessage());
			}
			out.flush();
			out.close();
		} 
*/
		private void buildHead(IDocument resp) {
			String line;
			BufferedInputStream inStream = new BufferedInputStream(resp.getContent());
			byte[] buf = new byte[1000];
			
			try {
				out.writeBytes("HTTP/1.1 200 OK\n");
				out.writeBytes("Date: "+RFC1123DateFormatter.format(new java.util.Date())+"\n");
				out.writeBytes("Server: ShareMe server Krizova Ves\n");
				out.writeBytes("Host: ruzin.fei.tuke.sk:7217\n");
				out.writeBytes("Connection: Close\n");
				out.writeBytes("Last-Modified: "+RFC1123DateFormatter.format(resp.getLastModified())+"\n");
				out.writeBytes("Content-Length: "+resp.getContentLength()+"\n");
				out.writeBytes("Content-Type: "+resp.getContentType()+"\n");
				out.writeBytes("Cache-Control: no-cache\n");
				out.writeBytes("Expires: "+RFC1123DateFormatter.format(new java.util.Date())+"\n\n");
				while ((inStream.read(buf)) != -1) {out.write(buf);buf = null;buf = new byte[1000];}
				out.flush();
	                        out.close();
			} catch (IOException ioe) {
				//System.err.println(ioe.getMessage());
			}
		}
			                      
		private void throwException(int code, String message) {
			HTTPException exc = new HTTPException(code, message);
			try {
				switch (code) {
					case 400: out.writeBytes("HTTP/1.1 "+code+" Bad Request\n");break;
					case 404: out.writeBytes("HTTP/1.1 "+code+" Not Found\n");break;
					case 501: out.writeBytes("HTTP/1.1 "+code+" Not Implemented\n");break;
					default:  break;
				}
				out.writeBytes("Date: "+RFC1123DateFormatter.format(new java.util.Date())+"\n");
        	        	out.writeBytes("Server: ShareMe server Krizova Ves\n");
				out.writeBytes("Host: ruzin.fei.tuke.sk:7217\n");
				out.writeBytes("Last-Modified: "+RFC1123DateFormatter.format(new java.util.Date())+"\n");
				out.writeBytes("Cache-Control: null\n");
				out.writeBytes("Content-Length: "+exc.getErrorDocument().getBytes().length+"\n");
				out.writeBytes("Content-Type: text/html\n");
				out.writeBytes("Connection: Close\n\n");
				out.writeBytes(exc.getErrorDocument());
				out.flush();
				out.close();
			} catch (IOException ioe) {
				System.err.println(ioe.getMessage());
			}
		}
	}
	
	public void run() {
		while (logicVar) {
			try {
				socket = sSocket.accept();
				RequestHandlerThread reqThread = new RequestHandlerThread(socket);
				reqThread.start();
			} catch (InterruptedIOException iioe) {
				//uplynul casovy okamih
			} catch (SecurityException se) {
				System.err.println(se.getMessage());
				try {
					socket.close();
				} catch (IOException ioe) {
					System.err.println(ioe.getMessage());
				}
			} catch (IOException ioe) {
				System.err.println(ioe.getMessage());
			}
		}
	}

	public void stop() {
		logicVar = false;
	}

	public void register(String path, IHTTPServerEntry url) throws ShareMeException {
		try {
			hash.put(path, url);
			//System.out.println(path+"\t"+url);
		} catch (NullPointerException npe) {
			throw new ShareMeException(npe.getMessage());
		}
	}

	public void unregister(java.lang.String path) throws ShareMeException {
		try {
			hash.remove(path);
		} catch (NullPointerException npe) {
			throw new ShareMeException(npe.getMessage());
		}
	}
}
