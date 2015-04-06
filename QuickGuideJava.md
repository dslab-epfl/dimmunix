# Quick Guide to Using Java Dimmunix #

This guide helps you install Java Dimmunix and use it to immunize Java programs against deadlock bugs.

We tested Java Dimmunix on JRE 1.6, on an Ubuntu 9.10 32 bit system.

**We recommend you to run the instructions from this guide in Ubuntu. Run all instructions in the same terminal window.**

## Install Java Dimmunix ##

Create a folder where to download Dimmunix.
```
mkdir $HOME/Dimmunix
cd $HOME/Dimmunix
```

### Install Dimmunix From Source Code ###

  1. Install prerequisite packages
```
sudo apt-get install ant subversion unzip wget
```
  1. Download and unpack the ASM instrumentation framework
```
wget http://download.forge.objectweb.org/asm/asm-3.2-bin.zip
unzip asm-3.2-bin.zip
```
  1. Check out Java Dimmunix
```
svn checkout http://dimmunix.googlecode.com/svn/trunk/java dimmunix-java
```
  1. Compile Java Dimmunix
```
cd dimmunix-java/src/Dimmunix
ant
cd ../DimmunixInstrumentation
export CLASSPATH=$CLASSPATH:$HOME/Dimmunix/asm-3.2/lib/asm-3.2.jar
ant
```

### Install Dimmunix From Precompiled Binaries ###

  1. Install prerequisite packages
```
sudo apt-get install unzip wget
```
  1. Download and unpack the ASM instrumentation framework
```
wget http://download.forge.objectweb.org/asm/asm-3.2-bin.zip
unzip asm-3.2-bin.zip
```
  1. Download Dimmunix
```
wget http://dimmunix.googlecode.com/files/Dimmunix.jar
```
  1. Download Dimmunix agent
```
wget http://dimmunix.googlecode.com/files/DimmunixAgent.jar
```

## Use Dimmunix on Test Java Programs ##

The test programs from dimmunix-java/test folder deadlock deterministically.

To use Dimmunix on a test program, do the following steps:

### If you installed Dimmunix from source code ###
1. Compile the tests
```
cd $HOME/Dimmunix/dimmunix-java/test
ant
```
2. Run the test from Test.java
```
cd $HOME/Dimmunix
java -cp ./dimmunix-java/test dimmunixTests.Test
```
> You notice that the test deadlocks every time you run it. You can terminate it by typing **Ctrl-C**.
3. Now run the test program with Dimmunix
```
java -cp ./dimmunix-java/test -Xbootclasspath/p:./dimmunix-java/src/Dimmunix/bin:./asm-3.2/lib/asm-3.2.jar -javaagent:./dimmunix-java/src/DimmunixInstrumentation/DimmunixAgent.jar dimmunixTests.Test
```
> You notice that the program deadlocks in the first one or two runs. Terminate the program with **Ctrl-C** every time it deadlocks. Run the test program with Dimmunix again. You notice that the program does not deadlock in the subsequent runs.

### If you installed Dimmunix from precompiled binaries ###
1. Download test programs
```
cd $HOME/Dimmunix
wget http://dimmunix.googlecode.com/files/DimmunixTests.jar
```

2. Run the test from Test.java
```
java -cp ./DimmunixTests.jar dimmunixTests.Test
```
> You notice that the test deadlocks every time you run it. You can terminate it by typing **Ctrl-C**.
3. Now run the test program with Dimmunix
```
java -cp ./DimmunixTests.jar -Xbootclasspath/p:./Dimmunix.jar:./asm-3.2/lib/asm-3.2.jar -javaagent:./DimmunixAgent.jar dimmunixTests.Test
```
> You notice that the program deadlocks in the first one or two runs. Terminate the program with **Ctrl-C** every time it deadlocks. Run the test program with Dimmunix again. You notice that the program does not deadlock in the subsequent runs.