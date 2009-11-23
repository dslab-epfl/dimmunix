CC = gcc
CFLAGS = -Wall -g -I../include
LDFLAGS = -lNL  -L../src -Wl,pthread -export-dynamic

OBJECTS = deadlock3.o 
TARGET = deadlock3


KLEE_ARGS = --simplify-sym-indices --write-cvcs --write-cov --output-module --max-memory=1000 --disable-inlining --optimize --use-forked-stp --use-cex-cache --with-libc --allow-external-sym-calls --use-call-paths=false --fork-on-schedule=false 

%.o : %.c
	$(CC) -c $(CFLAGS) $< -o $@

all: $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $(TARGET)

run:
	klee $(KLEE_ARGS) $(TARGET).bc
# all:
# 	$(CC) -Wall -g -o clientserver clientserver.c  -lNL -I../include -L../src -lpthread -rdynamic
# 	$(CC) -Wall -g -o deadlock deadlock.c -lNL -I../include -L../src -lpthread
# 	$(CC) -Wall -g -o deadlock2 deadlock2.c -lNL -I../include -L../src -lpthread
# 	$(CC) -Wall -g -o deadlock3 deadlock3.c -lNL -I../include -L../src -lpthread -rdynamic
# 	$(CC) -Wall -g -o threadpool threadpool.c -lNL -I../include -L../src -lpthread -rdynamic

clean:
	rm -f $(OBJECTS) $(TARGET) $(TARGET).bc

