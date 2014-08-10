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
		 * The index of this piece
		 */
		public int index;

		/**
		 * The bytes that make up this file piece
		 */
		public byte[] bytes;

		/**
		 * State of this piece
		 * (not downloaded, downloading or downloaded)
		 */
		public Status downloadStatus = Status.NOT_DOWNLOADED;
		
		public Piece(int index){
			this.index = index;
		}
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
	
	/**
	 * The number of bytes requested with the
	 * intent of uploading them to other peers
	 */
	private int mUploadedBytes = 0;

	public FileManager (String path, List<DownloadFile> files, int numPieces) throws Exception {
		mRoot = new File(path);

		// Add the correct number of pieces to mPieces
		mPieces = new ArrayList<Piece>(numPieces);
		for(int i = 0; i < numPieces; i++){
			mPieces.add(new Piece(i));
		}

		mDownloadFiles = files;

		if(mDownloadFiles.isEmpty()){
			throw new Exception("No download files specified");
		} else if (files.size() == 1){
			if(mRoot.exists()){
				// Check to see if this file does not need to be redownloaded
				if(mRoot.length() == files.get(0).getLength()){
					// File exists and is correct length
					setupPiecesFromFile(mRoot);
					return;
				}
			}
			
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
	 * Get the first piece that needs to be downloaded
	 */
	public int getNeededPiece(){
		List<Piece> neededPieces = new ArrayList<Piece>();
		
		for(int i = 0; i < mPieces.size(); i++){
			if(mPieces.get(i).downloadStatus != Piece.Status.DOWNLOADED){
				neededPieces.add(mPieces.get(i));
			}
		}
		
		if(neededPieces.isEmpty()){
			return -1;
		}
		
		return neededPieces.get((int)(Math.random() * neededPieces.size())).index;
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
		
		if(piece.downloadStatus == Piece.Status.DOWNLOADED){
			return;
		}
		
		piece.bytes = bytes;
		piece.downloadStatus = Piece.Status.DOWNLOADED;
		
		printProgress();

		if(arePiecesDownloaded()){
			try{
				System.out.println("\n _____________________________ ");
				System.out.println("|------------NOTE-------------|");
				System.out.println("|----Transfer has completed---|");
				System.out.println("|----File is being written----|");
				System.out.println("|-----------------------------|");
				writeToDisk();
				System.out.println("|-----Saved successfully------|");
				System.out.println("|-----------------------------|\n");
			} catch (Exception e){
				System.out.println("|-----COULD NOT SAVE FILE-----|");
				System.out.println("|-----------------------------|\n");
				//e.printStackTrace();
			}
		}
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
	
	private void printProgress(){
		List<Piece> neededPieces = new ArrayList<Piece>();
		
		for(int i = 0; i < mPieces.size(); i++){
			if(mPieces.get(i).downloadStatus != Piece.Status.DOWNLOADED){
				neededPieces.add(mPieces.get(i));
			}
		}
		
		int progress = (100 * (mPieces.size() - neededPieces.size())) / mPieces.size();
		System.out.print(progress + "%");
		int bars = 0;
		while(progress > 0){
			System.out.print("=");
			progress -= 4;
			bars++;
		}
		System.out.print(">");
		for(int i = 25 - bars; i > 0; i--){
			System.out.print(" ");
		}
		System.out.println("|");
	}

	/**
	 * @return The number of bytes that have been downloaded
	 */
	public int getDownloadedBytes() {
		int bytes = 0;
		
		for(Piece p : mPieces){
			if(p.downloadStatus == Piece.Status.DOWNLOADED){
				bytes += p.bytes.length;
			}
		}
		
		return bytes;
	}
	
	/**
	 * @return The number of bytes that were requested for upload
	 */
	public int getUploadedBytes() {
		return mUploadedBytes;
	}
	
	/**
	 * Get a piece's bytes if it has been downloaded;
	 * Increment the number of bytes uploaded
	 * @return the bytes of the piece, null if not already downloaded
	 */
	public byte[] getPieceForUpload(int index) {
		Piece p = mPieces.get(index);
		
		if(p.downloadStatus == Piece.Status.DOWNLOADED){
			mUploadedBytes += p.bytes.length;
			return p.bytes;
		}
		
		return null;
	}
	
	/**
	 * Setup
	 * @param f the file that is completely downloaded
	 */
	private void setupPiecesFromFile(File file){
		
	}

	/**
	 * Write the pieces to the file system
	 * in the appropriate file(s)
	 */
	private void writeToDisk() throws Exception {
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
