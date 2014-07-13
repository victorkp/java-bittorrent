package com.torrent.tracker;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.torrent.peer.PeerInfo;
import com.torrent.util.Bencoder2;
import com.torrent.util.Globals;
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

	private static Thread mTcpThread;
	private static ServerSocket mTcpSocket;

	public static List<PeerInfo> getPeers() {
		try {
			ByteBuffer infoHash = Globals.torrentInfo.info_hash;
			mAnnounceURL = Globals.torrentInfo.announce_url.toString();

			// Add all the URL params needed (info hash, our peer id, the port we'll listen on)
			// Also say we downloaded/uploaded nothing and that the current event is STARTED
			String connectURL = mAnnounceURL + "?" + Keys.INFO_HASH + "=" + HexStringConverter.toHexString(infoHash.array()) + "&" + Keys.PEER_ID + "="
					+ HexStringConverter.toHexString(Globals.peerID.getBytes()) + "&" + Keys.PORT + "=" + Globals.tcpPort + "&" + Keys.DOWNLOADED + "=0&" + Keys.UPLOADED + "=0&" + Keys.LEFT + "="
					+ Globals.torrentInfo.file_length + "&" + Keys.EVENT + "=" + Keys.Events.STARTED;

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
				// Get the tracker's response
				InputStream getResponse = getRequest.getInputStream();
				byte[] response = StreamUtil.streamToBytes(getResponse);
				getResponse.close();

				try {
					List<PeerInfo> peerInfos = new ArrayList<PeerInfo>();
					
					// Decode the response from the tracker
					HashMap<ByteBuffer, Object> decodedResponse = (HashMap<ByteBuffer, Object>) Bencoder2.decode(response);
					
					if(decodedResponse.containsKey(TrackerConstants.PEERS)){
						ArrayList<HashMap> peerMapList = (ArrayList<HashMap>) decodedResponse.get(TrackerConstants.PEERS);
						
						for(HashMap<ByteBuffer, Object> peerMap : peerMapList){
							PeerInfo peer = new PeerInfo();
							for(ByteBuffer key : peerMap.keySet()){
								String attributeKey = new String(key.array());
								Object value = peerMap.get(key);
								
								if(attributeKey.equals(TrackerConstants.Response.IP)){
									peer.setIP(new String(((ByteBuffer) value).array()));
								} else if (attributeKey.equals(TrackerConstants.Response.PEER_ID)){
									peer.setPeerID(new String(((ByteBuffer) value).array()));
								} else if (attributeKey.equals(TrackerConstants.Response.PORT)){
									peer.setPort((Integer) peerMap.get(key));
								}
							}
							
							peerInfos.add(peer);
						}
					}
					
					return peerInfos;
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		} catch (IOException e) {
			//e.printStackTrace();
			return null;
		}
	}

	

}
