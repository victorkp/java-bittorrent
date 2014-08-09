/**
 * Victor Kaiser-Pendergrast
 * James DiPierro
 * Grayson Phillips
 */

package com.torrent.peer;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.torrent.tracker.TrackerUtil;

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
	
	/**
	 * Whether or not to regularly check in with tracker
	 * and get a new list of peers 
	 */
	private boolean mTrackerPolling = false;
	
	/**
	 * The thread that polls the tracker and shuffles
	 * peers as needed
	 */
	private Thread mTrackerThread;
	
	/**
	 * The list of peers returned by the tracker
	 */
	private List<PeerInfo> mAvailablePeers;
	
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
						Socket peerSocket = mSocket.accept();
						PeerConnection peerConnection = new PeerConnection(peerSocket);
						
						if(peerConnection.getPeerInfo() != null){
							// The handshake received was good, so send our handshake
							peerConnection.sendHandshake();
							
							System.out.println("Established a connection with " + peerConnection);
						}
					} catch (Exception e) {	}
				}
			}
		})).start();
		
		// Start asking the tracker for peers and begin downloading
		(mTrackerThread = new Thread(new Runnable(){
			public void run() {
				// While the tracker 
				while(mTrackerPolling){
					mAvailablePeers = TrackerUtil.getPeers();
					
					// Check to make sure the tracker could be reached
					if(mAvailablePeers == null){
						// No way to contact tracker, so stop everything
						System.out.println("Error: cannot reach tracker");
						PeerManager.this.stop();
						break;
					}
					
					
				}
			}
		})).start();

	}
	
	public void stop() {
		// mSocketThread will pick up this message within
		// 10 seconds (because mSocket only blocks for 10s)
		// and stop
		mSocketListening = false;
		
		try {
			// Wait at most 10 seconds for each thread to stop
			mSocketThread.join(10000);
			mTrackerThread.join(10000);
		} catch (InterruptedException e){
			e.printStackTrace();
		}
	}

	private void addPeerConnection(PeerConnection conn) {
		mConnectionList.add(conn);
		mConnectionMap.put(conn.getPeerInfo(), conn);
	}

}
