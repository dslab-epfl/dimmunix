package dimmunix;



public class Signature {
	Vector<SignaturePosition> positions = new Vector<SignaturePosition>(10);
	Vector<InnerPosition> innerPositions = new Vector<InnerPosition>(10);
	boolean isDeadlockTemplate;
	
	int nYields = 0;
	int nYieldCycles = 0;

	public Signature(boolean isDeadlockTemplate) {
		this.isDeadlockTemplate = isDeadlockTemplate;
	}

	void add(SignaturePosition p) {
		positions.add(p);
	}
	
	void addInner(InnerPosition p) {
		innerPositions.add(p);
	}
	
	boolean equalsFrom(Signature tmpl, int startIndex) {
		for (int i = 0, k = startIndex; i < this.size(); i++, k = (k+1)% this.size()) {
			if (!positions.get(k).equals(tmpl.positions.get(i)))
				return false;
		}
		return true;
	}	

	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Signature))
			return false;
		Signature tmpl = (Signature)obj;
		if (this.size() != tmpl.size())
			return false;
		for (int i = 0; i < this.size(); i++) {
			if (this.equalsFrom(tmpl, i))
				return true;
		}
		return false;
	}	
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		if (isDeadlockTemplate)
			sb.append("deadlock_template=");
		else
			sb.append("livelock_template=");
		for (int i = 0; i < positions.size(); i++) {
			if (i > 0)
				sb.append(";");
			sb.append(positions.get(i));
			if (!innerPositions.isEmpty())
				sb.append("#"+ innerPositions.get(i));
		}
		return sb.toString();
	}
	
	int size() {
		return this.positions.size();
	}
	
/*	vaccine.Signature toVaccineSignature() {
		return new vaccine.Signature(this.toString());
	}
	
	public Signature(vaccine.Signature vaccineSig) {
		this.isDeadlockTemplate = true;
		
		java.util.Vector<vaccine.SigComponent> comps = vaccineSig.getComponents();
		for (vaccine.SigComponent c: comps) {
			Vector<String> csOuter = new Vector<String>();
			Vector<String> csInner = new Vector<String>();
			vaccine.CallStack csVaccOuter = c.getOuterCallStack();
			vaccine.CallStack csVaccInner = c.getInnerCallStack();
			
			for (vaccine.Frame f: csVaccOuter.getStack()) {
				csOuter.add(f.toString());
			}
			for (vaccine.Frame f: csVaccInner.getStack()) {
				csInner.add(f.toString());
			}
			
			SignaturePosition pOuter = new SignaturePosition(Dimmunix.dimmunix.rag.getPosition(csOuter), 1, Dimmunix.dimmunix);
			InnerPosition pInner = new InnerPosition(csInner);
			
			this.add(pOuter);
			this.addInner(pInner);
		}
	}*/
}
