package com.torrent.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtil {

	/**
	 * Reads a file into a byte array
	 * 
	 * @param file
	 * @return a byte array of the file contents
	 */
	public static byte[] fileAsBytes(File file) {
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(file);
			byte[] bytes = new byte[(int) file.length()];

			fstream.read(bytes);
			return bytes;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (fstream != null) {
					fstream.close();
				}
			} catch (Exception e) {
			}
		}

		return null;
	}

	public static byte[] streamToBytes(InputStream stream) {
		try {
			ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();

			int bytesRead;
			byte[] byteBuffer = new byte[4096];
			while ((bytesRead = stream.read(byteBuffer, 0, byteBuffer.length)) != -1) {
				byteOutStream.write(byteBuffer, 0, bytesRead);
			}

			byteOutStream.flush();

			return byteOutStream.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Read a stream into a String
	 * 
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	public static String streamToString(InputStream stream) throws IOException {
		String responseString = "";
		if (stream != null) {
			int next;
			while ((next = stream.read()) != -1) {
				responseString = responseString + ((char) next);
			}
		}

		return responseString;
	}

}
