package dIV.core.staticAnalysis;

/**
 * Keeps the corresponding indexes for a LockEvent detected in lock inversion
 * check
 * 
 * @author pinar
 */
public class InversionInstance {

	int tIndex;
	int fIndex;
	int bIndex;
	int lIndex;
	LockEvent l;

	public InversionInstance(int tIndex, int fIndex, int bIndex, int lIndex,
			LockEvent l) {
		this.tIndex = tIndex;
		this.fIndex = fIndex;
		this.bIndex = bIndex;
		this.lIndex = lIndex;
		this.l = l;
	}

	public void print() {
		System.out.println("TIndex: " + this.tIndex);
		System.out.println(" fIndex: " + this.fIndex);
		System.out.println("  bIndex: " + this.bIndex);
		System.out.println("   lIndex: " + this.lIndex);
		this.l.print();
	}
}
