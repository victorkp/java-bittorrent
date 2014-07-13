package com.torrent.peer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import com.torrent.util.Globals;

public class PeerConnection {

	private PeerInfo mPeer;
	private Socket mSocket;
	private InputStream mIn;
	private OutputStream mOut;

	private DataInputStream mDataIn;
	private DataOutputStream mDataOut;

	private boolean mBeingChoked;
	private boolean mOtherInterested;

	private boolean mChoking;
	private boolean mInterested;

	public PeerConnection(PeerInfo peer) {
		mPeer = peer;
		try {
			mSocket = new Socket(mPeer.getIP(), mPeer.getPort());
			mOut = mSocket.getOutputStream();
			mIn = mSocket.getInputStream();

			mDataIn = new DataInputStream(mIn);
			mDataOut = new DataOutputStream(mOut);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public PeerInfo getPeerInfo() {
		return mPeer;
	}

	public void closeConnection() {
		try {
			mDataIn.close();
			mDataOut.close();
			mSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Make a handshake with the peer
	 * 
	 * @return if the handshake seemed successful
	 */
	public boolean doHandshake() {
		try {
			mSocket.setSoTimeout(20000);
			mDataOut.write(PeerMessage.makeHandshake());
			mDataOut.flush();

			byte[] response = new byte[68];
			mDataIn.readFully(response);

			byte[] responseInfoHash = Arrays.copyOfRange(response, 28, 48);

			return Arrays.equals(Globals.torrentInfo.info_hash.array(), responseInfoHash);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public boolean initDownload() {
		try {
			// Ignore the bitfield response
			byte[] response = new byte[10];
			mDataIn.readFully(response);
			System.out.println("Length prefix: " + (int) response[3]);
			System.out.println("Type: " + (int) response[4]);
			
			
			if(!doInterested()) {
				System.out.println("Peer is still choking us.");
			}
			
			doDownload();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
	
	public void doDownload() throws IOException{
		int finalPieceLength;
		int currentPiece = 0;
		int requestBeginOffset = 0;
		int requestLength = 16384;
		
		// How long the final piece in the file is
		finalPieceLength = Globals.torrentInfo.file_length - ((Globals.torrentInfo.piece_hashes.length - 1) * Globals.torrentInfo.piece_length);
		System.out.println("Final piece length is " + finalPieceLength);
		
		while(currentPiece < Globals.torrentInfo.piece_hashes.length) {
			// We still need parts of the file
			
			if(currentPiece == Globals.torrentInfo.piece_hashes.length -1){
				// If this is the last piece, we have to be careful about not requesting
				// data past the end of the file
				
				requestLength = Math.min(finalPieceLength, 16384);
				finalPieceLength -= 16384;
			}
			
			System.out.println("Downloading piece " + currentPiece + " from offset " + requestBeginOffset + " and length " + requestLength);
			
			// Request a part of a piece
			byte[] bytes = doRequest(currentPiece, requestBeginOffset, requestLength);
			Globals.downloadFileOut.write(bytes);
			
			requestBeginOffset += requestLength;
			
			// If we have finished downloading this piece...
			if(requestBeginOffset == Globals.torrentInfo.piece_length){
				// move on to the next piece
				currentPiece++;
				requestBeginOffset = 0;
			}
			
			// Check to see if we're done download file
			if(finalPieceLength < 0){
				currentPiece ++;
			}
		}
	}
	

	/**
	 * Make a request to download data
	 * @param index index of the piece to download
	 * @param offset the byte offset of where to start in that piece 
	 * @param length how many bytes to download after the offset
	 * @return
	 */
	public byte[] doRequest(int index, int offset, int length) {
		try {
			mDataOut.write(PeerMessage.makeRequest(index, offset, length));
			mDataOut.flush();

			byte[] buffer = new byte[4];
			mDataIn.read(buffer);
			
			byte[] data = new byte[length];
			mDataIn.readFully(data);
			
			return data;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	/**
	 * Sends an "interested" message
	 * @return if the other peer has unchoked
	 */
	public boolean doInterested() {
		try {
			// Ignore 5
			mSocket.setSoTimeout(20000);
			mDataOut.write(PeerMessage.makeInterested());
			mDataOut.flush();
			
			byte[] response = new byte[5];
			mDataIn.readFully(response);

			byte peerInterested = response[4];
			return peerInterested == 1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}


	public void doKeepAlive() {
		try {
			mOut.write(PeerMessage.makeKeepAlive());
			mOut.flush();

			String response = "";
			int next;
			while ((next = mIn.read()) != -1) {
				response += (char) next;
			}

			System.out.println(response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void doNotChoking() {
		try {
			mOut.write(PeerMessage.makeNotChoking());
			mOut.flush();

			String response = "";
			int next;
			while ((next = mIn.read()) != -1) {
				response += (char) next;
			}

			System.out.println(response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getResponse(int length) throws IOException {
		String response = "";
		int next;
		for (int i = 0; i < length; i++) {
			response = ((next = mIn.read()) != -1) ? (response + (byte) next) : (response);
		}

		return response;
	}

	private String getResponse() throws IOException {

		String response = "";
		int next;
		while ((next = mIn.read()) != -1) {
			response += (char) next;
			System.out.print((char) next);
		}

		System.out.println("\n" + response);

		return response;
	}

}
