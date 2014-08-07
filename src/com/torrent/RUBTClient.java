/**
 * Victor Kaiser-Pendergrast
 * James DiPierro
 * Grayson Phillips
 */

package com.torrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.torrent.file.DownloadFile;
import com.torrent.file.FileManager;
import com.torrent.peer.PeerConnection;
import com.torrent.peer.PeerInfo;
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
	 * The TCP port that we will listen on for
	 * connections by peers
	 */
	private static int mTcpPort;

	public static void main(String[] args) {
		if (!checkArguments(args)) {
			return;
		}

		try {
			// Parse torrent file
			mTorrentFile = new File(args[0]);
			mTorrentInfo = new TorrentInfo(StreamUtil.fileAsBytes(mTorrentFile));

			System.out.println("Torrent has " + mTorrentInfo.piece_hashes.length + " pieces of length " + mTorrentInfo.piece_length);
			System.out.println("Total bytes to download: " + mTorrentInfo.file_length);

			FileManager.setPieceLength(mTorrentInfo.piece_length);

			// For now, only handle cases with one file
			List<DownloadFile> fileList = new ArrayList<DownloadFile>();
			fileList.add(new DownloadFile(mTorrentInfo.file_name, mTorrentInfo.file_length));
			
			// Setup the FileManager that will keep track of pieces and handle writes to disk
			mFileManager = new FileManager(args[1], fileList, mTorrentInfo.piece_hashes.length);

			// Generate and save a peer ID
			mPeerID = PeerUtil.getPeerID();

			// Open a TCP socket to listen on
			mTcpPort = PeerUtil.openTCP();

			TrackerUtil.setParams(mTorrentInfo.announce_url.toString(), mTorrentInfo.info_hash, mTorrentInfo.file_length, mPeerID, mTcpPort);
			PeerConnection.setParams(mTorrentInfo.announce_url.toString(), mTorrentInfo.info_hash, mTorrentInfo.file_length, mTorrentInfo.piece_hashes, mTorrentInfo.piece_length, mPeerID, mTcpPort);
			PeerConnection.setFileManager(mFileManager);
			PeerMessage.setParams(mTorrentInfo.info_hash, mPeerID);

			// Get the peers from the tracker
			List<PeerInfo> peerList = TrackerUtil.getPeers();
			if (peerList == null) {
				System.out.println("There was a problem contacting the tracker");
				return;
			}

			if(mTorrentFile.exists() && mTorrentFile.length() == mTorrentInfo.file_length) {
				// Already done downloading the file
				System.out.println("File(s) already downloaded");
			} else {

				PeerConnection peerConnection = null;

				for (PeerInfo peer : peerList) {
					// Iterate through peers, only use the one
					// that has an ID starting with RU1103
					if (peer.getPeerID().contains("RU1103")) {
						peerConnection = PeerUtil.handshakeWithPeer(peer);
						break;
					}
				}

				// If we have made a handshake with a peer...
				if (peerConnection != null) {
					// Try to get unchocked by the other peer
					if (peerConnection.indicateInterest()) {
						// If we get unchocked, start downloading
						peerConnection.doDownload();
					}
					peerConnection.closeConnection();
				}

				TrackerUtil.sendCompleted();

				System.out.println("---DONE--DOWNLOADING---");
			}

			// Wait for a keypress to exit
			System.out.println("Press <ENTER> to stop seeding");
			System.in.read();

			PeerUtil.closeTCP();

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
