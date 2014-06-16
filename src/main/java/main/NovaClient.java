package main;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;

public class NovaClient {
	private static Logger log = Logger.getLogger(Logger.class);

	private NovaApi novaApi;
	private String zone;

	public NovaClient() {
		try {
			Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule());
			
			ContextBuilder builder = ContextBuilder.newBuilder("openstack-nova");
			
			novaApi = builder.endpoint("http://os-ctrl:5000/v2.0/")
					.credentials(Config.TENANT + ":" + Config.USER, Config.PASSWORD)
					.modules(modules)
					.buildApi(NovaApi.class);
			
			Set<String> zones = novaApi.getConfiguredZones();
			if (zones.size() == 1) {
				zone = zones.stream().findFirst().get();
				log.info("Using zone: " + zone);
			} else {
				log.warn("Unexpected event - more than one configured Openstack zone found.");
			}

		} catch (Exception e) {
			log.error("Exception while building NovaApi", e);
		}
	}

	public void listServers() {

		ServerApi serverApi = novaApi.getServerApiForZone(zone);

		System.out.println("Servers in " + zone);

		for (Server server : serverApi.listInDetail().concat()) {
			System.out.println(" " + server);
		}

	}

	public void stopServers() {
		ServerApi serverApi = novaApi.getServerApiForZone(zone);

		for (Server server : serverApi.listInDetail().concat()) {
			log.info("Shutting down... " + server);
			serverApi.delete(server.getId());
		}
		
		log.info("Servers successfully stopped!");
	}
	
	public void listFlavors() {
		FlavorApi flavorApi = novaApi.getFlavorApiForZone(zone);
		
		log.info("Flavors = {");
		flavorApi.listInDetail().concat().forEach(f -> log.info("\t" + f));
		log.info("}");
	}

	public void createServer() {
		CreateServerOptions options = new CreateServerOptions();
		options.adminPass("demo");
		options.keyPairName("Ubuntu");
		options.networks("fdbf932c-faac-4b60-8cc6-4a114eaae8bb");
		options.availabilityZone("nova");
		options.securityGroupNames("default");

		ServerApi serverApi = novaApi.getServerApiForZone(zone);
		String imageRef = "7941b762-9992-4787-b86f-c462b52f031e"; 
																	
		ServerCreated status = serverApi.create("Ubuntu-Server-By-API", imageRef, "2", options);
		Server server = serverApi.get(status.getId());
		while (serverApi.get(status.getId()).getAddresses().size() == 0) {
			log.info("Waiting for server basic networking... ");
			try {
				TimeUnit.SECONDS.sleep(5);
			} catch (InterruptedException ex) {
				log.warn("Exception while sleeping", ex);
			}
			
		}
		
		assignFloatingIp(server);
	}
	
	public void assignFloatingIp(Server server) {
		FloatingIPApi floatingIPApi = novaApi.getFloatingIPExtensionForZone(zone).get();
		
		FloatingIP floatingIP = floatingIPApi.allocateFromPool("ext-net");
		
		try {
			floatingIPApi.addToServer(floatingIP.getIp(), server.getId());
		} catch (Exception ex) {
			novaApi.getServerApiForZone(zone).delete(server.getId());
		}
	}

	public void close() throws IOException {
		Closeables.close(novaApi, true);
	}
}
