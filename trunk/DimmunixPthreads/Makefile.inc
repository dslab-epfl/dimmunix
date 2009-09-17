.PATH: ${.CURDIR}/dimmu

#DEBUG_FLAGS=-O0 -DSTACK_OVERHEAD=4

CFLAGS+=-I/usr/local/include
CFLAGS+=-DNDEBUG

CXXFLAGS=${CFLAGS}

LDFLAGS+=-L/usr/local/lib
LDADD+=-lexecinfo -lstdc++

SRCS+= \
	Dimmunix.cc \
	dlock.c \
	util.c \
	position.cc \
	template.cc \
	mutex.cc \
	thread.cc \
	detection.cc \
	avoidance.cc \
	graph.cc

