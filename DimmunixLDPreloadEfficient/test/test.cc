/*
 * test.cc
 *
 *  Created on: Sep 7, 2009
 *      Author: Horatiu
 */

#include <pthread.h>
#include <unistd.h>
#include <stdio.h>

pthread_mutex_t mutex1;
pthread_mutex_t mutex2;

void f1() {
	pthread_mutex_lock(&mutex1);
	sleep(1);
	pthread_mutex_lock(&mutex2);
	pthread_mutex_unlock(&mutex2);
	pthread_mutex_unlock(&mutex1);
	printf("thread 1 done\n");
}

void g1() {
	pthread_mutex_lock(&mutex2);
	sleep(1);
	pthread_mutex_lock(&mutex1);
	pthread_mutex_unlock(&mutex1);
	pthread_mutex_unlock(&mutex2);
	printf("thread 2 done\n");
}

void* f(void* args) {
	f1();
}

void* g(void* args) {
	g1();
}

int main() {

	pthread_mutex_init(&mutex1, 0);
	pthread_mutex_init(&mutex2, 0);

	pthread_t t1;
	pthread_t t2;

	pthread_create(&t1, NULL, f, NULL);
	pthread_create(&t2, NULL, g, NULL);

	pthread_join(t1, NULL);
	pthread_join(t2, NULL);
}