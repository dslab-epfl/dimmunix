#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "nl.h"
#include <unistd.h>

NLmutex lock;

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

#define MAX_CLIENTS 2
NLenum socktype = NL_RELIABLE_PACKETS;
NLushort serverport = 25002;

volatile NLint clientnum = 0;
volatile  NLint group;


int shutdown = 0;


static void * work(void* socket)
{
  NLbyte buffer[128];
  int readlen;
  NLsocket s = *(NLsocket *)socket;


  
  while((readlen = nlRead(s, buffer, sizeof(buffer))) > 0)
    {
      buffer[127] = 0; 
    }
  

  if(readlen == NL_INVALID)
    {
      NLenum err = nlGetError();
      
      if( err == NL_MESSAGE_END || err == NL_SOCK_DISCONNECT)
	{
	  nlMutexLock(&lock);
	  nlGroupDeleteSocket(group, s);
	  clientnum--;
	  nlMutexUnlock(&lock);
	  
	  nlClose(s);
	}
    }

  
  if (!shutdown)
    {
      sleep(2);
      shutdown = 1;
      nlShutdown();
    }
  else
    {            
      printf("closing socket\n");
      nlClose(s);
    }

  

  
  printf("worker thread exited\n");
  return 0;
}



static void *mainServerLoop(void *s)
{
  NLsocket    sock = *(NLsocket *)s;
  NLsocket    client[MAX_CLIENTS];
  
  memset(client, 0, sizeof(client));
  
  group = nlGroupCreate();
  while(1)
    {
      NLint i, count;
      NLsocket s[MAX_CLIENTS];
      
      /* check for a new client */
      NLsocket newsock = nlAcceptConnection(sock);
      
      if(newsock != NL_INVALID)
        {
	  NLaddress   addr;
	  nlMutexLock(&lock);
	  nlGetRemoteAddr(newsock, &addr);
	  client[clientnum] = newsock;
	  nlGroupAddSocket(group, newsock);
	  clientnum++;
	  nlMutexUnlock(&lock);
        }
      else
        {
	  if(nlGetError() == NL_SYSTEM_ERROR)
            {
	      printf("\n\nServer shutdown!!!!!!!!!!!!!\n");
	      printErrorExit();
            }
        }
      
      /* check for incoming messages */
      count = nlPollGroup(group, NL_READ_STATUS, s, MAX_CLIENTS, 0);
      if(count == NL_INVALID)
        {
	  NLenum err = nlGetError();
          
	  if(err == NL_SYSTEM_ERROR)
            {
	      //printf("nlPollGroup system error: %s\n", nlGetSystemErrorStr(nlGetSystemError()));
            }
	  else
            {
	      printf("nlPollGroup HawkNL error: %s\n", nlGetErrorStr(err));
            }
        }
      else
	{
	  
	  if(count > 0)
	    {
	      //printf("\n\n!!!!!!!!!count = %d\n", count);
	    }
	  
	  /* loop through the clients and read the packets */
	  for(i=0;i<count;i++)
	    {
	      //spawn a thread for each request
	      (void) nlThreadCreate(work, (void*) &s[i], NL_TRUE);
	    }
	}
    }
  return 0;
}


static void *mainClientLoop(void *s)
{
  NLsocket sock[MAX_CLIENTS];
  NLaddress addr;
  NLint i, iterations = 4;
  NLbyte str[256];
  
  sleep(1);
  
  /* create the client sockets */
  for(i=0;i<MAX_CLIENTS;i++)
    {
      sock[i] = nlOpen(0, socktype);
      if(sock[i] == NL_INVALID)
	printErrorExit();
      /* now connect */
      nlGetLocalAddr(sock[i], &addr);
      nlSetAddrPort(&addr, serverport);
      if(!nlConnect(sock[i], &addr))
	{
	  printErrorExit();
	}
      printf("CLIENT %d connect to %s\n", i, nlAddrToString(&addr, str));
    }
  
  while( iterations-- > 0)
    {
      for(i=0;i<MAX_CLIENTS;i++)
	{
	  sprintf(str, "Client %d says hello, hello", i);
	  nlWrite(sock[i], str, strlen(str) + 1);
	  
	  sprintf(str, "... client %d out.", i);
	  nlWrite(sock[i], str, strlen(str) + 1);
	  
	  /*while(nlRead(sock[i], str, sizeof(str)) > 0)
	    {
	    printf("\"%s\",", str);
	    }
	  */
	  while(nlRead(sock[i], str, sizeof(str)) > 0)
	    {
	      printf("\"%s\",", str);
	    }
	}
    }
  
    
  for(i=0;i<MAX_CLIENTS;i++)
    {
      nlClose(sock[i]);
      printf("CLIENT %d CLOSED\n", i);
    }
  
  //sleep(1);
  return (void *)4;
}

int main(int argc, char **argv)
{
  NLsocket        serversock;
  NLthreadID      tid;
  NLenum          type = NL_IP;/* default network type */
  NLint           exitcode;


  
  
  if(!nlInit())
    printErrorExit();
  
  printf("nlGetString(NL_VERSION) = %s\n\n", nlGetString(NL_VERSION));
  printf("nlGetString(NL_NETWORK_TYPES) = %s\n\n", nlGetString(NL_NETWORK_TYPES));
  
  if (argc == 2)
    {
      if(strcmp(argv[1], "NL_IPX") == 0)
        {
	  type = NL_IPX;
        }
      else if(strcmp(argv[1], "NL_LOOP_BACK") == 0)
        {
	  type = NL_LOOP_BACK;
        }
    }
  
  if(!nlSelectNetwork(type))
    printErrorExit();
  
  nlMutexInit(&lock);
  
  if (argc > 1)
    serverport = atoi(argv[1]);


  /* create the server socket */
  serversock = nlOpen(serverport, socktype); /* just a random port number ;) */
  
  if(serversock == NL_INVALID)
    printErrorExit();
  
  if(!nlListen(serversock))       /* let's listen on this socket */
    {
      nlClose(serversock);
      printErrorExit();
    }
  /* start one server thread */
  (void) nlThreadCreate(mainServerLoop, (void *)&serversock, NL_FALSE);

  
  /*now enter the client loop */
  tid = nlThreadCreate(mainClientLoop, NULL, NL_TRUE);
  nlThreadJoin(tid, (void **)&exitcode);
  
  sleep(20);
  
  nlShutdown();
  nlMutexDestroy(&lock);
  return 0;
}

