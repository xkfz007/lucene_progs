#! /usr/bin/python

"""
This script makes sure all source files start with a license notice.
In case no license notice is found, the contents of the file
'default_license_notice.txt' are inserted.
"""

import os, os.path, re
from os.path import join, split
from time import localtime

# Go up one level
project_root = split(os.getcwd())[0]

for root, dirs, files in os.walk(project_root, 'src'):
	for filename in files:
		if not filename.endswith('.java') and not filename.endswith('.aj'):
			continue
		
		# Get license portion of the source file
		filepath = join(root, filename)
		src_file = open(filepath, 'r')
		license_lines = []
		for line in src_file:
			if line.startswith('package'):
				break
			license_lines.append(line)
		src_file.close()
		
		# Put new license in files that don't have a license notice
		if ''.join(license_lines).strip() == '':
			src_file = open(filepath, 'r')
			src_file_contents = ''.join([x for x in src_file]).lstrip()
			src_file.close()
			license_file = open('default_license_notice.txt', 'r')
			license_file_contents = ''.join([x for x in license_file]).strip()
			license_file.close()
			license_file_contents = license_file_contents.replace('{THIS_YEAR}', str(localtime()[0]))
			src_file_contents = license_file_contents + '\n\n' + src_file_contents
			src_file = open(filepath, 'w')
			src_file.write(src_file_contents)
			src_file.close()
			print 'License notice added:', filename
		
		# Replace outdated license notice
		else:
			continue
			last_modified = os.path.getmtime(filepath)
			last_modified_year = localtime(last_modified)[0]
			
			# Get the copyright line in the license notice
			copy_right_line = ''
			for line in license_lines:
				if line.lower().startswith(' * copyright (c)'):
					copy_right_line = line.strip()
			
			# Extract highest year from copyright line
			match = re.match('.*\(c\)([0-9, ]+).*', copy_right_line)
			copyright_years = match.group(1).strip()
				# Can be something like '2007' or '2007, 2008'
			copyright_year = copyright_years
			match = re.match('[0-9]+[, ]+([0-9]+)', copyright_years)
			if hasattr(match, 'group'):
				copyright_year = match.group(1)
			copyright_year = int(copyright_year)
			
			# Update copyright year in file if it's out of date
			if copyright_year != last_modified_year:
				src_file = open(filepath, 'r')
				src_file_contents = []
				for line in src_file:
					match = re.match('.*\(c\)([0-9, ]+).*', line)
					if hasattr(match, 'group'):
						newline = line.replace(copyright_years, '2007, ' + str(last_modified_year))
						print 'Updated', filename + ":", line.strip(), '===>', newline.strip()
						line = newline
					src_file_contents.append(line)
				src_file.close()
				src_file = open(filepath, 'w')
				src_file.writelines(src_file_contents)
				src_file.close()

print 'All source files have been updated.'
