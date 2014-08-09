/**
 * Victor Kaiser-Pendergrast
 * James DiPierro
 * Grayson Phillips
 */

package com.torrent.tracker;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.torrent.file.FileManager;
import com.torrent.peer.PeerInfo;
import com.torrent.util.Bencoder2;
import com.torrent.util.HexStringConverter;
import com.torrent.util.StreamUtil;

public class TrackerUtil {

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
	}
	
	/**
	 * Possible values for the EVENT parameter key
	 */
	public static class Events {
		public static final String STARTED = "started";
		public static final String COMPLETED = "completed";
		public static final String STOPPED = "stopped";
	}

	/**
	 * The constant keys that are used in the byte encoded response
	 */
	private static class TrackerConstants {
		public static final ByteBuffer COMPLETE = ByteBuffer.wrap("complete".getBytes());
		public static final ByteBuffer FAILURE_REASON = ByteBuffer.wrap("failure reason".getBytes());
		public static final ByteBuffer INTERVAL = ByteBuffer.wrap("interval".getBytes());
		public static final ByteBuffer MIN_INTERVAL = ByteBuffer.wrap("min interval".getBytes());
		public static final ByteBuffer PEERS = ByteBuffer.wrap("peers".getBytes());

		public static class Response {
			public static final String PORT = "port";
			public static final String IP = "ip";
			public static final String PEER_ID = "peer id";
		}
	}

	private static String mAnnounceURL;
	private static ByteBuffer mInfoHash;
	private static int mFileLength;

	private static String mPeerID;
	private static int mTcpPort;
	
	private static FileManager mFileManager;

	/**
	 * The interval that the tracker wants
	 * between regular requests to the tracker.
	 * Defaults to 60 seconds.
	 */
	private static int mInterval = 60000;
	
	private static boolean mFirstStart = true;

	public static void setParams(String announceURL, ByteBuffer infoHash, int fileLength, String peerID, int tcpPort, FileManager fileManager) {
		mAnnounceURL = announceURL;
		mInfoHash = infoHash;
		mFileLength = fileLength;
		mPeerID = peerID;
		mTcpPort = tcpPort;
		mFileManager = fileManager;
	}

	/**
	 * Get a list of peers from the tracker.
	 * Sends a STARTED event if this is the first
	 * request to the tracker
	 */
	public static List<PeerInfo> getPeers() {
		try {

			// Add all the URL params needed (info hash, our peer id, the port
			// we'll listen on)
			// Also say we downloaded/uploaded nothing and that the current
			// event is STARTED
			String connectURL = mAnnounceURL + "?" + Keys.INFO_HASH + "=" + HexStringConverter.toHexString(mInfoHash.array()) + "&" + Keys.PEER_ID + "="
					+ HexStringConverter.toHexString(mPeerID.getBytes()) + "&" + Keys.PORT + "=" + mTcpPort + "&" + Keys.DOWNLOADED + "=" + mFileManager.getDownloadedBytes() + "&"
					+ Keys.UPLOADED + "=" + mFileManager.getUploadedBytes() +"&" + Keys.LEFT + "="
					+ (mFileLength - mFileManager.getDownloadedBytes()) + ((mFirstStart) ? ("&" + Keys.EVENT + "=" + Events.STARTED) : (""));

			URL trackerURL = new URL(connectURL);

			HttpURLConnection getRequest = (HttpURLConnection) trackerURL.openConnection();
			getRequest.setRequestMethod("GET");

			// Helps with HttpURLConnection work when the port number is not 80
			getRequest.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
			getRequest.setRequestProperty("Accept", "*/*");

			getRequest.connect();

			int status = getRequest.getResponseCode();

			if (status != 200) {
				// There was a problem reaching the tracker
				InputStream errorStream = getRequest.getErrorStream();
				System.out.println(StreamUtil.streamToString(errorStream));
				if (errorStream != null) {
					errorStream.close();
				}

				return null;
			} else {
				// We've made the first request to the tracker
				mFirstStart = false;
				
				// Get the tracker's response
				InputStream getResponse = getRequest.getInputStream();
				byte[] response = StreamUtil.streamToBytes(getResponse);
				getResponse.close();

				try {
					List<PeerInfo> peerInfos = new ArrayList<PeerInfo>();

					// Decode the response from the tracker
					HashMap<ByteBuffer, Object> decodedResponse = (HashMap<ByteBuffer, Object>) Bencoder2.decode(response);

					if (decodedResponse.containsKey(TrackerConstants.PEERS)) {
						ArrayList<HashMap> peerMapList = (ArrayList<HashMap>) decodedResponse.get(TrackerConstants.PEERS);

						for (HashMap<ByteBuffer, Object> peerMap : peerMapList) {
							
							// Translate this data structure into a PeerInfo object
							PeerInfo peer = new PeerInfo();
							for (ByteBuffer key : peerMap.keySet()) {
								String attributeKey = new String(key.array());
								Object value = peerMap.get(key);

								if (attributeKey.equals(TrackerConstants.Response.IP)) {
									peer.setIP(new String(((ByteBuffer) value).array()));
								} else if (attributeKey.equals(TrackerConstants.Response.PEER_ID)) {
									peer.setPeerID(new String(((ByteBuffer) value).array()));
								} else if (attributeKey.equals(TrackerConstants.Response.PORT)) {
									peer.setPort((Integer) peerMap.get(key));
								}
							}

							peerInfos.add(peer);
						}
					}
					
					if (decodedResponse.containsKey(TrackerConstants.INTERVAL)) {
						Object interval = decodedResponse.get(TrackerConstants.INTERVAL);
						System.out.println("Interval is " + interval.getClass());
					}

					return peerInfos;
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		} catch (IOException e) {
			// e.printStackTrace();
			return null;
		}
	}

	public static void sendEvent(String event) {
		try {
	
			// Add all the URL params needed (info hash, our peer id, the port we'll listen on)
			// Also say we downloaded everything and that the current event is COMPLETED
			String connectURL = mAnnounceURL + "?" + Keys.INFO_HASH + "=" + HexStringConverter.toHexString(mInfoHash.array()) + "&" + Keys.PEER_ID + "="
					+ HexStringConverter.toHexString(mPeerID.getBytes()) + "&" + Keys.PORT + "=" + mTcpPort + "&" 
					+ Keys.DOWNLOADED + "=" + mFileManager.getDownloadedBytes() + "&" + Keys.UPLOADED + "=" + mFileManager.getUploadedBytes() + "&"
					+ Keys.LEFT + "=" + (mFileLength - mFileManager.getDownloadedBytes()) + "&"
					+ Keys.EVENT + "=" + event;
	
			URL trackerURL = new URL(connectURL);
	
			HttpURLConnection getRequest = (HttpURLConnection) trackerURL.openConnection();
			getRequest.setRequestMethod("GET");
	
			// Helps with HttpURLConnection work when the port number is not 80
			getRequest.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
			getRequest.setRequestProperty("Accept", "*/*");
	
			getRequest.connect();
	
			int status = getRequest.getResponseCode();
			
			if(status != 200){
				System.out.println("Problem telling tracker the \"" + event +"\" event.");
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the interval that the tracker wants regular
	 * requests to occur at
	 */
	public static int getInterval() {
		return mInterval;
	}

}
