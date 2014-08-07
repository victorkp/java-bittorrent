/**
 * Victor Kaiser-Pendergrast
 * James DiPierro
 * Grayson Phillips
 */

package com.torrent.peer;

import java.nio.ByteBuffer;

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
	
	private static ByteBuffer mInfoHash;
	private static String mPeerID;
	
	public static void setParams(ByteBuffer infoHash, String peerID){
		mInfoHash = infoHash;
		mPeerID = peerID;
	}

	/**
	 * Handshakes have the following format:
	 * HANDSHAKE_STRING + 8_BYTES_RESERVED + INFO_HASH + PEER_ID
	 */
	public static byte[] makeHandshake() {
		byte[] bytes = new byte[68];
		
		System.arraycopy(HANDSHAKE.getBytes(), 0, bytes, 0, 20);
		System.arraycopy(mInfoHash.array(), 0, bytes, 28, 20);
		System.arraycopy(mPeerID.getBytes(), 0, bytes, 48, 20);
		
		return bytes;
	}
	
	/**
	 * Keep alive messages are length 0, should be sent every two minutes
	 */
	public static byte[] makeKeepAlive() {
		return new byte[]{ 0, 0, 0, 0 };
	}
	
	public static byte[] makeInterested() {
		byte[] bytes = new byte[5];
		ByteBuffer.wrap(bytes).putInt(1).put(Type.INTERESTED);
		return bytes;
	}
	
	public static byte[] makeNotInterested() {
		byte[] bytes = new byte[5];
		ByteBuffer.wrap(bytes).putInt(1).put(Type.NOT_INTERESTED);
		return bytes;
	}
	
	public static byte[] makeChoking() {
		byte[] bytes = new byte[5];
		ByteBuffer.wrap(bytes).putInt(1).put(Type.CHOKE);
		return bytes;
	}
	
	public static byte[] makeNotChoking() {
		byte[] bytes = new byte[5];
		ByteBuffer.wrap(bytes).putInt(1).put(Type.UNCHOKE);
		return bytes;
	}
	
	public static byte[] makeHave(int pieceIndex) {
		byte[] bytes = new byte[9];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		
		buffer.putInt(0x05); // Length Prefix
		buffer.put(Type.HAVE);
		buffer.putInt(pieceIndex);
		
		return bytes;
	}
	
	// TODO Implement this
	public static byte[] makeBitField(){
		byte[] bytes = new byte[4];
		ByteBuffer.wrap(bytes).putInt(0);
		return bytes;
	}
	
	public static byte[] makeRequest(int index, int beginOffset, int length){
		byte[] bytes = new byte[17];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		
		buffer.putInt(13); // Length Prefix
		buffer.put(Type.REQUEST);
		buffer.putInt(index);
		buffer.putInt(beginOffset);
		buffer.putInt(length);
		
		return bytes;
	}
	
	public static byte[] makePiece(int index, int beginOffset, byte[] block){
		byte[] bytes = new byte[13 + block.length];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		
		buffer.putInt(9 + block.length); // Length Prefix
		buffer.put(Type.PIECE);
		buffer.putInt(index);
		buffer.putInt(beginOffset);
		buffer.put(block);
		
		return bytes;
	}
	
	public static byte[] makeCancel(int index, int beginOffset, int length){
		byte[] bytes = new byte[17];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		
		buffer.putInt(13); // Length Prefix
		buffer.put(Type.CANCEL);
		buffer.putInt(index);
		buffer.putInt(beginOffset);
		buffer.putInt(length);
		
		return bytes;
	}
	
}
