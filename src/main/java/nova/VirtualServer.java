package nova;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;
import org.jclouds.openstack.nova.v2_0.domain.Address;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class VirtualServer {
	private static Logger log = Logger.getLogger(VirtualServer.class);

	private boolean spawned;
	private Optional<Address> privateAddress;
	private String serverId;
	private byte[] initScriptBytes;
	
	public String getServerId() {
		return serverId;
	}
	
	protected void setInitScript(String script) throws UnsupportedEncodingException {
		initScriptBytes = Strings.isNullOrEmpty(script) ? script.getBytes("UTF-8") : null;
	}
	
	protected void spawn(String name) {
		spawn(name, initScriptBytes);
	}
	
	protected void spawn(String name, byte[] userData) {
		NovaClient novaClient = null;
		try {
			novaClient = new NovaClient();
			serverId = novaClient.createServer(name, userData);
			spawned = true;
		} catch (Exception ex) {
			log.error("Error while spawning WordPress frontend", ex);
		} finally {
			if (novaClient != null) {
				novaClient.close();
			}
		}
	}
	
	public Optional<Address> getPrivateAddress() {
		if (!spawned) {
			return Optional.absent();
		} else if (privateAddress != null) {
			return privateAddress;
		}
		
		NovaClient novaClient = null;
		try {
			novaClient = new NovaClient();
			return novaClient.getPrivateAddress(serverId, "demo-net");
		} catch (Exception ex) {
			log.error("Error while getting WordPress Backend private address", ex);
			return Optional.absent();
		} finally {
			if (novaClient != null) {
				novaClient.close();
			}
		}
	}
	
}
