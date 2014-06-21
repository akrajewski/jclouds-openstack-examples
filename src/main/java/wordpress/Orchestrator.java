package wordpress;

import java.util.ArrayList;
import java.util.List;


public class Orchestrator {
	
	private int frontendCount;
	
	public Orchestrator(int frontendCount) {
		this.frontendCount = frontendCount;
	}
	
	public void orchestrate() {
		Backend backend = Backend.getInstance();
		backend.spawn();
		
		List<Frontend> frontEnds = new ArrayList<>();
		for (int i = 0; i < frontendCount; i++) {
			Frontend frontend = new Frontend(backend);
			frontend.spawn();
			frontEnds.add(frontend);
		}
		
		if (frontendCount > 1) {
			LoadBalancer loadBalancer = LoadBalancer.getInstance();
			loadBalancer.frontends(frontEnds);
			loadBalancer.spawn();
		}
	}
}
