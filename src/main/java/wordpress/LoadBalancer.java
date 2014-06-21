package wordpress;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;

import nova.VirtualServer;

import org.apache.log4j.Logger;
import org.jclouds.openstack.nova.v2_0.domain.Address;


public class LoadBalancer extends VirtualServer {
	private static Logger log = Logger.getLogger(Logger.class);
	
	private static LoadBalancer instance;
	
	private String script;
	
	private LoadBalancer() {
		try {
			script = new ScriptReader().read("setup-wordpress-backend.sh"); 
			super.setInitScript(script);
		} catch (Exception ex) {
			log.error("Error while preparing WordPress load balancer init script. No auto setup will be performed.");
		}
	}
	
	public static LoadBalancer getInstance() {
		if (instance == null) {
			instance = new LoadBalancer();
		}
		return instance;
	}
	
	public void frontends(List<Frontend> frontends) {
		List<Address> frontendAddresses = frontends.stream()
				.map(frontend -> frontend.getPrivateAddress())
				.filter(addr -> addr.isPresent())
				.map(addr -> addr.get())
				.collect(Collectors.toList());
			
		try {
			super.setInitScript(injectBalancerMembers(script, frontendAddresses));
		} catch (UnsupportedEncodingException ex) {
			log.error("Error while encoding WordPress load balancer init script. Skipping and no auto setup will be performed.");
		}
	}
	
	private static String injectBalancerMembers(String script, List<Address> frontEndAddresses) {
		StringBuilder builder = new StringBuilder();

		for (Address addr : frontEndAddresses) {
			builder.append("\t" + "BalancerMember http://" + addr.getAddr() + "\n"); 
		}
		
		return script.replace("{balancer_members}", builder.toString());
	}
	
	public void spawn() {
		super.spawn("WordPress-LoadBalancer");
	}
}
