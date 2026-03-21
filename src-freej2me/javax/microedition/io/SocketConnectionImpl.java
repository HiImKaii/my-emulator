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
import java.net.Socket;
import java.net.URI;

public class SocketConnectionImpl implements SocketConnection
{
	private String urlString;
	private String host;
	private int port;
	private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;

	public SocketConnectionImpl(String url)
	{
		this.urlString = url;
		try
		{
			URI uri = new URI(url);
			this.host = uri.getHost();
			this.port = uri.getPort();
			if (port == -1) this.port = 80;
		}
		catch (Exception e)
		{
			this.host = "";
			this.port = 0;
		}
	}

	private void connect() throws Exception
	{
		if (socket != null) return;
		socket = new Socket(host, port);
		socket.setTcpNoDelay(true);
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
	}

	public String getAddress() { return host + ":" + port; }
	public String getLocalAddress() { try { return socket.getLocalAddress().getHostAddress(); } catch (Exception e) { return ""; } }
	public int getLocalPort() { try { return socket.getLocalPort(); } catch (Exception e) { return -1; } }
	public int getPort() { return port; }

	public int getSocketOption(byte option)
	{
		try
		{
			if (option == DELAY) return socket.getTcpNoDelay() ? 1 : 0;
			if (option == SNDBUF) return socket.getSendBufferSize();
			if (option == RCVBUF) return socket.getReceiveBufferSize();
			if (option == LINGER) return socket.getSoLinger();
			if (option == KEEPALIVE) return socket.getKeepAlive() ? 1 : 0;
		}
		catch (Exception e) {}
		return -1;
	}

	public void setSocketOption(byte option, int value)
	{
		try
		{
			if (option == DELAY) socket.setTcpNoDelay(value != 0);
			if (option == SNDBUF) socket.setSendBufferSize(value);
			if (option == RCVBUF) socket.setReceiveBufferSize(value);
			if (option == LINGER) socket.setSoLinger(value > 0, value);
			if (option == KEEPALIVE) socket.setKeepAlive(value != 0);
		}
		catch (Exception e) {}
	}

	public InputStream openInputStream()
	{
		try { connect(); return inputStream; } catch (Exception e) { return new java.io.ByteArrayInputStream(new byte[0]); }
	}

	public DataInputStream openDataInputStream()
	{
		return new DataInputStream(openInputStream());
	}

	public OutputStream openOutputStream()
	{
		try { connect(); return outputStream; } catch (Exception e) { return new java.io.OutputStream() { public void write(int b) {} }; }
	}

	public DataOutputStream openDataOutputStream()
	{
		return new DataOutputStream(openOutputStream());
	}

	public void close()
	{
		try { if (socket != null) socket.close(); } catch (Exception e) {}
	}
}
