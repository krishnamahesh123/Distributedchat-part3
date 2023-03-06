package PeerToPeerChat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

public class NodeInfo implements Serializable{

    // IP of the client
    String ip;
    // port of the client
    String port;
    // logical name of the client
    String name;
    
    // constructor
    public NodeInfo(String ip, String port) {
        
    	this.ip = ip;
        this.port = port;    
        
        String[] names = {"Alice", "Bob", "Charlie", "David", "Emma", "Frank", "Grace", "Henry", "Isabella", "Jack", "Katherine", "Liam", "Mia", "Noah", "Olivia", "Penelope", "Quinn", "Ryan", "Sophia", "Thomas"};
        
        this.name = names[(int)(Math.random() * names.length)];
        		
    }
    
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}	
}
