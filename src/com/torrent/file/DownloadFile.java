package com.torrent.file;

import java.util.ArrayList;
import java.util.Arrays;

public class DownloadFile {

	// How long pieces are, as told in torrent file,
	// Defaults to a sane value
	private static long mPieceLength = 16384;

	private static class FilePiece {
		// The bytes that make up this file piece
		public byte[] bytes;

		// Whether or not this file piece has
		// had its "bytes" field set
		public boolean isSet = false;
	}

	// Number of pieces that compose this file
	private int mNumPieces;

	// Pieces of the DownloadFile
	private ArrayList<FilePiece> mPieces;

	// Total file length;
	private long mLength;

	/**
	 * Sets how many bytes pieces are, as 
	 * told in the torrent file
	 */
	public static void setPieceLength(long length){
		mPieceLength = length;
	}

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
	 * Set a piece of this file
	 */
	public void setPiece(int index, byte[] bytes){
		FilePiece piece = mPieces.get(index);
		piece.bytes = bytes;
		piece.isSet = true;
	}

	/**
	 * Checks if all the pieces of this
	 * file have been downloaded
	 */
	public boolean isFileDownloaded() {
		boolean allDownloaded = true;

		for(FilePiece piece : mPieces){
			allDownloaded &= piece.isSet;
		}

		return allDownloaded;
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
