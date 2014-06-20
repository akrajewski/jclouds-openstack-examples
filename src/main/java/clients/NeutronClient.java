package clients;

import com.google.common.base.Optional;

import java.io.IOException;
import java.util.Set;

import main.Config;

import org.apache.log4j.Logger;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.neutron.v2_0.NeutronApi;
import org.jclouds.openstack.neutron.v2_0.domain.Network;
import org.jclouds.openstack.neutron.v2_0.features.NetworkApi;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;

public class NeutronClient {
	private static Logger log = Logger.getLogger(Logger.class);

	private NeutronApi neutronApi;
	private String zone;
	
	public NeutronClient() {
		try {
			Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule());

			ContextBuilder builder = ContextBuilder.newBuilder("openstack-neutron");

			neutronApi = builder
					.endpoint("http://os-ctrl:5000/v2.0/")
					.credentials(Config.TENANT + ":" + Config.USER, Config.PASSWORD).modules(modules)
					.buildApi(NeutronApi.class);

			Set<String> zones = neutronApi.getConfiguredZones();
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
	
	public Optional<String> getNetworkIdByName(String networkName) {
		NetworkApi networkApi = neutronApi.getNetworkApiForZone(zone);
		
		Optional<? extends Network> match = 
				networkApi.listInDetail()
				.concat()
				.firstMatch(net -> net.getName().equals(networkName));
		
		return match.isPresent() ? Optional.<String> of (match.get().getId()) : Optional.<String> absent();
	}
	
	public void listNetworks() {
		NetworkApi networkApi = neutronApi.getNetworkApiForZone(zone);
		log.info("Listing networks = {");
		for (Network net : networkApi.listInDetail().concat()) {
			log.info("\t" + net);
		}
		log.info("}");
	}

	public void close() {
		try {
			Closeables.close(neutronApi, true);
		} catch (IOException ex) {
			log.error("Error while closing neutronApi", ex);
		}
	}
}
