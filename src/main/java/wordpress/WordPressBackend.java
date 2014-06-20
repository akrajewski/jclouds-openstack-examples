package wordpress;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.jclouds.openstack.nova.v2_0.domain.Address;

import clients.NovaClient;

import com.google.common.base.Optional;
import com.google.common.io.CharStreams;

public class WordPressBackend {
	private static Logger log = Logger.getLogger(Logger.class);
	
	private static WordPressBackend instance;
	
	private String serverId;
	private Optional<Address> privateAddress;
	
	private byte[] wordPressScriptBytes;
	
	private WordPressBackend() {
		try (InputStream stream = WordPressFrontEnd.class.getResourceAsStream("/setup-wordpress-backend.sh")) {
			InputStreamReader in = new InputStreamReader(stream);
			String wordPressScript = CharStreams.toString(in);
			wordPressScriptBytes = wordPressScript.getBytes("UTF-8");
		} catch (IOException e) {
			log.error("Cannot read WordPress install script.", e);
			System.exit(1);
		}
	}
	
	public static WordPressBackend getInstance() {
		if (instance == null) {
			instance = new WordPressBackend();
		}
		return instance;
	}
	
	public void spawn() {
		NovaClient novaClient = null;
		try {
			novaClient = new NovaClient();
			serverId = novaClient.createServer("WordPress-Backend", wordPressScriptBytes);
		} catch (Exception ex) {
			log.error("Error while spawning WordPress-Backend", ex);
		} finally {
			if (novaClient != null) {
				novaClient.close();
			}
		}
	}
		
	public Optional<Address> getPrivateAddress() {
		if (privateAddress != null) {
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
