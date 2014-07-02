package com.torrent;

import java.io.File;
import java.io.FileInputStream;

import com.torrent.tracker.TrackerUtil;
import com.torrent.util.TorrentInfo;

public class Main {
	
	private static File mTorrentFile;
	private static TorrentInfo mTorrentInfo;

	public static void main(String[] args) {
		if(!checkArguments(args)){
			return;
		}
		
		try {
			mTorrentFile = new File(args[0]);
			mTorrentInfo = new TorrentInfo(fileAsBytes(mTorrentFile));
			
			if(!TrackerUtil.getPeers()){
				return;
			}
			
		} catch (Exception e){
			e.printStackTrace();
			return;
		}

	}
	
	/**
	 * Check if the arguments are the correct format
	 * @param args
	 * @return true if the arguments seem okay, false otherwise
	 */
	private static boolean checkArguments(String[] args){
		if(args.length != 2){
			System.out.println("Usage: RUBTClient <torrent file> <file to save into>");
			return false;
		}
		
		if(!args[0].endsWith(".torrent")){
			System.out.println("The first argument should be a *.torrent file");
			return false;
		}
		
		if(!new File(args[0]).exists()){
			System.out.println("The torrent file specified does not exist");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Reads a file into a byte array
	 * @param file
	 * @return a byte array of the file contents
	 */
	private static byte[] fileAsBytes(File file){
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(file);
			String content = "";
			int next;
			
			while((next = fstream.read()) != -1){
				content = content + ((char) next);
			}
			
			return content.getBytes();
		} catch (Exception e){
			e.printStackTrace();
		} finally {
			try{
				if(fstream != null){
					fstream.close();
				}
			} catch (Exception e) { }
		}
		
		return null;
	}

}
