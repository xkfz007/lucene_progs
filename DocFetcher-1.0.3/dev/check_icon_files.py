#! /usr/bin/python

"""
This script checks whether all icon files in the resource folder
are listed in the Icon.java file.
"""

import os, os.path, re
from os.path import join, exists, split


#======================================================
# Get entries out of Icon.java
#======================================================

# Go up one level
project_root = split(os.getcwd())[0]

# Read Icon.java file
icon_path = join(project_root, 'src/net/sourceforge/docfetcher/enumeration/Icon.java')
icon_file = open(icon_path, 'r')
icon_file_contents = ''.join([x for x in icon_file])
icon_file.close()

# Get file lines that contain the enum fields of interest
result = re.match('.*?public enum Icon \{(.*?);.*', icon_file_contents, re.DOTALL)
result = result.group(1)
result = result.split('\n')

# Get enum fields
icon_items = []
for line in result:
	line = line.strip()
	if line == '' or line.startswith('//'):
		continue
	icon_items.append(re.match('.*?\(\"(.*?)\"\).*', line).group(1))


#======================================================
# Compare Icon.java entries with files in resource folders
#======================================================

path = join(project_root, 'resources/icons')
icon_files = os.listdir(path)
to_remove = []
for icon_file in icon_files:
	if icon_file in icon_items:
		to_remove.append(icon_file)
for r in to_remove:
	icon_files.remove(r)
if icon_files:
	print 'The following icon files are unused:'
	for icon_file in icon_files:
		print icon_file
else:
	print 'All icon files are used.'
