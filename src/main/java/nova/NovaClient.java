package nova;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import main.Config;
import neutron.NeutronClient;

import org.apache.log4j.Logger;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;

public class NovaClient {
	private static Logger log = Logger.getLogger(NovaClient.class);

	private NovaApi novaApi;
	private String zone;
	
	public NovaClient() {
		Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule());
		
		ContextBuilder builder = ContextBuilder.newBuilder("openstack-nova");
		
		novaApi = builder.endpoint(Config.KEYSTONE_ENDPOINT)
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
	}
	
	public void listServers() {
		ServerApi serverApi = novaApi.getServerApiForZone(zone);

		log.info("Listing servers = {" + zone);
		for (Server server : serverApi.listInDetail().concat()) {
			log.info(" " + server);
		}
		log.info("}");
	}
	
	public void listFloatingIPs() {
		FloatingIPApi api = novaApi.getFloatingIPExtensionForZone(zone).get();
		
		log.info("Listing floating IPs = {");
		api.list().forEach(ip -> log.info("\t" + ip));
		log.info("}");
	}
	
	public Optional<Address> getPrivateAddress(String serverId, String networkName) {
		ServerApi serverApi = novaApi.getServerApiForZone(zone);
		
		Server server = serverApi.get(serverId);
		
		Collection<Address> addresses = server.getAddresses().get(networkName);
		
		if (addresses == null) {
			log.warn("No addresses found for server " + serverId + " Probably trying to retrieve before server networking was completed");
			return Optional.absent();
		} else if (addresses.size() == 1) {
			return Optional.of(addresses.stream().findFirst().get());
		} else { // two addresses
			FloatingIPApi floatingIPApi = novaApi.getFloatingIPExtensionForZone(zone).get();
			
			Optional<? extends FloatingIP> publicIP = 
					floatingIPApi.list()
						.filter(floatingIP -> addresses.contains(Address.createV4(floatingIP.getIp())))
						.first();
			
			return publicIP.isPresent() ?
					Optional.of(Address.createV4(publicIP.get().getFixedIp()))
					: Optional.absent();
		}
	}
	
	public Optional<Address> getPublicAddress(String serverId, String networkName) {
		
		ServerApi serverApi = novaApi.getServerApiForZone(zone);
		
		Server server = serverApi.get(serverId);
		
		Collection<Address> addresses = server.getAddresses().get(networkName);
		
		Optional<Address> fixed = getPrivateAddress(serverId, networkName);
		
		if (!fixed.isPresent()) {
			log.warn("Trying to obtain public IP address before server networking was completed");
			return Optional.absent();
		}
		
		java.util.Optional<Address> publicIP = addresses.stream().filter(addr -> !addr.equals(fixed.get())).findFirst();
		
		return publicIP.isPresent() ?
				Optional.of(publicIP.get())
				: Optional.absent();
	}

	public void stopServers() {
		ServerApi serverApi = novaApi.getServerApiForZone(zone);

		serverApi.listInDetail()
			.concat()
			.forEach(server -> { 
				log.info("Shutting down... " + server); serverApi.delete(server.getId()); 
			});

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
	
	public String createServer(String name) {
		return createServer(name, null);
	}
	
	public String createServer(String name, byte[] userData) {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Server name should not be null or empty");
		
		//Defaults:
		String image = "Ubuntu Server 12.04";
		String flavor = "m1.small";
		String network = "demo-net";
		String keyPair = "WordPress";
		
		return createServer(name, image, flavor, network, keyPair, userData);
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
	
	public void assignFloatingIp(Server server) {
		FloatingIPApi floatingIPApi = novaApi.getFloatingIPExtensionForZone(zone).get();
		
		FluentIterable<? extends FloatingIP> floatingIPs = floatingIPApi.list(); 
		
		Optional<? extends FloatingIP> freeIP = floatingIPs
				.filter(ip -> ip.getInstanceId() == null)
				.first();
		
		FloatingIP floatingIP = freeIP.isPresent() ? freeIP.get() : floatingIPApi.allocateFromPool("ext-net");
		try {
			floatingIPApi.addToServer(floatingIP.getIp(), server.getId());
		} catch (Exception ex) {
			log.error("Error while adding floating IP to server", ex);
			novaApi.getServerApiForZone(zone).delete(server.getId());
		}
	}

	public void close() {
		try {
			Closeables.close(novaApi, true);
		} catch (IOException ex) {
			log.error("Error while closing NovaApi");
		}
	}
}
