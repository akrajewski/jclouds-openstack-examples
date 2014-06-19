package clients;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import main.Config;

import org.apache.log4j.Logger;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
				log.warn("Unexpected event - zero or more than one configured Openstack zone found.");
			}

		} catch (Exception e) {
			log.error("Exception while building NovaApi", e);
		}
	}

	public void listServers() {
		ServerApi serverApi = novaApi.getServerApiForZone(zone);

		System.out.println("Listing servers =" + zone);

		for (Server server : serverApi.listInDetail().concat()) {
			System.out.println(" " + server);
			
		}

	}
	
	public void listFloatingIPs() {
		FloatingIPApi api = novaApi.getFloatingIPExtensionForZone(zone).get();
		
		log.info("Listing floating ips = {");
		for (FloatingIP ip : api.list()) {
			log.info("\t" + ip);
		}
		log.info("}");
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
	
	public Optional<? extends Image> getImageByName(String imageName) {
		ImageApi imageApi = novaApi.getImageApiForZone(zone);
		
		return imageApi.listInDetail()
			.concat()
			.firstMatch(img -> img.getName().equals(imageName));
	}
	
	public Optional<? extends Flavor> getFlavorByName(String flavorName) {
		FlavorApi flavorApi = novaApi.getFlavorApiForZone(zone);

		return flavorApi.listInDetail()
				.concat()
				.firstMatch(flv -> flv.getName().equals(flavorName));
	}
	
	public void createServer(String name) {
		createServer(name, null);
	}
	
	public void createServer(String name, byte[] userData) {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Server name should not be null or empty");
		
		String image = "Ubuntu Server 12.04";
		String flavor = "m1.small";
		String network = "demo-net";
		String keyPair = "WordPress";
		
		createServer(name, image, flavor, network, keyPair, userData);
	}

	public String createServer(String serverName, String imageName, String flavorName, String networkName, String keyPairName, byte[] userData) {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(serverName), "serverName should not be null nor empty");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(imageName), "imageName should not be null nor empty");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(flavorName), "flavorName should not be null nor empty");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkName), "networkName should not be null nor empty");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(keyPairName), "keyPairName should not be null nor empty");
		
		CreateServerOptions options = new CreateServerOptions();
		options.keyPairName(keyPairName);
		options.userData(userData);
		
		Optional<String> network = new NeutronClient().getNetworkIdByName(networkName);
		if (!network.isPresent()) {
			log.error("The requested network " + networkName + " was not found. Aborting");
			throw new IllegalArgumentException("networkName");
		}
		options.networks(network.get());
		
		Optional<? extends Flavor> flavor = getFlavorByName(flavorName);
		log.info("Flavor: " + flavor);
		if (!flavor.isPresent()) {
			log.error("The requested flavor " + flavorName + " was not found. Aborting.");
			throw new IllegalArgumentException("flavorName");
		}
		String flavorRef = flavor.get().getId();

		Optional<? extends Image> image = getImageByName(imageName);
		if (!image.isPresent()) {
			log.error("The requested image " + imageName + " was not found. Aborting.");
			throw new IllegalArgumentException("imageName");
		}
		String imageRef = image.get().getId();
		
		ServerApi serverApi = novaApi.getServerApiForZone(zone);
		ServerCreated status = serverApi.create(serverName, imageRef, flavorRef, options);
		Server server = serverApi.get(status.getId());
		
		while (serverApi.get(status.getId()).getAddresses().size() == 0) {
			log.info("Waiting for server networking... ");
			try {
				TimeUnit.SECONDS.sleep(5);
			} catch (InterruptedException ex) {
				log.warn("Exception while sleeping", ex);
			}
		}
		
		assignFloatingIp(server);
		
		return server.getId();
	}
	
	public void getFixedIpForServer(String serverId) {
		ServerApi serverApi = novaApi.getServerApiForZone(zone);
	}
	
	public void assignFloatingIp(Server server) {
		FloatingIPApi floatingIPApi = novaApi.getFloatingIPExtensionForZone(zone).get();
		FloatingIP floatingIP = floatingIPApi.allocateFromPool("ext-net");
		try {
			floatingIPApi.addToServer(floatingIP.getIp(), server.getId());
		} catch (Exception ex) {
			log.error("Error while adding floating IP to server", ex);
			novaApi.getServerApiForZone(zone).delete(server.getId());
		}
	}

	public void close() throws IOException {
		Closeables.close(novaApi, true);
	}
}
