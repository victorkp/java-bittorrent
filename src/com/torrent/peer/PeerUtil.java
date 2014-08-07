/**
 * Victor Kaiser-Pendergrast
 * James DiPierro
 * Grayson Phillips
 */

package com.torrent.peer;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.torrent.util.TorrentInfo;

public class PeerUtil {
	/**
	 * The lowest port that peers typically have open
	 */
	private static final int PORT_MIN = 6881;

	/**
	 * The highest port that peers typically have open
	 */
	private static final int PORT_MAX = 6889;

	private static ServerSocket mSocket;

	private static TorrentInfo mTorrentInfo;
	private static String mPeerID;

	private static Thread mListenThread;

	private static boolean mRunning = true;

	/**
	 * Generates a random 20 byte peer ID for sue with the torrent tracker
	 */
	private static void generatePeerID() {
		if (mPeerID == null) {
			mPeerID = "";
			for (int i = 0; i < 20; i++) {
				mPeerID = mPeerID + (int) (Math.random() * 10);
			}
		}
	}

	/**
	 * Get the peer ID to be used
	 */
	public static String getPeerID() {
		if (mPeerID == null) {
			generatePeerID();
		}

		return mPeerID;
	}

	/**
	 * Open a TCP socket
	 * 
	 * @return the port number of the socket, -1 if failed
	 */
	public static int openTCP() {
		for (int port = PORT_MIN; port < PORT_MAX; port++) {
			try {
				mSocket = new ServerSocket(port);
				return port;

			} catch (Exception e) {
			}
		}
		return -1;
	}

	/**
	 * Close the TCP socket that we were
	 * listening to
	 */
	public static void closeTCP() {
		try {
			mSocket.close();
		} catch (Exception e){
		}
	}

	/**
	 * Connect to a peer, and make a handshake
	 * @param peer
	 */
	public static PeerConnection handshakeWithPeer(PeerInfo peer) {
		PeerConnection connection = new PeerConnection(peer);
		if (connection.doHandshake() ){
			return connection;
		} else {
			System.out.println("Handshake with peer failed, cannot continue");
			connection.closeConnection();
			return null;
		}
	}

}
