package wordpress;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;
import org.jclouds.openstack.nova.v2_0.domain.Address;

import clients.NovaClient;

import com.google.common.base.Optional;
import com.google.common.io.CharStreams;

public class WordPressFrontEnd {
	private static Logger log = Logger.getLogger(Logger.class);
	
	private static int count = 0;
	private static String wordPressScript;

	private byte[] wordPressScriptBytes;
	
	static {
		try (InputStream stream = WordPressFrontEnd.class.getResourceAsStream("/setup-wordpress-frontend.sh")) {
			InputStreamReader in = new InputStreamReader(stream);
			wordPressScript = CharStreams.toString(in);
		} catch (IOException e) {
			log.error("Cannot read WordPress install script.", e);
			System.exit(1);
		}
	}
	
	public WordPressFrontEnd(WordPressBackend backend) {
		Optional<Address> backendIP = backend.getPrivateAddress();
		
		if (!backendIP.isPresent()) {
			log.error("Unable to get WordPress backend private ip address");
			wordPressScriptBytes = null;
		}
		try {
			wordPressScriptBytes = wordPressScript.replace("{backend_ip_address}", backendIP.get().getAddr()).getBytes("UTF-8");
		} catch (UnsupportedEncodingException ex) {
			log.error("Error while encoding WordPress frontend setup script", ex);
			wordPressScriptBytes = null;
		}
	}
	
	public void spawn() {
		if (wordPressScriptBytes == null) {
			log.error("Unable to spawn WordPress frontend. Script not properly set.");
			return;
		}
		
		NovaClient novaClient = null;
		try {
			novaClient = new NovaClient();
			novaClient.createServer("WordPress-FrontEnd-" + (count++), wordPressScriptBytes);
		} catch (Exception ex) {
			log.error("Error while spawning WordPress frontend", ex);
		} finally {
			if (novaClient != null) {
				novaClient.close();
			}
		}
	}
}
