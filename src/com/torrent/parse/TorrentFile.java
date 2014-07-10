package com.torrent.parse;

import java.util.ArrayList;
import java.util.List;

public class TorrentFile {

	private static final String TRACKER_REGEX = "^[\\d:]$";

	private List<String> mInfo;
	private List<String> mAnnounce;
	private List<String> mFiles;
	private List<String> mPaths;
	private List<String> mComments;

	private long mCreationDate;

	private int mPieces;
	private int mPieceLength;

	private String mCreatedBy;

	public TorrentFile(TorrentFileParser parser) {
		mInfo = new ArrayList<String>(3);
		mAnnounce = new ArrayList<String>(3);
		mFiles = new ArrayList<String>(3);
		mPaths = new ArrayList<String>(3);
		mComments = new ArrayList<String>(3);
		
		//adds the info from the parser to the TorrentFile object
		
		this.addInfo(parser.info_hash.toString());
		this.addAnnounce(parser.announce_url.toString());
		this.addFile(parser.file_name);
		//this.addFile(file, path);
		
		//this.addComment(comment);
		
		//this.setCreationDate(date);
		this.setFilePieces(parser.piece_hashes.length);
		this.setFilePieceLength(parser.piece_length);
		//mTorrentFile.setCreator(creator);
	}

	public void addFile(String file){
		mFiles.add(file);
	}
	
	public void addFile(String file, String path){
		addFile(file);
		mPaths.add(path);
	}

	public void addComment(String comment){
		mComments.add(comment);
	}

	public void addAnnounce(String announce){
		mAnnounce.add(announce);
	}

	public List<String> getAnnounce() {
		return mAnnounce;
	}

	/**
	 * Find the first String from the torrent's
	 * announce list that appears to be an IP address
	 */
	public String getFirstTracker() {
		for(int i = 0; i < mAnnounce.size(); i++){
			if(mAnnounce.get(i).matches(TRACKER_REGEX)){
				return mAnnounce.get(i);
			}
		}
		return null;
	}

	public void addInfo(String info){
		mInfo.add(info);
	}

	public List<String> getInfo() {
		return mInfo;
	}

	public void setCreationDate(long date){
		mCreationDate = date;
	}
	
	public long getCreationDate() {
		return mCreationDate;
	}

	public void setCreator(String creator){
		mCreatedBy = creator;
	}

	public String getCreator() {
		return mCreatedBy;
	}

	public void setFilePieces(int pieces){
		mPieces = pieces;
	}

	public int getFilePieces() {
		return mPieces;
	}

	public void setFilePieceLength(int length){
		mPieceLength = length;
	}

	public int getFilePieceLength() {
		return mPieceLength;
	}

}
