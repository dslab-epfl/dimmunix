#!/bin/sh

jar uf `java -cp bin FindJavaHome`/lib/rt.jar -C bin dimmunix
