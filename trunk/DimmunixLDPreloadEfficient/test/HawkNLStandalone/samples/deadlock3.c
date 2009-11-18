#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "nl.h"
#include <unistd.h>


NLushort serverport = 25002;
NLenum socktype = NL_RELIABLE_PACKETS;


void* closeThread(void* socket)
{
  int i = 0;
  
  //while (i < 0x1234567)
  //i++;

  //usleep(10000);

  NLsocket s = *(NLsocket *) socket;
  
  nlClose(s);
  printf("nl close finished\n");
  return (void*)i;
}

void* shutdownThread(void *s)
{
  
  int i = 0;
  
  while (i < 0x1223000)
    i++;

  usleep(10000);

  nlShutdown();
  printf("nl shutdown finished\n");
  return (void*) i;
}


void printErrorExit(void)
{
  NLenum err = nlGetError();
  
  if(err == NL_SYSTEM_ERROR)
    {
      printf("System error: %s\n", nlGetSystemErrorStr(nlGetSystemError()));
    }
  else
    {
      printf("HawkNL error: %s\n", nlGetErrorStr(err));
    }
  nlShutdown();
  exit(1);
}


int main(int argc, char **argv)
{
  NLsocket serversock;
  NLthreadID tid1, tid2;
  NLenum type = NL_IP;
  NLint exitcode;
  

  int i = 0;

  for(i = 0; i< 1000; i++)
    {
      
      if(!nlInit())
	printErrorExit();
      
      if(!nlSelectNetwork(type))
	printErrorExit();
      
      
      if (argc > 1)
	serverport = atoi(argv[1]);
      
      
      serversock = nlOpen(serverport, socktype); /* just a random port number ;) */
      
      if(serversock == NL_INVALID)
	printErrorExit();
      
      tid1 = nlThreadCreate(closeThread, (void *) & serversock, NL_TRUE);
      tid2 = nlThreadCreate(shutdownThread, (void*) & serversock, NL_TRUE);
      
      nlThreadJoin(tid1, (void ** ) &exitcode);
      nlThreadJoin(tid2, (void ** ) &exitcode);
    }    
  
  
  return 0;
}

