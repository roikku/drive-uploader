package io.uploader.drive.drive.media;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDriveApiProgressListener implements 
		com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener {
	
	private static final Logger logger = LoggerFactory.getLogger(CustomDriveApiProgressListener.class);
	
	public void progressChanged(com.google.api.client.googleapis.media.MediaHttpUploader uploader)
			throws IOException {
		switch (uploader.getUploadState()) {
		case INITIATION_STARTED:
			logger.info("Initiation has started!");
			break;
		case INITIATION_COMPLETE:
			logger.info("Initiation is complete!");
			break;
		case MEDIA_IN_PROGRESS:
			try {
				logger.info("Progress: " + uploader.getNumBytesUploaded() + " ; percentage: " + (uploader.getProgress() * 100) + " %");
			} catch (Exception e) {
				logger.info("Progress: "
						+ io.uploader.drive.util.FileUtils
								.humanReadableByteCount(
										uploader.getNumBytesUploaded(),
										true));
			}
			break;
		case MEDIA_COMPLETE:
			logger.info("Upload is complete!");
		case NOT_STARTED:
			break;
		default:
			break;
		}
	}
}