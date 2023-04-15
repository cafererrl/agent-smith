# Agent Smith

A Java solution to implement object protection and guard against unauthorized domain switches within various forms of an access matrix.

Running this program will generate a random number of Domains ("users") and a random number of Objects ("files").
A table displays the contained permissions (`read`, `write`, `read-write`, `null`) for the stubbed files and user domains. Contents of these files are strings, and are also printed prior to user activity. This is simulated through each threads attempts to read to a file, write to a file, or to switch user domain. According to object permissions, these operations are denied or succeed. 
