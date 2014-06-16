package main;

import java.io.IOException;



public class Program {
	public static void main(String[] args) {
		
		NovaClient novaClient = new NovaClient();
		
		try {
			novaClient.createServer();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				novaClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
