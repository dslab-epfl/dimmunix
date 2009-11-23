package dimmunixTests;



import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.beancontext.BeanContext;
import java.beans.beancontext.BeanContextSupport;

public class TestBeanContextSupport {

	public static void main(String[] args) {
		final Object source = new Object();
		final BeanContextSupport support = new BeanContextSupport();
		BeanContext oldValue = support.getBeanContextPeer();
		Object newValue = new Object();
		final PropertyChangeEvent event = new PropertyChangeEvent(source, "beanContext", oldValue, newValue);
		
		support.add(source);
		try {
			support.vetoableChange(event);
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		}
		
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				support.propertyChange(event);
			}
		});
		Thread t2 = new Thread(new Runnable() {
			public void run() {
				support.remove(source);
			}
		});
		
		t1.start();
		t2.start();
		
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("done");		
	}
}
