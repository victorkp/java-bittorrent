package com.torrent.util;

public class HexStringConverter {
	
	private static final char[] hexChars = "0123456789ABCDEF".toCharArray();
	
	public static String toHexString(byte[] bytes) {
		char[] hexCharArray = new char[bytes.length * 3];
		for (int i = 0; i < bytes.length; i++) {
			hexCharArray[i * 3] = '%';
			hexCharArray[i * 3 + 1] = hexChars[(bytes[i] >> 4) & 0x0f];
			hexCharArray[i * 3 + 2] = hexChars[bytes[i] & 0x0f];
		}

		return new String(hexCharArray);
	}

}
