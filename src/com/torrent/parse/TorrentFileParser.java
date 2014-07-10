package com.torrent.parse;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import com.torrent.util.Bencoder2;
import com.torrent.util.BencodingException;
import com.torrent.util.TorrentInfo;

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
	private static final ByteBuffer INFO = ByteBuffer.wrap(new byte[]{ 'i', 'n', 'f', 'o' });
	private static final ByteBuffer ANNOUNCE = ByteBuffer.wrap(new byte[]{'a','n','n','o','u','n','c','e'});
	private static final ByteBuffer ANNOUNCE_LIST = ByteBuffer.wrap(new byte[]{'a','n','n','o','u','n','c','e',' ','l','i','s','t'});
	private static final ByteBuffer CREATION_DATE = ByteBuffer.wrap(new byte[]{'c','r','e','a','t','i','o','n',' ','d','a','t','e'});
	private static final ByteBuffer COMMENT = ByteBuffer.wrap(new byte[]{'c','o','m','m','e','n','t'});
	private static final ByteBuffer CREATED_BY = ByteBuffer.wrap(new byte[]{'c','r','e','a','t','e','d',' ','b','y'});
	private static final ByteBuffer PIECE_LENGTH = ByteBuffer.wrap(new byte[]{'p', 'i', 'e', 'c', 'e', ' ', 'l', 'e', 'n', 'g', 't', 'h' });
	private static final ByteBuffer PIECES = ByteBuffer.wrap(new byte[]{'p', 'i', 'e', 'c', 'e', 's' });
	private static final ByteBuffer NAME = ByteBuffer.wrap(new byte[]{'n', 'a', 'm', 'e' });
	private static final ByteBuffer LENGTH = ByteBuffer.wrap(new byte[]{'l','e','n','g','t','h'});
	private static final ByteBuffer FILES = ByteBuffer.wrap(new byte[]{'f','i','l','e','s'});
	private static final ByteBuffer PATH = ByteBuffer.wrap(new byte[]{'p','a','t','h'});

	// The torrent file stored as a String
	private String mTorrentString;

	// The TorrentFile Object that will wrap all the metadata
	private TorrentFile mTorrentFile;
	
	// Storage for torrent info
	//A byte array containing the raw bytes of the torrent metainfo file.
	public final byte[] torrent_file_bytes;
	
	//The base dictionary of the torrent metainfo file.&nbsp; 
	public final Map<ByteBuffer,Object> torrent_file_map;
	
	//The unbencoded info dictionary of the torrent metainfo file.&nbsp; 
	public final Map<ByteBuffer,Object> info_map;
	
	///The SHA-1 hash of the bencoded form of the info dictionary from the torrent metainfo file.
	public final ByteBuffer info_hash;
	
	//The base URL of the tracker for client scrapes.
	public final URL announce_url;
	
	//The default length of each piece in bytes.&nbsp;
	public final int piece_length;
	
	//The name of the file referenced in the torrent metainfo file.
	public final String file_name;
	
	//The length of the file in bytes.
	public final int file_length;
	
	//The SHA-1 hashes of each piece of the file.
	public final ByteBuffer[] piece_hashes;


	/**
	 * Construct a TorrentFileParser from a String of
	 * the file's contents
	 
	public TorrentFileParser (String fileContent){
		mTorrentFile = new TorrentFile();
		mTorrentString = fileContent;
	}
*/
	/**
	 * Construct a TorrentFileParser from an InputStream
	 * the file's contents

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
	 */	
	/**
	 * Construct from byte[]
	 */
	
	public TorrentFileParser(byte[] torrent_file_bytes) throws BencodingException {
		
		// Validate
		if(torrent_file_bytes == null || torrent_file_bytes.length == 0){
			throw new IllegalArgumentException("byte array does not exist");
		}
		
		// Assign the byte array
		this.torrent_file_bytes = torrent_file_bytes;
		
		// Assign the metainfo map
		this.torrent_file_map = (Map<ByteBuffer,Object>)Bencoder2.decode(torrent_file_bytes);

		// Try to extract the announce URL
				ByteBuffer url_buff = (ByteBuffer)this.torrent_file_map.get(ANNOUNCE);
				if(url_buff == null)
					throw new BencodingException("Could not retrieve anounce URL from torrent metainfo.  Corrupt file?");
				
				try {
					String url_string = new String(url_buff.array(), "ASCII");
					URL announce_url = new URL(url_string);
					this.announce_url = announce_url;
				}
				catch(UnsupportedEncodingException uee)
				{
					throw new BencodingException(uee.getLocalizedMessage());
				}
				catch(MalformedURLException murle)
				{
					throw new BencodingException(murle.getLocalizedMessage());
				}
		
		// Try to extract the info dictionary
		ByteBuffer info_bytes = Bencoder2.getInfoBytes(torrent_file_bytes);
		Map<ByteBuffer,Object> info_map = (Map<ByteBuffer,Object>)this.torrent_file_map.get(TorrentInfo.KEY_INFO);
				
		if(info_map == null)
			throw new BencodingException("Could not extract info dictionary from torrent metainfo dictionary.  Corrupt file?");
		this.info_map = info_map;
		
		// Try to generate the info hash value
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.update(info_bytes.array());
			byte[] info_hash = digest.digest();
			this.info_hash = ByteBuffer.wrap(info_hash);
		}
		catch(NoSuchAlgorithmException nsae)
		{
			throw new BencodingException(nsae.getLocalizedMessage());
		}
		
		// Extract the piece length from the info dictionary
		Integer piece_length = (Integer)this.info_map.get(PIECE_LENGTH);
		if(piece_length == null)
			throw new BencodingException("Could not extract piece length from info dictionary.  Corrupt file?");
		this.piece_length = piece_length.intValue();
		
		/**
		 * Program divides depending on whether or not the torrent has multiple files
		 */
		
		// Checks if the torrent is a singular file.
		// If it is, then adds the file
		// Else, parses all the files and adds the path name then adds file
			
		if(this.info_map.get(LENGTH) != null){
			
			// Extract the file name from the info dictionary
			ByteBuffer name_bytes = (ByteBuffer)this.info_map.get(NAME);
			if(name_bytes == null)
				throw new BencodingException("Could not retrieve file name from info dictionary.  Corrupt file?");
			try {
			this.file_name = new String(name_bytes.array(),"ASCII");
			}
			catch(UnsupportedEncodingException uee)
			{
				throw new BencodingException(uee.getLocalizedMessage());
			}		
		
						
			// Extracts the file length from the info dictionary
			Integer file_length = (Integer)this.info_map.get(LENGTH);
			if(file_length == null)
				throw new BencodingException("Could not extract file length from info dictionary.  Corrupt file?");
			this.file_length = file_length.intValue();
				
			// Extract the piece hashes from the info dictionary
			ByteBuffer all_hashes = (ByteBuffer)this.info_map.get(PIECES);
			if(all_hashes == null)
				throw new BencodingException("Could not extract piece hashes from info dictionary.  Corrupt file?");
			byte[] all_hashes_array = all_hashes.array();
				
			// Verify that the length of the array is a multiple of 20 bytes (160 bits)
			if(all_hashes_array.length % 20 != 0)
				throw new BencodingException("Piece hashes length is not a multiple of 20.  Corrupt file?");
			int num_pieces = all_hashes_array.length / 20;
				
			// Copy the values of the piece hashes into the local field
			this.piece_hashes = new ByteBuffer[num_pieces];
			for(int i = 0; i < num_pieces; i++)
			{
				byte[] temp_buff = new byte[20];
				System.arraycopy(all_hashes_array,i*20,temp_buff,0,20);
				this.piece_hashes[i] = ByteBuffer.wrap(temp_buff);
			}
			
		}
		
		// parses a multiple file torrent
		else{
			this.file_length = 0;
			this.file_name = null;
			this.piece_hashes = null;
		}
		
		//Sets the data parsed to a TorrentFile object
				
		
		

		
		
		
		
		
		
		
	}



}
