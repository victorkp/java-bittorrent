package com.torrent.file;

import java.util.ArrayList;
import java.util.Arrays;

public class DownloadFile {

	// How long pieces are, as told in torrent file,
	// Defaults to a sane value
	private static long mPieceLength = 16384;

	private static class FilePiece {
		public enum Status {
			NOT_DOWNLOADED,
			DOWNLOADING,
			DOWNLOADED };

		// The bytes that make up this file piece
		public byte[] bytes;

		// Whether or not this file piece has
		// had its "bytes" field set
		public Status downloadStatus = Status.NOT_DOWNLOADED;
	}

	/**
	 * Number of pieces that compose this file
	 */
	private int mNumPieces;

	/**
	 * Pieces of the DownloadFile
	 */
	private ArrayList<FilePiece> mPieces;

	/**
	 * Total file length;
	 */
	private long mLength;

	/**
	 * The path this file should be written to
	 * null if there is only one file being downloaded
	 */
	private String mFilePath;

	/**
	 * Sets how many bytes pieces are, as 
	 * told in the torrent file
	 */
	public static void setPieceLength(long length){
		mPieceLength = length;
	}

	/**
	 * Create a download file
	 * @param numPieces the number of pieces that compose this file
	 * @param length the total file's length
	 */
	public DownloadFile(int numPieces, long length){
		mNumPieces = numPieces;
		mLength = length;

		// Initialize the DownloadFile pieces with 
		// blank, unset FilePieces
		mPieces = new ArrayList<FilePiece>(numPieces);
		for(int i = 0; i < mNumPieces; i++){
			mPieces.add(new FilePiece());
		}
	}

	/**
	 * @param numPieces the number of pieces that compose this file
	 * @param length the total file's length
	 * @param path the path that this file should be written to
	 */
	public DownloadFile(int numPieces, int length, String path){
		this(numPieces, length);
		mFilePath = path;
	}

	/**
	 * The path this file should be written to
	 */
	public String getPath(){
		return mFilePath;
	}

	/**
	 * Indicate this piece is being downloaded
	 */
	public void setDownloading(int index, boolean isBeingDownloaded){
		mPieces.get(index).downloadStatus = (isBeingDownloaded) ? 
					FilePiece.Status.DOWNLOADING : FilePiece.Status.NOT_DOWNLOADED;
	}

	/**
	 * Set a piece of this file
	 */
	public void setPiece(int index, byte[] bytes){
		FilePiece piece = mPieces.get(index);
		piece.bytes = bytes;
		piece.downloadStatus = FilePiece.Status.DOWNLOADED;
	}

	/**
	 * Checks if all the pieces of this
	 * file have been downloaded
	 */
	public boolean isFileDownloaded() {
		for(FilePiece piece : mPieces){
			if(piece.downloadStatus != FilePiece.Status.DOWNLOADED){
				return false;
			}
		}

		return true;
	}

	/**
	 * Get the DownloadFile's raw bytes
	 * @return null if the file is not fully downloaded
	 */
	public byte[] getBytes(){
		if(!isFileDownloaded()){
			return null;
		}

		byte[] bytes = new byte[(int) mLength];
		int offset = 0;

		for(int i = 0; i < mNumPieces - 1; i++){
			System.arraycopy(mPieces.get(i).bytes, 0, bytes, offset, (int) mPieceLength);
			offset += mPieceLength;
		}
		System.arraycopy(mPieces.get(mNumPieces - 1).bytes, 0, bytes, offset, (int) (mLength % mPieceLength) );

		return bytes;
	}

}
