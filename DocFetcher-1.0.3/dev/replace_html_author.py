#!/usr/bin/python

import os, os.path, sys
from os.path import join, exists

"""
This script replaces the author tags in all HTML files in the given directory. It must be run with three parameters: The first is the
directory with the HTML files, the second is the old authorname and
the third is the new authorname.
"""

if len(sys.argv) != 4:
	print 'Three parameters expected: path, old author, new author'
	exit(0)

if not exists(target_dir):
	print 'Folder does not exist:', target_dir

target_dir = sys.argv[1]
old_author = sys.argv[2]
new_author = sys.argv[3]

for root, dirs, files in os.walk(target_dir):
	for filename in files:
		if not filename.endswith('.html'): continue
		path = join(root, filename)
		f = open(path, 'r')
		content = []
		found = False
		for line in f:
			if '<meta content=\"' + old_author + '\" name=\"author\">' in line:
				line = line.replace(old_author, new_author)
				found = True
			content.append(line)
		f.close()
		if not found: continue
		f = open(path, 'w')
		f.writelines(content)
		f.close()
		
