package com.torrent.file;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

	/** How long pieces are, as told in torrent file,
	 * Defaults to a sane value
	 */
	private static long mPieceLength = 16384;

	private static class Piece {
		public enum Status {
			NOT_DOWNLOADED,
			DOWNLOADING,
			DOWNLOADED };

		/**
		 * The bytes that make up this file piece
		 */
		public byte[] bytes;

		/**
		 * State of this piece
		 * (not downloaded, downloading or downloaded)
		 */
		public Status downloadStatus = Status.NOT_DOWNLOADED;
	}

	/**
	 * Sets how many bytes pieces are, as 
	 * told in the torrent file
	 */
	public static void setPieceLength(long length){
		mPieceLength = length;
	}

	/**
	 * In cases where torrent is one file: this is the file
	 * In cases where torrent is multiple files: this is the directory
	 */
	private File mRoot;

	/**
	 * The list of files in this torrent including
	 * their paths and their lengths
	 */
	private List<DownloadFile> mDownloadFiles;

	/**
	 * Pieces of the torrent
	 */
	private ArrayList<Piece> mPieces;

	public FileManager (String path, List<DownloadFile> files) throws Exception {
		mRoot = new File(path);

		mDownloadFiles = files;

		if(mDownloadFiles.isEmpty()){
			throw new Exception("No download files specified");
		} else if (files.size() == 1){
			// Only one file, so mRoot will be that file
			if (!mRoot.createNewFile()) {
				mRoot.delete();
				mRoot.createNewFile();
			}
		} else {
			// Multiple files, so mRoot will be the directory
			// containing the files
			mRoot.mkdir();
		}
	}

	/**
	 * Indicate this piece is being or not downloaded
	 */
	public void setPieceDownloading(int index, boolean isBeingDownloaded){
		mPieces.get(index).downloadStatus = (isBeingDownloaded) ? 
					Piece.Status.DOWNLOADING : Piece.Status.NOT_DOWNLOADED;
	}

	/**
	 * Set a piece of this file
	 */
	public void setPieceDownloaded(int index, byte[] bytes){
		Piece piece = mPieces.get(index);
		piece.bytes = bytes;
		piece.downloadStatus = Piece.Status.DOWNLOADED;
	}

	/**
	 * Checks if all the pieces of this
	 * file have been downloaded
	 */
	public boolean arePiecesDownloaded() {
		for(Piece piece : mPieces){
			if(piece.downloadStatus != Piece.Status.DOWNLOADED){
				return false;
			}
		}

		return true;
	}

	/**
	 * Write the pieces to the file system
	 * in the appropriate file(s)
	 */
	public void saveToDisk(DownloadFile file) throws Exception {
		if(!arePiecesDownloaded()){
			throw new Exception("Not all pieces are downloaded");
		}

		if(mDownloadFiles.size() == 1){
			// One file, so write everything to mRoot
			FileOutputStream out = new FileOutputStream(mRoot);
			for(int i = 0; i < mPieces.size(); i++){
				out.write(mPieces.get(i).bytes);
			}

			out.flush();
			out.close();
		} else {
			// Multiple files, so write proper amount to each file
			int offset = 0;

			for(DownloadFile downloadFile : mDownloadFiles) {
				File outFile = new File(mRoot, downloadFile.getPath());

				// Make sure all the parent directories exist
				outFile.getParentFile().mkdirs();

				// Create the file, overwriting any existing file
				if(!outFile.createNewFile()){
					outFile.delete();
					outFile.createNewFile();
				}

				// Write the file
				FileOutputStream out = new FileOutputStream(outFile);
				out.write(getBytesFromPieces(offset, downloadFile.getLength()));
				out.flush();
				out.close();

				// We copied downloadFile.getLength() bytes,
				// so offset by that much more for the next file
				offset += downloadFile.getLength();
			}
		}
	}

	/**
	 * Allows access to byte[] in mPieces as
	 * if they are a single continuous array of bytes
	 * @param offset which byte to start at
	 * @param length number of bytes to request
	 */
	private byte[] getBytesFromPieces(int offset, int length){
		byte[] bytes = new byte[length];

		// Jump to the correct starting piece
		int currentPiece = offset / (int) mPieceLength;

		// Jump to the correct position in the starting piece
		int pieceOffset = offset - (currentPiece * (int) mPieceLength);

		// 'length' is now used as a bytes remaining counter
		while(length > 0 && currentPiece < mPieces.size()){
			int copyLength = Math.min(length, (int) mPieceLength - pieceOffset);
			System.arraycopy(mPieces.get(currentPiece), pieceOffset, bytes, bytes.length - length, copyLength);
			
			// We copied copyLength bytes
			length -= copyLength;

			// Move to the next piece (if needed)
			currentPiece++;

			// pieceOffset is only used for the starting piece
			// when it may be needed to start not at the 
			// 0 index of a piece
			pieceOffset = 0;
		}

		return bytes;
	}

}
