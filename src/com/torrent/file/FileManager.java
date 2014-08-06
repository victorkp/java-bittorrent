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

	public void writeFile(DownloadFile file) throws Exception {
		// If there is only one file (file.getPath() == null), then mRoot is the output file.
		// Otherwise, mRoot is a folder and this file should be under mRoot
		File outFile = (file.getPath() == null) ? (mRoot) : (new File(mRoot, file.getPath()));

		// Double check the file is downloaded
		if(file.isFileDownloaded()){
			FileOutputStream out = new FileOutputStream(mRoot);
			out.write(file.getBytes());
			out.flush();
			out.close();
		}
	}

}
