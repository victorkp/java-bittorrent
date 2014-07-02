package com.torrent.tracker;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Random;

import com.torrent.util.TorrentInfo;

public class TrackerUtil {

	/**
	 * The lowest port that peers typically have open
	 */
	private static final int PORT_MIN = 6881;

	/**
	 * The highest port that peers typically have open
	 */
	private static final int PORT_MAX = 6889;

	/**
	 * The keys used as HTTP parameters in requests to the tracker
	 */
	private static class Keys {
		public static final String PEER_ID = "peer_id";
		public static final String INFO_HASH = "info_hash";
		public static final String IP = "ip";
		public static final String PORT = "port";
		public static final String UPLOADED = "uploaded";
		public static final String DOWNLOADED = "downloaded";
		public static final String LEFT = "left";
		public static final String EVENT = "event";

		/**
		 * Possible values for the EVENT parameter key
		 */
		public static class Events {
			public static final String STARTED = "started";
			public static final String COMPLETED = "completed";
			public static final String STOPPED = "stopped";
		}
	}

	/**
	 * A 20 byte ID used by the tracker and other peers.
	 * Created in the generatePeerID() method
	 */
	private static byte[] mPeerID;

	public static boolean getPeers(TorrentInfo torrentInfo) {
		generatePeerID();

		try {
			ByteBuffer infoHash = torrentInfo.info_hash;
			URL trackerURL = torrentInfo.announce_url;

			HttpURLConnection getRequest = (HttpURLConnection) trackerURL.openConnection();
			getRequest.setDoInput(true);
			getRequest.setRequestMethod("GET");
			
			getRequest.addRequestProperty(Keys.PEER_ID, new String(mPeerID));
			getRequest.addRequestProperty(Keys.INFO_HASH, new String(infoHash.array()));
			getRequest.addRequestProperty(Keys.PORT, "" + PORT_MIN);
			getRequest.addRequestProperty(Keys.EVENT, Keys.Events.STARTED);
			getRequest.addRequestProperty(Keys.UPLOADED, "0");
			getRequest.addRequestProperty(Keys.DOWNLOADED, "0");
			
			InputStream getResponse = getRequest.getInputStream();
			
			getRequest.connect();
			
			int next;
			while((next = getResponse.read()) != -1){
				System.out.print((char) next);
			}
			
			getResponse.close();
			
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Generates a random 20 byte peer ID for sue with the torrent tracker
	 */
	private static void generatePeerID() {
		if (mPeerID == null) {
			mPeerID = new byte[20];
			new Random().nextBytes(mPeerID);
		}
	}

}
