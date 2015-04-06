We provide two distributions of Dimmunix: one for **Java** and one for **POSIX Threads**.
Java Dimmunix intercepts the synchronization operations using the [ASM instrumentation framework](http://asm.ow2.org/).
POSIX Threads Dimmunix works for **Linux** systems and intercepts synchronization operations using LD\_PRELOAD.

Here is a [quick guide to using Java Dimmunix](QuickGuideJava.md).

Here is a [quick guide to using POSIX Threads Dimmunix](QuickGuidePOSIXThreads.md).

The early prototypes used for measurements reported in [Dimmunix paper](http://dslab.epfl.ch/pubs/dimmunix) are available upon request.
These older versions are harder to use.
The old POSIX Threads Dimmunix modifies the NPTL library; therefore, the NPTL library needs to be recompiled.
The old Java Dimmunix uses AspectJ to statically instrument the bytecode of the application classes with Dimmunix; this can become a tedious task for large Java applications (e.g., JBoss).
The new Dimmunix distributions that we provide instrument applications at load-time; therefore, no recompilation or static instrumentation are needed.