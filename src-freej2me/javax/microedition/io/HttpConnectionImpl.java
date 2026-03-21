/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package javax.microedition.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class HttpConnectionImpl implements HttpConnection
{
	private String urlString;
	private String method = GET;
	private java.util.HashMap<String, String> requestHeaders = new java.util.HashMap<String, String>();

	private HttpURLConnection connection;
	private int responseCode = -1;
	private String responseMessage = "";

	private InputStream inputStream;
	private OutputStream outputStream;

	public HttpConnectionImpl(String url) { this.urlString = url; }

	private void connect()
	{
		if (connection != null) return;
		try {
			URL url = new URL(urlString);
			URLConnection urlConn = url.openConnection();
			if (!(urlConn instanceof HttpURLConnection)) return;
			connection = (HttpURLConnection) urlConn;
			connection.setRequestMethod(method);
			for (java.util.Map.Entry<String, String> e : requestHeaders.entrySet()) {
				connection.setRequestProperty(e.getKey(), e.getValue());
			}
			if (method.equals(POST)) connection.setDoOutput(true);
			connection.connect();
		} catch (Exception e) { /* silent */ }
	}

	public String getURL() { return urlString; }
	public String getProtocol() { try { return new URI(urlString).getScheme(); } catch (Exception e) { return "http"; } }
	public String getHost() { try { return new URI(urlString).getHost(); } catch (Exception e) { return ""; } }
	public int getPort() { try { return new URI(urlString).getPort(); } catch (Exception e) { return -1; } }
	public String getFile() { try { return new URI(urlString).getPath(); } catch (Exception e) { return ""; } }
	public String getQuery() { try { return new URI(urlString).getQuery(); } catch (Exception e) { return null; } }
	public String getRef() { try { return new URI(urlString).getFragment(); } catch (Exception e) { return null; } }

	public String getRequestMethod() { return method; }
	public void setRequestMethod(String m) { this.method = m; }
	public String getRequestProperty(String key) { return requestHeaders.get(key); }
	public void setRequestProperty(String key, String value) { requestHeaders.put(key, value); }

	public int getResponseCode() { try { connect(); if (responseCode==-1) { responseCode=connection.getResponseCode(); responseMessage=connection.getResponseMessage(); } return responseCode; } catch (Exception e) { return -1; } }
	public String getResponseMessage() { try { connect(); if (responseCode==-1) { responseCode=connection.getResponseCode(); responseMessage=connection.getResponseMessage(); } return responseMessage; } catch (Exception e) { return ""; } }
	public long getExpiration() { try { connect(); return connection.getExpiration(); } catch (Exception e) { return 0; } }
	public long getDate() { try { connect(); long d=connection.getDate(); return d>0?d:0; } catch (Exception e) { return 0; } }
	public long getLastModified() { try { connect(); return connection.getLastModified(); } catch (Exception e) { return 0; } }
	public String getHeaderField(String name) { try { connect(); return connection.getHeaderField(name); } catch (Exception e) { return null; } }
	public String getHeaderField(int n) { try { connect(); return connection.getHeaderField(n); } catch (Exception e) { return null; } }
	public String getHeaderFieldKey(int n) { try { connect(); return connection.getHeaderFieldKey(n); } catch (Exception e) { return null; } }
	public long getHeaderFieldDate(String name, long def) { try { connect(); return connection.getHeaderFieldDate(name, def); } catch (Exception e) { return def; } }
	public int getHeaderFieldInt(String name, int def) { try { connect(); return connection.getHeaderFieldInt(name, def); } catch (Exception e) { return def; } }

	public String getEncoding() { return null; }
	public long getLength() { return getHeaderFieldInt("Content-Length", -1); }
	public String getType() { return getHeaderField("Content-Type"); }

	public InputStream openInputStream()
	{
		if (inputStream != null) return inputStream;
		try {
			connect();
			int code = getResponseCode();
			if (code >= 200 && code < 300) {
				inputStream = connection.getInputStream();
			} else {
				inputStream = connection.getErrorStream();
				if (inputStream == null) inputStream = new java.io.ByteArrayInputStream(new byte[0]);
			}
		} catch (Exception e) { inputStream = new java.io.ByteArrayInputStream(new byte[0]); }
		return inputStream;
	}

	public DataInputStream openDataInputStream() { return new DataInputStream(openInputStream()); }
	public OutputStream openOutputStream() { try { connect(); if (outputStream==null) outputStream=connection.getOutputStream(); return outputStream; } catch (Exception e) { return new java.io.OutputStream() { public void write(int b) {} }; } }
	public DataOutputStream openDataOutputStream() { return new DataOutputStream(openOutputStream()); }
	public void close() { try { if (connection!=null) connection.disconnect(); } catch (Exception e) {} }
}
