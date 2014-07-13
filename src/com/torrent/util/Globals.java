package com.torrent.util;

import java.io.File;
import java.io.FileOutputStream;

import com.torrent.peer.PeerConnections;

public class Globals {
	
	/**
	 * The TorrentInfo object containing information
	 * retrieved from the .torrent file
	 */
	public static TorrentInfo torrentInfo;

	/**
	 * The file that will house the downloaded
	 * contents
	 */
	public static File downloadFile;
	
	/**
	 * The file outputstream to Globals.downloadFile
	 */
	public static FileOutputStream downloadFileOut;
	
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
