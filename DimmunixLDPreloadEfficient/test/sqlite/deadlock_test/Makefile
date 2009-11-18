CC = gcc
CFLAGS = -Wall -g 
LD_FLAGS = ../.libs/libsqlite3.a -lpthread -export-dynamic
SRC = deadlock.c
OBJECTS = deadlock.o
TARGET = deadlock
LD = ld 
#KLEE_ARGS = --simplify-sym-indices --write-cvcs --write-cov --output-module --max-memory=1000 --disable-inlining --optimize --use-forked-stp --use-cex-cache --with-libc --allow-external-sym-calls --use-call-paths=false --fork-on-schedule=true --scheduler-preemption-bound=2 --with-file-model=release --init-env 


%o: %c
	$(CC) $(CFLAGS) -c $< -o $@

all: $(OBJECTS)
	$(CC) $(CFLAGS) $(OBJECTS) $(LD_FLAGS) -o $(TARGET)
#	$(CC) $(CFLAGS) -c $(SRC) $(LD_FLAGS) -o $(TARGET)
#	$(LD) $(OBJECTS) -o $(TARGET) 

#run:
#	klee $(KLEE_ARGS) $(TARGET).bc



clean:
	rm -f $(TARGET)