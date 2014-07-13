package com.torrent;

import java.io.File;
import java.util.List;

import com.torrent.parse.TorrentFileParser;
import com.torrent.peer.PeerInfo;
import com.torrent.peer.PeerMessage;
import com.torrent.peer.PeerUtil;
import com.torrent.tracker.TrackerUtil;
import com.torrent.util.Globals;
import com.torrent.util.StreamUtil;
import com.torrent.util.TorrentInfo;
import com.torrent.parse.TorrentFile;

public class Main {
	
	private static File mTorrentFile;
	private static TorrentInfo mTorrentInfo;
	private static TorrentFileParser TorrentInfo;
	private static TorrentFile Torrent;

	public static void main(String[] args) {
		if(!checkArguments(args)){
			return;
		}
		
		try {
			mTorrentFile = new File(args[0]);
			mTorrentInfo = new TorrentInfo(StreamUtil.fileAsBytes(mTorrentFile));
			TorrentInfo = new TorrentFileParser(StreamUtil.fileAsBytes(mTorrentFile));
			Torrent = new TorrentFile(TorrentInfo);

			Globals.peerID = PeerUtil.getPeerID();
			Globals.tcpPort = PeerUtil.openTCP();
			Globals.torrentInfo = mTorrentInfo;
			
			System.out.println(Globals.torrentInfo.info_hash.array());
			
			List<PeerInfo> peerList = TrackerUtil.getPeers();
			if(peerList == null){
				System.out.println("There was a problem contacting the tracker");
				return;
			}
			
			System.out.println("Peers:");
			for(PeerInfo peer : peerList){
				System.out.println("  Peer:  " + peer);
				if(peer.getPeerID().contains("RU1103")){
					PeerUtil.connectToPeer(peer);
					break;
				}
			}
			
			System.out.println("---END---");

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
	
}
