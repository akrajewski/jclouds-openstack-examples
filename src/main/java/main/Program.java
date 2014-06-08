package main;

import java.io.IOException;



public class Program {
	public static void main(String[] args) {
		
		NovaExample example = new NovaExample();
		
		try {
			
			example.listServers();
			example.stopServers();
			example.createServer();
			example.listServers();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				example.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
