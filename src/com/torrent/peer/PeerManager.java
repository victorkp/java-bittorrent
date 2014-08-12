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
	 * How frequently the peers should be evaluated in order
	 * to determine which ones should be dropped
	 */
	private static final long MONITOR_INTERVAL = 30000;
	
	/**
	 * Maximum number of peers that can 
	 * be downloaded from simultaneously
	 */
	private static final int MAX_DOWNLOAD_PEERS = 3;
	
	/**
	 * Maximum number of peers that can 
	 * be uploaded to simultaneously
	 */
	private static final int MAX_UPLOAD_PEERS = 3;

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
	 * Whether or not to monitor the peers every 30 seconds
	 * and continue allowing them to download / upload
	 */
	private boolean mMonitorPeers = false;
	
	/**
	 * The thread that monitors the peers, stopping connections
	 * to the slowest upload and download peer, and connects to new
	 * peers
	 */
	private Thread mMonitorThread;
	
	/**
	 * The list of peers returned by the tracker
	 */
	private List<PeerInfo> mAvailablePeers;
	
	/**
	 * Peers that are currently connected to
	 */
	private List<PeerConnection> mDownloadPeers, mUploadPeers;
	
	
	public PeerManager(ServerSocket socket) {
		mSocket = socket;
	
		mDownloadPeers = new ArrayList<PeerConnection>(3);
		mUploadPeers = new ArrayList<PeerConnection>(3);
	}

	public void start() {
		// Start listening for connections on this socket
		mSocketListening = true;
		(mSocketThread = new Thread(new Runnable() {
			public void run() {
				try {
					// Set socket to never timeout
					mSocket.setSoTimeout(0);
				} catch (Exception e) { }
				while (mSocketListening) {
					try {
						System.out.println("Waiting for incoming connection");
						Socket peerSocket = mSocket.accept();
						
						System.out.println("Accepting new peer");
						
						PeerConnection peerConnection = new PeerConnection(peerSocket);
						
						if(peerConnection.getPeerInfo() != null){
							// The handshake received was good, so send our handshake
							peerConnection.sendHandshake();
							System.out.println("Accepted a connection with " + peerConnection);

							// Drop a leeching peer if needed
							if(mUploadPeers.size() >= MAX_UPLOAD_PEERS){
								PeerConnection slowest = getSlowestUploadPeer();
								if(slowest != null) {
									System.out.println("Dropping leech " + slowest);
									slowest.stop();
								}
							}

							peerConnection.start();
							mUploadPeers.add(peerConnection);
						}
					} catch (Exception e) { 
						if(!e.getMessage().contains("Socket closed")){
							e.printStackTrace(); 
						}
					}
				}
			}
		})).start();
		

		// Start asking the tracker for peers and begin downloading
		mTrackerPolling = true;
		(mTrackerThread = new Thread(new Runnable(){
			public void run() {
				// While the tracker 
				while(mTrackerPolling){
					// Get the peers and filter by IP address
					mAvailablePeers = filterPeers(TrackerUtil.getPeers());
					
					System.out.println("Available peers:");
					for(PeerInfo peer : mAvailablePeers){
						System.out.println("     " + peer);
					}
					System.out.println();
					
					// Check to make sure the tracker could be reached
					if(mAvailablePeers == null){
						// No way to contact tracker, so stop everything
						System.out.println("Error: cannot reach tracker");
						break;
					}
					
					if(mAvailablePeers.isEmpty()){
						System.out.println("No peers available");
						break;
					}
					
					
					// Wait the Tracker's request interval
					try{
						// Keep checking to make sure this thread isn't being stopped
						for(int i = 0; i < 10; i++){
							Thread.sleep((int)(TrackerUtil.getInterval() / 10f));
							if(!mTrackerPolling){
								break;
							}
						}
					} catch (InterruptedException e){ }
				}
			}
		})).start();

		mMonitorPeers = true;
		(mMonitorThread = new Thread(new Runnable(){
			public void run() {
				while(mMonitorPeers) {
					try{
						Thread.sleep(MONITOR_INTERVAL / 10);
					} catch (InterruptedException e) { }


					if(mDownloadPeers.size() >= MAX_DOWNLOAD_PEERS) {
						// There's a different peer we can try to connect to
						// Drop the slowest peer and pick a new one
						PeerConnection slowest = getSlowestDownloadPeer();
						if(slowest != null) {
							System.out.println("Dropping " + slowest.getPeerInfo());
							slowest.stop();
							mDownloadPeers.remove(slowest);
						}
					}
					
					if(mDownloadPeers.size() < MAX_DOWNLOAD_PEERS && mDownloadPeers.size() < mAvailablePeers.size()){
						for(int i = 0; i < MAX_DOWNLOAD_PEERS && mDownloadPeers.size() <= MAX_DOWNLOAD_PEERS; i++) {
							PeerInfo peer = pickRandomUnconnectedPeer(mAvailablePeers);
							
							if(peer == null){
								return;
							}
							
							System.out.println("Connecting to " + peer);
							
							// Connect to this peer
							PeerConnection peerConnection = PeerUtil.handshakeWithPeer(peer);
							
							if (peerConnection != null) {
								peerConnection.start();
								mDownloadPeers.add(peerConnection);
							}
						}
					}

					// Keep checking to make sure this thread isn't being stopped
					for(int i = 0; i < 9; i++){
						try{
							Thread.sleep(MONITOR_INTERVAL / 10);
						} catch (InterruptedException e) { }
						if(!mMonitorPeers){
							break;
						}
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
		mTrackerPolling = false;
		mMonitorPeers = false;

		System.out.println("\n _____________________________ ");
		System.out.println("|--------SHUTTING-DOWN--------|");
		System.out.println("|-----------------------------|");
		
		
		try {
			System.out.println("|-----Disconnecting peers-----|");
			System.out.println("|-----------------------------|");
			
			for(PeerConnection peer : mDownloadPeers){
				peer.stop();
			}
			
			for(PeerConnection peer : mUploadPeers){
				peer.stop();
			}

			System.out.println("|-----Closing TCP socket------|");
			System.out.println("|-----------------------------|");
			mSocketThread.join(3000);
			
			System.out.println("|--Stopping tracker requests--|");
			System.out.println("|-----------------------------|");
			mTrackerThread.join(10000);
			
			System.out.println("|-Ending performance monitor--|");
			System.out.println("|-----------------------------|");
			mMonitorThread.join(10000);
			
		} catch (InterruptedException e){
			e.printStackTrace();
		}
		
		// Tell the tracker that we're stopped
		System.out.println("|--Notifying tracker of STOP--|");
		System.out.println("|-----------------------------|");
		TrackerUtil.sendEvent(TrackerUtil.Events.STOPPED);
		
		System.out.println("|------------DONE-------------|");
		System.out.println("|-----------------------------|\n");
		
	}
	
	/**
	 * Filter peers in a list by IP address
	 * @param peerList a list of peers
	 * @return a list of peers that can be connected to, as specified by assignment
	 */
	private List<PeerInfo> filterPeers(List<PeerInfo> peerList){
		if(peerList == null){
			return null;
		}
		
		List<PeerInfo> goodPeers = new ArrayList<PeerInfo>();
		
		for(PeerInfo peer : peerList) {
			if(peer.getIP().contains("128.6.5.130") || peer.getIP().contains("128:6:5:130") || peer.getIP().contains("128.6.5.131") || peer.getIP().contains("128:6:5:131") || peer.getPeerID().contains("RU1103")){
				goodPeers.add(peer);
			}
		}

		return goodPeers;
	}

	/**
	 * @return the currently slowest PeerConnection that data is being received from
	 */
	private PeerConnection getSlowestDownloadPeer(){
		if(mDownloadPeers == null){
			return null;
		}
		
		PeerConnection slowestPeer = null;
		int slowestRate = Integer.MAX_VALUE;
		for(PeerConnection peer : mDownloadPeers) {
			if(peer.getBytesReceived() < slowestRate){
				slowestRate = peer.getBytesReceived();
				slowestPeer = peer;
			}
		}
		
		return slowestPeer;
	}
	
	/**
	 * @return the currently slowest PeerConnection that data is being received from
	 */
	private PeerConnection getSlowestUploadPeer(){
		if(mUploadPeers == null){
			return null;
		}
		
		PeerConnection slowestPeer = null;
		int slowestRate = Integer.MAX_VALUE;
		for(PeerConnection peer : mUploadPeers) {
			if(peer.getBytesSent() < slowestRate){
				slowestRate = peer.getBytesReceived();
				slowestPeer = peer;
			}
		}
		
		return slowestPeer;
	}
	
	/**
	 * @return if there is an active connection to this peer
	 */
	private boolean isConnectedTo(PeerInfo info){
		for(PeerConnection conn : mDownloadPeers){
			if(conn.getPeerInfo().getPeerID().equals(info.getPeerID())){
				return true;
			}
		}
		
		for(PeerConnection conn : mDownloadPeers){
			if(conn.getPeerInfo().getPeerID().equals(info.getPeerID())){
				return true;
			}
		}
		
		return false;
	}
	
	private PeerInfo pickRandomUnconnectedPeer(List<PeerInfo> peerList){
		if(mAvailablePeers == null || mAvailablePeers.isEmpty()){
			return null;
		}

		List<PeerInfo> notConnectedPeers = new ArrayList<PeerInfo>();
		
		for(PeerInfo info : peerList){
			if(!isConnectedTo(info)){
				notConnectedPeers.add(info);
			}
		}

		return (notConnectedPeers.isEmpty()) ? (null) : (notConnectedPeers.get((int) (Math.random() * notConnectedPeers.size())));
	}

}
