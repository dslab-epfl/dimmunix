CC = g++
FLAGS = -lpthread -rdynamic -g
SRCS = test.cc
MAIN = test

all:    $(SRCS)
	$(CC) $(FLAGS) $(SRCS) -o $(MAIN)
clean:
	rm -f *.o *~ $(MAIN) microbenchmark

