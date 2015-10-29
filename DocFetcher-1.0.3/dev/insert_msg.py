#! /usr/bin/python

'''
Inserts a new message entry into the class net.sourceforge.docfetcher.enumeration.Msg
and into all resource bundle files. The insertion position can be specified with an
existing message entry and a prefix "after" or "before".

Usage examples:
insert_msg.py after some_entry new_entry
insert_msg.py before some_entry new_entry

The values of the entries inserted into the properties files can be modified via the
variable props_default_value (e.g. new_entry=$TODO$).
'''

import sys, os, os.path, re
from os.path import join, split

# The default value for the entries inserted into the properties files
props_default_value = "$TODO$"

# Get command line arguments
if len(sys.argv) != 4:
	print 'Expected parameters: after/before entry new_entry'
	exit(0)
pos_str = sys.argv[1].lower()
if pos_str != 'before' and pos_str != 'after':
	print 'First parameter must be either \'before\' or \'after\''
	exit(0)
entry_name = sys.argv[2]
new_entry_name = sys.argv[3]

# Go up one level
project_root = split(os.getcwd())[0]

# Read Msg.java file, find position of target entry, insert new entry
msg_path = join(project_root, 'src/net/sourceforge/docfetcher/enumeration/Msg.java')
msg_file = open(msg_path, 'r')
found = False
inside_msg_list = False
new_msg_file = []
for index, line in enumerate(msg_file):
	if not inside_msg_list:
		if line.strip().startswith('public enum Msg'):
			inside_msg_list = True
	elif ';' in line:
		inside_msg_list = False
	doit = False
	if inside_msg_list:
		if entry_name + ',' in line.strip():
			doit = True
		if re.match(new_entry_name + r'\s*,.*', line.strip()):
			print 'Potential duplicate of new entry found in Msg.java, line %i.' % (index + 1)
			msg_file.close()
			exit(0)
	if doit and pos_str == 'before':
		new_msg_file.append('\t' + new_entry_name + ',\n')
	new_msg_file.append(line)
	if doit and pos_str == 'after':
		new_msg_file.append('\t' + new_entry_name + ',\n')
	if doit:
		found = True
msg_file.close()
if not found:
	print 'Target entry not found in Msg.java.'
	exit(0)
msg_file = open(msg_path, 'w')
msg_file.writelines(new_msg_file)
msg_file.close()

# Insert entry in properties files
src_dir = join(project_root, 'resources/lang')
propfiles = os.listdir(src_dir)
for filename in propfiles:
	if not (filename.startswith('Resource') and filename.endswith('.properties')):
		continue
	filepath = join(src_dir, filename)
	propfile = open(filepath, 'r')
	found = False
	newfile = []
	for index, line in enumerate(propfile):
		doit = False
		pos = index + 1
		if line.startswith(entry_name + '='):
			doit = True
		if doit and pos_str == 'before':
			newfile.append(new_entry_name + '=' + props_default_value + '\n')
		newfile.append(line)
		if doit and pos_str == 'after':
			newfile.append(new_entry_name + '=' + props_default_value + '\n')
			pos = pos + 1
		if doit:
			found = True
			print 'Please fill out the rest in %s, line %i.' % (filename, pos) 
	propfile.close()
	if not found:
		print 'Missing target entry in %s, file omitted.' % filename
		continue
	propfile = open(filepath, 'w')
	propfile.writelines(newfile)
	propfile.close()

print 'Done!'
