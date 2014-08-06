package com.torrent.file;

import java.io.File;
import java.io.FileOutputStream;

public class FileManager {

	private File mRoot;

	public FileManager (String path, boolean isFolder) throws Exception {
		mRoot = new File(path);
		if (!mRoot.createNewFile()) {
			mRoot.delete();
			mRoot.createNewFile();
		}
	}

	public void writeToRoot(DownloadFile file) throws Exception {
		FileOutputStream out = new FileOutputStream(mRoot);
		out.write(file.getBytes());
		out.flush();
		out.close();
	}

}
