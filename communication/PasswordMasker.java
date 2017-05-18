
public class PasswordMasker implements Runnable {
	private boolean active;
	
	public void run() {
		active = true;
		while(active) {
			System.out.print("\010*");
			try {
				Thread.currentThread().sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stop() {
		this.active = false;
	}
}
