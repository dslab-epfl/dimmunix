#! /usr/bin/python

import commands, sys, os, time, Numeric

print 'generating history'

print commands.getoutput('java dimmunixTests.GenerateHistory 64 2 10')

print 'running without Dimmunix'

for i in range(1):
    print 'run ', i
    print commands.getoutput('java dimmunixTests.TestPerformance 60 1024 8 1 1000')

print 'running with Dimmunix'

for i in range(1):
    print 'run ', i
    print commands.getoutput('java -agentlib:hprof=heap=sites -cp $CLASSPATH:/home/horatiu/asm-3.2/lib/asm-3.2.jar -Xbootclasspath/p:/home/horatiu/workspace/DimmunixASM/bin -javaagent:/home/horatiu/workspace/DimmunixInstrumentation/Dimmunix.jar dimmunixTests.TestPerformance 60 1024 8 1 1000')
#    print commands.getoutput('java -cp $CLASSPATH:/home/horatiu/asm-3.2/lib/asm-3.2.jar -Xbootclasspath/p:/home/horatiu/workspace/DimmunixASM/bin -javaagent:/home/horatiu/workspace/DimmunixInstrumentation/Dimmunix.jar dimmunixTests.TestPerformance 60 1024 8 1 1000')
