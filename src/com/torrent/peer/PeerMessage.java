/**
 * Victor Kaiser-Pendergrast
 * James DiPierro
 * Grayson Phillips
 */

package com.torrent.peer;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PeerMessage {

	private static final String HANDSHAKE = ((char) 19) + "BitTorrent Protocol";

	public static class Type {
		public static final byte CHOKE = 0x00;
		public static final byte UNCHOKE = 0x01;
		public static final byte INTERESTED = 0x02;
		public static final byte NOT_INTERESTED = 0x03;
		public static final byte HAVE = 0x04;
		public static final byte BITFIELD = 0x05;
		public static final byte REQUEST = 0x06;
		public static final byte PIECE = 0x07;
		public static final byte CANCEL = 0x08;
	}

	public static class Message {
		public byte type;

		// The following are only used with requests,
		// sending block messages, haves, and cancels
		public int index;
		public int offset;
		public int length;

		public byte[] data;
	}

	private static ByteBuffer mInfoHash;
	private static String mPeerID;
	
	public static void setParams(ByteBuffer infoHash, String peerID){
		mInfoHash = infoHash;
		mPeerID = peerID;
	}

	public static Message readMessage(DataInputStream inStream){
		Message message = new Message();

		try {
			// Read the length of the message
			int length = inStream.readInt();

			if(length == 0){
				// This is a keep alive message
				// that has no importance
				return null;
			}

			// First byte after the length bytes is the message type
			message.type = inStream.readByte();

			switch (message.type){
				case Type.BITFIELD:
					// TODO implement bitfield parsing
					inStream.readFully(new byte[length - 1]);
					break;

				case Type.HAVE:
					message.index = inStream.readInt();
					break;

				case Type.REQUEST:
				case Type.CANCEL:
					message.index = inStream.readInt();
					message.offset = inStream.readInt();
					message.length = inStream.readInt();
					break;
				case Type.PIECE:
					message.index = inStream.readInt();
					message.offset = inStream.readInt();
					message.data = new byte[length - 9];
					inStream.readFully(message.data);
					break;
				default:
					break;
			}

			inStream.readFully(new byte[inStream.available()]);

			return message;
		} catch (IOException e) { 
			return null;
		}
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
