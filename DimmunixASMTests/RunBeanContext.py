#! /usr/bin/python

import commands, sys, os, time, Numeric

#print 'running without Dimmunix'

#for i in range(100):
#    print 'run ', i
#    commands.getoutput('java dimmunixTests.TestBeanContextSupport')

print 'running with Dimmunix'

for i in range(100):
    print 'run ', i
    print commands.getoutput('java -cp $CLASSPATH:/home/horatiu/asm-3.2/lib/asm-3.2.jar -Xbootclasspath/p:/home/horatiu/workspace/DimmunixASM/bin -javaagent:/home/horatiu/workspace/DimmunixInstrumentation/Dimmunix.jar dimmunixTests.TestBeanContextSupport')
