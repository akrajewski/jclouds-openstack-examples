package wordpress;

import nova.VirtualServer;

import org.apache.log4j.Logger;
import org.jclouds.openstack.nova.v2_0.domain.Address;

import com.google.common.base.Optional;

public class Frontend extends VirtualServer {
	private static Logger log = Logger.getLogger(Logger.class);
	
	private static int count = 0;

	private static String script = null;
	
	static {
		try {
			script = new ScriptReader().read("setup-wordpress-frontend.sh");
		} catch (Exception ex) {
			log.error("Error while preparing WordPress backend init script. No auto setup will be performed.", ex);
		}
	}
	
	public Frontend(Backend backend) {
		Optional<Address> backendIP = backend.getPrivateAddress();
		
		String scriptWithBackendIP = null;
		if (backendIP.isPresent()) {
			scriptWithBackendIP = injectBackendIPAddress(script, backendIP.get().getAddr());
		} else {
			log.error("Unable to get WordPress backend private ip address. Using localhost.");
			scriptWithBackendIP = injectBackendIPAddress(script, "localhost");
		}
		
		try {
			setInitScript(scriptWithBackendIP);
		} catch (Exception ex) {
			log.error("Error while preparing WordPress frontend init script. No auto setup will be performed.", ex);
		}
	}
	
	private static String injectBackendIPAddress(String script, String ipAddr) {
		return script.replace("{backend_ip_address}", ipAddr);
	}
	
	public void spawn() {
		super.spawn("WordPress-Frontend-" + (count++));
	}
}
