package com.torrent.peer;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.torrent.util.StreamUtil;

public class PeerConnection {
	
	private PeerInfo mPeer;
	private Socket mSocket;
	private InputStream mIn;
	private OutputStream mOut;
	
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public PeerInfo getPeerInfo() {
		return mPeer;
	}
	
	public void doHandshake() {
		try {
			mOut.write(PeerMessage.makeHandshake());
			
			String response = "";
			int next;
			while((next = mIn.read()) != -1){
				next += (char) next;
			}
			
			System.out.println(response);
		
		} catch (Exception e) {
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
				next += (char) next;
			}
			
			System.out.println(response);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void doInterested() {
		try {
			mOut.write(PeerMessage.makeKeepAlive());
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

}
