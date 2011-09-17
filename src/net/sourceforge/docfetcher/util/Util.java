/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.WString;

/**
 * @author Tran Nam Quang
 */
public final class Util {
	
	/*
	 * TODO pre-release: consider structuring the methods in this class by putting them into
	 * public static inner classes.
	 */

	/** Whether the platform is Windows. */
	public static final boolean IS_WINDOWS;

	/** Whether the platform is Linux. */
	public static final boolean IS_LINUX;
	
	/** Whether the platform is Mac OS X. */
	public static final boolean IS_MAC_OS_X;
	
	/** The system's temporary directory. Does not contain backward slashes. */
	public static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
	
	/** The current directory. Does not contain backward slashes. */
	public static final String USER_DIR_PATH = System.getProperty("user.dir").replace('\\', '/');
	
	/** The current directory. */
	public static final File USER_DIR = new File(USER_DIR_PATH);
	
	/** The user's home directory. Does not contain backward slashes. */
	public static final String USER_HOME_PATH = System.getProperty("user.home");
	
	static {
		String osName = System.getProperty("os.name").toLowerCase();
		IS_WINDOWS = osName.contains("windows");
		IS_LINUX = osName.contains("linux");
		IS_MAC_OS_X = osName.equals("mac os x");
	}

	/** Line separator character ('\r\n' on Windows, '\n' on Linux). */
	public static final String LS = System.getProperty("line.separator");

	/**
	 * File separator character. On Windows, this is '\', and on Linux, it's
	 * '/'.
	 */
	public static final String FS = System.getProperty("file.separator");

	private Util() {}

	/**
	 * Splits the given string into an integer array. Any characters other than
	 * digits and the 'minus' are treated as separators.
	 * <p>
	 * If the string cannot be parsed, the given array of default values is
	 * returned. If the string contains numbers that are greater than
	 * {@code Integer.MAX_VALUE} or less than {@code Integer.MIN_VALUE}, those
	 * numbers will be clamped.
	 */
	public static int[] toIntArray(String str, int[] defaultValues) {
		if (str.trim().equals(""))
			return new int[0];
		String[] rawValues = str.split("[^-\\d]+");
		int[] array = new int[rawValues.length];
		for (int i = 0; i < rawValues.length; i++) {
			try {
				array[i] = Integer.parseInt(rawValues[i]);
			}
			catch (NumberFormatException e) {
				if (rawValues[i].matches("\\d{10,}"))
					array[i] = Integer.MAX_VALUE;
				else if (rawValues[i].matches("-\\d{10,}"))
					array[i] = Integer.MIN_VALUE;
				else
					return defaultValues;
			}
		}
		return array;
	}

	/**
	 * Returns the given integer string as an {@code int} value. Leading and
	 * trailing whitespaces are ignored. If the string cannot be parsed, the
	 * given default value is returned. If the string is a number, but greater
	 * than {@code Integer.MAX_VALUE} or less than {@code Integer.MIN_VALUE}, a
	 * clamped value is returned.
	 */
	public static int toInt(String value, int defaultValue) {
		value = value.trim();
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			if (value.matches("\\d{10,}"))
				return Integer.MAX_VALUE;
			else if (value.matches("-\\d{10,}"))
				return Integer.MIN_VALUE;
		}
		return defaultValue;
	}

	/**
	 * Encodes the given collection of strings into a single string, using the
	 * specified separator. The resulting string is a concatenation of the
	 * elements of the collection, which are separated by the given separator
	 * and where occurrences of the separator and backslashes are escaped
	 * appropriately.
	 * 
	 * @see Util#decodeStrings(String, char)
	 */
	@NotNull
	public static String encodeStrings(	@NotNull String sep,
										@NotNull Collection<String> parts) {
		Util.checkNotNull(sep, parts);
		if (parts.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;
		for (String part : parts) {
			if (!isFirst)
				sb.append(sep);
			sb.append(part.replace("\\", "\\\\").replace(sep, "\\" + sep));
			isFirst = false;
		}
		return sb.toString();
	}

	/**
	 * Decodes the given string into a list of strings, using the specified
	 * separator. This method basically splits the given string at those
	 * occurrences of the separator that aren't escaped with a backslash.
	 * <p>
	 * Special case: If the given string is an empty string, an empty list is
	 * returned.
	 *
	 * @see Util#encodeStrings(String, char)
	 */
	@MutableCopy
	@NotNull
	public static List<String> decodeStrings(char sep, @NotNull String str) {
		Util.checkNotNull(str);
		if (str.isEmpty())
			return new ArrayList<String>(0);
		boolean precedingBackslash = false;
		List<String> parts = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == sep && ! precedingBackslash) {
				parts.add(sb.toString());
				sb.delete(0, sb.length());
			}
			else if (c != '\\' || precedingBackslash)
				sb.append(c);
			if (c == '\\')
				precedingBackslash = ! precedingBackslash;
			else
				precedingBackslash = false;
		}
		parts.add(sb.toString());
		return parts;
	}
	
	public static <T> boolean equals(@NotNull Collection<T> col, @NotNull T[] a) {
		Util.checkNotNull(col, a);
		if (col.size() != a.length)
			return false;
		int i = 0;
		for (T e1 : col) {
			if (!e1.equals(a[i]))
				return false;
			i++;
		}
		return true;
	}
	
	public static String ensureLinuxLineSep(@NotNull String input) {
		return input.replace("\r\n", "\n");
	}
	
	public static String ensureWindowsLineSep(@NotNull String input) {
		// Two replace passes are needed to avoid converting "\r\n" to "\r\r\n".
		return input.replace("\r\n", "\n").replace("\n", "\r\n");
	}

	/**
	 * Centers the given shell relative to its parent shell and sets the shell's
	 * width and height. If there is no parent shell, the given shell is
	 * centered relative to the screen.
	 */
	public static void setCenteredBounds(	@NotNull Shell shell,
											int width,
											int height) {
		shell.setSize(width, height);
		Composite parent = shell.getParent();
		Rectangle parentBounds = null;
		if (parent == null)
			parentBounds = shell.getMonitor().getBounds();
		else
			parentBounds = parent.getBounds();
		int shellPosX = (parentBounds.width - width) / 2;
		int shellPosY = (parentBounds.height - height) / 2;
		if (parent != null) {
			shellPosX += parentBounds.x;
			shellPosY += parentBounds.y;
		}
		shell.setLocation(shellPosX, shellPosY);
	}
	
	/**
	 * Packs the given shell and then centers it relative to its parent shell.
	 * If there is no parent shell, the given shell is centered relative to the
	 * screen.
	 */
	public static void setCenteredBounds(@NotNull Shell shell) {
		shell.pack();
		Point shellSize = shell.getSize();
		Composite parent = shell.getParent();
		Rectangle parentBounds = null;
		if (parent == null)
			parentBounds = shell.getMonitor().getBounds();
		else
			parentBounds = parent.getBounds();
		int shellPosX = (parentBounds.width - shellSize.x) / 2;
		int shellPosY = (parentBounds.height - shellSize.y) / 2;
		if (parent != null) {
			shellPosX += parentBounds.x;
			shellPosY += parentBounds.y;
		}
		shell.setLocation(shellPosX, shellPosY);
	}
	
	/**
	 * Packs the given shell and then centers it relative to the given control.
	 */
	public static void setCenteredBounds(	@NotNull Shell shell,
											@NotNull Control control) {
		shell.pack();
		Point shellSize = shell.getSize();
		Composite parent = control.getParent();
		Rectangle bounds = control.getBounds();
		bounds = control.getDisplay().map(parent, null, bounds);
		int x = bounds.x + (bounds.width - shellSize.x) / 2;
		int y = bounds.y + (bounds.height - shellSize.y)/ 2;
		shell.setLocation(x, y);
	}

	/**
	 * Centers the given shell relative to its parent shell and sets the shell's
	 * minimum width and height. The actual width and height may be greater to
	 * provide enough space for the shell's children. If the given shell has no
	 * parent shell, it is centered relative to the screen.
	 */
	public static void setCenteredMinBounds(@NotNull Shell shell,
											int minWidth,
											int minHeight) {
		Point prefSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		int width = Math.max(prefSize.x, minWidth);
		int height = Math.max(prefSize.y, minHeight);
		setCenteredBounds(shell, width, height);
	}
	
	/**
	 * Returns whether the first bit mask contains the second bit mask.
	 * <p>
	 * Example: {@code contains(SWT.CTRL | SWT.ALT, SWT.CTRL) == true}
	 */
	public static boolean contains(int bit1, int bit2) {
		return (bit1 & bit2) == bit2;
	}

	/**
	 * Creates and returns a {@link org.eclipse.swt.layout.FillLayout
	 * FillLayout} with the given margin.
	 */
	public static FillLayout createFillLayout(int margin) {
		FillLayout layout = new FillLayout();
		layout.marginWidth = layout.marginHeight = margin;
		return layout;
	}

	/**
	 * Creates and returns a {@link org.eclipse.swt.layout.GridLayout
	 * GridLayout} with the given arguments.
	 */
	public static GridLayout createGridLayout(int numColumns, boolean makeColumsEqualWidth, int margin, int spacing) {
		GridLayout layout = new GridLayout(numColumns, makeColumsEqualWidth);
		layout.marginWidth = layout.marginHeight = margin;
		layout.horizontalSpacing = layout.verticalSpacing = spacing;
		return layout;
	}

	/**
	 * Creates and returns a {@link org.eclipse.swt.layout.FormLayout
	 * FormLayout} with the given margin.
	 */
	public static FormLayout createFormLayout(int margin) {
		FormLayout layout = new FormLayout();
		layout.marginWidth = layout.marginHeight = margin;
		return layout;
	}

	/**
	 * Splits the given file path at any path separators, i.e. forward or
	 * backward slashes. Example:
	 * 
	 * <pre>
	 * /path/to/file/ -> '', 'path', 'to', 'file'
	 * </pre>
	 * 
	 * Note that a leading path separator will produce an empty string at the
	 * beginning of the returned list, while a (single) trailing path separator
	 * won't.
	 */
	@MutableCopy
	@NotNull
	public static List<String> splitPath(@NotNull String path) {
		List<String> parts = new ArrayList<String>();
		int lastStart = 0;
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (c == '/' || c == '\\') {
				parts.add(path.substring(lastStart, i));
				lastStart = i + 1;
			}
		}
		if (lastStart < path.length())
			parts.add(path.substring(lastStart));
		return parts;
	}
	
	/**
	 * A {@link com.google.common.base.CharMatcher CharMatcher} that matches
	 * forward and backward slashes. See the {@code CharMatcher} Javadocs for
	 * more.
	 */
	public static final CharMatcher fileSepMatcher = CharMatcher.anyOf("/\\").precomputed();
	
	/**
	 * Creates a file path by joining the given parts. All leading and trailing
	 * forward and backward slashes are stripped from the parts, except for the
	 * first part, where only the trailing slashes are stripped. All backward
	 * slashes are replaced by forward slashes.
	 */
	@NotNull
	public static String joinPath(@NotNull String... parts) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (i == 0) {
				sb.append(fileSepMatcher.trimTrailingFrom(parts[i]));
			} else {
				sb.append('/');
				sb.append(fileSepMatcher.trimFrom(parts[i]));
			}
		}
		return sb.toString().replace('\\', '/');
	}
	
	/**
	 * Same as {@link #joinPath(String...)}, but reads the parts from an
	 * <tt>Iterable</tt>.
	 */
	@NotNull
	public static String joinPath(@NotNull Iterable<?> parts) {
		Iterator<?> it = parts.iterator();
		if (! it.hasNext())
			return "";
		StringBuilder sb = new StringBuilder();
		sb.append(fileSepMatcher.trimTrailingFrom(it.next().toString()));
		while (it.hasNext()) {
			sb.append('/');
			sb.append(fileSepMatcher.trimFrom(it.next().toString()));
		}
		return sb.toString().replace('\\', '/');
	}
	
	@NotNull
	public static String join(@NotNull String separator, @NotNull String... parts) {
		Util.checkNotNull(separator);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (i == 0) {
				sb.append(parts[i]);
			} else {
				sb.append(separator);
				sb.append(parts[i]);
			}
		}
		return sb.toString();
	}
	
	@NotNull
	public static String join(@NotNull String separator, @NotNull Iterable<?> parts) {
		Util.checkNotNull(separator);
		Iterator<?> it = parts.iterator();
		if (! it.hasNext())
			return "";
		StringBuilder sb = new StringBuilder();
		sb.append(it.next().toString());
		while (it.hasNext()) {
			sb.append(separator);
			sb.append(it.next().toString());
		}
		return sb.toString();
	}
	
	/**
	 * Splits the given path string at the last path separator character (either
	 * forward or backward slash). If the given string does not contain path
	 * separators, the returned array contains the given string and an empty
	 * string.
	 */
	@NotNull
	public static String[] splitPathLast(@NotNull String string) {
		for (int i = string.length() - 1; i >= 0; i--) {
			char c = string.charAt(i);
			if (c == '/' || c == '\\') {
				return new String[] {
					string.substring(0, i),
					string.substring(i + 1)
				};
			}
		}
		return new String[] {string, ""};
	}
	
	/**
	 * For the given file, returns an absolute path in which all backward
	 * slashes have been replaced by forward slashes.
	 */
	@NotNull
	@SuppressAjWarnings
	public static String getAbsPath(@NotNull File file) {
		return file.getAbsolutePath().replace('\\', '/');
	}
	
	/**
	 * For the given path string, returns an absolute path in which all backward
	 * slashes have been replaced by forward slashes.
	 */
	@NotNull
	public static String getAbsPath(@NotNull String path) {
		return getAbsPath(new File(path));
	}
	
	/**
	 * Equivalent to {@link java.io.File#getAbsolutePath()}.
	 */
	@NotNull
	@SuppressAjWarnings
	public static String getSystemAbsPath(@NotNull File file) {
		return file.getAbsolutePath();
	}
	
	/**
	 * Equivalent to {@link java.io.File#getAbsolutePath() new
	 * java.io.File(path).getAbsolutePath()}.
	 */
	@NotNull
	@SuppressAjWarnings
	public static String getSystemAbsPath(@NotNull String path) {
		return new File(path).getAbsolutePath();
	}
	
	/**
	 * Returns all files and directories directly underneath the given
	 * directory. This works like {@link File#listFiles()}, except that when
	 * access to the directory is denied, an empty array is returned, not a null
	 * pointer.
	 */
	@NotNull
	@SuppressAjWarnings
	public static File[] listFiles(@NotNull File dir) {
		File[] files = dir.listFiles();
		return files == null ? new File[0] : files;
	}

	/**
	 * Returns all files and directories directly underneath the given directory
	 * that are not filtered by the given {@code filter}. This works like
	 * {@link File#listFiles(FilenameFilter)}, except that when access to the
	 * directory is denied, an empty array is returned, not a null pointer.
	 */
	@NotNull
	@SuppressAjWarnings
	public static File[] listFiles(	@NotNull File dir,
									@Nullable FilenameFilter filter) {
		File[] files = dir.listFiles(filter);
		return files == null ? new File[0] : files;
	}

	/**
	 * Returns all files and directories directly underneath the given directory
	 * that are not filtered by the given {@code filter}. This works like
	 * {@link File#listFiles(FileFilter)}, except that when access to the
	 * directory is denied, an empty array is returned, not a null pointer.
	 */
	@NotNull
	@SuppressAjWarnings
	public static File[] listFiles(	@NotNull File dir,
									@Nullable FileFilter filter) {
		File[] files = dir.listFiles(filter);
		return files == null ? new File[0] : files;
	}

	/**
	 * Returns whether the given file is a symlink. Returns false if the file
	 * doesn't exists or if an IOException occured. The symlink detection is
	 * based on the comparison of the absolute and canonical path of a link: If
	 * those two differ, the given file can be assumed to be a symlink.
	 * <p>
	 * Note: If the given file is an instance of TFile and
	 * represents an archive entry, this method always returns false.
	 */
	@SuppressAjWarnings
	public static boolean isSymLink(@NotNull File file) {
		try {
			String absPath = file.getAbsolutePath();
			String canPath = file.getCanonicalPath();
			return ! absPath.equals(canPath);
		} catch (IOException e) {
			return false;
		}
	}
	
	private interface Kernel32 extends Library {
		public int GetFileAttributesW(WString fileName);
	}

	private static Kernel32 lib = null;
	
	private static int getWin32FileAttributes(File file) throws IOException {
		if (lib == null) {
			synchronized (Kernel32.class) {
				lib = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);
			}
		}
		return lib.GetFileAttributesW(new WString(file.getCanonicalPath()));
	}

	/**
	 * Returns whether the given file is a Windows junction or symlink. Returns
	 * false if the platform is not Windows, if the file doesn't exists or if an
	 * IOException occured.
	 * <p>
	 * Note: If the given file is an instance of TFile and
	 * represents an archive entry, this method always returns false.
	 */
	public static boolean isJunctionOrSymlink(@NotNull File file) {
		if (! IS_WINDOWS) return false;
		try {
			return file.exists() && (0x400 & getWin32FileAttributes(file)) != 0;
		} catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * Returns the parent of the given file. Unlike the standard method
	 * {@link File#getParentFile()}, this method will not return null if the
	 * given file was constructed with a relative path.
	 */
	@NotNull
	@SuppressAjWarnings
	public static File getParentFile(@NotNull File file) {
		Util.checkNotNull(file);
		File parent = file.getParentFile();
		if (parent == null)
			parent = file.getAbsoluteFile().getParentFile();
		return parent;
	}
	
	/**
	 * @see #getParentFile(File)
	 */
	@NotNull
	@SuppressAjWarnings
	public static File getParentFile(@NotNull String path) {
		Util.checkNotNull(path);
		File file = new File(path);
		File parent = file.getParentFile();
		if (parent == null)
			parent = file.getAbsoluteFile().getParentFile();
		return parent;
	}

	/**
	 * Returns true if <tt>objects</tt> contains an object that is equal to
	 * <tt>object</tt>. Returns false if <tt>objects</tt> is null.
	 */
	public static boolean containsEquality(	@Nullable Object[] objects,
											@Nullable Object object) {
		if (objects == null)
			return false;
		for (Object candidate : objects)
			if (candidate.equals(object))
				return true;
		return false;
	}

	/**
	 * Equivalent to {@link #splitFilename(String)
	 * splitFilename(file.getName())}.
	 * 
	 * @see Util#splitFilename(String)
	 */
	public static String[] splitFilename(@NotNull File file) {
		return Util.splitFilename(file.getName());
	}

	/**
	 * Splits the given filename into a base name and the file extension,
	 * omitting the '.' character, e.g. "data.xml" -> ["data", "xml"]. The file
	 * extension is an empty string if the file has no extension (i.e. it
	 * doesn't contain the '.' character). The returned file extension is always
	 * lowercase, even if it wasn't lowercased in the given filename. It is also
	 * guaranteed that the returned array is always of length 2.
	 * <p>
	 * Exception: If the file ends with ".xxx.gz", then the returned file
	 * extension is "xxx.gz", not "gz". Examples:
	 * <ul>
	 * <li>"archive.tar.gz" -> ["archive", "tar.gz"]
	 * <li>"abiword.abw.gz" -> ["abiword", "abw.gz"]
	 * </ul>
	 * <p>
	 * Note: This method also accepts filepaths.
	 * 
	 * @throws NullPointerException
	 *             if the given filename is null.
	 */
	@NotNull
	public static String[] splitFilename(@NotNull String filename) {
		int index = filename.lastIndexOf('.');
		if (index == -1)
			return new String[] {filename, ""};
		String ext = filename.substring(index + 1).toLowerCase();
		if (ext.equals("gz")) {
			int index2 = filename.lastIndexOf('.', index - 1);
			if (index2 != -1)
				return new String[] {
					filename.substring(0, index2),
					filename.substring(index2 + 1).toLowerCase()
			};
		}
		return new String[] {filename.substring(0, index), ext};
	}
	
	@NotNull
	public static String getExtension(@NotNull String filename) {
		return splitFilename(filename)[1];
	}

	/**
	 * For the given filename and a list of file extensions, this method returns
	 * true if any of the file extensions match the filename. A match occurs
	 * when the given filename, after being lower-cased, ends with '.' and the
	 * matching lower-cased file extension.
	 * <p>
	 * Example: The filename <code>'some_file.TXT'</code> matches the file
	 * extension <code>'txt'</code>.
	 * <p>
	 * Note: This method also accepts filepaths.
	 */
	public static boolean hasExtension(	@NotNull String filename,
										@NotNull String... extensions) {
		filename = filename.toLowerCase();
		for (String ext : extensions)
			if (filename.endsWith("." + ext.toLowerCase()))
				return true;
		return false;
	}
	
	/**
	 * @see #hasExtension(String, String...)
	 */
	public static boolean hasExtension(@NotNull String filename,
	                                   @NotNull Collection<String> extensions) {
		filename = filename.toLowerCase();
		for (String ext : extensions)
			if (filename.endsWith("." + ext.toLowerCase()))
				return true;
		return false;
	}
	
	public static void assertSwtThread() {
		if (Display.getCurrent() == null)
			throw new IllegalStateException();
	}
	
	/**
	 * Throws an <code>IllegalArgumentException</code> if the given condition is
	 * false.
	 */
	public static void checkThat(boolean condition) {
		if (! condition)
			throw new IllegalArgumentException();
	}
	
	/**
	 * Throws an <code>IllegalArgumentException</code> if the provided argument
	 * is null. If not, the argument is returned.
	 */
	public static <T> T checkNotNull(T a) {
		/*
		 * Generally, it does not make sense to check that a method argument of
		 * type Boolean is not null - if the Boolean is not allowed to be null,
		 * one could use a primitive boolean instead. If someone does call this
		 * method with a Boolean, he/she might have done so by accident by
		 * confusing checkNotNull with checkThat. To prevent this, we'll throw
		 * an exception.
		 */
		if (a instanceof Boolean)
			throw new UnsupportedOperationException();
		if (a == null)
			throw new IllegalArgumentException();
		return a;
	}
	
	/**
	 * Throws an <code>IllegalArgumentException</code> if any of the provided
	 * arguments is null.
	 */
	public static void checkNotNull(Object a, Object b) {
		if (a == null || b == null)
			throw new IllegalArgumentException();
	}
	
	/**
	 * Throws an <code>IllegalArgumentException</code> if any of the provided
	 * arguments is null.
	 */
	public static void checkNotNull(Object a, Object b, Object c) {
		if (a == null || b == null || c == null)
			throw new IllegalArgumentException();
	}
	
	/**
	 * Throws an <code>IllegalArgumentException</code> if any of the provided
	 * arguments is null.
	 */
	public static void checkNotNull(Object a, Object b, Object c, Object d) {
		if (a == null || b == null || c == null || d == null)
			throw new IllegalArgumentException();
	}
	
	/**
	 * Throws an <code>IllegalArgumentException</code> if any of the provided
	 * arguments is null.
	 */
	public static void checkNotNull(Object a, Object b, Object c, Object d, Object e) {
		if (a == null || b == null || c == null || d == null || e == null)
			throw new IllegalArgumentException();
	}
	
	private static long lastTimeStamp = -1;
	
	/**
	 * Returns a unique identifier based on {@link System#currentTimeMillis()}.
	 * The returned ID is guaranteed to differ from all previous IDs obtained by
	 * this method.
	 */
	@ThreadSafe
	public static synchronized long getTimestamp() {
		/*
		 * Try to create a timestamp and don't return until the last timestamp
		 * and the current one are unequal.
		 */
		long newTimeStamp = System.currentTimeMillis();
		while (newTimeStamp == lastTimeStamp)
			newTimeStamp = System.currentTimeMillis();
		lastTimeStamp = newTimeStamp;
		return newTimeStamp;
	}

	/**
	 * Returns true if the directory given by <tt>dir</tt> is a direct or
	 * indirect parent directory of the file or directory given by
	 * <tt>fileOrDir</tt>.
	 */
	public static boolean contains(	@NotNull File dir,
									@NotNull File fileOrDir) {
		return contains(getAbsPath(dir), getAbsPath(fileOrDir));
	}

	/**
	 * Returns true if the directory given by the absolute path <tt>dirPath</tt>
	 * is a direct or indirect parent directory of the file or directory given
	 * by the absolute path <tt>fileOrDirPath</tt>.
	 */
	public static boolean contains(	@NotNull String dirPath,
									@NotNull String fileOrDirPath) {
		dirPath = dirPath.replace('\\', '/');
		fileOrDirPath = fileOrDirPath.replace('\\', '/');
		if (dirPath.length() >= fileOrDirPath.length())
			return false;
		char c = fileOrDirPath.charAt(dirPath.length());
		if (c != '/')
			return false;
		if (! fileOrDirPath.startsWith(dirPath))
			return false;
		return true;
	}

	/**
	 * Returns the last element of the given list. Returns null if the given
	 * list is empty or null.
	 */
	@Nullable
	public static <T> T getLast(@Nullable List<T> list) {
		if (list == null) return null;
		int size = list.size();
		if (size == 0) return null;
		return list.get(size - 1);
	}
	
	/**
	 * Runs the given {@code Runnable} in a way that avoids throwing errors of
	 * the type {@link SWT#ERROR_THREAD_INVALID_ACCESS}. This is useful for
	 * running GUI-accessing code from non-GUI threads.
	 * <p>
	 * The given Runnable is <b>not</b> run if the given given widget is null or
	 * disposed. This helps avoid the common pitfall of trying to access widgets
	 * from a non-GUI thread when these widgets have already been disposed.
	 * <p>
	 * The returned Boolean indicates whether the Runnable was run (true) or not
	 * (false).
	 */
	public static boolean runSwtSafe(	@Nullable final Widget widget,
										@NotNull final Runnable runnable) {
		if (Display.getCurrent() != null) {
			boolean wasRun = widget != null && !widget.isDisposed();
			if (wasRun)
				runnable.run();
			return wasRun;
		}
		else {
			return runSyncExec(widget, runnable);
		}
	}
	
	/**
	 * @see #runSwtSafe(Widget, Runnable)
	 */
	public static boolean runSwtSafe(	@Nullable final Display display,
										@NotNull final Runnable runnable) {
		if (Display.getCurrent() != null) {
			boolean wasRun = display != null && !display.isDisposed();
			if (wasRun)
				runnable.run();
			return wasRun;
		}
		else {
			return runSyncExec(display, runnable);
		}
	}
	
	/**
	 * Runs the given {@code Runnable} via {@link Display#syncExec(Runnable)}.
	 * This is useful for running GUI-accessing code from non-GUI threads.
	 * <p>
	 * The given Runnable is <b>not</b> run if the given given widget is null or
	 * disposed. This helps avoid the common pitfall of trying to access widgets
	 * from a non-GUI thread when these widgets have already been disposed.
	 * <p>
	 * The returned Boolean indicates whether the Runnable was run (true) or not
	 * (false).
	 */
	public static boolean runSyncExec(	@Nullable final Widget widget,
										@NotNull final Runnable runnable) {
		if (widget == null || widget.isDisposed())
			return false;
		final boolean[] wasRun = { false };
		widget.getDisplay().syncExec(new Runnable() {
			public void run() {
				wasRun[0] = !widget.isDisposed();
				if (wasRun[0])
					runnable.run();
			}
		});
		return wasRun[0];
	}
	
	/**
	 * @see #runSyncExec(Widget, Runnable)
	 */
	public static boolean runSyncExec(	@Nullable final Display display,
										@NotNull final Runnable runnable) {
		if (display == null || display.isDisposed())
			return false;
		final boolean[] wasRun = { false };
		display.syncExec(new Runnable() {
			public void run() {
				wasRun[0] = !display.isDisposed();
				if (wasRun[0])
					runnable.run();
			}
		});
		return wasRun[0];
	}

	/**
	 * Runs the given {@code Runnable} via {@link Display#asyncExec(Runnable)}.
	 * This is useful for running GUI-accessing code from non-GUI threads.
	 * <p>
	 * The given Runnable is <b>not</b> run if the given given widget is null or
	 * disposed. This helps avoid the common pitfall of trying to access widgets
	 * from a non-GUI thread when these widgets have already been disposed.
	 */
	public static void runAsyncExec(@Nullable final Widget widget,
									@NotNull final Runnable runnable) {
		/*
		 * Note: Unlike the syncExec variant, here it's not possible to return a
		 * boolean flag that indicates whether the Runnable was run, since
		 * asyncExec may not execute the Runnable immediately.
		 */
		if (widget == null || widget.isDisposed())
			return;
		widget.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!widget.isDisposed())
					runnable.run();
			}
		});
	}

	/**
	 * Launches the given filename or filepath, and returns whether the file was
	 * successfully launched. This method first tries to launch the file via the
	 * SWT method {@link Program#launch(String)}. If this fails and the
	 * application is running on Linux, this method tries to call xdg-open. This
	 * is what usually happens on KDE-based Linux variants, which are not
	 * supported by SWT.
	 */
	@SuppressAjWarnings
	public static boolean launch(@NotNull String filename) {
		Util.checkNotNull(filename);
		if (Program.launch(filename))
			return true;
		if (! IS_LINUX)
			return false;
		try {
			String[] cmd = {"xdg-open", filename};
			Process process = Runtime.getRuntime().exec(cmd);
			
			ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
			InputStream errorIn = process.getErrorStream();
			
			int exitValue = process.waitFor();
			ByteStreams.copy(errorIn, errorOut);
			Closeables.closeQuietly(errorIn);
			Closeables.closeQuietly(errorOut);
			
			return exitValue == 0 && errorOut.size() == 0;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * @see #launch(String)
	 */
	public static boolean launch(@NotNull File fileOrDir) {
		Util.checkNotNull(fileOrDir);
		return launch(getSystemAbsPath(fileOrDir));
	}

	/**
	 * Equivalent to {@link #createTempFile(String, String, File)
	 * createTempFile(String, String, null)}.
	 */
	public static File createTempFile(	@NotNull String prefix,
										@Nullable String suffix)
			throws IOException {
		return createTempFile(prefix, suffix, null);
	}
	
	/**
	 * Equivalent to {@link File#createTempFile(String, String, File)}, except:
	 * <ul>
	 * <li>The returned file will be deleted automatically after JVM shutdown.
	 * <li>Unlike {@link File#createTempFile(String, String, File)}, this method
	 * will not throw an exception if the prefix is shorter than 3 characters.
	 * Instead, the prefix will be right-padded with underscores to make it 3
	 * characters long.
	 * </ul>
	 * 
	 * @see {@link File#createTempFile(String, String, File)}
	 */
	@SuppressAjWarnings
	public static File createTempFile(	@NotNull String prefix,
										@Nullable String suffix,
										@Nullable File directory)
			throws IOException {
		int prefixLength = prefix.length();
		if (prefixLength < 3)
			prefix += Strings.repeat("_", 3 - prefixLength);
		File file = File.createTempFile(prefix, suffix, directory);
		file.deleteOnExit();
		return file;
	}
	
	@NotNull
	public static File createDerivedTempFile(	@NotNull String filename,
												@NotNull File tempDir)
			throws IOException {
		String[] nameParts = Util.splitFilename(filename);
		if (! nameParts[1].equals(""))
			nameParts[1] = "." + nameParts[1];
		return Util.createTempFile(
				nameParts[0], nameParts[1], tempDir
		);
	}
	
	public static boolean equals(@NotNull File file1, @NotNull File file2) {
		return file1.getAbsoluteFile().equals(file2.getAbsoluteFile());
	}

	// TODO now: Debug method; show AspectJ warnings whenever it is used
	@SuppressAjWarnings
	public static void println(@NotNull Object... objects) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Object object : objects) {
			if (first) {
				first = false;
			} else {
				sb.append("; ");
			}
			sb.append(object);
		}
		System.out.println(sb.toString());
	}
	
	/**
	 * Equivalent to <code>System.err.println(String)</code>. This method can be
	 * called instead to suppress AspectJ warnings.
	 */
	@SuppressAjWarnings
	public static void printErr(@NotNull String message) {
		System.err.println(message);
	}
	
	/**
	 * Equivalent to {@link Throwable#printStackTrace()}. This method can be
	 * called instead to suppress AspectJ warnings.
	 */
	@SuppressAjWarnings
	public static void printErr(@NotNull Throwable t) {
		t.printStackTrace();
	}

	/**
	 * Applying this method to the given widget will cause all the text in it to
	 * become selected if the user clicks on it after coming back from another
	 * part of the GUI or another program. The widget must be a Combo or a Text
	 * widget.
	 */
	public static void selectAllOnFocus(@NotNull final Control text) {
		Util.checkThat(text instanceof Combo || text instanceof Text);
		
		class SelectAllOnFocus extends MouseAdapter implements FocusListener {
			private boolean focusGained = false;
			public void focusGained(FocusEvent e) {
				focusGained = true;
			}
			public void focusLost(FocusEvent e) {
			}
			public void mouseDown(MouseEvent e) {
				if (! focusGained) return;
				if (text instanceof Combo)
					selectAll((Combo) text);
				else if (text instanceof Text)
					((Text) text).selectAll();
				focusGained = false;
			}
		}
		
		SelectAllOnFocus listener = new SelectAllOnFocus();
		text.addFocusListener(listener);
		text.addMouseListener(listener);
	}
	
	@Nullable private static KeyListener selectAllKeyListener;
	
	public static void registerSelectAllKey(@NotNull final StyledText st) {
		if (selectAllKeyListener == null) {
			selectAllKeyListener = new KeyAdapter() {
				public void keyPressed(org.eclipse.swt.events.KeyEvent e) {
					if (e.stateMask == SWT.MOD1 || e.keyCode == 'a')
						st.selectAll();
				}
			};
		}
		st.addKeyListener(selectAllKeyListener);
	}
	
	/**
	 * Selects all the text in the given combo.
	 */
	public static void selectAll(@NotNull Combo combo) {
		int length = combo.getText().length();
		combo.setSelection(new Point(0, length));
	}

	public static int clamp(int value, int minimum, int maximum) {
		Util.checkThat(minimum <= maximum);
		if (value > maximum) return maximum;
		if (value < minimum) return minimum;
		return value;
	}
	
	public static boolean isInterrupted() {
		return Thread.currentThread().isInterrupted();
	}

	/**
	 * Returns an array of files from the system clipboard, or null if there are
	 * no files on the clipboard. This method should not be called from a
	 * non-GUI thread, and it should not be called before an SWT display has
	 * been created.
	 */
	@Nullable
	public static List<File> getFilesFromClipboard() {
		assertSwtThread();
		Clipboard clipboard = new Clipboard(Display.getDefault());
		try {
			TransferData[] types = clipboard.getAvailableTypes();
			for (TransferData type : types) {
				if (!FileTransfer.getInstance().isSupportedType(type))
					continue;

				Object data = clipboard.getContents(FileTransfer.getInstance());
				if (data == null || !(data instanceof String[]))
					continue;

				String[] paths = (String[]) data;
				List<File> files = new ArrayList<File>(paths.length);
				for (String path : paths)
					files.add(new File(path));
				return files;
			}
			return null;
		}
		finally {
			clipboard.dispose();
		}
	}

	/**
	 * Replaces the contents of the given clipboard with the given text and
	 * returns the clipboard. If the given clipboard is null, it will be
	 * created. This will only work if an SWT Display has been created.
	 */
	public static void setClipboard(@NotNull Collection<File> files) {
		Util.checkNotNull(files);
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		Transfer[] types = new Transfer[] {
				TextTransfer.getInstance(),
				FileTransfer.getInstance()
		};
		StringBuilder sb = new StringBuilder();
		String[] filePaths = new String[files.size()];
		int i = 0;
		for (File file : files) {
			if (i != 0)
				sb.append("\n");
			String path = Util.getSystemAbsPath(file);
			sb.append(path);
			filePaths[i] = path;
			i++;
		}
		clipboard.setContents(new Object[] {sb.toString(), filePaths}, types);
		clipboard.dispose();
	}

}