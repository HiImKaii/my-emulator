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
package org.recompile.mobile;


import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.net.URL;
import java.net.URLClassLoader;

import java.lang.ClassLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.HashMap;
import java.util.ArrayList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.io.*;
import javax.microedition.midlet.MIDletStateChangeException;

public class MIDletLoader extends URLClassLoader
{
	public String name;
	public String icon;
	private String className;

	// Game JAR URL used by loadManifest() to read MANIFEST.MF directly
	public String gameJarUrl;

	public String suitename;

	private Class<?> mainClass;
	private MIDlet mainInst;

	private HashMap<String, String> properties = new HashMap<String, String>(32);


		// Constructor with explicit parent ClassLoader
	public MIDletLoader(ClassLoader parent, URL urls[])
	{
		super(urls, parent);

		init();
	}

	// Default constructor (backwards compatible)
	public MIDletLoader(URL urls[])
	{
		super(urls);
		init();
	}

	public void setGameJarUrl(String url)
	{
		this.gameJarUrl = url;
		loadManifest();  // Now safe to call — gameJarUrl is set
	}

	private void init()
	{
		try
		{
			System.setProperty("microedition.platform", "j2me");
			System.setProperty("microedition.profiles", "MIDP-2.0");
			System.setProperty("microedition.configuration", "CLDC-1.0");
			System.setProperty("microedition.locale", "en-US");
			System.setProperty("microedition.encoding", "file.encoding");
		}
		catch (Exception e)
		{
			System.out.println("Can't add CLDC System Properties");
		}

		properties.put("microedition.platform", "j2me");
		properties.put("microedition.profiles", "MIDP-2.0");
		properties.put("microedition.configuration", "CLDC-1.0");
		properties.put("microedition.locale", "en-US");
		properties.put("microedition.encoding", "file.encoding");
	}

	public void start() throws MIDletStateChangeException
	{
		Method start = null;

		try
		{
			mainClass = loadClass(className, true);

			Constructor constructor;
			constructor = mainClass.getConstructor();
			constructor.setAccessible(true);

			MIDlet.initAppProperties(properties);
			mainInst = (MIDlet)constructor.newInstance();
		}
		catch (Exception e)
		{
			System.out.println("Problem Constructing " + name + " class: " +className);
			System.out.println("Reason: "+e.getMessage());
			e.printStackTrace();
			System.exit(0);
			return;
		}

		try
		{
			while (start == null)
			{
				try
				{
					start = mainClass.getDeclaredMethod("startApp");
					start.setAccessible(true);
				}
				catch (NoSuchMethodException e)
				{
					mainClass = mainClass.getSuperclass();
					if (mainClass == null || mainClass == MIDlet.class)
					{
						throw e;
					}

					mainClass = loadClass(mainClass.getName(), true);
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Can't Find startApp Method");
			e.printStackTrace();
			System.exit(0);
			return;
		}

		try
		{
			start.invoke(mainInst);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void loadManifest()
	{
		// Read MANIFEST.MF directly from the game JAR URL,
		// not via findResource() which would find FreeJ2ME's own manifest first
		// (since FreeJ2ME JAR is first in MIDletLoader's URLs).
		java.util.jar.Manifest manifest = null;
		try {
			// JarFile works reliably; JarInputStream only returns manifest
			// when META-INF/MANIFEST.MF is the first entry (not always true).
			java.net.URI uri = new java.net.URI(gameJarUrl);
			java.util.jar.JarFile jarFile = new java.util.jar.JarFile(new java.io.File(uri), false);
			manifest = jarFile.getManifest();
			jarFile.close();
		} catch (Exception e) {
			System.out.println("Can't read game JAR manifest: " + e.getClass().getName() + ": " + e.getMessage());
		}

		if (manifest == null) {
			System.out.println("Manifest not found in game JAR!");
			return;
		}

		java.util.jar.Attributes attrs = manifest.getMainAttributes();
		for (java.util.Map.Entry<Object, Object> entry : attrs.entrySet()) {
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();
			properties.put(key, value);

			if (key.equals("MIDlet-1")) {
				System.out.println("Manifest MIDlet-1: " + value);
				String[] parts = value.split(",");
				if (parts.length >= 3) {
					name = parts[0].trim();
					icon = parts[1].trim();
					className = parts[2].trim();
					suitename = name;
					System.out.println("Parsed className: '" + className + "'");
				}
			}
		}

		if (suitename != null) {
			suitename = suitename.replace(":", "");
		}
	}


	public InputStream getResourceAsStream(String resource)
	{
		URL url;
		//System.out.println("Loading Resource: " + resource);

		if(resource.startsWith("/"))
		{
			resource = resource.substring(1);
		}

		try
		{
			url = findResource(resource);
			// Read all bytes, return ByteArrayInputStream //
			InputStream stream = url.openStream();

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count=0;
			byte[] data = new byte[4096];
			while (count!=-1)
			{
				count = stream.read(data);
				if(count!=-1) { buffer.write(data, 0, count); }
			}
			return new ByteArrayInputStream(buffer.toByteArray());
		}
		catch (Exception e)
		{
			// Resource not found — return empty stream instead of null
			return new ByteArrayInputStream(new byte[0]);
		}
	}


	public URL getResource(String resource)
	{
		if(resource.startsWith("/"))
		{
			resource = resource.substring(1);
		}
		URL url = findResource(resource);
		if (url != null) {
			return url;
		}
		// Resource not found — return a dummy URL to avoid NPE in callers
		try {
			return new URL("jar:file:/dev/null!//" + resource);
		} catch (Exception e) {
			return null;
		}
	}

	/*
		********  loadClass Modifies Methods with ObjectWeb ASM  ********
		Replaces java.lang.Class.getResourceAsStream calls with calls
		to Mobile.getResourceAsStream which calls
		MIDletLoader.getResourceAsStream(class, string)
	*/

	public InputStream getMIDletResourceAsStream(String resource)
	{
		//System.out.println("Get Resource: "+resource);

		URL url = getResource(resource);

		// Read all bytes, return ByteArrayInputStream //
		try
		{
			InputStream stream = url.openStream();

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count=0;
			byte[] data = new byte[4096];
			while (count!=-1)
			{
				count = stream.read(data);
				if(count!=-1) { buffer.write(data, 0, count); }
			}
			return new ByteArrayInputStream(buffer.toByteArray());
		}
		catch (Exception e)
		{
			// Resource not found — return empty stream instead of null
			// to prevent NPE crashes in game code (e.g. ByteArrayInputStream(null))
			return new ByteArrayInputStream(new byte[0]);
		}
	}

	public byte[] getMIDletResourceAsByteArray(String resource)
	{
		URL url = getResource(resource);

		// If URL is null or dummy, return empty array
		if (url == null) {
			return new byte[0];
		}

		try
		{
			InputStream stream = url.openStream();

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count=0;
			byte[] data = new byte[4096];
			while (count!=-1)
			{
				count = stream.read(data);
				if(count!=-1) { buffer.write(data, 0, count); }
			}
			return buffer.toByteArray();
		}
		catch (Exception e)
		{
			System.out.println(resource + " Not Found");
			return new byte[0];
		}
	}


	public Class loadClass(String name, boolean resolve) throws ClassNotFoundException
	{
		// Check if already loaded
		Class c = findLoadedClass(name);
		if (c != null) return c;

		// For java.* classes, always delegate to parent (standard Java classes)
		// This prevents ClassFormatError and maintains boot classloader hierarchy
		if (name.startsWith("java.")) {
			return super.loadClass(name, resolve);
		}

		// For org.recompile.mobile.*: always delegate to parent.
		// These are FreeJ2ME platform classes whose static fields (platform, display)
		// were initialized when the app started. Redefining them in MIDletLoader
		// would create a separate static namespace with null fields.
		if (name.startsWith("org.recompile.mobile.")) {
			return super.loadClass(name, resolve);
		}

		// For javax.microedition.*: delegate to parent (parent-first).
		// Loading these child-first into MIDletLoader causes type mismatches:
		// the preloaded javax.microedition.lcdui.Image is a different Class object
		// than the Image that PlatformImage implements → VerifyError at runtime.
		// With parent-first, they all come from AppClassLoader → same types throughout.
		if (name.startsWith("javax.microedition.")) {
			return super.loadClass(name, resolve);
		}

		// For game classes (everything else): child-first with ASM instrumentation.
		// Only game classes are instrumented to intercept Class.getResourceAsStream().
		String resource = name.replace(".", "/") + ".class";
		InputStream stream = getResourceAsStream(resource);
		if (stream != null) {
			try {
				// Instrument: replace Class.getResourceAsStream → Mobile.getResourceAsStream
				byte[] code = instrument(stream);
				c = defineClass(name, code, 0, code.length);
				if (resolve) resolveClass(c);
				return c;
			} catch (Exception e) {
				// Fall through to parent on error
			}
		}

		// Last resort: delegate to parent
		return super.loadClass(name, resolve);
	}

	public Class loadClass(String name) throws ClassNotFoundException
	{
		return loadClass(name, true);
	}


/* **************************************************************
 * Special Siemens Stuff
 * ************************************************************** */

	public InputStream getMIDletResourceAsSiemensStream(String resource)
	{
		URL url = getResource(resource);

		try
		{
			InputStream stream = url.openStream();

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count=0;
			byte[] data = new byte[4096];
			while (count!=-1)
			{
				count = stream.read(data);
				if(count!=-1) { buffer.write(data, 0, count); }
			}
			return new SiemensInputStream(buffer.toByteArray());
		}
		catch (Exception e)
		{
			return super.getResourceAsStream(resource);
		}
	}

	private class SiemensInputStream extends InputStream
	{
		private ByteArrayInputStream iostream;

		public SiemensInputStream(byte[] data)
		{
			iostream = new ByteArrayInputStream(data);
		}

		public int read()
		{
			int t = iostream.read();
			if (t == -1) { return 0; }
			return t;
		}
		public int read(byte[] b, int off, int len)
		{
			int t = iostream.read(b, off, len);
			if (t == -1) { return 0; }
			return t;
		}
	}


/* ************************************************************** 
 * Instrumentation
 * ************************************************************** */

	private byte[] instrument(InputStream stream) throws Exception
	{
		ClassReader reader = new ClassReader(stream);
		ClassWriter writer = new ClassWriter(0);
		ClassVisitor visitor = new ASMVisitor(writer);
		reader.accept(visitor, 0);
		return writer.toByteArray();
	}

	private class ASMVisitor extends ClassAdapter
	{
		public ASMVisitor(ClassVisitor visitor)
		{
			super(visitor);
		}

		public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces)
		{
			super.visit(version, access, name, signature, superName, interfaces);
		}

		public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions)
		{
			return new ASMMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
		}

		private class ASMMethodVisitor extends MethodAdapter implements Opcodes
		{
			public ASMMethodVisitor(MethodVisitor visitor)
			{
				super(visitor);
			}

			public void visitMethodInsn(int opcode, String owner, String name, String desc)
			{
				if(opcode == INVOKEVIRTUAL && name.equals("getResourceAsStream") && owner.equals("java/lang/Class"))
				{
					mv.visitMethodInsn(INVOKESTATIC, "org/recompile/mobile/Mobile", name, "(Ljava/lang/Class;Ljava/lang/String;)Ljava/io/InputStream;");
				}
				else
				{
					mv.visitMethodInsn(opcode, owner, name, desc);
				}
			}
		}
	}
}
