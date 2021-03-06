
set rcsid {$Id: omittest.tcl,v 1.1 2006/01/26 13:11:37 danielk1977 Exp $}

# Documentation for this script. This may be output to stderr
# if the script is invoked incorrectly.
set ::USAGE_MESSAGE {
This Tcl script is used to test the various compile time options 
available for omitting code (the SQLITE_OMIT_xxx options). It
should be invoked as follows:

    <script> ?-makefile PATH-TO-MAKEFILE?

The default value for ::MAKEFILE is "../Makefile.linux.gcc".

This script builds the testfixture program and runs the SQLite test suite
once with each SQLITE_OMIT_ option defined and then once with all options
defined together. Each run is performed in a seperate directory created
as a sub-directory of the current directory by the script. The output
of the build is saved in <sub-directory>/build.log. The output of the
test-suite is saved in <sub-directory>/test.log.

Almost any SQLite makefile (except those generated by configure - see below)
should work. The following properties are required:

  * The makefile should support the "testfixture" target.
  * The makefile should support the "test" target.
  * The makefile should support the variable "OPTS" as a way to pass
    options from the make command line to lemon and the C compiler.

More precisely, the following two invocations must be supported:

  make -f $::MAKEFILE testfixture OPTS="-DSQLITE_OMIT_ALTERTABLE=1"
  make -f $::MAKEFILE test

Makefiles generated by the sqlite configure program cannot be used as
they do not respect the OPTS variable.
}


# Build a testfixture executable and run quick.test using it. The first
# parameter is the name of the directory to create and use to run the
# test in. The second parameter is a list of OMIT symbols to define
# when doing so. For example:
#
#     run_quick_test /tmp/testdir {SQLITE_OMIT_TRIGGER SQLITE_OMIT_VIEW}
#
#
proc run_quick_test {dir omit_symbol_list} {
  # Compile the value of the OPTS Makefile variable.
  set opts "-DSQLITE_MEMDEBUG=2 -DSQLITE_DEBUG -DOS_UNIX" 
  foreach sym $omit_symbol_list {
    append opts " -D${sym}=1"
  }

  # Create the directory and do the build. If an error occurs return
  # early without attempting to run the test suite.
  file mkdir $dir
  puts -nonewline "Building $dir..."
  flush stdout
  set rc [catch {
    exec make -C $dir -f $::MAKEFILE testfixture OPTS=$opts >& $dir/build.log
  }]
  if {$rc} {
    puts "No good. See $dir/build.log."
    return
  } else {
    puts "Ok"
  }
  
  # Create an empty file "$dir/sqlite3". This is to trick the makefile out 
  # of trying to build the sqlite shell. The sqlite shell won't build 
  # with some of the OMIT options (i.e OMIT_COMPLETE).
  if {![file exists $dir/sqlite3]} {
    set wr [open $dir/sqlite3 w]
    puts $wr "dummy"
    close $wr
  }

  # Run the test suite.
  puts -nonewline "Testing $dir..."
  flush stdout
  set rc [catch {
    exec make -C $dir -f $::MAKEFILE test OPTS=$opts >& $dir/test.log
  }]
  if {$rc} {
    puts "No good. See $dir/test.log."
  } else {
    puts "Ok"
  }
}


# This proc processes the command line options passed to this script.
# Currently the only option supported is "-makefile", default
# "../Makefile.linux-gcc". Set the ::MAKEFILE variable to the value of this
# option.
#
proc process_options {argv} {
  set ::MAKEFILE ../Makefile.linux-gcc              ;# Default value
  for {set i 0} {$i < [llength $argv]} {incr i} {
    switch -- [lindex $argv $i] {
      -makefile {
        incr i
        set ::MAKEFILE [lindex $argv $i]
      }
  
      default {
        puts stderr [string trim $::USAGE_MESSAGE]
        exit -1
      }
    }
    set ::MAKEFILE [file normalize $::MAKEFILE]
  }
}

# Main routine.
#
proc main {argv} {
  # List of SQLITE_OMIT_XXX symbols supported by SQLite.
  set ::SYMBOLS [list                  \
    SQLITE_OMIT_COMPLETE               \
    SQLITE_OMIT_ALTERTABLE             \
    SQLITE_OMIT_AUTOVACUUM             \
    SQLITE_OMIT_AUTHORIZATION          \
    SQLITE_OMIT_AUTOINCREMENT          \
    SQLITE_OMIT_BLOB_LITERAL           \
    SQLITE_OMIT_COMPOUND_SELECT        \
    SQLITE_OMIT_CONFLICT_CLAUSE        \
    SQLITE_OMIT_DATETIME_FUNCS         \
    SQLITE_OMIT_EXPLAIN                \
    SQLITE_OMIT_FLOATING_POINT         \
    SQLITE_OMIT_FOREIGN_KEY            \
    SQLITE_OMIT_INTEGRITY_CHECK        \
    SQLITE_OMIT_MEMORYDB               \
    SQLITE_OMIT_PAGER_PRAGMAS          \
    SQLITE_OMIT_PRAGMA                 \
    SQLITE_OMIT_PROGRESS_CALLBACK      \
    SQLITE_OMIT_REINDEX                \
    SQLITE_OMIT_SCHEMA_PRAGMAS         \
    SQLITE_OMIT_SCHEMA_VERSION_PRAGMAS \
    SQLITE_OMIT_SUBQUERY               \
    SQLITE_OMIT_TCL_VARIABLE           \
    SQLITE_OMIT_TRIGGER                \
    SQLITE_OMIT_UTF16                  \
    SQLITE_OMIT_VACUUM                 \
    SQLITE_OMIT_VIEW                   \
  ]

  # Process any command line options.
  process_options $argv
  
  # First try a test with all OMIT symbols except SQLITE_OMIT_FLOATING_POINT 
  # and SQLITE_OMIT_PRAGMA defined. The former doesn't work (causes segfaults)
  # and the latter is currently incompatible with the test suite (this should
  # be fixed, but it will be a lot of work).
  set allsyms [list]
  foreach s $::SYMBOLS {
    if {$s!="SQLITE_OMIT_FLOATING_POINT" && $s!="SQLITE_OMIT_PRAGMA"} {
      lappend allsyms $s
    }
  }
  run_quick_test test_OMIT_EVERYTHING $allsyms
  
  # Now try one quick.test with each of the OMIT symbols defined. Included
  # are the OMIT_FLOATING_POINT and OMIT_PRAGMA symbols, even though we
  # know they will fail. It's good to be reminded of this from time to time.
  foreach sym $::SYMBOLS {
    set dirname "test_[string range $sym 7 end]"
    run_quick_test $dirname $sym
  }
}

main $argv
