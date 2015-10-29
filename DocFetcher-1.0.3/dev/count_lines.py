#! /usr/bin/python

'''
Simple script for counting source code lines.
'''

import os
from os.path import join, split

total = 0
total_nonempty = 0
extensions = ['java', 'aj']

# Go up one level
project_root = split(os.getcwd())[0]

for root, dirs, files in os.walk(join(project_root, 'src')):
    for filename in files:
        if filename.split('.')[-1] in extensions:
            filepath = join(root, filename)
            f = open(filepath)
            for line in f:
            	total += 1
            	if line.strip() != '':
            		total_nonempty += 1
            #total += len(f.readlines())
            f.close()
            
print 'Line count all:', total
print 'Line count without blank lines:', total_nonempty
print 'Ratio filled lines to total lines:', str(total_nonempty * 100 / total) + '%'
