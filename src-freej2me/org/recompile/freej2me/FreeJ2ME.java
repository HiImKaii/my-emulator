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
package org.recompile.freej2me;

/*
	FreeJ2ME - AWT
*/

import org.recompile.mobile.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import javax.imageio.ImageIO;

public class FreeJ2ME
{
	public static void main(String args[])
	{
		FreeJ2ME app = new FreeJ2ME(args);
	}

	private Frame main;
	private int lcdWidth;
	private int lcdHeight;
	private int scaleFactor = 1;

	private LCD lcd;

	private int xborder;
	private int yborder;

	private PlatformImage img;

	private Config config;
	private boolean useNokiaControls = false;
	private boolean useSiemensControls = false;
	private boolean useMotorolaControls = false;
	private boolean rotateDisplay = false;
	private int limitFPS = 0;

	private boolean[] pressedKeys = new boolean[128];

	// True when the current screen is a TextBox or TextField — enables laptop keyboard input
	public static volatile boolean isTextInputMode = false;

	public static void setTextInputMode(boolean mode) {
		isTextInputMode = mode;
	}

	// Linux/X11 key repeat: AWT fires multiple keyPressed before keyReleased.
	// Track the last released keycode to detect and filter autorepeat.
	private int lastReleasedKeycode = -1;
	private long lastReleasedTime = 0;
	private static final long KEY_RELEASE_WINDOW_MS = 50;

	// Guard: prevent keyTyped from calling handlePhoneKey multiple times for the same
	// physical key press. When keyPressed sends a key, it sets this to that keycode.
	// keyTyped checks this and skips if the same key is still "in flight".
	private int keyTypedInFlight = -1;

	// When a mapped game key is pressed (mobikey != 0), set guardActive=true and
	// record timestamp. keyTyped checks this and skips while guard is active.
	// Guard expires after 100ms so that normal typing is not affected.
	private long gameKeyGuardTime = 0;
	private boolean gameKeyGuardActive = false;
	private static final long GAME_KEY_GUARD_NS = 100_000_000; // 100ms in nanoseconds

	private static int keyEventCounter = 0;

	// Nokia multi-tap: map laptop key to how many presses needed
	// Key groups: 2=ABC, 3=DEF, 4=GHI, 5=JKL, 6=MNO, 7=PQRS, 8=TUV, 9=WXYZ, 0=space
	private static int getTapCount(char c) {
		c = Character.toLowerCase(c);
		if ("abc2".indexOf(c) >= 0) return "abc2".indexOf(c) + 1;
		if ("def3".indexOf(c) >= 0) return "def3".indexOf(c) + 1;
		if ("ghi4".indexOf(c) >= 0) return "ghi4".indexOf(c) + 1;
		if ("jkl5".indexOf(c) >= 0) return "jkl5".indexOf(c) + 1;
		if ("mno6".indexOf(c) >= 0) return "mno6".indexOf(c) + 1;
		if ("pqrs7".indexOf(c) >= 0) return "pqrs7".indexOf(c) + 1;
		if ("tuv8".indexOf(c) >= 0) return "tuv8".indexOf(c) + 1;
		if ("wxyz9".indexOf(c) >= 0) return "wxyz9".indexOf(c) + 1;
		if (" 0".indexOf(c) >= 0) return 1;
		return 0;
	}

	private static int getKeyCode(char c) {
		c = Character.toLowerCase(c);
		if ("abc2".indexOf(c) >= 0) return Mobile.KEY_NUM2;
		if ("def3".indexOf(c) >= 0) return Mobile.KEY_NUM3;
		if ("ghi4".indexOf(c) >= 0) return Mobile.KEY_NUM4;
		if ("jkl5".indexOf(c) >= 0) return Mobile.KEY_NUM5;
		if ("mno6".indexOf(c) >= 0) return Mobile.KEY_NUM6;
		if ("pqrs7".indexOf(c) >= 0) return Mobile.KEY_NUM7;
		if ("tuv8".indexOf(c) >= 0) return Mobile.KEY_NUM8;
		if ("wxyz9".indexOf(c) >= 0) return Mobile.KEY_NUM9;
		if (" 0".indexOf(c) >= 0) return Mobile.KEY_NUM0;
		return 0;
	}

	// Track last key for Nokia multi-tap cycling (e.g. 2 twice = B, 2 three times = C)
	private int lastTapKey = 0;
	private int lastTapCount = 0;
	private long lastTapTime = 0;
	private static final long TAP_TIMEOUT_MS = 600;
	// Minimum delay between consecutive phone key press/release pairs (in ms).
	// Prevents events from arriving faster than the game thread can process them,
	// which would cause key state to be jumbled/confused.
	private static final long KEY_INJECTION_DELAY_MS = 25;
	// Flag: prevents keyTyped from re-sending a key that keyPressed already handled
	private boolean keyPressedHandled = false;

	private void handlePhoneKey(char c) {
		int keyCode = getKeyCode(c);

		if (keyCode == 0) {
			// Not a phone key (letters not in phone-key groups, accented chars, etc.)
			// Inject the raw character directly into the TextBox/TextField.
			Mobile.getDisplay().injectChar(c);
			return;
		}

		// When in text input mode, inject the decoded character directly.
		// Do NOT send phone key press/release events — those are for in-game navigation,
		// not for text input. The multi-tap logic still cycles through the character
		// group so the correct character is selected.
		if (isTextInputMode) {
			int tapCount = getTapCount(c);
			if (tapCount == 0) {
				Mobile.getDisplay().injectChar(c);
				return;
			}
			long now = System.currentTimeMillis();
			if (keyCode == lastTapKey && (now - lastTapTime) < TAP_TIMEOUT_MS) {
				lastTapCount = (lastTapCount + 1) % tapCount;
			} else {
				lastTapCount = 0;
			}
			lastTapKey = keyCode;
			lastTapTime = now;
			// Get the character at current tap position and inject it
			char groupChar = getCharFromTapGroup(c, lastTapCount);
			Mobile.getDisplay().injectChar(groupChar);
			return;
		}

		// Non-text mode: send phone key press/release events for in-game navigation.
		int tapCount = getTapCount(c);
		if (tapCount == 0) return; // safety guard
		long now = System.currentTimeMillis();

		if (keyCode == lastTapKey && (now - lastTapTime) < TAP_TIMEOUT_MS) {
			// Same key: cycle through chars in group
			lastTapCount = (lastTapCount + 1) % tapCount;
		} else {
			lastTapCount = 0;
		}
		lastTapKey = keyCode;
		lastTapTime = now;

		// Press the phone key N times to get the right character.
		// Each press+release pair is separated by KEY_INJECTION_DELAY_MS to give
		// the game thread time to process and update its internal key state
		// before the next event arrives.
		for (int i = 0; i <= lastTapCount; i++) {
			Mobile.getPlatform().keyPressed(keyCode);
			try { Thread.sleep(KEY_INJECTION_DELAY_MS); } catch (InterruptedException ignored) {}
			Mobile.getPlatform().keyReleased(keyCode);
			if (i < lastTapCount) {
				try { Thread.sleep(KEY_INJECTION_DELAY_MS); } catch (InterruptedException ignored) {}
			}
		}
	}

	// Get character at position 'tap' in the phone key group for character c.
	// e.g. c='c' (group ABC/2), tap=2 → 'c'
	private static char getCharFromTapGroup(char c, int tap) {
		c = Character.toLowerCase(c);
		String group = null;
		if ("abc2".indexOf(c) >= 0) group = "abc2";
		else if ("def3".indexOf(c) >= 0) group = "def3";
		else if ("ghi4".indexOf(c) >= 0) group = "ghi4";
		else if ("jkl5".indexOf(c) >= 0) group = "jkl5";
		else if ("mno6".indexOf(c) >= 0) group = "mno6";
		else if ("pqrs7".indexOf(c) >= 0) group = "pqrs7";
		else if ("tuv8".indexOf(c) >= 0) group = "tuv8";
		else if ("wxyz9".indexOf(c) >= 0) group = "wxyz9";
		if (group != null && tap < group.length()) {
			return group.charAt(tap);
		}
		return c;
	}

	public FreeJ2ME(String args[])
	{
		main = new Frame("FreeJ2ME");
		main.setSize(350,450);
		main.setBackground(new Color(0,0,64));
		try
		{
			main.setIconImage(ImageIO.read(main.getClass().getResourceAsStream("/org/recompile/icon.png")));	
		}
		catch (Exception e) { }

		main.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});

		// Setup Device //

		lcdWidth = 240;
		lcdHeight = 320;

		String jarfile = "";
		if(args.length>=1)
		{
			jarfile = args[0];
		}
		if(args.length>=3)
		{
			lcdWidth = Integer.parseInt(args[1]);
			lcdHeight = Integer.parseInt(args[2]);
		}
		if(args.length>=4)
		{
			scaleFactor = Integer.parseInt(args[3]);
		}

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight));

		lcd = new LCD();
		lcd.setFocusable(true);
		// Disable AWT Input Method (IME) so keyTyped fires with raw characters
		// and the composition preview window doesn't appear below the LCD
		lcd.enableInputMethods(false);
		main.add(lcd);

		// AWT on Linux/X11: requestFocus() must be called AFTER the window is visible.
		// Use windowOpened event to guarantee the window is on screen before requesting focus.
		main.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				// requestFocus() (deprecated) reliably grants keyboard focus on Linux/X11.
				// requestFocusInWindow() alone is not sufficient on many Linux window managers.
				lcd.requestFocus();
			}
		});

		config = new Config();
		config.onChange = new Runnable() { public void run() { settingsChanged(); } };

		Mobile.getPlatform().setPainter(new Runnable()
		{
			public void run()
			{
				lcd.paint(lcd.getGraphics());
			}
		});

		lcd.addKeyListener(new KeyListener()
		{
			public void keyPressed(KeyEvent e)
			{
				int keycode = e.getKeyCode();

				// Linux/X11 key repeat: keyPressed fires multiple times before keyReleased.
				// Detect autorepeat by checking if this key was released very recently.
				// If so, skip this event — the game will handle its own key repeat via keyRepeated().
				long now = System.currentTimeMillis();
				if (keycode == lastReleasedKeycode && (now - lastReleasedTime) < KEY_RELEASE_WINDOW_MS) {
					// Autorepeat keyPressed — skip
					lastReleasedKeycode = -1; // reset so next real press is not filtered
					return;
				}

				int mobikey = getMobileKey(keycode);
				int mobikeyN = (mobikey + 64) & 0x7F; //Normalized value for indexing the pressedKeys array
				if (keycode == 10 || keycode == 8) {
					System.out.println("DEBUG Enter/Backspace VK=" + keycode + " mobikey=" + mobikey);
				}

				switch(keycode) // Handle emulator control keys
				{
					// Volume / zoom controls — only these are truly emulator-level
					case KeyEvent.VK_PLUS:
					case KeyEvent.VK_ADD:
						scaleFactor++;
						main.setSize(lcdWidth * scaleFactor + xborder, lcdHeight * scaleFactor + yborder);
					break;
					case KeyEvent.VK_MINUS:
					case KeyEvent.VK_SUBTRACT:
						if( scaleFactor > 1 )
						{
							scaleFactor--;
							main.setSize(lcdWidth * scaleFactor + xborder, lcdHeight * scaleFactor + yborder);
						}
					break;
					case KeyEvent.VK_C:
						if(e.isControlDown())
						{
							ScreenShot.takeScreenshot(false);
						}
					break;
				}

				if (mobikey == 0)
				{
					// Key not mapped to a phone game key — do NOT return early.
					// Let keyTyped handle it for Nokia multi-tap text input.
					// keyPressedHandled stays false so keyTyped fires normally.

					// Linux key repeat: mark that keyTyped should skip processing for this key.
					// This prevents keyTyped from calling handlePhoneKey multiple times
					// when AWT fires keyTyped repeatedly for a held key.
					if (Character.isLetterOrDigit(e.getKeyChar()) ||
					    e.getKeyChar() == ' ' || e.getKeyChar() == '\b') {
						keyTypedInFlight = keycode;
					}
					return;
				}

				// For text input: mark that keyTyped should process this key.
				// This guards against keyTyped firing multiple times from Linux key repeat.
				if (isTextInputMode) {
					keyTypedInFlight = keycode;
				}

				// When in TextBox/TextField, letter keys Q/W/E/R should type text, not act as game softkeys.
				// keyPressedHandled stays false so keyTyped routes the character through Nokia multi-tap.
				if (isTextInputMode)
				{
					keyPressedHandled = false;
					return;
				}

				if(config.isRunning)
				{
					config.keyPressed(mobikey);
				}
				else
				{
					if (pressedKeys[mobikeyN] == false)
					{
						Mobile.getPlatform().keyPressed(mobikey);
					}
					else
					{
						Mobile.getPlatform().keyRepeated(mobikey);
					}
					// Mark which AWT keycode triggered the game key so keyTyped skips it.
					// This prevents Arrow Up from also injecting '2' into a TextField.
					gameKeyGuardActive = true;
					gameKeyGuardTime = System.nanoTime();
				}
				pressedKeys[mobikeyN] = true;

			}

			public void keyReleased(KeyEvent e)
			{
				int keycode = e.getKeyCode();
				// Track last released key to detect Linux/X11 key repeat in keyPressed.
				lastReleasedKeycode = keycode;
				lastReleasedTime = System.currentTimeMillis();

				// Emulator control keys — no game key to release.
				if (keycode == KeyEvent.VK_PLUS || keycode == KeyEvent.VK_ADD ||
				    keycode == KeyEvent.VK_MINUS || keycode == KeyEvent.VK_SUBTRACT ||
				    keycode == KeyEvent.VK_C) {
					return;
				}

				int mobikey = getMobileKey(keycode);
				int mobikeyN = (mobikey + 64) & 0x7F; //Normalized value for indexing the pressedKeys array

				if (mobikey == 0) // Unmapped keys: nothing to release
				{
					return;
				}

				// Only release if this key was actually tracked as pressed.
				// This guards against edge cases (e.g. Q/W/E/R pressed then switching to text mode)
				// and ensures we don't accidentally release a key that was never sent.
				if (!pressedKeys[mobikeyN]) {
					return;
				}

				pressedKeys[mobikeyN] = false;

				if(config.isRunning)
				{
					config.keyReleased(mobikey);
				}
				else
				{
					Mobile.getPlatform().keyReleased(mobikey);
					// Do NOT reset gameKeycodeJustPressed here — it expires via timestamp.
					// Resetting here caused bug where pressing A then B rapidly:
					// A.keyPressed → guard=A; A.keyReleased → guard=-1; B.keyTyped fires
					// before B.keyPressed (race), but guard is now -1 so B.keyTyped runs.
				}
			}

			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if (c == KeyEvent.CHAR_UNDEFINED) return;

				// Skip if keyPressed already handled this key (Backspace, Enter, Ctrl+C).
				// These set keyPressedHandled=true and handle everything internally.
				if (keyPressedHandled) {
					keyPressedHandled = false;
					return;
				}

				// Skip all keyTyped events while a game key guard is active.
				// This prevents Arrow Up → '2', Arrow Down → '8', etc. from also
				// injecting characters into a TextField.
				if (gameKeyGuardActive &&
				    (System.nanoTime() - gameKeyGuardTime) < GAME_KEY_GUARD_NS) {
					return;
				}
				gameKeyGuardActive = false;

				// For text input: process directly on EDT — no queue.
				if (isTextInputMode) {
					handlePhoneKey(c);
					return;
				}

				// Non-text mode (game is running):
				// Do NOT send any phone key events here. The game already receives
				// keyPressed/keyReleased from keyPressed() with correct game/NOKIA keycodes.
				// Only Backspace (handled above) and Enter (handled above) produce output.
				// This prevents handlePhoneKey from firing extra keyPressed events that
				// corrupt game input (e.g. NUM5 appearing without a corresponding key press).
				// If the game has a TextField active (detected via isTextInputMode), text
				// input is handled via injectChar() above.
			}

		});

		lcd.addMouseListener(new MouseListener()
		{
			public void mousePressed(MouseEvent e)
			{
				int x = (int)((e.getX()-lcd.cx) * lcd.scalex);
				int y = (int)((e.getY()-lcd.cy) * lcd.scaley);

				// Adjust the pointer coords if the screen is rotated, same for mouseReleased
				if(rotateDisplay)
				{
					x = (int)((lcd.ch-(e.getY()-lcd.cy)) * lcd.scaley);
					y = (int)((e.getX()-lcd.cx) * lcd.scalex);
				}

				Mobile.getPlatform().pointerPressed(x, y);
			}

			public void mouseReleased(MouseEvent e)
			{
				int x = (int)((e.getX()-lcd.cx) * lcd.scalex);
				int y = (int)((e.getY()-lcd.cy) * lcd.scaley);

				if(rotateDisplay)
				{
					x = (int)((lcd.ch-(e.getY()-lcd.cy)) * lcd.scaley);
					y = (int)((e.getX()-lcd.cx) * lcd.scalex);
				}

				Mobile.getPlatform().pointerReleased(x, y);
			}

			public void mouseExited(MouseEvent e) { }
			public void mouseEntered(MouseEvent e) { }
			public void mouseClicked(MouseEvent e) { }

		});

		lcd.addMouseMotionListener(new MouseMotionAdapter() 
		{
			public void mouseDragged(MouseEvent e)
			{
				int x = (int)((e.getX()-lcd.cx) * lcd.scalex);
				int y = (int)((e.getY()-lcd.cy) * lcd.scaley);

				if(rotateDisplay)
				{
					x = (int)((lcd.ch-(e.getY()-lcd.cy)) * lcd.scaley);
					y = (int)((e.getX()-lcd.cx) * lcd.scalex);
				}
				
				Mobile.getPlatform().pointerDragged(x, y); 
			}
		});

		main.addComponentListener(new ComponentAdapter()
		{
			public void componentResized(ComponentEvent e)
			{
				resize();
			}
		});

		main.setVisible(true);
		main.pack();

		resize();
		main.setSize(lcdWidth*scaleFactor+xborder, lcdHeight*scaleFactor+yborder);

		if(args.length<1)
		{
			FileDialog t = new FileDialog(main, "Open JAR File", FileDialog.LOAD);
			t.setFilenameFilter(new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					return name.toLowerCase().endsWith(".jar");
				}
			});
			t.setVisible(true);
			jarfile = new File(t.getDirectory()+File.separator+t.getFile()).toURI().toString();
		}
		if(Mobile.getPlatform().loadJar(jarfile))
		{
			config.init();

			/* Allows FreeJ2ME to set the width and height passed as cmd arguments. */
			if(args.length>=3)
			{
				lcdWidth = Integer.parseInt(args[1]);
				lcdHeight = Integer.parseInt(args[2]);
				config.settings.put("width",  ""+lcdWidth);
				config.settings.put("height", ""+lcdHeight);
			}

			settingsChanged();

			Mobile.getPlatform().runJar();
		}
		else
		{
			System.out.println("Couldn't load jar...");
		}
	}

	private void settingsChanged()
	{
		int w = Integer.parseInt(config.settings.get("width"));
		int h = Integer.parseInt(config.settings.get("height"));

		limitFPS = Integer.parseInt(config.settings.get("fps"));
		if(limitFPS>0) { limitFPS = 1000 / limitFPS; }

		String sound = config.settings.get("sound");
		Mobile.sound = false;
		if(sound.equals("on")) { Mobile.sound = true; }

		String phone = config.settings.get("phone");
		useNokiaControls = false;
		useSiemensControls = false;
		useMotorolaControls = false;
		Mobile.nokia = false;
		Mobile.siemens = false;
		Mobile.motorola = false;
		if(phone.equals("Nokia")) { Mobile.nokia = true; useNokiaControls = true; }
		if(phone.equals("Siemens")) { Mobile.siemens = true; useSiemensControls = true; }
		if(phone.equals("Motorola")) { Mobile.motorola = true; useMotorolaControls = true; }

		String rotate = config.settings.get("rotate");
		if(rotate.equals("on")) { rotateDisplay = true; }
		if(rotate.equals("off")) { rotateDisplay = false; }

		// Create a standard size LCD if not rotated, else invert window's width and height.
		if(!rotateDisplay) 
		{
			lcdWidth = w;
			lcdHeight = h;

			Mobile.getPlatform().resizeLCD(w, h);
			
			resize();
			main.setSize(lcdWidth*scaleFactor+xborder , lcdHeight*scaleFactor+yborder);
		}
		else 
		{
			lcdWidth = h;
			lcdHeight = w;

			Mobile.getPlatform().resizeLCD(w, h);

			resize();
			main.setSize(lcdWidth*scaleFactor+xborder , lcdHeight*scaleFactor+yborder);
		}
	}

	private int getMobileKey(int keycode)
	{
		if(useNokiaControls)
		{
			switch(keycode)
			{
				case KeyEvent.VK_UP: return Mobile.NOKIA_UP;
				case KeyEvent.VK_DOWN: return Mobile.NOKIA_DOWN;
				case KeyEvent.VK_LEFT: return Mobile.NOKIA_LEFT;
				case KeyEvent.VK_RIGHT: return Mobile.NOKIA_RIGHT;
				case KeyEvent.VK_ENTER: return Mobile.NOKIA_SOFT3;
			}
		}

		if(useSiemensControls)
		{
			switch(keycode)
			{
				case KeyEvent.VK_UP: return Mobile.SIEMENS_UP;
				case KeyEvent.VK_DOWN: return Mobile.SIEMENS_DOWN;
				case KeyEvent.VK_LEFT: return Mobile.SIEMENS_LEFT;
				case KeyEvent.VK_RIGHT: return Mobile.SIEMENS_RIGHT;
				case KeyEvent.VK_Q: return Mobile.SIEMENS_SOFT1;
				case KeyEvent.VK_W: return Mobile.SIEMENS_SOFT2;
				case KeyEvent.VK_ENTER: return Mobile.SIEMENS_FIRE;
			}
		}

		if(useMotorolaControls)
		{
			switch(keycode)
			{
				case KeyEvent.VK_UP: return Mobile.MOTOROLA_UP;
				case KeyEvent.VK_DOWN: return Mobile.MOTOROLA_DOWN;
				case KeyEvent.VK_LEFT: return Mobile.MOTOROLA_LEFT;
				case KeyEvent.VK_RIGHT: return Mobile.MOTOROLA_RIGHT;
				case KeyEvent.VK_Q: return Mobile.MOTOROLA_SOFT1;
				case KeyEvent.VK_W: return Mobile.MOTOROLA_SOFT2;
				case KeyEvent.VK_ENTER: return Mobile.MOTOROLA_FIRE;
			}
		}

		switch(keycode)
		{
			case KeyEvent.VK_0: return Mobile.KEY_NUM0;
			case KeyEvent.VK_1: return Mobile.KEY_NUM1;
			case KeyEvent.VK_2: return Mobile.KEY_NUM2;
			case KeyEvent.VK_3: return Mobile.KEY_NUM3;
			case KeyEvent.VK_4: return Mobile.KEY_NUM4;
			case KeyEvent.VK_5: return Mobile.KEY_NUM5;
			case KeyEvent.VK_6: return Mobile.KEY_NUM6;
			case KeyEvent.VK_7: return Mobile.KEY_NUM7;
			case KeyEvent.VK_8: return Mobile.KEY_NUM8;
			case KeyEvent.VK_9: return Mobile.KEY_NUM9;
			case KeyEvent.VK_ASTERISK: return Mobile.KEY_STAR;
			case KeyEvent.VK_NUMBER_SIGN: return Mobile.KEY_POUND;

			case KeyEvent.VK_NUMPAD0: return Mobile.KEY_NUM0;
			case KeyEvent.VK_NUMPAD7: return Mobile.KEY_NUM1;
			case KeyEvent.VK_NUMPAD8: return Mobile.KEY_NUM2;
			case KeyEvent.VK_NUMPAD9: return Mobile.KEY_NUM3;
			case KeyEvent.VK_NUMPAD4: return Mobile.KEY_NUM4;
			case KeyEvent.VK_NUMPAD5: return Mobile.KEY_NUM5;
			case KeyEvent.VK_NUMPAD6: return Mobile.KEY_NUM6;
			case KeyEvent.VK_NUMPAD1: return Mobile.KEY_NUM7;
			case KeyEvent.VK_NUMPAD2: return Mobile.KEY_NUM8;
			case KeyEvent.VK_NUMPAD3: return Mobile.KEY_NUM9;

			// Arrow keys: map to GAME keycodes (UP=1, LEFT=2, RIGHT=5, DOWN=6, FIRE=8)
			// NOT num keycodes (NUM2=50, NUM4=52, NUM6=54, NUM8=56, NUM5=53).
			// Some games (e.g. kpah) use getGameAction() which expects GAME keycodes.
			case KeyEvent.VK_UP: return Mobile.NOKIA_UP;
			case KeyEvent.VK_DOWN: return Mobile.NOKIA_DOWN;
			case KeyEvent.VK_LEFT: return Mobile.NOKIA_LEFT;
			case KeyEvent.VK_RIGHT: return Mobile.NOKIA_RIGHT;

			case KeyEvent.VK_ENTER: return Mobile.NOKIA_FIRE;    // Enter = Nokia center/Fire button
			case KeyEvent.VK_BACK_SPACE: return Mobile.NOKIA_SOFT2; // Backspace = Right soft key (back)
			case KeyEvent.VK_F1: return Mobile.NOKIA_SOFT1;         // F1 = Left soft key (menu/OK)
			case KeyEvent.VK_F2: return Mobile.NOKIA_SOFT2;         // F2 = Right soft key (back/cancel)
			case KeyEvent.VK_ESCAPE: return Mobile.NOKIA_SOFT2;     // ESC = Right soft key (back)

			// Letter keys Q/W = Nokia soft keys
			case KeyEvent.VK_Q: return Mobile.NOKIA_SOFT1;
			case KeyEvent.VK_W: return Mobile.NOKIA_SOFT2;
		}
		return 0;
	}

	private void resize()
	{
		xborder = main.getInsets().left+main.getInsets().right;
		yborder = main.getInsets().top+main.getInsets().bottom;

		double vw = (main.getWidth()-xborder)*1;
		double vh = (main.getHeight()-yborder)*1;

		double nw = lcdWidth;
		double nh = lcdHeight;

		nw = vw;
		nh = nw*((double)lcdHeight/(double)lcdWidth);

		if(nh>vh)
		{
			nh = vh;
			nw = nh*((double)lcdWidth/(double)lcdHeight);
		}

		lcd.updateScale((int)nw, (int)nh);
	}

	private class LCD extends Canvas
	{
		public int cx=0;
		public int cy=0;
		public int cw=240;
		public int ch=320;

		public double scalex=1;
		public double scaley=1;

		public void updateScale(int vw, int vh)
		{
			cx = (this.getWidth()-vw)/2;
			cy = (this.getHeight()-vh)/2;
			cw = vw;
			ch = vh;
			scalex = (double)lcdWidth/(double)vw;
			scaley = (double)lcdHeight/(double)vh;
		}

		public void paint(Graphics g)
		{
			try
			{
				Graphics2D cgc = (Graphics2D)this.getGraphics();
				if (config.isRunning)
				{
					if(!rotateDisplay)
					{
						g.drawImage(config.getLCD(), cx, cy, cw, ch, null);
					}
					else
					{
						// If rotated, simply redraw the config menu with different width and height
						g.drawImage(config.getLCD(), cy, cx, cw, ch, null);
					}
				}
				else
				{
					if(!rotateDisplay)
					{
						g.drawImage(Mobile.getPlatform().getLCD(), cx, cy, cw, ch, null);
					}
					else
					{
						// Rotate the FB 90 degrees counterclockwise with an adjusted pivot
						cgc.rotate(Math.toRadians(-90), ch/2, ch/2);
						// Draw the rotated FB with adjusted cy and cx values
						cgc.drawImage(Mobile.getPlatform().getLCD(), 0, cx, ch, cw, null);
					}

					if(limitFPS>0)
					{
						Thread.sleep(limitFPS);
					}
				}
			}
			catch (Exception e)
			{
				System.out.println(e.getMessage());
			}
		}
	}
}
