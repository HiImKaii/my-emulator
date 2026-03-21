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

import org.recompile.mobile.Mobile;

public class Connector
{
	public static final int READ = 1;
	public static final int READ_WRITE = 3;
	public static final int WRITE = 2;

	public static InputStream openInputStream(String name)
	{
		//System.out.println("Connector: " + name);
		if(name.startsWith("resource:")) // older Siemens phones?
		{
			return Mobile.getPlatform().loader.getMIDletResourceAsSiemensStream(name.substring(9));
		}
		else
		{
			//return Mobile.getPlatform().loader.getMIDletResourceAsStream(name); // possible
			System.out.println("Faked InputStream for "+name); // just in case //
			return new FakeInputStream();
		}
	}

	public static DataInputStream openDataInputStream(String name)
	{
		System.out.println("Faked DataInputStream: "+name);
		return new DataInputStream(new FakeInputStream());
	}

	public static Connection open(String name)
	{
		return open(name, READ_WRITE, false);
	}

	public static Connection open(String name, int mode)
	{
		return open(name, mode, false);
	}

	public static Connection open(String name, int mode, boolean timeouts)
	{
		try
		{
			if (name.startsWith("http://") || name.startsWith("https://"))
			{
				return new HttpConnectionImpl(name);
			}
			else if (name.startsWith("socket://"))
			{
				return new SocketConnectionImpl(name);
			}
			else if (name.startsWith("comm:"))
			{
				// Serial port — return a stub
				return new Connection() { public void close() {} };
			}
			else if (name.startsWith("bluetooth://"))
			{
				// Bluetooth — return a stub
				return new Connection() { public void close() {} };
			}
			else
			{
				// Default: return a stub connection
				System.out.println("Connector.open: unknown protocol: " + name);
				return new Connection() { public void close() {} };
			}
		}
		catch (Exception e)
		{
			System.out.println("Connector.open error: " + e.getClass().getName() + ": " + e.getMessage());
			return new Connection() { public void close() {} };
		}
	}

	public static DataOutputStream openDataOutputStream(String name)
	{
		try
		{
			OutputConnection c = (OutputConnection) open(name, WRITE, false);
			return c.openDataOutputStream();
		}
		catch (Exception e)
		{
			System.out.println("Connector.openDataOutputStream error: " + e.getMessage());
			return new DataOutputStream(new OutputStream() { public void write(int b) {} });
		}
	}

	public static OutputStream openOutputStream(String name)
	{
		try
		{
			OutputConnection c = (OutputConnection) open(name, WRITE, false);
			return c.openOutputStream();
		}
		catch (Exception e)
		{
			System.out.println("Connector.openOutputStream error: " + e.getMessage());
			return new OutputStream() { public void write(int b) {} };
		}
	}

	// Fake inputstream
	private static class FakeInputStream extends InputStream
	{
		public int available() { return 0; }
		public void close() { }
		public void mark() { }
		public boolean markSupported() { return false; }
		public int read() { return -1; }
		public int read(byte[] b) { return 0; }
		public int read(byte[] b, int off, int len) { return 0; }
		public void reset() { }
		public long skip(long n) { return 0; }
	}
}
