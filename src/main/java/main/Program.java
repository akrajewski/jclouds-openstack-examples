package main;

import java.io.IOException;

import wordpress.WordPressFrontEnd;
import wordpress.WordPressOrchestrator;
import clients.NeutronClient;
import clients.NovaClient;



public class Program {
	public static void main(String[] args) {
		
		NovaClient novaClient = new NovaClient();
		NeutronClient neutronClient = new NeutronClient();
		
		try {
			WordPressOrchestrator orchestrator = new WordPressOrchestrator(2);
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
