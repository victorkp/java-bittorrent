package com.torrent.peer;

import java.nio.ByteBuffer;

import com.torrent.util.Globals;
import com.torrent.util.HexStringConverter;
import com.torrent.util.TorrentInfo;

public class PeerMessage {

	private static final String HANDSHAKE = ((char) 19) + "BitTorrent Protocol";

	public static class Type {
		public static byte CHOKE = 0x00;
		public static byte UNCHOKE = 0x01;
		public static byte INTERESTED = 0x02;
		public static byte NOT_INTERESTED = 0x03;
		public static byte HAVE = 0x04;
		public static byte BITFIELD = 0x05;
		public static byte REQUEST = 0x06;
		public static byte PIECE = 0x07;
		public static byte CANCEL = 0x08;
	}

	/**
	 * Handshakes have the following format:
	 * HANDSHAKE_STRING + 8_BYTES_RESERVED + INFO_HASH + PEER_ID
	 */
	public static byte[] makeHandshake() {
		byte[] bytes = new byte[68];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		
		buffer.put(HANDSHAKE.getBytes());
		buffer.put(new byte[8]);
		buffer.put(Globals.torrentInfo.info_hash.array());
		buffer.put(Globals.peerID.getBytes());
		
		return bytes;
	}
	
	/**
	 * Keep alive messages are length 0, should be sent every two minutes
	 */
	public static byte[] makeKeepAlive() {
		return new byte[1];
	}
	
	public static byte[] makeInterested() {
		return new byte[]{ 0x01, Type.INTERESTED };
	}
	
	public static byte[] makeNotInterested() {
		return new byte[]{ 0x01, Type.NOT_INTERESTED };
	}
	
	public static byte[] makeHave(int pieceIndex) {
		byte[] bytes = new byte[6];
		bytes[0] = 0x05; // Length Prefix
		bytes[1] = Type.HAVE;
		
	}

}
