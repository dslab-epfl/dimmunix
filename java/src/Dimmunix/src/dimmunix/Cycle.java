package dimmunix;

public class Cycle {
	Vector<Node> nodes = new Vector<Node>(10);
	Vector<Position> positions = new Vector<Position>(10);
	
	public Cycle() {
	}
	
	public Cycle(int capacity) {
		nodes = new Vector<Node>(capacity);
		positions = new Vector<Position>(capacity);
	}
	
	void add(Node node, Position position) {
		this.nodes.add(node);
		this.positions.add(position);
	}
	
	void remove() {
		this.nodes.remove();
		this.positions.remove();
	}
	
	boolean equalsFrom(Cycle cycle, int startIndex) {
		for (int i = 0, k = startIndex; i < this.size(); i++, k = (k+1)% this.size()) {
			if (nodes.get(k) != cycle.nodes.get(i) || positions.get(k) != cycle.positions.get(i))
				return false;
		}
		return true;
	}	

	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Cycle))
			return false;
		Cycle cycle = (Cycle)obj;
		if (this.size() != cycle.size())
			return false;
		for (int i = 0; i < this.size(); i++) {
			if (this.equalsFrom(cycle, i))
				return true;
		}
		return false;
	}	
	
	int size() {
		return nodes.size();
	}
	
	boolean isYieldEdge(int i) {
		return (nodes.get(i) instanceof ThreadNode) && (nodes.get((i+ 1)% size()) instanceof ThreadNode);
	}
	
	boolean isHoldEdge(int i) {
		return (nodes.get(i) instanceof LockNode) && (nodes.get((i+ 1)% size()) instanceof ThreadNode);		
	}
	
	boolean isRequestEdge(int i) {
		return (nodes.get(i) instanceof ThreadNode) && (nodes.get((i+ 1)% size()) instanceof LockNode);		
	}
	
	boolean isDeadlock() {
		for (int i = 0; i < size(); i++) {
			if (isYieldEdge(i))
				return false;
		}
		return true;
	}	
	
	void clear() {
		nodes.clear();
		positions.clear();
	}
	
	boolean contains(Node x) {
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i) == x)
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < size(); i++)
			sb.append(nodes.get(i)+ "-->"+ nodes.get((i+ 1)% size())+ " ");
		return sb.toString();
	}	
}
