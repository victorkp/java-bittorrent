package com.torrent.util;

import com.torrent.peer.PeerConnections;

public class Globals {
	
	public static TorrentInfo torrentInfo;
	
	/**
	 * A 20 byte ID used by the tracker and other peers. Created in the
	 * generatePeerID() method
	 */
	public static String peerID;
	
	/**
	 * The port number used; found in PeerUtil]
	 */
	public static int tcpPort;
	
	/**
	 * A list and map of all connections to peers
	 */
	public static PeerConnections connections = new PeerConnections();

}
