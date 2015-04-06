# Quick Guide to Using the Optimized Java Dimmunix #

This guide helps you install and use the optimized Java Dimmunix.

**We recommend you to run the instructions from this guide in Ubuntu. Run all instructions in the same terminal window.**

## Install the Optimized Java Dimmunix ##

  1. Install prerequisite packages
```
sudo apt-get install ant subversion unzip wget
```
  1. Download and unpack the ASM instrumentation framework
```
wget http://download.forge.objectweb.org/asm/asm-3.2-bin.zip
unzip asm-3.2-bin.zip
```
  1. Download and compile our modified AspectJ weaver
```
wget http://dimmunix.googlecode.com/files/aspectjweaver1.6.8-src.zip
unzip aspectjweaver1.6.8-src.zip
cd aspectjweaver1.6.8-src
ant
```
  1. Download the Soot bytecode analysis framework
```
wget http://www.sable.mcgill.ca/software/sootall-2.3.0.tar.gz
mkdir sootall-2.3.0 ; tar -xzf sootall-2.3.0.tar.gz -C sootall-2.3.0
```
  1. Check out Dimmunix
```
svn checkout http://dimmunix.googlecode.com/svn/trunk/java/src/DimmunixOptimized Dimmunix
```
  1. Compile Dimmunix
```
cd /path/to/Dimmunix
ant
```

## Run Java Programs with the Optimized Dimmunix ##

Prepend the following to the command line arguments:
```
java -Xbootclasspath/p:/path/to/aspectjweaver1.6.8-src/aspectjweaver.jar:/path/to/Dimmunix:/path/to/sootall-2.3.0/soot-2.3.0/lib/sootclasses-2.3.0.jar:/path/to/asm-3.2/lib/asm-3.2.jar -javaagent:/path/to/aspectjweaver1.6.8-src/aspectjweaver.jar -javaagent:/path/to/Dimmunix/DimmunixAgent.jar ...
```

For instance, to run Eclipse with Dimmunix, add the following lines at the end of eclipse.ini:
```
-Xbootclasspath/p:/path/to/aspectjweaver1.6.8-src/aspectjweaver.jar:/path/to/Dimmunix:/path/to/sootall-2.3.0/soot-2.3.0/lib/sootclasses-2.3.0.jar:/path/to/asm-3.2/lib/asm-3.2.jar
-javaagent:/path/to/aspectjweaver1.6.8-src/aspectjweaver.jar 
-javaagent:/path/to/Dimmunix/DimmunixAgent.jar
```