#include <pthread.h>
#include <stdio.h>
#include <unistd.h>
#include "../sqlite3.h"

#define SQLITE_UNIX_THREADS 1

extern void sqlite3UnixEnterMutex();
extern void sqlite3UnixLeaveMutex();


void* work0(void* arg)
{
  sqlite3UnixEnterMutex();
  sqlite3UnixEnterMutex();

  printf("work0 \n");

  sqlite3UnixLeaveMutex();
  sqlite3UnixLeaveMutex();
  return 0;
}

void* work1(void* arg)
{
	  sleep(1);
  sqlite3UnixEnterMutex();

  printf("work1 \n");

  sqlite3UnixLeaveMutex();
  return 0;
}

int main(int argc, char* argv[])
{
  pthread_t t0, t1;
  int rc;

  rc = pthread_create (&t0, 0, work0, 0);
  rc = pthread_create (&t1, 0, work1, 0);


  pthread_join(t0, 0);
  pthread_join(t1, 0);

  return 0;
}
