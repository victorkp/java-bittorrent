/**
 * Victor Kaiser-Pendergrast
 * James DiPierro
 * Grayson Phillips
 */

package com.torrent.peer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.torrent.file.FileManager;

public class PeerConnection {

	private static String mAnnounceURL;
	private static ByteBuffer mInfoHash;

	private static int mFileLength;

	private static String mPeerID;
	private static int mTcpPort;

	private static ByteBuffer mPieceHashes[];
	private static int mPieceLength;

	public static void setParams(String announceURL, ByteBuffer infoHash, int fileLength, ByteBuffer[] pieceHashes, int pieceLength, String peerID, int tcpPort) {
		mAnnounceURL = announceURL;
		mInfoHash = infoHash;
		mFileLength = fileLength;
		mPieceHashes = pieceHashes;
		mPieceLength = pieceLength;
		mPeerID = peerID;
		mTcpPort = tcpPort;
	}

	private static FileManager mFileManager;

	public static void setFileManager(FileManager fileManager) {
		mFileManager = fileManager;
	}

	private PeerInfo mPeer;
	private Socket mSocket;
	private InputStream mIn;
	private OutputStream mOut;

	private DataInputStream mDataIn;
	private DataOutputStream mDataOut;

	private boolean mBeingChoked = true;
	private boolean mOtherInterested = false;

	private boolean mChoking = true;
	private boolean mInterested = false;

	private byte[] mCurrentPieceBytes;

	private Thread mDownloadThread;

	/**
	 * How many bytes received by this peer in the last period of the
	 * PeerManager
	 */
	private int mBytesReceived;

	/**
	 * How many bytes sent to this peer in the last period of the PeerManager
	 */
	private int mBytesSent;

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

	/**
	 * Create a new PeerConnection with a peer that initiated the connection
	 * 
	 * @param socket
	 *            the socket the other peer initiated
	 */
	public PeerConnection(Socket socket) {
		try {
			mSocket = socket;
			mOut = mSocket.getOutputStream();
			mIn = mSocket.getInputStream();
			mDataIn = new DataInputStream(mIn);
			mDataOut = new DataOutputStream(mOut);

			// Parse the handshake
			mSocket.setSoTimeout(20000);

			byte[] response = new byte[68];
			mDataIn.readFully(response);

			byte[] responseInfoHash = Arrays.copyOfRange(response, 28, 48);

			// If the hash is correct
			if (Arrays.equals(mInfoHash.array(), responseInfoHash)) {
				// Then setup the peer fully
				String peerId = new String(Arrays.copyOfRange(response, 48, 68));
				mPeer = new PeerInfo(socket.getInetAddress().getHostAddress(), socket.getPort(), peerId);
			} else {
				// This is a bad peer
				mPeer = null;
			}
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
	 * Make a handshake with the peer: send handshake, and parse other's
	 * handshake
	 * 
	 * @return if the handshake seemed successful
	 */
	public boolean doHandshake() {
		try {
			sendHandshake();

			byte[] response = new byte[68];
			mDataIn.readFully(response);

			byte[] responseInfoHash = Arrays.copyOfRange(response, 28, 48);

			return Arrays.equals(mInfoHash.array(), responseInfoHash);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Send our handshake to the peer, expect no response
	 */
	public void sendHandshake() {
		try {
			mSocket.setSoTimeout(20000);
			mDataOut.write(PeerMessage.makeHandshake());
			mDataOut.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tell the peer that a piece has been downloaded
	 * 
	 * @param index
	 *            the index of the piece
	 * @return true if successful
	 */
	public boolean doHave(int index) {
		try {
			mSocket.setSoTimeout(20000);
			mDataOut.write(PeerMessage.makeHave(index));
			mDataOut.flush();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Indicate interest to the peer, returns true if we may begin downloading
	 * (if the other peer is not choking us)
	 */
	public boolean indicateInterest() {
		mInterested = true;

		try {
			// Ignore the bitfield response
			byte[] lengthPrefix = new byte[4];
			mDataIn.readFully(lengthPrefix);
			ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthPrefix);
			int length = lengthBuffer.getInt();

			byte[] response = new byte[length];
			mDataIn.readFully(response);

			if (!doInterested()) {
				System.out.println("Error: peer is still choking us.");
				return false;
			}

			return true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public Thread startAsyncDownload() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					doDownload();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.start();

		return t;
	}

	public void stopAsyncDownload() {
		mInterested = false;
	}

	public void doDownload() throws IOException {
		mCurrentPieceBytes = new byte[mPieceLength];

		int finalPieceLength, finalPieceRemaining;
		int currentPiece = mFileManager.getNeededPiece();
		int requestBeginOffset = 0;
		int requestLength = 16384;

		// How long the final piece in the file is
		finalPieceRemaining = finalPieceLength = mFileLength - ((mPieceHashes.length - 1) * mPieceLength);

		while (currentPiece != -1 && mInterested) {
			// We still need parts of the file and we are interested in this
			// Peer

			// Keep track that this piece is being downloaded, so that other
			// PeerConnections
			// don't also try to download this piece
			mFileManager.setPieceDownloading(currentPiece, true);

			if (currentPiece == mPieceHashes.length - 1) {
				// If this is the last piece, we have to be careful about not
				// requesting
				// data past the end of the file

				requestLength = Math.min(finalPieceRemaining, 16384);
				finalPieceRemaining -= 16384;
			}

			debug(mPeer.getPeerID() + ": Downloading piece " + currentPiece + "/" + (mPieceHashes.length - 1));

			// Request a part of a piece
			byte[] bytes = doRequest(currentPiece, requestBeginOffset, requestLength);
			System.arraycopy(bytes, 0, mCurrentPieceBytes, requestBeginOffset, requestLength);
			// Globals.downloadFileOut.write(bytes);

			requestBeginOffset += requestLength;

			// If we have finished downloading this piece...
			if (requestBeginOffset == mPieceLength || finalPieceRemaining <= 0) {

				// If this is the final piece, only hash up to the final piece
				// length
				if (currentPiece == mPieceHashes.length - 1) {
					if (checkPieceHash(currentPiece, finalPieceLength)) {
						// The hash is good, so tell the peer, write the data,
						// and finish
						doHave(currentPiece);

						mBytesReceived += finalPieceLength;
						mFileManager.setPieceDownloaded(currentPiece, Arrays.copyOf(mCurrentPieceBytes, finalPieceLength));
					} else {
						debug("Final piece Hash failed");
					}
				} else {
					if (checkPieceHash(currentPiece, mPieceLength)) {
						// not last piece, so hash all bytes

						// The hash is good, so tell the peer,
						doHave(currentPiece);

						mBytesReceived += mCurrentPieceBytes.length;
						mFileManager.setPieceDownloaded(currentPiece, Arrays.copyOf(mCurrentPieceBytes, mCurrentPieceBytes.length));
					} else {
						debug("Hash failed");
					}
				}
				
				// Pick another piece that needs to be downloaded
				currentPiece = mFileManager.getNeededPiece();

				Arrays.fill(mCurrentPieceBytes, (byte) 0);
				requestBeginOffset = 0;
			}
		}
	}

	/**
	 * Compare the SHA-1 hash of this piece with what's expected
	 * 
	 * @param index
	 *            index of the piece to be checked
	 * @param length
	 *            how much of the piece should be hashed
	 * @return true if the hash matches
	 */
	private boolean checkPieceHash(int index, int length) {
		try {
			byte[] pieceHash = new byte[20];
			MessageDigest md = MessageDigest.getInstance("SHA-1");

			pieceHash = md.digest(Arrays.copyOfRange(mCurrentPieceBytes, 0, length));

			return Arrays.equals(pieceHash, mPieceHashes[index].array());

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Make a request to download data
	 * 
	 * @param index
	 *            index of the piece to download
	 * @param offset
	 *            the byte offset of where to start in that piece
	 * @param length
	 *            how many bytes to download after the offset
	 * @return
	 */
	public byte[] doRequest(int index, int offset, int length) {
		try {
			mDataOut.write(PeerMessage.makeRequest(index, offset, length));
			mDataOut.flush();

			// Ignore length prefix, piece index, and offset (we already know
			// all this)
			byte[] buffer = new byte[13];
			mDataIn.readFully(buffer);

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
	 * 
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return number of bytes received from this peer
	 */
	public int getBytesReceived() {
		return mBytesReceived;
	}

	/**
	 * @return number of bytes sent to this peer
	 */
	public int getBytesSent() {
		return mBytesSent;
	}

	/**
	 * Reset the number of bytes sent to and received from this peer
	 */
	public void resetBytesSentAndReceived() {
		mBytesReceived = 0;
		mBytesSent = 0;
	}
	
	/**
	 * Change the if statement to show debug messages
	 */
	private void debug(String s){
		if("1".equals("2")){
			System.out.println(s);
		}
	}

	@Override
	public String toString() {
		return String.format("PeerConnection to %s, at %s:%s", mPeer.getPeerID(), mPeer.getIP(), mPeer.getPort());
	}

}
