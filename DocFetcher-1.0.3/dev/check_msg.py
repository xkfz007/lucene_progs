#! /usr/bin/python

"""
This script checks for inconsistencies related
to the usage of localized message strings in the
source code, in the Msg.java class and in the
Resource properties files.
"""

import os, os.path, re
from os.path import join, exists, split


#======================================================
# Get message strings out of Msg.java
#======================================================

# Go up one level
project_root = split(os.getcwd())[0]

# Read Msg.java file
msg_path = join(project_root, 'src/net/sourceforge/docfetcher/enumeration/Msg.java')
msg_file = open(msg_path, 'r')
msg_file_contents = ''.join([x for x in msg_file])
msg_file.close()

# Get file lines that contain the enum fields of interest
result = re.match('.*?public enum Msg \{(.*?);.*', msg_file_contents, re.DOTALL)
result = result.group(1)
result = result.split('\n')

# Get enum fields
msg_items = []
for line in result:
	line = line.strip()
	if line == '' or line.startswith('//'):
		continue
	msg_items.append(re.match('(.*?),.*', line).group(1))


#======================================================
# Search for messages in Msg.java that are out of use
#======================================================

search_path = join(project_root, 'src')
src_files = []
for root, dirs, files in os.walk(search_path):
	for filename in files:
		if filename.endswith('.java') or filename.endswith('.aj'):
			src_files.append(join(root, filename))
msg_items_left = [x for x in msg_items]

for src_file_path in src_files:
	if not msg_items_left:
		break
	if os.path.basename(src_file_path) == 'Msg.java':
		continue
	src_file = open(src_file_path, 'r')
	for iline, line in enumerate(src_file):
		if not msg_items_left:
			break
		if not 'Msg.' in line:
			continue
		to_remove = []
		for item in msg_items:
			if 'Msg.' + item + '.' in line:
				to_remove.append(item)
		for item_to_remove in to_remove:
			try:
				msg_items_left.remove(item_to_remove)
			except ValueError:
				continue
		if not to_remove:
			package_path = src_file_path.replace(project_root, '')
			print 'Warning: Line %i in file %s was skipped: %s' % (iline + 1, package_path, line.strip())
	src_file.close()

if msg_items_left:
	print 'The following message strings in Msg.java seem to be out of use:'
	for item in msg_items_left:
		print '  * ' + item
	print ''
else:
	print 'No unused message strings in Msg.java found.\n'


#======================================================
# Get message strings out of all properties files
#======================================================

props_path = join(project_root, 'resources/lang')
propfilenames = [x for x in os.listdir(props_path) if x.startswith('Resource') and x.endswith('.properties')]

properties = []
for propfilename in propfilenames:
	path = join(props_path, propfilename)
	propfile = open(path, 'r')
	prop_items = []
	for line in propfile:
		line = line.strip()
		if line == '' or line.startswith('#'):
			continue
		prop_items.append(re.match('(.*?)\=.*', line, re.DOTALL).group(1))
	properties.append((propfilename, prop_items))
	propfile.close()


#======================================================
# Find unused messages in each properties file
#======================================================

for prop in properties:
	props_left = prop[1]
	to_remove = []
	for item in msg_items:
		if item in props_left:
			to_remove.append(item)
	for item in to_remove:
		try:
			props_left.remove(item)
		except ValueError:
			continue
	if props_left:
		print '%s contains message strings not used in Msg.java:' % prop[0]
		for item in props_left:
			print '  * ' + item
	else:
		print '%s: No unused entries found.' % prop[0]
	print ''
