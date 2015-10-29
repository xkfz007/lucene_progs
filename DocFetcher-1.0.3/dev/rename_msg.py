#! /usr/bin/python

import sys, os, os.path
from os.path import join, exists, split

"""
Changes the name of an entry in Msg.java and updates all references
to it in the properties files and in the source files.
Warning: This script cannot handle special characters in the new name,
such as German umlauts.
It is strongly recommended to make a project backup before running
this script.
Expected parameters: oldname newname
"""

# Get command line arguments
if len(sys.argv) != 3:
	print 'Enter the name of an entry in Msg.java and its new name.'
	exit(0)
msg_oldname = sys.argv[1]
msg_newname = sys.argv[2]

# Go up one level
project_root = split(os.getcwd())[0]

# Read Msg.java file and replace oldname with newname
msg_path = join(project_root, 'src/net/sourceforge/docfetcher/enumeration/Msg.java')
msg_file = open(msg_path, 'r')
inside_msg_list = False
new_msg_file = []
replaced = False
for line in msg_file:
	if not inside_msg_list:
		if line.strip().startswith('public enum Msg'):
			inside_msg_list = True
	elif ';' in line:
		inside_msg_list = False
	if inside_msg_list and msg_oldname + ',' in line.strip():
		oldline = line.strip()
		line = line.replace(msg_oldname, msg_newname)
		replaced = True
		print 'Replacing line in Msg.java:', oldline, ' ==> ', line.strip()
	new_msg_file.append(line)
msg_file.close()
if replaced:
	msg_file = open(msg_path, 'w')
	msg_file.writelines(new_msg_file)
	msg_file.close()
else:
	print 'Entry not found in Msg.java.'
	exit(0)

# Replace references in properties files
langpath = join(project_root, 'resources/lang')
for filename in os.listdir(langpath):
	if not filename.startswith('Resource') or not filename.endswith('.properties'):
		continue
	filepath = join(langpath, filename)
	f = open(filepath, 'r')
	newfile = []
	for line in f:
		if line.startswith(msg_oldname + '='):
			line = line.replace(msg_oldname, msg_newname)
			print 'Occurrence in %s replaced.' % filename
		newfile.append(line)
	f.close()
	f = open(filepath, 'w')
	f.writelines(newfile)
	f.close()

# Replace references in source code
for root, dirs, files in os.walk(join(project_root), 'src'):
	for filename in files:
		if not filename.endswith('.java') and not filename.endswith('.aj'):
			continue
		filepath = join(root, filename)
		f = open(filepath, 'r')
		newfile = []
		replaced = False
		for line in f:
			if line.strip().endswith('Msg.'):
				print 'Warning: \'Msg.\' in %s omitted.' % filename
			elif 'Msg.' + msg_oldname in line:
				line = line.replace('Msg.' + msg_oldname, 'Msg.' + msg_newname)
				replaced = True
				print 'Occurrence in %s replaced.' % filename
			newfile.append(line)
		f.close()
		
		# Only write new file if reference was found
		if replaced:
			f = open(filepath, 'w')
			f.writelines(newfile)
			f.close()

print 'Done!'
