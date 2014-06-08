package main;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaApiMetadata;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;

public class NovaExample {

	private NovaApi novaApi;
	private Set<String> zones;

	public NovaExample() {
		
		try {
			Iterable<Module> modules = ImmutableSet
					.<Module> of(new SLF4JLoggingModule());
			String provider = "openstack-nova";
			String identity = "demo:demo";
			String credential = "demo";
			ContextBuilder builder = ContextBuilder.newBuilder(provider);
			novaApi = builder
					.endpoint("http://172.16.0.2:5000/v2.0/")
					.credentials(identity, credential).modules(modules)
					.buildApi(NovaApi.class);
			zones = novaApi.getConfiguredZones();
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			
		}
		
		
	}
	
	
	
	
	
	
	public void listServers() {
		for (String zone : zones) {
			ServerApi serverApi = novaApi.getServerApiForZone(zone);
			
			System.out.println("Servers in " + zone);
			
			for (Server server : serverApi.listInDetail().concat()) {
				System.out.println(" " + server);
			}
			
		}
	}
	
	public void stopServers() {
		for (String zone : zones) {
			ServerApi serverApi = novaApi.getServerApiForZone(zone);
			
			for (Server server : serverApi.listInDetail().concat()) {
				System.out.println("Shutting down: " + server);
				serverApi.delete(server.getId());
			}
		}
	}
	
	public void createServer() {

		
		CreateServerOptions options = new CreateServerOptions();
		options.adminPass("demo");
		options.keyPairName("Ubuntu");
		options.networks("fdbf932c-faac-4b60-8cc6-4a114eaae8bb");
		options.availabilityZone("nova");
		options.securityGroupNames("default");		
		

		
		if (zones.size() != 1) {
			System.out.println("More than one zone! Exiting.");
			return;
		}
		
		for (String zone : zones) {
			ServerApi serverApi = novaApi.getServerApiForZone(zone);
			
			String imageRef = "7941b762-9992-4787-b86f-c462b52f031e"; //Ubuntu Server 12.04
			
			
			serverApi.create("Ubuntu-Server-By-API", imageRef, "2", options);
		}
			
		
	}
	
	public void close() throws IOException {
		Closeables.close(novaApi, true);
	}

}
