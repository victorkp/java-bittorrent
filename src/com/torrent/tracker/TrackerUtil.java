package com.torrent.tracker;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.ByteBuffer;

import com.torrent.util.HexStringConverter;
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
	 * A 20 byte ID used by the tracker and other peers. Created in the
	 * generatePeerID() method
	 */
	private static String mPeerID;
	
	private static String mAnnounceURL;

	private static Thread mTcpThread;
	private static ServerSocket mTcpSocket;

	public static boolean getPeers(TorrentInfo torrentInfo) {
		generatePeerID();

		try {
			ByteBuffer infoHash = torrentInfo.info_hash;
			mAnnounceURL = torrentInfo.announce_url.toString();

			byte[] hash = new byte[infoHash.remaining()];
			infoHash.get(hash, 0, infoHash.remaining());
			
			String connectURL = mAnnounceURL + "?" + 
								Keys.INFO_HASH + "=" + HexStringConverter.toHexString(infoHash.array()) + "&" +
								Keys.PEER_ID + "=" + HexStringConverter.toHexString(mPeerID.getBytes()) + "&" +
								Keys.PORT + "=" + PORT_MIN + "&" +
								Keys.DOWNLOADED + "=0&" +
								Keys.UPLOADED + "=0&" +
								Keys.LEFT + "=" + torrentInfo.file_length + "&" +
								Keys.EVENT + "=" + Keys.Events.STARTED;
			
			
			URL trackerURL = new URL(connectURL);

			System.out.println("Connecting to tracker at \"" + trackerURL + "\"");

			HttpURLConnection getRequest = (HttpURLConnection) trackerURL.openConnection();
			getRequest.setRequestMethod("GET");
			
			getRequest.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
			getRequest.setRequestProperty("Accept", "*/*");

			getRequest.connect();
			
			int status = getRequest.getResponseCode();

			System.out.println("Response code: " + status);

			if (status != 200) {
				InputStream errorStream = getRequest.getErrorStream();
				System.out.println(streamToString(errorStream));
				if(errorStream != null) {
					errorStream.close();
				}
			} else {
				InputStream getResponse = getRequest.getInputStream();
				System.out.println(streamToString(getResponse));
				getResponse.close();
			}

			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private static void openTcpPort(final int port){
		mTcpThread = new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					mTcpSocket = new ServerSocket(port);
					while(true){
						mTcpSocket.accept();
						System.out.println("Accepted on TCP Socket");
					}
				} catch (Exception e){ }
			}
		});
	}

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

	private static String streamToString(InputStream stream) throws IOException {
		String responseString = "";
		if (stream != null) {
			int next;
			while ((next = stream.read()) != -1) {
				responseString = responseString + ((char) next);
			}
		}

		return responseString;
	}

}
