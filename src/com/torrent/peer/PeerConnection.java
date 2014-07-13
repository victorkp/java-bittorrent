package com.torrent.peer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
	
	public PeerConnection(PeerInfo peer){
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
			mSocket.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Make a handshake with the peer
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
	
	public void doInterested() {
		try {
			mSocket.setSoTimeout(20000);
			mDataOut.write(PeerMessage.makeInterested());
			mDataOut.flush();
			
			byte[] response = new byte[5];
			mDataIn.readFully(response);
			
			System.out.println(new String(response));
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	
	public void doKeepAlive() {
		try {
			mOut.write(PeerMessage.makeKeepAlive());
			mOut.flush();
			
			String response = "";
			int next;
			while((next = mIn.read()) != -1){
				response += (char) next;
			}
			
			System.out.println(response);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void doNotChoking() {
		try {
			mOut.write(PeerMessage.makeNotChoking());
			mOut.flush();
			
			String response = "";
			int next;
			while((next = mIn.read()) != -1){
				response += (char) next;
			}
			
			System.out.println(response);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void doRequest(int index, int offset, int length){
		try {
			mOut.write(PeerMessage.makeRequest(index, offset, length));
			mOut.flush();
			
			String response = "";
			int next;
			while((next = mIn.read()) != -1){
				next += (char) next;
			}
			
			System.out.println(response);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	private String getResponse(int length) throws IOException {
		String response = "";
		int next;
		for(int i = 0; i < length; i++){
			response = ((next = mIn.read()) != -1) ? (response + (byte) next) : (response);
		}
		
		return response;
	}
	
	private String getResponse() throws IOException {
		
		String response = "";
		int next;
		while((next = mIn.read()) != -1){
			response += (char) next;
			System.out.print((char) next);
		}
		
		System.out.println("\n" + response);
		
		return response;
	}

}
