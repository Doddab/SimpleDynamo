package Helpers;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

import android.util.Log;

@SuppressWarnings("serial")
public class LinkedListNode implements Serializable {

	private String portNumber;
	private String portHash;
	private LinkedListNode next;
	private LinkedListNode previous;
	private boolean alive;
	
	public LinkedListNode(String portNo, LinkedListNode n, LinkedListNode p) {
		this.portNumber = portNo;
		this.next = n;
		this.previous = p;
		this.alive = true;
		
		try {
			int avdNo = Integer.valueOf(portNo) / 2;
			this.portHash = AppData.genHash(String.valueOf(avdNo));
			Log.v(AppData.TAG, String.valueOf(avdNo) + " : " + this.portHash);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	public String getPortNumber() {
		return this.portNumber;
	}
	
	public String getPortHashedNumber() {
		return this.portHash;
	}
	
	public LinkedListNode getNextNode() {
		return this.next;
	}
	
	public void setNextNode(LinkedListNode node) {
		this.next = node;
	}

	public LinkedListNode getPreviousNode() {
		return this.previous;
	}
	
	public void setPreviousNode(LinkedListNode node) {
		this.previous = node;
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

}
