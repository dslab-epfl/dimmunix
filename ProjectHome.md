Dimmunix is a tool for imparting deadlock immunity to Java/C/C++ software (both legacy and new) without any assistance from programmers or users.

Deadlock immunity is a property by which programs, once afflicted by a deadlock pattern, develop the ability to avoid future occurrences of that deadlock pattern. Over time, programs with such an "immune system" progressively increase their resistance to deadlocks. Dimmunix was originally developed in the [Dependable Systems Lab](http://dslab.epfl.ch/) at [EPFL](http://ic.epfl.ch/) and is now open-source.

Dimmunix is well suited for general purpose software: desktop and enterprise applications, server software, etc. Dimmunix is available for Java, Linux NPTL (POSIX Threads), and FreeBSD libthr (POSIX Threads). Dimmunix was shown to work on real systems (JBoss, MySQL, ActiveMQ, Apache httpd, MySQL JDBC, Java JDK, Limewire) and effectively avoid real, reported deadlock bugs, while introducing only modest performance overhead.

Recently, Dimmunix has been augmented with a vaccination framework. The vaccine leverages application communities - networked computer nodes running the same program - to collectively boost the immunity of all members of that community. Antibodies can be shared by mutually untrusting members of the community, and a three-stage static analysis filter enables each node to verify these antibodies before using them.

To get started with Dimmunix proper, see the [Getting Started](http://code.google.com/p/dimmunix/wiki/GettingStarted) page. For the vaccination framework, see the [Vaccination Guide](http://code.google.com/p/dimmunix/wiki/GettingStartedVaccine) page.

End users who are not savvy programmers may find the [FAQ](http://code.google.com/p/dimmunix/wiki/FAQ) useful. For an in-depth description of the concepts behind Dimmunix, see [Deadlock Immunity: Enabling Systems To Defend Against Deadlocks](http://dslab.epfl.ch/pubs/dimmunix), a research paper published in the USENIX Symposium on Operating Systems Design and Implementation ([OSDI](http://www.usenix.org/events/osdi08/)), December 2008.

**Developers wanted**: We are looking for people interested in getting involved with the project and helping us make Dimmunix accessible to a wide audience. If you are interested, please [email us](mailto:dimmunix-dev@googlegroups.com).