/*
 *  Copyright 2014 Loic Merckel
 *  Copyright 2014 Dirk Boye 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
*
* The original version of this file (i.e., the one that is copyrighted 2014 Dirk Boye) 
* can  be found here:
*
*  https://github.com/dirkboye/GDriveUpload
*  
*  Massive changes have been made
*
*/


package io.uploader.drive.drive.largefile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;

import io.uploader.drive.config.HasConfiguration;
import io.uploader.drive.drive.DriveUtils.HasDescription;
import io.uploader.drive.drive.DriveUtils.HasId;
import io.uploader.drive.drive.DriveUtils.HasMimeType;
import io.uploader.drive.util.FileUtils.InputStreamProgressFilter;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

class GDriveUpload {

    private static final Logger logger = LoggerFactory.getLogger(GDriveUpload.class);
    
    private final HasConfiguration config ;
    private final String title ;
    private final String filename ;
    private final InputStreamProgressFilter.StreamProgressCallback progressCallback ;
    private final HasMimeType mimeType ;
    private final HasId parentId ;
    private final HasDescription description ;
    private final HasId fileId ;
    
    private final String tmpFilePath ;
    private String md5 = null ;
    
    // https://developers.google.com/google-apps/documents-list/#uploading_a_new_document_or_file_with_both_metadata_and_content
    // Important: Always choose a chunk size that is a multiple of 512 kilobytes. The last chunk may be smaller than 512 kilobytes.
    private final int chunkSize = 10 * 1024 * 1024 ;
    
    protected GDriveUpload (HasConfiguration config, String title,
			HasDescription description, HasId parentId, HasMimeType mimeType,
			String filename, InputStreamProgressFilter.StreamProgressCallback progressCallback) {
    	super () ;
    	
    	this.config = config ;
    	this.title = title ;
    	this.filename = filename ;
    	this.progressCallback = progressCallback ;
    	this.mimeType = mimeType ;
    	this.parentId = parentId ;
    	this.description = description ;
    	this.fileId = null ;
    	
    	tmpFilePath = config.getTmpDirectory() + title + ".tmp" ;
    	logger.info ("Tmp file: " + tmpFilePath) ;
    }
        
    
    protected GDriveUpload (HasConfiguration config, HasId fileId, HasMimeType mimeType,
			String filename, InputStreamProgressFilter.StreamProgressCallback progressCallback) {
    	super () ;
    	
    	this.config = config ;
    	this.title = null ;
    	this.filename = filename ;
    	this.progressCallback = progressCallback ;
    	this.mimeType = mimeType ;
    	this.parentId = null ;
    	this.description = null ;
    	this.fileId = fileId ;
    	
    	tmpFilePath = config.getTmpDirectory() + Paths.get(filename).getFileName().toString() + "-update.tmp" ;
    	logger.info ("Tmp file: " + tmpFilePath) ;
    }
    
    
    private static class TransferException extends IOException {

		private static final long serialVersionUID = 1L;
		
		private final boolean notResumable ;
    	
		public TransferException(boolean notResumable, String message) {
			super(message);
			this.notResumable = notResumable ;
		}

		public boolean isNotResumable() {
			return notResumable;
		}
    }
    
    
    private String uploadFile(DriveResumableUpload upload, BasicFileAttributes attr) throws IOException {
    	
        long currentBytePosition = upload.getCurrentByte();
        File file = new File(filename);
        if (currentBytePosition > -1 && currentBytePosition < attr.size()) {
            byte[] chunk;
            int retries = 0;
            while (retries < 5) {
                InputStream stream = io.uploader.drive.util.FileUtils
						.getInputStreamWithProgressFilter(
								progressCallback, attr.size(),
								new FileInputStream(file)) ;
                if (currentBytePosition > 0) {
                    stream.skip(currentBytePosition);
                }
                chunk = new byte[chunkSize];
                int bytes_read = stream.read(chunk, 0, chunkSize);
                stream.close();
                if (bytes_read > 0) {
                    int status = upload.uploadChunk(chunk, currentBytePosition, bytes_read);
                    if (status == 308) {
                        // If Status is 308 RESUME INCOMPLETE there's no retry done.
                        retries = 0;
                    } else if (status >= 500 && status < 600) {
                        // Good practice: Exponential backoff
                        try {
                            long seconds = Math.round(Math.pow(2, retries + 1));
                            logger.info("Exponential backoff. Waiting " + seconds + " seconds.");
                            Thread.sleep(seconds*1000);
                        } catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    } else if (status == 401) {
                    	logger.info("Tokan has experied, need to be refreshed...") ;
                        upload.updateAccessToken();
                    } else if (status == 200 || status == 201) {
                    	
                        boolean success = upload.checkMD5(md5);
                        logger.info("local md5sum: " + md5);
                        logger.info("File upload complete.");
                        if (!success) {
                        	throw new TransferException (false, "The md5 values do not macth") ;
                        }
                        break ;
                    } else if (status == 404) {
                    	// this can be due to a remaining temporary file with an out-dated link
                    	// we throw that exception with no recovery option (not resumable) in order to 
                    	// delete this file (if any)
                    	throw new TransferException (false, "The file cannot be found") ;
                    }
                    else {
                    	logger.info("Status: " + String.valueOf(status));
                    }
                }
                ++retries;
                currentBytePosition = upload.getCurrentByte();
            }
        } else if (currentBytePosition == attr.size ()) {
            boolean success = upload.checkMD5(md5);
            logger.info("local md5sum: " + md5);
            logger.info("File upload complete.");
            
            if (!success) {
            	throw new IOException ("The md5 values do not macth") ;
            }
        } else {
            // Some BUG occured. lastbyte = -1.
        	throw new TransferException (false, "Some anomalies have been observed") ;
        }
        // get file id
        return upload.getFileId() ;
    }
    
    
	protected String uploadFile(boolean update) throws IOException {
		
    	Preconditions.checkState(update == (fileId != null));
    	
		DriveResumableUpload upload = null;
		BasicFileAttributes attr = null;
		File fstatus = null;
		try {
			String googleLocation;
			String filestatus = tmpFilePath;
			attr = io.uploader.drive.util.FileUtils.getFileAttr(Paths
					.get(filename));

			fstatus = new File(filestatus);
			if (fstatus.exists()) {
				BufferedReader in = new BufferedReader(new FileReader(
						filestatus));
				md5 = in.readLine();
				googleLocation = in.readLine();
				in.close();

				if (update) {
					upload = new DriveResumableUpload(
							config.getHttpProxySettings(), new DriveAuth(config), googleLocation, fileId, mimeType, filename, attr.size(),
							progressCallback);
				} else {
					upload = new DriveResumableUpload(
							config.getHttpProxySettings(), new DriveAuth(config), googleLocation, title,
							description, parentId, mimeType, filename, attr.size(),
							progressCallback);
				}
			} else {
				if (md5 == null) {
					FileInputStream fis = new FileInputStream(
							new File(filename));
					md5 = org.apache.commons.codec.digest.DigestUtils
							.md5Hex(fis);
					logger.info("md5: " + md5);
					fis.close();
				}
				// Completely new upload. location: null
				if (update) {
					upload = new DriveResumableUpload(
							config.getHttpProxySettings(), new DriveAuth(config), null, fileId, mimeType, filename, attr.size(),
							progressCallback);
				} else {
					upload = new DriveResumableUpload(
							config.getHttpProxySettings(), new DriveAuth(config), null, title,
							description, parentId, mimeType, filename, attr.size(),
							progressCallback);
				}

				// Write location and md5 to file for later resume
				BufferedWriter out = new BufferedWriter(new FileWriter(
						filestatus));
				out.write(md5 + "\n" + upload.getLocation() + "\n");
				out.close();
			}
		} catch (URISyntaxException e) {
			logger.error("Error occurred while uploading files", e);
			throw new RuntimeException("Error occurred while uploading files "
					+ e.getMessage());
		}
		try {
			String ret = uploadFile(upload, attr);
			if (fstatus != null && fstatus.exists()) {
				fstatus.delete();
			}
			return ret;
		} catch (TransferException e) {
			if (e.isNotResumable()) {
				if (fstatus != null && fstatus.exists()) {
					fstatus.delete();
				}
			}
			throw new IOException (e.getMessage()) ;
		}
    }
}