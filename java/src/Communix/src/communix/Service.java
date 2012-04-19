package communix;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Service extends Thread {
	protected volatile boolean running = true;
	
	protected SignatureDatabase database;
	
	protected final Random random = new Random(10);
	
	protected AtomicInteger nRequests = new AtomicInteger(0);
	
	public void kill() {
		this.running = false;
	}
	
	public Signature generateRandomSignature() {		
		Frame frame;
		try {
			frame = new Frame("package.class.method"+ this.random.nextInt(1000)+ "(File.java:50)"+ this.random.nextInt(1000000));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		Vector<Frame> frames = new Vector<Frame>();
		for (int i = 0; i < 10; i++) {
			frames.add(frame);
		}		
		CallStack cs = new CallStack(frames);
		
		Vector<CallStack> stacks = new Vector<CallStack>();
		stacks.add(cs);
		stacks.add(cs);
		
		Signature sig = new Signature(stacks, stacks);
		
		return sig;
	}
}
