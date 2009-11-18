#include <pthread.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include "../sqlite3.h"


#define SQLITE_UNIX_THREADS 1

extern void sqlite3UnixEnterMutex();
extern void sqlite3UnixLeaveMutex();



static int callback(void *NotUsed, int argc, char **argv, char **azColName){
  int i;
  for(i=0; i<argc; i++){
    printf("%s = %s\n", azColName[i], argv[i] ? argv[i] : "NULL");
  }
  printf("\n");
  return 0;
}



int
test2(int y)
{
  sqlite3 *db;
  char *zErrMsg = 0;
  int rc;

  
  char dbname[10];
  char querry[20];
  
//  klee_make_symbolic(dbname, sizeof(dbname));
//  klee_make_symbolic(querry, sizeof(dbname));

  rc = sqlite3_open(dbname, &db);
  printf("dupa open\n");
  if( rc ){
    fprintf(stderr, "Can't open database: %s\n", sqlite3_errmsg(db));
    sqlite3_close(db);
    exit(1);
  }
  
  rc = sqlite3_exec(db, querry, callback, 0, &zErrMsg);
  if( rc!=SQLITE_OK ){
    fprintf(stderr, "SQL error: %s\n", zErrMsg);
    sqlite3_free(zErrMsg);
  }
  sqlite3_close(db);
  return 0;
}


void deadlock0()
{
  sqlite3 *db;
  char *zErrMsg = 0;
  int rc;

  char dbname[10];
  
  klee_make_symbolic(dbname, sizeof(dbname));

  rc = sqlite3_open(dbname, &db);
  if( rc ){
    fprintf(stderr, "Can't open database: %s\n", sqlite3_errmsg(db));
    sqlite3_close(db);
    exit(1);
  }

  sqlite3_close(db);
  
  
  sqlite3UnixEnterMutex();
  sqlite3UnixEnterMutex();
  

  printf("deadlock0 \n");
  
  sqlite3UnixLeaveMutex();
  sqlite3UnixLeaveMutex();
}


void deadlock1()
{
  usleep(5000);
  sqlite3UnixEnterMutex();

  printf("deadlock1 \n");
  
  sqlite3UnixLeaveMutex();
}


void* work0(void* arg)
{
  int c = 0;
  int x, y, z, t;
//  klee_make_symbolic(&x, sizeof(x));
//  klee_make_symbolic(&y, sizeof(y));
//  klee_make_symbolic(&z, sizeof(z));
//  klee_make_symbolic(&t, sizeof(t));
  
  switch(x)
    {
    case 0:
      c = 1;
      test2(y);
      break;
    case 1:
      c = 2;
      test2(y);
      break;
    case 2:
      c = 2;
      test2(y);
      break;
    case 3:
      c = 3;
      break;
    default:
      c = 4;
    }

  if(c == 3)
    {
      if(z == 1)
	if( t == 2)
	  {
	    test2(z);
	    deadlock0();
	  }
    }
  
  return 0;
}




void* work1(void* arg)
{
  deadlock1();
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
