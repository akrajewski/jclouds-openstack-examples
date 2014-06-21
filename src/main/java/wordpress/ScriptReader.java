package wordpress;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.google.common.io.CharStreams;

public class ScriptReader {
	private static Logger log = Logger.getLogger(ScriptReader.class);
	
	public String read(String filename) throws IOException {
		InputStream stream = null;
		try {
			stream = getClass().getResourceAsStream("/" + filename);
			InputStreamReader in = new InputStreamReader(stream);
			return CharStreams.toString(in);
		} catch (IOException ex) {
			log.error("Cannot read script.", ex);
			throw ex;
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException ex) {
					log.error("Error while closing stream", ex);
				}
			}
		}
	}
}
