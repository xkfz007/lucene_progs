/*******************************************************************************
 * Copyright (c) 2007, 2008 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.FileAlreadyExistsException;
import net.sourceforge.docfetcher.parse.ParserRegistry;

import org.eclipse.swt.program.Program;

/**
 * File operation related utility methods
 * 
 * @author Tran Nam Quang
 */
public class UtilFile {
	
	/**
	 * A <tt>FileFilter</tt> that filters out everything that is not a
	 * directory or a symbolic link to a directory.
	 */
	private static final FileFilter dirsOnlyFilter = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	};
	/**
	 * A <tt>FileFilter</tt> that filters out everything that is not a file or
	 * a symbolic link to a file.
	 */
	private static final FileFilter filesOnlyFilter = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isFile();
		}
	};
	
	private static String lastTimeStamp = ""; //$NON-NLS-1$

	/**
	 * Static use only.
	 */
	private UtilFile() {
	}

	/**
	 * Returns whether the given folder is a root folder. On Windows, root
	 * folders can be "C:", "D:", and so on. On Linux, the root folder is "/".
	 * Returns false if the given file object does not represent a folder.
	 */
	public static boolean isRoot(File file) {
		File[] roots = File.listRoots();
		for (File root : roots)
			if (file.equals(root))
				return true;
		return false;
	}
	
	/**
	 * Returns an array of files for the given array of absolute file paths.
	 */
	public static File[] toFiles(String[] filePaths) {
		File[] files = new File[filePaths.length];
		for (int i = 0; i < files.length; i++)
			files[i] = new File(filePaths[i]);
		return files;
	}
	
	/**
	 * Returns an array of absolute paths for the given array of files.
	 */
	public static String[] toPaths(File[] files) {
		String[] paths = new String[files.length];
		for (int i = 0; i < paths.length; i++)
			paths[i] = files[i].getAbsolutePath();
		return paths;
	}
	
	/**
	 * Returns all files and folders in the given directory (only first level).
	 * Returns an empty array if access to the directory was denied.
	 * <p>
	 * <b>Note</b>: This does not exclude symbolic links!
	 */
	public static File[] listAll(File dir) {
		File[] files = dir.listFiles();
		return files == null ? new File[] {} : files;
	}
	
	public static File[] listAll(File dir, FileFilter filter) {
		File[] files = dir.listFiles(filter);
		return files == null ? new File[] {} : files;
	}
	
	/**
	 * Returns all files in the given directory (only first level). Returns an
	 * empty array if access to the directory was denied.
	 * <p>
	 * <b>Note</b>: This does not exclude symbolic links!
	 */
	public static File[] listFiles(File dir) {
		File[] files = dir.listFiles(filesOnlyFilter);
		return files == null ? new File[] {} : files;
	}
	
	/**
	 * Returns all folders of the given folder (only first level). Returns an
	 * empty array if access to the directory was denied.
	 * <p>
	 * <b>Note</b>: This does not exclude symbolic links!
	 */
	public static File[] listFolders(File dir) {
		File[] files = dir.listFiles(dirsOnlyFilter);
		return files == null ? new File[] {} : files;
	}

	/**
	 * Returns the parent file of the given file. Unlike
	 * {@link File#getParentFile}, this method will never return null.
	 */
	public static File getParentFile(File file) {
		File parent = file.getParentFile();
		if (parent == null)
			parent = file.getAbsoluteFile().getParentFile();
		return parent;
	}
	
	/**
	 * Returns the parent of the given file. Unlike {@link File#getParent}, this
	 * method will never return null.
	 */
	public static String getParent(File file) {
		String parent = file.getParent();
		if (parent == null)
			parent = file.getAbsoluteFile().getParent();
		return parent;
	}

	/**
	 * Returns whether the given file is a link. Returns true if the file
	 * doesn't exists or if an IOException occured. The link detection is based
	 * on the comparison of the absolute and canonical path of a link: If those
	 * two differ, it can be assumed that the file is a link.
	 */
	public static boolean isSymLink(File file) {
		try {
			if (!file.exists())
				return true;
			String absPath = file.getAbsolutePath();
			String canPath = file.getCanonicalPath();
			return ! absPath.equals(canPath);
		} catch (IOException e) {
			return true;
		}
	}

	/**
	 * Given an HTML folder, this method extracts the name of the folder without
	 * the HTML suffix and the separator character. For example, if the given
	 * HTML folder has the name "foo_files", then "foo" will be returned.
	 * <p>
	 * Returns null if the given file object is not a folder ending with one of
	 * the known HTML suffixes.
	 */
	public static String getHTMLDirBasename(File dir) {
		if (! dir.isDirectory()) return null;
		String dirName = dir.getName();
		for (String suffix : Const.HTML_FOLDER_SUFFIXES) {
			if (dirName.endsWith(suffix))
				return dirName.substring(0, dirName.length() - suffix.length());
		}
		return null;
	}

	/**
	 * If <tt>target</tt> is a directory, this will delete all files and
	 * directories under it. If the <tt>includeTopLevel</tt> flag is set, the
	 * given directory is deleted also.
	 * <p>
	 * If <tt>target</tt> is a file or a symbolic link (be it to a directory
	 * or a file), it will only be deleted if the <tt>includeTopLevel</tt>
	 * flag is set.
	 */
	public static void delete(File target, boolean includeTopLevel) {
		if (target.isDirectory() && ! isSymLink(target)) {
			File[] files = listAll(target);
			for (File file : files)
				delete(file, true);
		}
		if (includeTopLevel)
			target.delete();
	}
	
	/**
	 * Returns an enhanced version of the given file set according to the rules
	 * of HTML pairing. For example, if "foo.htm" and "bar_files" are given, the
	 * output will be "foo.htm", "foo_files", "bar.htm" and "bar_files" if the
	 * additional files exist. Some characteristics of this method:
	 * <ul>
	 * <li>In case of an ambiguity the first match is selected.</li> <li>
	 * Duplicates are eliminated.</li> <li>The order of the input files is
	 * preserved.</li>
	 * </ul>
	 */
	public static File[] completeHTMLPairs(File... files) {
		List<File> out = new ArrayList<File> ();
		for (File file : files) {
			if (! out.contains(file))
				out.add(file);
			if (file.isDirectory()) {
				String baseName = UtilFile.getHTMLDirBasename(file);
				if (baseName == null) continue;
				File[] candidates = listAll(getParentFile(file), new FileFilter() {
					public boolean accept(File candidate) {
						return candidate.isFile() && ParserRegistry.isHTMLFile(candidate);
					}
				});
				for (File candidate : candidates)
					if (UtilFile.getNameNoExt(candidate).equals(baseName)) {
						if (! out.contains(candidate))
							out.add(candidate);
						break; // Only add the first occurrence
					}
			}
			else if (file.isFile() && ParserRegistry.isHTMLFile(file)) {
				String baseName = UtilFile.getNameNoExt(file);
				File[] candidates = listAll(getParentFile(file), new FileFilter() {
					public boolean accept(File candidate) {
						if (! candidate.isDirectory()) return false;
						String baseName = UtilFile.getHTMLDirBasename(candidate);
						if (baseName == null) return false;
						return true;
					}
				});
				for (File candidate : candidates)
					if (UtilFile.getHTMLDirBasename(candidate).equals(baseName)) {
						if (! out.contains(candidate))
							out.add(candidate);
						break; // Only add the first occurrence
					}
			}
			else
				continue;
		}
		return out.toArray(new File[out.size()]);
	}

	/**
	 * Copies <tt>srcFile</tt> into the directory <tt>newParent</tt>. The
	 * latter is created if it doesn't exist. <tt>srcFile</tt> can be either a
	 * file, a directory or a symbolic link to a file. If it's a file, then an
	 * existing destination file will be overwritten. If it's a directory, all
	 * it's children will be copied as well. If it's a symbolic link to
	 * directory, this method does nothing.
	 * 
	 * @throws IOException
	 *             if the copy process failed
	 */
	public static void copy(File srcFile, File newParent) throws FileNotFoundException, FileAlreadyExistsException, IOException {
		if (srcFile.isDirectory() && isSymLink(srcFile))
			return;
		if (! srcFile.exists())
			throw new FileNotFoundException();
		File destFile = new File(newParent, srcFile.getName());
		if (destFile.exists())
			throw new FileAlreadyExistsException(
					destFile, Msg.file_already_exists.format(destFile.getAbsolutePath()));
		newParent.mkdirs();
		if (srcFile.isFile()) {
			FileInputStream in = null;
			FileOutputStream out = null;
			try {
				in = new FileInputStream(srcFile);
				out = new FileOutputStream(destFile);
				byte[] buf = new byte[1024];
				int len;
				while((len = in.read(buf)) > 0)
					out.write(buf, 0, len);
			} finally {
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			}
		}
		else if (srcFile.isDirectory()) {
			destFile.mkdirs();
			File[] srcFiles = listAll(srcFile);
			for (File subSrcFile : srcFiles)
				copy(subSrcFile, destFile);
		}
	}

	/**
	 * Returns the file extension of the given file, without the '.' character.
	 * Returns an empty string if the file has no extension (meaning that it
	 * doesn't contain the '.' character).
	 * <p>
	 * An exception is made for files ending with ".xxx.gz" (e.g.
	 * "archive.tar.gz" and "abiword.abw.gz"): The double extension "xxx.gz" is
	 * returned instead of "gz".
	 */
	public static String getExtension(File file) {
		String fileName = file.getName();
		int index = fileName.lastIndexOf('.');
		if (index == -1) return ""; //$NON-NLS-1$
		String ext = fileName.substring(index + 1).toLowerCase();
		if (! ext.equals("gz")) //$NON-NLS-1$
			return ext;
		int index2 = fileName.lastIndexOf('.', fileName.length() - 4);
		if (index2 == -1) return ext;
		return fileName.substring(index2 + 1).toLowerCase();
	}

	/**
	 * Returns the filename of the given file without its file extension.
	 */
	public static String getNameNoExt(File file) {
		int extLength = getExtension(file).length();
		if (extLength != 0) extLength += 1;
		String fileName = file.getName();
		return fileName.substring(0, fileName.length() - extLength);
	}
	
	/**
	 * Returns the file extension of the given file, without the '.' character.
	 * Returns an empty string if the file has no extension (meaning that it
	 * doesn't contain the '.' character).
	 * <p>
	 * An exception is made for files ending with ".xxx.gz" (e.g.
	 * "archive.tar.gz" and "abiword.abw.gz"): The double extension "xxx.gz" is
	 * returned instead of "gz".
	 */
	public static String getExtension(String fileName) {
		int index = fileName.lastIndexOf('.');
		if (index == -1) return ""; //$NON-NLS-1$
		String ext = fileName.substring(index + 1).toLowerCase();
		if (! ext.equals("gz")) //$NON-NLS-1$
			return ext;
		int index2 = fileName.lastIndexOf('.', fileName.length() - 4);
		if (index2 == -1) return ext;
		return fileName.substring(index2 + 1).toLowerCase();
	}

	/**
	 * Returns the filename of the given file without its file extension.
	 */
	public static String getNameNoExt(String fileName) {
		int extLength = getExtension(fileName).length();
		if (extLength != 0) extLength += 1;
		return fileName.substring(0, fileName.length() - extLength);
	}

	/**
	 * Recursively collects all file extensions under the given directory and
	 * then sorts and returns them. Files with an extension, but no basename
	 * (e.g. ".classpath") are omitted.
	 */
	public static Set<String> listExtensions(File rootDir) {
		Set<String> exts = new TreeSet<String> ();
		listExtensions(exts, rootDir);
		return exts;
	}
	
	/**
	 * Recursively collects all file extensions under the given directory and
	 * puts them into the given Set. Files with an extension, but no basename
	 * (e.g. ".classpath") are omitted.
	 * <p>
	 * Does nothing if the given directory cannot be accessed.
	 */
	private static void listExtensions(Set<String> exts, File rootDir) {
		File[] files = listAll(rootDir);
		for (File file : files) {
			if (Thread.currentThread().isInterrupted()) return;
			if (file.isFile()) {
				String ext = UtilFile.getExtension(file);
				if (ext.trim().equals("")) continue; //$NON-NLS-1$
				
				// skip files with extension, but no basename (e.g. ".classpath")
				if (ext.length() + 1 == file.getName().length()) continue;
				
				exts.add(ext);
			}
			else if (file.isDirectory() && ! UtilFile.isSymLink(file))
				listExtensions(exts, file);
		}
	}

	/**
	 * Returns the 'last modified' property of the given file in a
	 * human-readable date format.
	 */
	public static String getLastModified(File file) {
		DateFormat format = null;
		String prefDateFormat = Pref.Str.DateFormat.getValue();
		if (prefDateFormat.equals("")) { //$NON-NLS-1$
			int dateStyle = DateFormat.MEDIUM;
			int timeStyle = DateFormat.SHORT;
			format = DateFormat.getDateTimeInstance(dateStyle, timeStyle);
		}
		else
			format = new SimpleDateFormat(prefDateFormat);
		return format.format(new Date(file.lastModified()));
	}

	/**
	 * Returns the total filesize of the given files in kilobytes. Directories
	 * will be recursively searched in.
	 */
	public static long getSizeInKB(File... files) {
		if (files == null) return 0;
		long sum = 0;
		for (File file : files) {
			if (file.isFile()) {
				long length = file.length();
				long extra = length % 1024 == 0 ? 0 : 1;
				sum += length / 1024 + extra;
			}
			else if (file.isDirectory() && ! isSymLink(file)) {
				sum += getSizeInKB(listAll(file));
			}
		}
		return sum;
	}
	
	/**
	 * Returns the total filesize of the given files in kilobytes.
	 */
	public static long getSizeInKB(List<File> files) {
		long sum = 0;
		for (File file : files) {
			long length = file.length();
			long extra = length % 1024 == 0 ? 0 : 1;
			sum += length / 1024 + extra;
		}
		return sum;
	}
	
	/**
	 * Returns whether the two given path strings are identical. Two paths are
	 * considered identical even if they contain different path separators.
	 * Also, it doesn't matter whether they end with an extra path separator or
	 * not.
	 */
	public static boolean equalPaths(String path1, String path2) {
		path1 = path1.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		path2 = path2.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		if (path1.endsWith("/")) //$NON-NLS-1$
			path1 = path1.substring(0, path1.length() - 1);
		if (path2.endsWith("/")) //$NON-NLS-1$
			path2 = path2.substring(0, path2.length() - 1);
		return path1.equals(path2);
	}

	/**
	 * Returns true if the folder given by the absolute path in <tt>dirPath</tt>
	 * is a direct or indirect parent folder of the file or folder given by the
	 * absolute path <tt>dirOrFilePath</tt>.
	 */
	public static boolean contains(String dirPath, String dirOrFilePath) {
		dirPath = dirPath.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		dirOrFilePath = dirOrFilePath.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		List<String> parts1 = UtilList.split(dirPath, "/"); //$NON-NLS-1$
		List<String> parts2 = UtilList.split(dirOrFilePath, "/"); //$NON-NLS-1$
		if (parts1.size() >= parts2.size())
			return false;
		for (int i = 0; i < parts1.size(); i++)
			if (! parts1.get(i).equals(parts2.get(i)))
				return false;
		return true;
	}
	
	/**
	 * Returns true if <tt>dir</tt> is a direct or indirect parent folder of the
	 * file or folder given by <tt>dirOrFile</tt>.
	 */
	public static boolean contains(File dir, File dirOrFile) {
		return contains(dir.getAbsolutePath(), dirOrFile.getAbsolutePath());
	}

	/**
	 * Returns true if the folder given by the absolute path in <tt>dirPath</tt>
	 * is the parent folder of the file or folder given by the absolute path
	 * <tt>dirOrFilePath</tt>.
	 */
	public static boolean containsDirect(String dirPath, String dirOrFilePath) {
		dirPath = dirPath.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		dirOrFilePath = dirOrFilePath.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		List<String> parts1 = UtilList.split(dirPath, "/"); //$NON-NLS-1$
		List<String> parts2 = UtilList.split(dirOrFilePath, "/"); //$NON-NLS-1$
		if (parts1.size() + 1 != parts2.size())
			return false;
		for (int i = 0; i < parts1.size(); i++)
			if (! parts1.get(i).equals(parts2.get(i)))
				return false;
		return true;
	}
	
	/**
	 * If <tt>fileOrDir</tt> is a file inside the folder <tt>parentFile</tt>,
	 * this method returns a file object whose path is relative to
	 * <tt>parentFile</tt>.
	 * <p>
	 * Example: If the path of <tt>parentFile</tt> is "/media/mydevice/" and the
	 * path of <tt>fileOrDir</tt> is "/media/mydevice/mydocuments/mydoc.html",
	 * then a file whose path is "mydocuments/mydoc.html" is returned.
	 * <p>
	 * The method returns <tt>fileOrDir</tt> itself if that file is not
	 * contained in <tt>parentFile</tt> or if <tt>parentFile</tt> is not a
	 * directory.
	 */
	public static File getRelativeFile(File parentFile, File fileOrDir) {
		if (! parentFile.isDirectory() || ! contains(parentFile, fileOrDir))
			return fileOrDir;
		int beginIndex = parentFile.getAbsolutePath().length() + 1;
		return new File(fileOrDir.getAbsolutePath().substring(beginIndex));
	}

	/**
	 * Returns the path component of the absolute path <tt>fileOrDirPath</tt>
	 * relative to its (direct or indirect) parent <tt>parentPath</tt>.
	 * <p>
	 * Example: If <tt>parentPath</tt> is "/media/mydevice/" and
	 * <tt>fileOrDirPath</tt> is "/media/mydevice/mydocuments/mydoc.html", then
	 * "mydocuments/mydoc.html" is returned.
	 * <p>
	 * The method returns <tt>fileOrDirPath</tt> itself if <tt>parentPath</tt>
	 * is not the parent of <tt>fileOrDirPath</tt>.
	 */
	public static String getRelativePath(String parentPath, String fileOrDirPath) {
		if (! UtilFile.contains(parentPath, fileOrDirPath))
			return fileOrDirPath;
		String relPath = fileOrDirPath.substring(parentPath.length());
		/*
		 * Remove preceding file separator; both Linux and Windows file
		 * separators can occur here if we're running the portable version of
		 * DocFetcher.
		 */
		if (UtilList.startsWith(relPath, Const.FS, "/", "\\")) //$NON-NLS-1$ //$NON-NLS-2$
			return relPath.substring(1);
		return relPath;
	}
	
	/**
	 * Returns the path of <tt>file</tt> relative to the current working directory.
	 * 
	 * @see #getRelativePath(String, String)
	 */
	public static String getRelativePath(File file) {
		return getRelativePath(Const.USER_DIR_FILE.getAbsolutePath(), file.getAbsolutePath());
	}

	/**
	 * Tries to return the file separator used in the given path (either Linux
	 * or Windows). If the given path contains to file separator, a Windows file
	 * separator is returned.
	 */
	private static String getFS(String path) {
		if (path.contains("/")) //$NON-NLS-1$
			return "/"; //$NON-NLS-1$
		else if (path.contains("\\")) //$NON-NLS-1$
			return "\\"; //$NON-NLS-1$
		return Const.FS;
	}
	
	/**
	 * Returns the concatenation of two filepath strings.
	 */
	public static String join(String parentPath, String childPath) {
		String fs = getFS(parentPath);
		if (! parentPath.endsWith(fs))
			parentPath += fs;
		if (! childPath.startsWith(fs))
			return parentPath + childPath;
		return parentPath + childPath.substring(1);
	}

	/**
	 * Returns a unique identifier string (currently using
	 * <tt>System.currentTimeMillis()</tt>). It is guaranteed that the
	 * returned ID differs from all previous IDs obtained by this method.
	 */
	public static String getUniqueID() {
		/*
		 * Try to create a timestamp and don't return until the last timestamp
		 * and the current one are unequal.
		 */
		String newTimeStamp = String.valueOf(System.currentTimeMillis());
		while (newTimeStamp.equals(lastTimeStamp))
			newTimeStamp = String.valueOf(System.currentTimeMillis());
		lastTimeStamp = newTimeStamp;
		return newTimeStamp;
	}

	/**
	 * Returns a name for a subfolder in <tt>parentFolder</tt> that doesn't
	 * exist yet.
	 */
	public static String suggestNewSubfolderName(File parentFolder) {
		if (! parentFolder.isDirectory())
			throw new IllegalArgumentException();
		File[] subDirs = listFolders(parentFolder);
		String[] subDirNames = new String[subDirs.length];
		for (int i = 0; i < subDirs.length; i++)
			subDirNames[i] = subDirs[i].getName();
		if (! UtilList.containsEquality(subDirNames, Msg.untitled.value()))
			return Msg.untitled.value();
		int i = 2;
		while (true) {
			if (! UtilList.containsEquality(subDirNames, Msg.untitled.value() + i))
				return Msg.untitled.value() + i;
			i++;
		}
	}

	/**
	 * Tries to return a new file object with <tt>parentFolder</tt> as parent
	 * and <tt>initialName</tt> as filename. If a file with this name already
	 * exists in the parent folder, the method appends a number to the filename
	 * so that the resulting file is one that doesn't exist yet.
	 * <p>
	 * Example: If the parent folder contains "test.txt", "test (1).txt" and
	 * "test (3).txt", this method returns a file with the name "test (2).txt".
	 */
	public static File getNewFile(File parentFolder, String initialName) {
		if (! parentFolder.isDirectory())
			throw new IllegalArgumentException();
		File file = new File(parentFolder, initialName);
		if (! file.exists()) return file;
		String basename = getNameNoExt(initialName);
		String ext = getExtension(initialName);
		int i = 1;
		while (true) {
			file = new File(parentFolder, basename + " (" + i + ")." + ext); //$NON-NLS-1$ //$NON-NLS-2$
			if (! file.exists()) return file;
			i++;
		}
	}
	
	/**
	 * Expects a string array containing file or directory paths and returns
	 * existing, "dissociated" directories. That means, the returned set of
	 * files does not contain directories that are subdirectories of other
	 * directories from the input list.
	 */
	public static Set<File> getDissociatedDirectories(String... input) {
		List<File> existingDirs = new ArrayList<File> (input.length);
		for (String candidate : input) {
			File file = new File(candidate);
			if (file.exists() && file.isDirectory())
				existingDirs.add(file);
		}
		Set<File> output = new HashSet<File> (existingDirs.size()); 
		for (int i = 0; i < existingDirs.size(); i++) {
			boolean foundParent = false;
			File parent = getParentFile(existingDirs.get(i));
			for (int j = 0; j < existingDirs.size(); j++) {
				if (parent.equals(existingDirs.get(j))) {
					foundParent = true;
					break;
				}
			}
			if (! foundParent)
				output.add(existingDirs.get(i));
		}
		return output;
	}
	
	/**
	 * Normalizes the given path string so that only the path separator of the
	 * current environment (Windows vs. Linux) is used.
	 */
	public static String normPathSep(String path) {
		if (Const.IS_WINDOWS)
			return path.replace("/", "\\"); //$NON-NLS-1$ //$NON-NLS-2$
		return path.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Launches the given filename or filepath, returning whether the file was
	 * successfully launched. This method first tries to launch the file via the
	 * SWT method {@link Program#launch(String)}. If that fails and the
	 * application is running on Linux, this method tries to call xdg-open. This
	 * is what usually happens on KDE-based Linuxes, which are not supported by
	 * SWT.
	 */
	public static boolean launch(String fileName) {
		if (Program.launch(fileName))
			return true;
		if (! Const.IS_LINUX)
			return false;
		try {
			Process p = Runtime.getRuntime().exec(new String[] {"xdg-open", fileName}); //$NON-NLS-1$
			return p.waitFor() == 0;
		} catch (Exception e) {
			return false;
		}
	}

}
