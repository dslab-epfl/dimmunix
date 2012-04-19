#ifndef _dimmunix
#define _dimmunix

#define STACKSIZE 1000
#define MAXFRAMES 1
#define MAXPOSITIONS 16384
#define MAXTEMPLATESIZE 4
#define MAXTEMPLATES 1000
#define MAXCYCLESIZE 1000
#define DIMHISTFILE "/data/anr/dimmunix.hist"
#define STATS_PERIOD_SEC 30

/*
int {
    false, true
};
*/

struct QueueElem{
    void* elem;
    struct QueueElem * next;
};
struct Queue {
    struct QueueElem * head;
    struct QueueElem * tail;
    int size;
};

struct Position {
	char* id;
	struct Queue lockGrantees;
	struct Queue removedGrantees;
	bool inHist;
};

struct Node {
    void* id;
	int color;// WHITE = 0, GREY = 1, BLACK = 2
	int type;// thread = 0, lock = 1
	struct Node* next;//next node if t then t if o then o
	struct Position* posNext;

	// fields for lock nodes
	struct ThreadNode* owner;//thread
	volatile struct Position* acqPos;
	int nLockings;

    // fields for thread nodes
	struct Node** yields;
	struct Position** posYields;
	int nYields;
};

struct Cycle {
	struct Node* nodes[MAXCYCLESIZE];
	struct Position* labels [MAXCYCLESIZE];
	int len;
};

struct Template {
	struct Position* positions[MAXTEMPLATESIZE];
	struct Node* threads[MAXTEMPLATESIZE];// the threads that instantiate the template, assigned by instance() method
	int size;
	pthread_cond_t avoidanceCondVar;
};


void initDimmunix();
void stopDimmunix();
void* printStats(void* args);

void init_Cycle(struct Cycle* _c);
int hasCycleNext(struct Node* _n);
int hasCycleYields(struct Node* _n);
void copy(struct Cycle* _cIn, struct Cycle* _cOut);
void saveTemplate(struct Template* _tmpl);
void loadHistory();
int Template_equalsFrom(struct Template* _x, struct Template* _y, int _startIndex);
int Template_next(int _start, int _i, int _size);
void addToQueue(struct Queue* q, void* data);
void addElemToQueue(struct Queue* q, struct QueueElem* elem, void* data);
struct QueueElem* removeFromQueue(struct Queue* q, void* _elem, int doFree);
struct QueueElem* popQueue(struct Queue* q);
int addToHistory(struct Template* _tmpl);
void before(struct Thread* t, struct Node* tnode, struct Node* lnode);
void after(struct Thread* t, struct Node* tnode, struct Node* lnode);
void beforeUnlock(struct Node* tnode, struct Node* lnode);
int Request(int _first, struct Node* _t, struct Node* _l, struct Position* _p);
int instance(struct Template* templ, int n);
int threadIsNew(struct Template* templ, int n, struct Node* t);
void Acquired(struct Thread* t, struct Node* tnode, struct Node* lnode);
void Release(struct Node* _t, struct Node* _l);
void initQueue(struct Queue* q);
int hashOfStack(char* _stack);
void* findInQueue(struct Queue* q, void* _elem);
struct Position* findInQueuePos(struct Queue* q, char* _id);
void initNode(struct Node* _node, void* _id, int _type);
void* malloc_zero(size_t size);
void clearQueue(struct Queue* q);

#endif
