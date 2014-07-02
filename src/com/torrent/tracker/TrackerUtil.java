package com.torrent.tracker;

import java.nio.ByteBuffer;
import java.util.Random;

import com.torrent.util.TorrentInfo;

public class TrackerUtil {

	private static byte[] mPeerID;

	public static boolean getPeers(TorrentInfo torrentInfo) {
		generatePeerID();

		ByteBuffer infoHash = torrentInfo.info_hash;

		return false;
	}

	/**
	 * Generates a random 20 byte peer ID
	 * for sue with the torrent tracker
	 */
	private static void generatePeerID() {
		if (mPeerID == null) {
			mPeerID = new byte[20];
			new Random().nextBytes(mPeerID);
		}
	}

}
