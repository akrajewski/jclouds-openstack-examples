package main;

import java.io.IOException;

import wordpress.WordPressFrontEnd;
import clients.NeutronClient;
import clients.NovaClient;



public class Program {
	public static void main(String[] args) {
		
		NovaClient novaClient = new NovaClient();
		NeutronClient neutronClient = new NeutronClient();
		
		try {
			//novaClient.stopServers();
			//novaClient.createServer();
			novaClient.listFlavors();
			new WordPressFrontEnd().spawn();
			//neutronClient.listNetworks();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				novaClient.close();
				neutronClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
