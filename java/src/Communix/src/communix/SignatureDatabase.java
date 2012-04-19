package communix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class SignatureDatabase {
	//need to keep this public for efficiency
	private final Vector<Signature> signatures = new Vector<Signature>();
	
	private final HashSet<Signature> signaturesSet = new HashSet<Signature>();
	
	private final String file;
	
	public final int initialSize;
	
	public SignatureDatabase() {
		this.file = null;
		
		this.initialSize = 0;
	}
	
	public SignatureDatabase(String file) {
		this.file = System.getProperty("user.home")+ "/"+ file;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(this.file));
			
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				
				try {
					Signature sig = new Signature(line);
					this.signatures.add(sig);
					this.signaturesSet.add(sig);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			br.close();
		} catch (FileNotFoundException e) {
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}
		
		this.initialSize = this.signatures.size();
	}
	
	public void addSignature(Signature sig) {
		synchronized (this.signatures) {
			if (this.signaturesSet.add(sig)) {
				this.signatures.add(sig);
			}			
		}
	}	
	
	public void save(int fromIndex) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
			
			for (int i = fromIndex; i < this.signatures.size(); i++) {
				bw.write(this.signatures.get(i)+ "\n");
				bw.flush();
			}
			
			bw.close();
			
			bw = new BufferedWriter(new FileWriter(file+ "_size"));
			bw.write(""+ this.signatures.size());
			bw.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public List<Signature> getSignatures() {
		return Collections.unmodifiableList(this.signatures);
	}
	
	public int size() {
		synchronized (signatures) {
			return this.signatures.size();			
		}
	}
	
	public void clear() {
		this.signatures.clear();
		this.signaturesSet.clear();
	}
}
