/*******************************************************************************
 * Copyright (c) 2009 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.parse.Parser;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import ca.beq.util.win32.registry.RegistryException;
import ca.beq.util.win32.registry.RegistryKey;
import ca.beq.util.win32.registry.RegistryValue;
import ca.beq.util.win32.registry.RootKey;

/**
 * This class handles command line parameters.
 * 
 * @author Tran Nam Quang
 */
@SuppressWarnings("nls")
public class CommandLineHandler {
	
	private static final String REGEX = "REGEX";
	private static final String EXCLUDE = "exclude";
	private static final String INCLUDE = "include";
	private static final String EXTRACT_DIR = "extract-dir";
	private static final String EXTRACT = "extract";
	private static final String UNREGISTER_CONTEXTMENU = "unregister-contextmenu";
	private static final String REGISTER_CONTEXTMENU = "register-contextmenu";

	/**
	 * Returns whether the rest of the program should be started.
	 */
	public static boolean handle(String[] args) {
		if (args.length == 0) return true;
		Options options = createOptions();
		CommandLineParser cmdlParser = new PosixParser();
		try {
			CommandLine line = cmdlParser.parse(options, args);
			// Print help
			if (line.hasOption("h")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("docfetcher", options);
				return false;
			}
			// Print version info
			if (line.hasOption("v")) {
				String[] version = DocFetcher.getProgramVersion();
				if (version == null) version = new String[] {""};
				System.out.println("DocFetcher " + version[0]);
				return false;
			}
			if (! handleWindowsRegistry(line))
				return false;
			if (! handleTextExtraction(line))
				return false;
		} catch (ParseException e) {
			System.out.println("Invalid arguments: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	@SuppressWarnings("static-access")
	private static Options createOptions() {
		Options options = new Options();
		
		// Windows registry manipulation options
		OptionGroup registryGroup = new OptionGroup();
		registryGroup.addOption(OptionBuilder.withLongOpt(REGISTER_CONTEXTMENU)
				.withDescription("add search entry to Windows explorer contextmenu")
				.create());
		registryGroup.addOption(OptionBuilder.withLongOpt(UNREGISTER_CONTEXTMENU)
				.withDescription("remove search entry from Windows explorer contextmenu")
				.create());
		options.addOptionGroup(registryGroup);
		
		// Text extraction options
		OptionGroup extractGroup = new OptionGroup();
		extractGroup.addOption(OptionBuilder.withLongOpt(EXTRACT)
				.withDescription("extract text from documents to textfile")
				.hasOptionalArgs()
				.create());
		extractGroup.addOption(OptionBuilder.withLongOpt(EXTRACT_DIR)
				.withDescription("extract text from documents to directory")
				.hasOptionalArgs()
				.create());
		options.addOptionGroup(extractGroup);
		
		// Inclusion and exclusion filter options
		options.addOption(OptionBuilder.withLongOpt(INCLUDE)
				.withDescription("regex inclusion filter for text extraction")
				.hasArg()
				.withArgName(REGEX)
				.create());
		options.addOption(OptionBuilder.withLongOpt(EXCLUDE)
				.withDescription("regex exclusion filter for text extraction")
				.hasArg()
				.withArgName(REGEX)
				.create());
		
		// Version info and help options
		options.addOption("h", "help", false, "print this help and exit");
		options.addOption("v", "version", false, "print version number and exit");
		
		return options;
	}
	
	/**
	 * Registers or unregisters context menu entry on Windows. Returns false if
	 * the program should terminate after executing this method, and returns
	 * true if the program is allowed to proceed.
	 */
	private static boolean handleWindowsRegistry(CommandLine line) {
		if (! line.hasOption(REGISTER_CONTEXTMENU) && ! line.hasOption(UNREGISTER_CONTEXTMENU))
			return true;
		if (! Const.IS_WINDOWS) {
			System.out.println("This option is only available on Windows.");
			return false;
		}
		File exe = new File("DocFetcher.exe");
		String exePath = exe.getAbsolutePath();
		if (exe.exists()) {
			String regkey = "Directory\\shell\\" + Msg.search_with_docfetcher.value();
			if (line.hasOption(REGISTER_CONTEXTMENU)) {
				RegistryKey key = new RegistryKey(RootKey.HKEY_CLASSES_ROOT, regkey + "\\command");
				key.create();
				key.setValue(new RegistryValue("\"" + exePath + "\" \"%1\""));
				System.out.println("Registered: " + exePath);
				return false;
			}
			else if (line.hasOption(UNREGISTER_CONTEXTMENU)) {
				try {
					new RegistryKey(RootKey.HKEY_CLASSES_ROOT, regkey).delete();
				} catch (RegistryException e) {
					System.out.println("Registry entry not found.");
				}
				System.out.println("Unregistered: " + exePath);
				return false;
			}
		}
		else {
			System.out.println("File not found: " + exePath);
			return false;
		}
		return true;
	}

	/**
	 * Handles command line text extraction. Returns false if the program should
	 * terminate after executing this method, and returns true if the program is
	 * allowed to proceed.
	 */
	private static boolean handleTextExtraction(CommandLine line) {
		if (! line.hasOption(EXTRACT) && ! line.hasOption(EXTRACT_DIR)) return true;

		// Create inclusion and exclusion filters
		Pattern includeFilter = null;
		Pattern excludeFilter = null;
		try {
			String includeString = line.getOptionValue(INCLUDE); // may be null
			includeFilter = Pattern.compile(includeString);
		} catch (Exception e1) {
			// Ignore
		}
		try {
			String excludeString = line.getOptionValue(EXCLUDE); // may be null
			excludeFilter = Pattern.compile(excludeString);
		} catch (Exception e1) {
			// Ignore
		}

		// Get sources and destination
		boolean writeToDir = false;
		String[] args1 = null;
		if (line.hasOption(EXTRACT)) {
			args1 = line.getOptionValues(EXTRACT);
		}
		else {
			writeToDir = true;
			args1 = line.getOptionValues(EXTRACT_DIR);
		}
		if (args1 == null)
			args1 = new String[0];
		String[] args2 = line.getArgs();
		String[] args = UtilList.concatenate(args1, args2, new String[args1.length + args2.length]);
		if (args.length < 2) {
			System.out.println("Text extraction requires at least one source and one destination.");
			return false;
		}

		// Create source file objects, check for existence
		int lastIndex = args.length - 1;
		File[] topLevelSources = new File[lastIndex];
		for (int i = 0; i < topLevelSources.length; i++) {
			String path = args[i];
			File file = new File(path);
			if (! file.exists()) {
				System.out.println("File not found: " + path);
				return false;
			}
			topLevelSources[i] = file;
		}

		// Check validity of destination
		File dest = new File(args[lastIndex]);
		if (writeToDir) {
			if (dest.exists()) {
				if (! dest.isDirectory()) {
					System.out.println("Not a directory: " + dest.getAbsolutePath());
					return false;
				}
			}
			else {
				if (! dest.mkdirs()) {
					System.out.println("Could not create directory: " + dest.getAbsolutePath());
					return false;
				}
			}
		}

		/*
		 * The source files are collected beforehand because
		 * 1) the text output will depend on the number of files to process,
		 * so we first have to determine how many files there are
		 * 2) the target file(s) might lie inside one of the source directories,
		 * so we first collect all source files in order to avoid confusion
		 * 3) by using a Map, we make sure there aren't any duplicate sources.
		 */
		Map<File, Parser> sources = new LinkedHashMap<File, Parser> ();
		for (File topLevelSource : topLevelSources)
			collect(topLevelSource, sources, includeFilter, excludeFilter, true);
		int nSources = sources.size(); // must be set *after* filling the sources list!
		
		// Perform text extraction
		if (writeToDir) {
			int i = 1;
			for (Entry<File, Parser> item : sources.entrySet()) {
				File source = item.getKey();
				System.out.println("Extracting (" + i + "/" + nSources + "): " + source.getName());
				try {
					File outFile = UtilFile.getNewFile(dest, source.getName() + ".txt");
					String text = item.getValue().renderText(source);
					FileWriter writer = new FileWriter(outFile, false);
					writer.write(text);
					writer.close();
				} catch (Exception e) {
					System.out.println("  Error: " + e.getMessage());
				}
				i++;
			}
		}
		else {
			// First check that the destination is not one of the source files
			for (File source : sources.keySet()) {
				if (source.equals(dest)) {
					System.out.println("Invalid input: Destination is identical to one of the source files.");
					return false;
				}
			}
			// If there's only one file to process, don't decorate the text output
			if (nSources == 1) {
				Entry<File, Parser> item = sources.entrySet().iterator().next();
				System.out.println("Extracting: " + item.getKey().getName());
				try {
					String text = item.getValue().renderText(item.getKey());
					FileWriter writer = new FileWriter(dest);
					writer.write(text);
					writer.close();
				} catch (Exception e) {
					System.out.println("  Error: " + e.getMessage());
				}
			}
			// Multiple files to process:
			else {
				try {
					FileWriter writer = new FileWriter(dest, false); // overwrite
					int i = 1;
					for (Entry<File, Parser> item : sources.entrySet()) {
						File source = item.getKey();
						System.out.println("Extracting (" + i + "/" + nSources + "): " + source.getName());
						try {
							String text = item.getValue().renderText(source); // This may fail, so do it first
							writer.write("Source: " + source.getAbsolutePath() + Const.LS);
							writer.write("============================================================" + Const.LS);
							writer.write(text);
							writer.write(Const.LS + Const.LS + Const.LS);
						} catch (Exception e) {
							System.out.println("  Error: " + e.getMessage());
						}
						i++;
					}
					writer.close();
				} catch (IOException e) {
					System.out.println("Can't write to file: " + e.getMessage());
				}
			}
		}
		return false;
	}
	
	/**
	 * Recursively collects all the files in the given folder which DocFetcher
	 * is able to read, using the given inclusion and exclusion filters. If the
	 * given file object is a file, the file itself is returned.
	 * <p>
	 * The inclusion and exclusion filter can both be null. In case of a
	 * conflict, the exclusion filter is given higher priority.
	 */
	private static void collect(File file, Map<File, Parser> output, Pattern includeFilter, Pattern excludeFilter, boolean isTopLevel) {
		if (file.isFile()) {
			if (excludeFilter != null && excludeFilter.matcher(file.getName()).matches())
				return;
			if (includeFilter != null && ! includeFilter.matcher(file.getName()).matches())
				return;
			Parser parser = ParserRegistry.getParser(file);
			if (parser != null)
				output.put(file, parser);
			else if (isTopLevel)
				output.put(file, ParserRegistry.getTextParser());
		}
		else if (file.isDirectory()) {
			File[] children = UtilFile.listAll(file);
			Arrays.sort(children);
			for(File child : children) 
				collect(child, output, includeFilter, excludeFilter, false);
		}
	}

}
