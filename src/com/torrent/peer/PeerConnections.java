package com.torrent.peer;

import java.util.ArrayList;
import java.util.HashMap;

public class PeerConnections {
	
	ArrayList<PeerConnection> mConnectionList;
	HashMap<PeerInfo, PeerConnection> mConnectionMap;
	
	public PeerConnections(){
		mConnectionList = new ArrayList<PeerConnection>();
		mConnectionMap = new HashMap<PeerInfo, PeerConnection>();
	}
	
	public void addPeerConnection(PeerConnection conn){
		mConnectionList.add(conn);
		mConnectionMap.put(conn.getPeerInfo(), conn);
	}

}
