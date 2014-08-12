/**
 * Victor Kaiser-Pendergrast
 * James DiPierro
 * Grayson Phillips
 */

package com.torrent;

import java.io.File;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import com.torrent.file.DownloadFile;
import com.torrent.file.FileManager;
import com.torrent.peer.PeerConnection;
import com.torrent.peer.PeerInfo;
import com.torrent.peer.PeerManager;
import com.torrent.peer.PeerMessage;
import com.torrent.peer.PeerUtil;
import com.torrent.tracker.TrackerUtil;
import com.torrent.util.StreamUtil;
import com.torrent.util.TorrentInfo;

public class RUBTClient {

	private static File mTorrentFile;

	/**
	 * Information parsed from Torrent file
	 */
	private static TorrentInfo mTorrentInfo;

	/**
	 * Keeps track of pieces, their status, and writes
	 * everything to disk
	 */
	private static FileManager mFileManager;

	/**
	 * A randomly generated Peer ID
	 */
	private static String mPeerID;

	/**
	 * The TCP socket that we will listen on for
	 * connections by peers
	 */
	private static ServerSocket mTcpSocket;
	
	/**
	 * The TCP port (corresponding to mTcpSocket)
	 * that we will listen on for
	 * connections by peers
	 */
	private static int mTcpPort;
	
	/**
	 * Handles the retrieval, selection,
	 * starting, and stopping of peers
	 */
	private static PeerManager mPeerManager;

	public static void main(String[] args) {
		if (!checkArguments(args)) {
			return;
		}

		try {
			// Parse torrent file
			mTorrentFile = new File(args[0]);
			mTorrentInfo = new TorrentInfo(StreamUtil.fileAsBytes(mTorrentFile));
			
			System.out.println("\n _____________________________ ");
			System.out.println("|----------STARTING-----------|");
			System.out.println("|-----------------------------|\n");

			FileManager.setPieceLength(mTorrentInfo.piece_length);

			// For now, only handle torrents with one file and no folders
			List<DownloadFile> fileList = new ArrayList<DownloadFile>();
			fileList.add(new DownloadFile(mTorrentInfo.file_name, mTorrentInfo.file_length));
			
			// Setup the FileManager that will keep track of pieces and handle writes to disk
			mFileManager = new FileManager(args[1], fileList, mTorrentInfo.piece_hashes.length);

			// Generate and save a peer ID
			mPeerID = PeerUtil.getPeerID();

			// Open a TCP socket to listen on
			mTcpSocket = PeerUtil.openTCP();
			mTcpPort = mTcpSocket.getLocalPort();

			// Setup links among all the components that need to communicate
			TrackerUtil.setParams(mTorrentInfo.announce_url.toString(), mTorrentInfo.info_hash, mTorrentInfo.file_length, mPeerID, mTcpPort, mFileManager);
			PeerConnection.setParams(mTorrentInfo.announce_url.toString(), mTorrentInfo.info_hash, mTorrentInfo.file_length, mTorrentInfo.piece_hashes, mTorrentInfo.piece_length, mPeerID, mTcpPort);
			PeerConnection.setFileManager(mFileManager);
			PeerMessage.setParams(mTorrentInfo.info_hash, mPeerID);
			
			// Setup the PeerManager that will handle which peers to use
			mPeerManager = new PeerManager(mTcpSocket);
			
			// Start the PeerManager - which will handle all download related tasks from here
			mPeerManager.start();
			
			// If the file was already downloaded, then send the Tracker a COMPLETED
			TrackerUtil.sendEvent(TrackerUtil.Events.COMPLETED);
			
			System.out.println(" _____________________________ ");
			System.out.println("|------------INFO-------------|");
			System.out.println("|--Press <ENTER> at any time--|");
			System.out.println("|-----to exit this client-----|");
			System.out.println("|-----------------------------|\n");
			
			// Wait for a keypress to stop everything
			System.in.read();
			
			// Stop the PeerManager's threads
			mPeerManager.stop();

			// Tell the tracker we're stopped
			TrackerUtil.sendEvent(TrackerUtil.Events.STOPPED);

			// Close socket that was open for incoming peers
			PeerUtil.closeTCP();
			
			// Print a warning if file not fully downloaded
			if(!mFileManager.arePiecesDownloaded()) {
				System.out.println("\n _____________________________ ");
				System.out.println("|------------NOTE-------------|");
				System.out.println("|--File not fully downloaded--|");
				System.out.println("|-----------------------------|\n");
			}

		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

	}

	/**
	 * Check if the arguments are the correct format
	 * 
	 * @param args
	 * @return true if the arguments seem okay, false otherwise
	 */
	private static boolean checkArguments(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: RUBTClient <torrent file> <file to save into>");
			return false;
		}

		if (!args[0].endsWith(".torrent")) {
			System.out.println("The first argument should be a *.torrent file");
			return false;
		}

		if (!new File(args[0]).exists()) {
			System.out.println("The torrent file specified does not exist");
			return false;
		}

		return true;
	}

}
