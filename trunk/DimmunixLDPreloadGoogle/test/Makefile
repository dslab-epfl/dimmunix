CC = g++
FLAGS = -lpthread -rdynamic -g
SRCS = test_perf.cc
MAIN = test_perf

all:    $(SRCS)
	$(CC) $(FLAGS) $(SRCS) -o $(MAIN)
	$(CC) $(FLAGS) test.cc -o test
clean:
	rm -f *.o *~ $(MAIN)

