package wordpress;


public class WordPressOrchestrator {
	
	private int frontendCount;
	
	public WordPressOrchestrator(int frontendCount) {
		this.frontendCount = frontendCount;
	}
	
	public void orchestrate() {
		WordPressBackend backend = WordPressBackend.getInstance();
		backend.spawn();
		
		for (int i = 0; i < frontendCount; i++) {
			WordPressFrontEnd frontend = new WordPressFrontEnd(backend);
			frontend.spawn();
		}
	}
}
