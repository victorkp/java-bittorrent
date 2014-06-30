package com.torrent.parse;

import java.io.IOException;
import java.io.InputStream;

public class TorrentFileParser {

	/**
	 * Torrent data types
	 */
	private static final String DICTIONARY = "d";
	private static final String STRING = "s";
	private static final String LIST = "l";
	private static final String INTEGER = "i";

	/**
	 * Torrent metadata keys
	 */
	private static final String INFO = "info";
	private static final String ANNOUNCE = "announce";
	private static final String ANNOUNCE_LIST = "announce-list";
	private static final String CREATION_DATE = "creation date";
	private static final String COMMENT = "comment";
	private static final String CREATED_BY = "created by";
	private static final String PIECE_LENGTH = "piece length";
	private static final String PIECES = "pieces";
	private static final String NAME = "name";
	private static final String LENGTH = "length";
	private static final String FILES = "files";
	private static final String PATH = "path";

	// The torrent file stored as a String
	private String mTorrentString;

	// The TorrentFile Object that will wrap all the metadata
	private TorrentFile mTorrentFile;


	/**
	 * Construct a TorrentFileParser from a String of
	 * the file's contents
	 */
	public TorrentFileParser (String fileContent){
		mTorrentFile = new TorrentFile();
		mTorrentString = fileContent;
	}

	/**
	 * Construct a TorrentFileParser from an InputStream
	 * the file's contents
	 */
	public TorrentFileParser (InputStream fileStream){
		mTorrentString = "";

		try {
			int next;
			while( (next = fileStream.read()) != -1){
				mTorrentString = mTorrentString + (char) next;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}




}
