/**
 * Victor Kaiser-Pendergrast
 * James DiPierro
 * Grayson Phillips
 */

package com.torrent;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import com.torrent.parse.TorrentFile;
import com.torrent.parse.TorrentFileParser;
import com.torrent.peer.PeerConnection;
import com.torrent.peer.PeerInfo;
import com.torrent.peer.PeerUtil;
import com.torrent.tracker.TrackerUtil;
import com.torrent.util.Globals;
import com.torrent.util.StreamUtil;
import com.torrent.util.TorrentInfo;

public class RUBTClient {

	private static File mTorrentFile;
	private static TorrentInfo mTorrentInfo;

	public static void main(String[] args) {
		if (!checkArguments(args)) {
			return;
		}

		try {
			// Parse torrent file
			mTorrentFile = new File(args[0]);
			mTorrentInfo = new TorrentInfo(StreamUtil.fileAsBytes(mTorrentFile));

			System.out.println("Torrent has " + mTorrentInfo.piece_hashes.length + " pieces of length " + mTorrentInfo.piece_length);
			System.out.println("Total file length is " + mTorrentInfo.file_length);

			// Generate and save a peer ID
			Globals.peerID = PeerUtil.getPeerID();

			// Open a TCP socket to listen on
			Globals.tcpPort = PeerUtil.openTCP();

			Globals.torrentInfo = mTorrentInfo;

			// Get the peers from the tracker
			List<PeerInfo> peerList = TrackerUtil.getPeers();
			if (peerList == null) {
				System.out.println("There was a problem contacting the tracker");
				return;
			}

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

			Globals.downloadFileOut.flush();
			Globals.downloadFileOut.close();

			TrackerUtil.sendCompleted();

			System.out.println("---DONE--DOWNLOADING---");

			// Wait for a keypress to exit
			System.out.println("Press any key to stop seeding");
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

		Globals.downloadFile = new File(args[1]);
		try {
			if (!Globals.downloadFile.createNewFile()) {
				Globals.downloadFile.delete();
				Globals.downloadFile.createNewFile();
			}

			Globals.downloadFileOut = new FileOutputStream(Globals.downloadFile);
		} catch (Exception e) {
			System.out.println("Need permission to create " + args[1] + "\nPerhaps run as su?");
			return false;
		}

		return true;
	}

}
