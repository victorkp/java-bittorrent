/**
 * Victor Kaiser-Pendergrast
 * James DiPierro
 * Grayson Phillips
 */

package com.torrent.peer;

import java.util.ArrayList;
import java.util.HashMap;

public class PeerManager {
	
	ArrayList<PeerConnection> mConnectionList;
	HashMap<PeerInfo, PeerConnection> mConnectionMap;
	
	public PeerManager(){
		mConnectionList = new ArrayList<PeerConnection>();
		mConnectionMap = new HashMap<PeerInfo, PeerConnection>();
	}
	
	public void addPeerConnection(PeerConnection conn){
		mConnectionList.add(conn);
		mConnectionMap.put(conn.getPeerInfo(), conn);
	}

}
