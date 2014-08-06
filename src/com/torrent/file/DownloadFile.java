package com.torrent.file;

import java.util.ArrayList;
import java.util.Arrays;

public class DownloadFile {

	/**
	 * Total file length;
	 */
	private int mLength;

	/**
	 * The path this file should be written to
	 * null if there is only one file being downloaded
	 */
	private String mFilePath;

	/**
	 * @param numPieces the number of pieces that compose this file
	 * @param length the total file's length
	 * @param path the path that this file should be written to
	 */
	public DownloadFile(int length, String path){
		mLength = length;
		mFilePath = path;
	}

	/**
	 * The path this file should be written to
	 */
	public String getPath(){
		return mFilePath;
	}

	/**
	 * The length of this file in bytes
	 */
	public int getLength() {
		return mLength;
	}

}
