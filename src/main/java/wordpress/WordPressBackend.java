package wordpress;

import org.apache.log4j.Logger;

public class WordPressBackend {
	private static Logger log = Logger.getLogger(Logger.class);
	
	private static WordPressBackend instance;
	
	private WordPressBackend() {
		
	}
	
	public static WordPressBackend getInstance() {
		if (instance == null) {
			instance = new WordPressBackend();
		}
		return instance;
	}
	
	
}
