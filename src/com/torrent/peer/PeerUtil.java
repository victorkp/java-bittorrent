package com.torrent.peer;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.torrent.util.Globals;
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
		System.out.println("Peer ID is " + mPeerID);
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
		for (int port = PORT_MIN; port < -PORT_MAX; port++) {
			try {
				mSocket = new ServerSocket(port);
				return port;
				
				/*
				mListenThread = new Thread(new Runnable() {

					@Override
					public void run() {
						while (mRunning) {
							try {
								final Socket s = mSocket.accept();
								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											int i;
											while ((i = s.getInputStream().read()) != -1) {
												System.out.print(i);
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}).start();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				});

				mListenThread.start();
				*/
			} catch (Exception e) {
			}
		}
		return -1;
	}

	public static void connectToPeer(PeerInfo peer) {
		PeerConnection connection = new PeerConnection(peer);
		if (connection.doHandshake() ){
			System.out.println("Made handshake!");
		} else {
			System.out.println("Handshake failed");
			connection.closeConnection();
		}
		
		connection.initDownload();
		
		//connection.doInterested();
		//connection.doNotChoking();
		//connection.doRequest(0, 0, Globals.torrentInfo.piece_length);
		
		connection.closeConnection();
	}

}