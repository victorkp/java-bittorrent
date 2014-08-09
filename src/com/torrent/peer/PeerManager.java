/**
 * Victor Kaiser-Pendergrast
 * James DiPierro
 * Grayson Phillips
 */

package com.torrent.peer;

import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Runnable;

public class PeerManager {

	/**
	 * The Socket opened in PeerUtil to listen
	 * for incoming peer connections
	 */
	private ServerSocket mSocket;
	
	/**
	 * The thread that listens to mSocket
	 */
	private Thread mSocketThread;
	
	/**
	 * Whether or not to listen for and accept connections
	 * on the TCP Socket
	 */
	private boolean mSocketListening = false;

	private ArrayList<PeerConnection> mConnectionList;
	private HashMap<PeerInfo, PeerConnection> mConnectionMap;

	public PeerManager(ServerSocket socket) {
		mSocket = socket;
		
		// Only let listening to the socket block for 10 seconds,
		try {
			mSocket.setSoTimeout(10000);
		} catch (SocketException e) { }

		mConnectionList = new ArrayList<PeerConnection>();
		mConnectionMap = new HashMap<PeerInfo, PeerConnection>();
	}

	public void start() {
		// Start listening for connections on this socket
		mSocketListening = true;
		(mSocketThread = new Thread(new Runnable() {
			public void run() {
				while (mSocketListening) {
					try {
						mSocket.accept();

					} catch (Exception e) {	}
				}
			}
		})).start();

	}
	
	public void stop() {
		// mSocketThread will pick up this message within
		// 10 seconds (because mSocket only blocks for 10s)
		mSocketListening = false;
	}

	private void addPeerConnection(PeerConnection conn) {
		mConnectionList.add(conn);
		mConnectionMap.put(conn.getPeerInfo(), conn);
	}

}
