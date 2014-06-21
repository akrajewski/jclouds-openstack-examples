package main;

import java.io.IOException;

import neutron.NeutronClient;
import nova.NovaClient;
import wordpress.Frontend;
import wordpress.Orchestrator;



public class Program {
	public static void main(String[] args) {
		
		NovaClient novaClient = new NovaClient();
		NeutronClient neutronClient = new NeutronClient();
		
		try {
			Orchestrator orchestrator = new Orchestrator(2);
			orchestrator.orchestrate();
			
			//novaClient.stopServers();
			//novaClient.createServer();
			//novaClient.listFlavors();
			//novaClient.listServers();
			//novaClient.listFloatingIPs();
			//System.out.println("Private IP in demo-net: " + novaClient.getPrivateAddress("027a60d1-8e84-462d-be54-c1031854341a", "demo-net"));
			//System.out.println("Public IP in demo-net: " + novaClient.getPublicAddress("027a60d1-8e84-462d-be54-c1031854341a", "demo-net"));
			//new WordPressFrontEnd().spawn();
			//neutronClient.listNetworks();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			novaClient.close();
			neutronClient.close();
		}
	}
}
