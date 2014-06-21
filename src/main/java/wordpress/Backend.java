package wordpress;

import nova.VirtualServer;

import org.apache.log4j.Logger;

public class Backend extends VirtualServer {
	private static Logger log = Logger.getLogger(Logger.class);
	
	private static Backend instance;
	
	private Backend() {
		try {
			String script = new ScriptReader().read("setup-wordpress-backend.sh"); 
			super.setInitScript(script);
		} catch (Exception ex) {
			log.error("Error while preparing WordPress backend init script. No auto setup will be performed.");
		}
	}
	
	public static Backend getInstance() {
		if (instance == null) {
			instance = new Backend();
		}
		return instance;
	}
	
	public void spawn() {
		super.spawn("WordPress-Backend");
	}
}
