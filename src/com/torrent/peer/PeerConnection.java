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

	private static final int DEFAULT_REQUEST_LENGTH = 16384;

	/**
	 * Whether or not to display verbose debugging messages
	 */
	private static boolean DEBUG = false;

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
	private int mCurrentRequestIndex = -1;
	private int mCurrentRequestOffset;
	private int mCurrentRequestLength;

	/**
	 * The thread that sends requests for pieces
	 */
	private Thread mDownloadThread;

	/**
	 * Thread that mointors incoming messages on mSocket and responds
	 * appropriately
	 */
	private Thread mSocketThread;

	/**
	 * If this peer should have an active connection
	 */
	private boolean mActive = false;

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

	public void start() {
		mActive = true;
		(mSocketThread = new Thread(new Runnable() {
			public void run() {
				sendInterested();
				while (!mSocket.isClosed() && mActive) {
					try {
						// Wait for and parse a message
						PeerMessage.Message message = PeerMessage.readMessage(mDataIn);

						// Determine what to do
						switch (message.type) {
						case PeerMessage.Type.BITFIELD:
							// Send bitfield to the FileManager 
							debug(mPeer + " sent bitfield");
							mFileManager.addBitfield(message.data);
							break;
						case PeerMessage.Type.CANCEL:
							// Try a different piece if we get a cancel
							debug(mPeer + String.format(" received cancel %d(%d-%d)", message.index, message.offset, message.length));
							break;
						case PeerMessage.Type.CHOKE:
							// Try to get unchoked again
							debug(mPeer + " is choking");
							mBeingChoked = true;
							sendInterested();
							break;
						case PeerMessage.Type.HAVE:
							// Ignore HAVEs
							debug(mPeer + " HAVE piece " + message.index);
							break;
						case PeerMessage.Type.INTERESTED:
							// Unchoke with some probability
							debug(mPeer + " is interested");
							if(Math.random() > 0.5){
								mChoking = false;
								mOtherInterested = true;
								sendNotChoking();
							}
							break;
						case PeerMessage.Type.NOT_INTERESTED:
							// The other peer is no longer interested
							debug(mPeer + " is not interested");
							mOtherInterested = false;
							break;
						case PeerMessage.Type.PIECE:
							// Handle pieces
							debug(mPeer + String.format(" sent us %d(%d-%d)", message.index, message.offset, message.data.length));
							debug("handling");
							handleBlock(message.index, message.offset, message.data);
							debug("requesting");
							requestPiece();
							break;
						case PeerMessage.Type.REQUEST:
							// Send if we're not choking
							debug(mPeer + String.format(" requested %d(%d-%d)", message.index, message.offset, message.length));
							if (!mChoking) {
								sendPiece(message.index, message.offset, message.length);
							}
							break;
						case PeerMessage.Type.UNCHOKE:
							// We were unchoked, so begin requesting pieces
							debug(mPeer + " is not choking");
							mBeingChoked = false;
							requestPiece();
							break;
						}

					} catch (Exception e) {
					}
				}
			}
		})).start();
	}

	public void stop() {
		debug("Stopping " + mPeer);

		mActive = false;

		// Send a choke
		sendChoking();

		// Try to peacefully stop
		try {
			mSocketThread.join(8000);
		} catch (Exception e) {
		}

		// Close socket to forcefully stop
		closeConnection();

		// Tell the filemanager that this piece
		// is not being downloaded any more
		mFileManager.setPieceDownloading(mCurrentRequestIndex, false);
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
	 * Process the block that was received by the other peer
	 */
	private void handleBlock(int index, int offset, byte[] data) {
		if (mCurrentPieceBytes == null) {
			mCurrentPieceBytes = new byte[mPieceLength];
		}

		System.arraycopy(data, 0, mCurrentPieceBytes, mCurrentRequestOffset, mCurrentRequestLength);
		mCurrentRequestOffset += mCurrentRequestLength;

		// Need to take into account if this is the last piece
		int pieceLength = (mCurrentRequestIndex == mPieceHashes.length - 1) ? (mFileLength % mPieceLength) : (mPieceLength);

		if (mCurrentRequestOffset >= pieceLength) {
			// Should be done downloading this piece; check hash
			debug("Checking hash on piece " + mCurrentRequestIndex);

			if (checkPieceHash(mCurrentRequestIndex, pieceLength)) {
				// Hash is good, send to FileManager
				mFileManager.setPieceDownloaded(mCurrentRequestIndex, Arrays.copyOf(mCurrentPieceBytes, pieceLength));

				// Increment number of good bytes received
				mBytesReceived += pieceLength;

				// Send a HAVE message
				sendHave(mCurrentRequestIndex);
			} else {
				debug("Hash failed: " + mPeer);
			}

			// Reset for next piece
			mCurrentRequestOffset = 0;
			Arrays.fill(mCurrentPieceBytes, (byte) 0);

			// Get the next piece to download
			mCurrentRequestIndex = mFileManager.getNeededPiece();
		}
	}

	/**
	 * Request a block of a piece that is not already downloaded
	 */
	private void requestPiece() {
		if (mBeingChoked) {
			// Need to get unchoked first
			sendInterested();
			debug("Can't request, being choked: " + mPeer);
		} else {
			// If this is the first piece requested, find what is needed
			// to be downloaded
			if (mCurrentRequestIndex == -1) {
				mCurrentRequestIndex = mFileManager.getNeededPiece();
			}

			// If we can request something
			if (mCurrentRequestIndex != -1) {
				// Piece length based on whether or not this is the final piece
				int pieceLength = (mCurrentRequestIndex == mPieceHashes.length - 1) ? (mFileLength % mPieceLength) : (mPieceLength);

				if (mCurrentRequestOffset + mCurrentRequestLength > pieceLength) {
					// Adjust requested length if needed
					mCurrentRequestLength = pieceLength % DEFAULT_REQUEST_LENGTH;
				} else {
					mCurrentRequestLength = DEFAULT_REQUEST_LENGTH;
				}

				debug(String.format(" requesting piece %d from %d to %d: " + mPeer, mCurrentRequestIndex, mCurrentRequestOffset, mCurrentRequestOffset + mCurrentRequestLength));

				sendRequest(mCurrentRequestIndex, mCurrentRequestOffset, mCurrentRequestLength);
			} else {
				debug("Can't request, nothing needed: " + mPeer);
			}
		}
	}

	private void sendPiece(int index, int offset, int length) {
		debug(String.format(" sending piece %d from %d to %d: " + mPeer, index, offset, offset + length));

		byte[] piece = mFileManager.getPieceForUpload(index);

		// If we don't have this piece, send no bytes for the data
		piece = (piece == null) ? (new byte[0]) : (Arrays.copyOfRange(piece, offset, offset + length));

		byte[] message = PeerMessage.makePiece(index, offset, piece);

		try {
			mDataOut.write(message);
			mDataOut.flush();
		} catch (Exception e) {
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
		debug(" sending handshake: " + mPeer);
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
	public boolean sendHave(int index) {
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
	 * Send a request to download data
	 * 
	 * @param index
	 *            index of the piece to download
	 * @param offset
	 *            the byte offset of where to start in that piece
	 * @param length
	 *            how many bytes to download after the offset
	 */
	private void sendRequest(int index, int offset, int length) {
		try {
			mDataOut.write(PeerMessage.makeRequest(index, offset, length));
			mDataOut.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends an "interested" message
	 * 
	 * @return if the other peer has unchoked
	 */
	public void sendInterested() {
		debug(" sending interested: " + mPeer);

		try {
			mSocket.setSoTimeout(20000);
			mDataOut.write(PeerMessage.makeInterested());
			mDataOut.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send a blank, keepalive message
	 */
	public void sendKeepAlive() {
		try {
			mOut.write(PeerMessage.makeKeepAlive());
			mOut.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send a choking message
	 */
	public void sendChoking() {
		debug(" sending choking: " + mPeer);

		try {
			mOut.write(PeerMessage.makeChoking());
			mOut.flush();
		} catch (Exception e) {
			if (!e.getMessage().contains("Socket closed")) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Send a not choking message
	 */
	public void sendNotChoking() {
		debug(" sending not choking: " + mPeer);

		try {
			mOut.write(PeerMessage.makeNotChoking());
			mOut.flush();
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
	private void debug(String s) {
		if (DEBUG) {
			System.out.println(s);
		}
	}

	@Override
	public String toString() {
		return String.format("PeerConnection to %s, at %s:%s", mPeer.getPeerID(), mPeer.getIP(), mPeer.getPort());
	}

}
