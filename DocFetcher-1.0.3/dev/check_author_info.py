#! /usr/bin/python

"""
This script searches for source files that do not contain an '@author' tag.
"""

import os, os.path, re
from os.path import join, split

# Go up one level
project_root = split(os.getcwd())[0]

for root, dirs, files in os.walk(join(project_root, 'src')):
    for filename in files:
        if not filename.endswith('.java') and not filename.endswith('.aj'):
            continue
        src_file = open(join(root, filename), 'r')
        src_file_contents = ''.join([x for x in src_file])
        if not '@author' in src_file_contents:
            print 'Does not contain an author tag:', filename

print 'Search for author tags completed.'
