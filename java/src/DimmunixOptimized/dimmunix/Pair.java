package dimmunix;


public class Pair<T1, T2> {
	public T1 v1;
	public T2 v2;
	
	public Pair(T1 v1, T2 v2) {
		this.v1 = v1;
		this.v2 = v2;
	}
	
	public int hashCode() {
		return v1.hashCode()^ v2.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		Pair<T1, T2> p = (Pair<T1, T2>)obj;
		return p.v1.equals(this.v1) && p.v2.equals(this.v2);
	}
	
	public String toString() {
		return this.v1+ ","+ this.v2;
	}
}
