package com.torrent.peer;

public class PeerInfo {
	
	private String mIP;
	private int mPort;
	private String mPeerID;
	
	public PeerInfo(){
		
	}

	public PeerInfo(String ip, int port, String peerId){
		mIP = ip;
		mPort = port;
		mPeerID = peerId;
	}
	
	public String getIP() {
		return mIP;
	}

	public void setIP(String mIP) {
		this.mIP = mIP;
	}

	public String getPeerID() {
		return mPeerID;
	}

	public void setPeerID(String mPeerID) {
		this.mPeerID = mPeerID;
	}

	public int getPort() {
		return mPort;
	}

	public void setPort(int mPort) {
		this.mPort = mPort;
	}
	
	@Override
	public String toString(){
		return mPeerID + " @ " + mIP + ":" + mPort;
	}
	
}
