package wordpress;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import clients.NovaClient;

import com.google.common.io.CharStreams;

public class WordPressFrontEnd {
	private static Logger log = Logger.getLogger(Logger.class);
	
	private static int count = 0;
	private static byte[] wordPressScriptBytes;

	static {
		try (InputStream stream = WordPressFrontEnd.class.getResourceAsStream("/install-wordpress.sh")) {
			InputStreamReader in = new InputStreamReader(stream);
			String wordPressScript = CharStreams.toString(in);
			wordPressScriptBytes = wordPressScript.getBytes("UTF-8");
		} catch (IOException e) {
			log.error("Cannot read WordPress install script.", e);
			System.exit(1);
		}
	}
	
	public WordPressFrontEnd() {
		
	}
	
	public void spawn() {
		NovaClient novaClient = new NovaClient();
		novaClient.createServer("WordPress-FrontEnd-" + (count++), wordPressScriptBytes);
	}
}
