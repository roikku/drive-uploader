/*
 * Copyright 2014 Loic Merckel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.uploader.drive.drive;

import io.uploader.drive.drive.DriveUtils.HasDescription;
import io.uploader.drive.drive.DriveUtils.HasId;
import io.uploader.drive.drive.DriveUtils.HasMimeType;
import io.uploader.drive.util.FileUtils.FileFinderOption;
import io.uploader.drive.util.FileUtils.InputStreamProgressFilter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.base.Preconditions;

public class DriveOperations {
	private static final Logger logger = LoggerFactory.getLogger(DriveOperations.class);
	
	private final static int maxNumberOfRetry  = 10 ;
	
	public interface HasStatusReporter {
		public abstract void setStatus (String str) ;
		public abstract void setTotalProgress (double p) ;
		public abstract void setCurrentProgress (double p) ;
	}
	
	public interface StopRequester {
		public boolean isStopRequested();
	}
	
	private DriveOperations() {
		super();
		throw new IllegalStateException();
	}
	
	
	public enum OperationCompletionStatus {
		COMPLETED,
		STOPPED,
		ERROR,
		WARNING,
		UNKNOWN
	}
	
	
	public static class OperationResult {
		
		public static interface HasWarning {
			public String getWarningMessage () ;
		}
		
		public static HasWarning newWarning (final String message) {
			return new HasWarning () {

				@Override
				public String getWarningMessage() {
					return message;
				}} ;
		}
		
		private OperationCompletionStatus status = OperationCompletionStatus.UNKNOWN ;
		private final Map<Path, Throwable> pathErrorMap = new HashMap<Path, Throwable> () ;
		private final Map<Path, HasWarning> pathWarningMap = new HashMap<Path, HasWarning> () ;
		
		public OperationCompletionStatus getStatus() {
			return status;
		}
		
		public void setStatus(OperationCompletionStatus status) {
			//if (this.status == OperationCompletionStatus.ERROR) {
			//	return ;
			//} 
			this.status = status;
		}
		
		public boolean hasError () {
			return !(pathErrorMap.isEmpty()) ;
		}
		
		public boolean hasWarning () {
			return !(pathWarningMap.isEmpty()) ;
		}
		
		public Map<Path, Throwable> getPathErrorMap() {
			return pathErrorMap;
		}
		
		public void addError (Path path, Throwable e) {
			pathErrorMap.put(path, e) ;
		}
		
		public Map<Path, HasWarning> getPathWarningMap() {
			return pathWarningMap;
		}
		
		public void addWarning (Path path, HasWarning warn) {
			pathWarningMap.put(path, warn) ;
		}
	}
	
	
	private static boolean isRetryable (Throwable  e) {
		logger.info("Check whether the error is recoverable");
		if (e instanceof com.google.api.client.googleapis.json.GoogleJsonResponseException) {
			return true ;
		} else if (e instanceof java.net.SocketTimeoutException) {
			return true ;
		} else if (e instanceof IOException) {
			return true ;
		} else if (e instanceof RuntimeException) {
			return true ;
		}
		logger.info("No. It is not.");
		return false ;
	}
	
	
	private static void dealWithException (Throwable e, AtomicInteger counter) throws Throwable
	{		
		if (isRetryable (e))
		{
			if (counter.getAndIncrement() >= maxNumberOfRetry) {
				throw e ;
			}
			logger.info(String.format("Error: %s", e.toString()));
		}
		else
			throw e ;
	}
	
	
	private static File createDirectoryIfNotExist (Drive client, final File parent, String title) throws Throwable {
		File driveDirectory = null ;
		AtomicInteger tryCounter = new AtomicInteger () ;
		while (true) {
			try {
				FileList dirs = DriveUtils.findDirectoriesWithTitle(client, title, DriveUtils.newId(parent), (Integer)null) ;
				if (dirs.getItems() == null || dirs.getItems().isEmpty()) {
					logger.info(
							String.format("The directory %s does not exists%s. It will be created.",
							title,
							((parent == null) ? ("") : (" (under " + parent.getTitle() + ")"))));
					driveDirectory = DriveUtils.insertDirectory(client, title, null, DriveUtils.newId(parent));
				} else if (dirs.getItems().size() > 1) {
					throw new IllegalStateException ("There are " + dirs.size() + " directories with the name " + title + "...") ;
				} else {
					driveDirectory = dirs.getItems().get(0) ;		
				}
				break ;
			} catch (Throwable e) {
				dealWithException (e, tryCounter) ;
			}
		}
		return driveDirectory ;
	}
	
	
	private final static Tika tika = new Tika() ;
	private static String findMineType (Path path) {
		if (path == null) {
			return null ;
		}
		try {
			//return Files.probeContentType(path) ;
			return tika.detect(new FileInputStream(path.toFile()));
		} catch (IOException e) {
			logger.error ("Error occurred while attempting to determine the mine type of " + path.toString(), e) ;
			return null ;
		}
	}
	
	
	private static File insertFile (Drive service, String title,
			HasDescription description, HasId parentId, HasMimeType mimeType,
			String filename, InputStreamProgressFilter.StreamProgressCallback progressCallback) throws IOException {
		
		logger.info("Upload file  " + filename);
		return DriveUtils.insertFile(service, title, description, 
				parentId, mimeType, filename, progressCallback) ;
	}
	
	
	private static File updateFile (String localMd5, Drive service, File driveFile, String newTitle,
			HasDescription newDescription, HasMimeType newMimeType,
			String filename,
			InputStreamProgressFilter.StreamProgressCallback progressCallback) throws IOException {
		
		File ret = null ;
		String driveMd5 = driveFile.getMd5Checksum() ;
		logger.info("Local md5: " + localMd5);
		logger.info("drive md5: " + driveMd5);
		if (!localMd5.equals(driveMd5)) {
			logger.info("A different version of the file with the name '" + driveFile.getTitle() + "' and type '" + driveFile.getMimeType() + "' already exists, it will be overwritten");
			logger.info("Upload and overwrite file " + filename);
			ret = DriveUtils.updateFile(service, DriveUtils.newId(driveFile.getId()), null, null, 
					newMimeType, filename, progressCallback) ;
		} else {
			logger.info("An identical version of the file with the name '" + driveFile.getTitle() + "' and type '" + driveFile.getMimeType() + "' already exists, it will not be uploaded again");
			ret = driveFile ;
		}
		return ret ;
	}
	
	
	public static File uploadFile (OperationResult operationResult, Drive client, final File driveParent, Path path, boolean overwrite, InputStreamProgressFilter.StreamProgressCallback progressCallback) throws Throwable {
		
		File ret = null ;
		AtomicInteger tryCounter = new AtomicInteger () ;
		while (true) {
			try {
				// check if file already exists, if yes, check the etag
				String mineType = findMineType (path) ;
				String title = path.getFileName().toString() ;
				
				//FileList fileList = DriveUtils.findFilesWithTitleAndMineType(client, title, 
				//		DriveUtils.newId(driveParent), DriveUtils.newMineType(mineType), null);
				FileList fileList = DriveUtils.findFilesWithTitleAndMineType(client, title, 
						DriveUtils.newId(driveParent), null, null);
		
				if (fileList.getItems() == null || fileList.getItems().isEmpty()) {
					// there exists no file with the name title, we create it
					ret = insertFile (client, path.getFileName().toString(), null, 
							DriveUtils.newId(driveParent), DriveUtils.newMineType(mineType), path.toString(), progressCallback) ;
				} else if (!overwrite) {
					// there already exists at least one file with the name title, we do nothing
					logger.info("File with the name '"+ title + "' and type '" + mineType + "' already exists in directory " + ((driveParent==null)?("root"):(driveParent.getTitle())) + " (there are "+ fileList.getItems().size() + " copies), it will be ignored");
					ret = fileList.getItems().get(0) ;	
				} else {
					// there exists at least one file with the name title
					if (fileList.getItems().size() > 1) {
						// here there are more than one file with the name title.
						// this is an unexpected situation! A warning message will be displayed
						StringBuilder sb = new StringBuilder () ;
						sb.append ("The folder '") ;
						sb.append (driveParent.getTitle()) ;
						sb.append ("' contains ") ;
						sb.append (fileList.getItems().size()) ;
						sb.append (" files with the same name '") ;
						sb.append (path.getFileName().toString()) ;
						sb.append ("'.") ;
						
						// all the files with the name title are identical, we delete the unnecessary copies
						String refMd5 = fileList.getItems().get(0).getMd5Checksum() ;
						boolean allIdentical = true ;
						for (File file : fileList.getItems()) {
							if (!refMd5.equals(file.getMd5Checksum())) {
								allIdentical = false ;
								break ;
							}
						}
						if (allIdentical) {
							// remove unnecessary copies
							boolean toBeTrashed = false ;
							for (File file : fileList.getItems()) {
								if (toBeTrashed) {
									logger.info("Trashed duplicated file " + file.getTitle()) ;
									DriveUtils.trashFile(client, DriveUtils.newId(file.getId())) ;
								}
								toBeTrashed = true ;
							}
							sb.append (" The duplicated copies have been trashed and the remaining copy has been updated'") ;
							operationResult.addWarning(path, OperationResult.newWarning(sb.toString()));
							
							//  we update the now unique remaining file if required
							String localEtag = io.uploader.drive.util.FileUtils.getMD5(path.toFile()) ;
							ret = updateFile (localEtag, client, fileList.getItems().get(0), null, null, 
									DriveUtils.newMineType(mineType), path.toString(), progressCallback) ;
						} else {
							// there are discrepancies between the files with the name title
							// we add the new file without modifying the existing ones
							sb.append (" The file '") ;
							sb.append (path.toString()) ;
							sb.append ("' was uploaded as a new file") ;
							operationResult.addWarning(path, OperationResult.newWarning(sb.toString()));
							
							ret = insertFile (client, path.getFileName().toString(), null, 
									DriveUtils.newId(driveParent), DriveUtils.newMineType(mineType), path.toString(), progressCallback) ;
						}
					} else {
						// there already exists only one file with the name title, we update the file if required
						String localEtag = io.uploader.drive.util.FileUtils.getMD5(path.toFile()) ;
						ret = updateFile (localEtag, client, fileList.getItems().get(0), null, null, 
								DriveUtils.newMineType(mineType), path.toString(), progressCallback) ;
					} 
				}
				break ;
			} catch (Throwable e) {
				dealWithException (e, tryCounter) ;
				logger.info("Is about to retry...");
			}
		}
		return ret ;
	}
	
	
	private static boolean hasStopBeenRequested (StopRequester stopRequester) {
		if (stopRequester == null) {
			return false ;
		} else {
			return stopRequester.isStopRequested() ;
		}
	}
	
	
	public static OperationResult uploadDirectory (Drive client, DriveDirectory destDir, Path srcDir, boolean overwrite, final StopRequester stopRequester, final HasStatusReporter statusReporter) throws Throwable {
		
		if (client == null) {
			throw new IllegalArgumentException ("The Drive cannot be null") ;
		}
		File driveDestDirectory = null ;
		if (destDir == null || org.apache.commons.lang3.StringUtils.isEmpty(destDir.getId())) {
			// create the parent directory
			logger.info("Check parent directory " + destDir.getTitle());
			driveDestDirectory = createDirectoryIfNotExist (client, null, Paths.get(destDir.getTitle()).getFileName().toString()) ;
		} else {
			driveDestDirectory = DriveUtils.getFile(client, destDir) ;
		}
		return uploadDirectory (client, driveDestDirectory, srcDir, overwrite, stopRequester, statusReporter) ;
	}
	
	
	private static Map<Path, File> createDirectoriesStructure (OperationResult operationResult, Drive client, File driveDestDirectory, Path srcDir , final StopRequester stopRequester, final HasStatusReporter statusReporter) throws IOException {
		
		Queue<Path> directoriesQueue = io.uploader.drive.util.FileUtils
				.getAllFilesPath(srcDir,
						FileFinderOption.DIRECTORY_ONLY);
		
		if (statusReporter != null) {
			statusReporter.setCurrentProgress(0.0) ;
			statusReporter.setTotalProgress(0.0);
			statusReporter.setStatus("Checking/creating directories structure...");
		}
		
		long count = 0 ;
		Path topParent = srcDir.getParent() ;
		Map<Path, File> localPathDriveFileMapping = new HashMap <Path, File> () ;
		localPathDriveFileMapping.put(topParent, driveDestDirectory) ;
		for (Path path : directoriesQueue) {
			try {
				if (statusReporter != null) {
					statusReporter.setCurrentProgress(0.0) ;
					statusReporter.setStatus("Checking/creating directories structure... (" + path.getFileName().toString() + ")");
				}
				
				if (hasStopBeenRequested (stopRequester)) {
					if (statusReporter != null) {
						statusReporter.setStatus("Stopped!");
					}
					operationResult.setStatus (OperationCompletionStatus.STOPPED) ;
					return localPathDriveFileMapping ;
				}
				
				File driveParent = localPathDriveFileMapping.get(path.getParent()) ;
				if (driveParent == null) {
					throw new IllegalStateException ("The path " + path.toString() + " does not have any parent in the drive (parent path " + path.getParent().toString() + ")...") ;
				}
				// check whether driveParent already exists, otherwise create it
				File driveDirectory = createDirectoryIfNotExist (client, driveParent, path.getFileName().toString()) ;
				localPathDriveFileMapping.put(path, driveDirectory) ;
				
				++count ;
				if (statusReporter != null) {
					double p = ((double)count) / directoriesQueue.size() ;
					statusReporter.setTotalProgress(p) ;
					statusReporter.setCurrentProgress(1.0) ;
				}
			} catch (Throwable e) {
				logger.error("Error occurred while creating the directory " + path.toString (), e);
				operationResult.setStatus (OperationCompletionStatus.ERROR) ;
				operationResult.addError(path, e);
			}
		}
		return localPathDriveFileMapping ;
	}
	
	
	private static void uploadFiles (OperationResult operationResult, Map<Path, File> localPathDriveFileMapping, Drive client, Path srcDir , boolean overwrite, final StopRequester stopRequester, final HasStatusReporter statusReporter) throws IOException {
		
		Queue<Path> filesQueue = io.uploader.drive.util.FileUtils
				.getAllFilesPath(srcDir,
						FileFinderOption.FILE_ONLY);

		int count = 0 ;
		for (Path path : filesQueue) {
			try {
				if (statusReporter != null) {
					BasicFileAttributes attr =  io.uploader.drive.util.FileUtils.getFileAttr(path) ;
					StringBuilder sb = new StringBuilder () ;
					sb.append("Transfering files (") ;
					sb.append(path.getFileName().toString()) ;
					if (attr != null) {
						sb.append(" - size: ") ;
						sb.append(io.uploader.drive.util.FileUtils.humanReadableByteCount(attr.size(), true)) ;
					}
					sb.append(")") ;
					statusReporter.setStatus(sb.toString());
				}
				
				if (hasStopBeenRequested (stopRequester)) {
					if (statusReporter != null) {
						statusReporter.setStatus("Stopped!");
					}
					operationResult.setStatus (OperationCompletionStatus.STOPPED) ;
					return ;
				}
				
				final File driveParent = localPathDriveFileMapping.get(path.getParent()) ;
				if (driveParent == null) {
					throw new IllegalStateException ("The path " + path.toString() + " does not have any parent in the drive (parent path " + path.getParent().toString() + ")...") ;
				}
				
				InputStreamProgressFilter.StreamProgressCallback progressCallback = null ;
				if (statusReporter != null) {
					progressCallback = new InputStreamProgressFilter.StreamProgressCallback () {
	
						@Override
						public void onStreamProgress(double progress) {
							if (statusReporter != null) {
								statusReporter.setCurrentProgress(progress) ;
							}
						}} ;
				}
				uploadFile (operationResult, client, driveParent, path, overwrite, progressCallback) ;
				
				++count ;
				if (statusReporter != null) {
					double p = ((double)count) / filesQueue.size() ;
					statusReporter.setTotalProgress(p) ;
					statusReporter.setStatus("Transfering files...");
				}
			} catch (Throwable e) {
				logger.error("Error occurred while transfering the file " + path.toString (), e);
				operationResult.setStatus (OperationCompletionStatus.ERROR) ;
				operationResult.addError(path, e);
			}
		}
	}
	
	
	public static OperationResult uploadDirectory (Drive client, File destDir, Path srcDir, boolean overwrite, final StopRequester stopRequester, final HasStatusReporter statusReporter) throws Throwable {
		
		if (client == null) {
			throw new IllegalArgumentException ("The Drive cannot be null") ;
		}
		Preconditions.checkNotNull(destDir) ;
		Preconditions.checkNotNull(srcDir) ;
		
		OperationResult ret = new OperationResult () ;
		ret.setStatus(OperationCompletionStatus.COMPLETED);
		
		// create the parent directory
		File driveDestDirectory = destDir ;
		
		// first, we create the directories structure
		Map<Path, File> localPathDriveFileMapping = createDirectoriesStructure (ret, client, driveDestDirectory, srcDir, stopRequester, statusReporter) ;
		// If the directory structure is ill-formed, then we should not go any further...
		Preconditions.checkState(ret.getStatus() != OperationCompletionStatus.ERROR) ;
		Preconditions.checkNotNull(localPathDriveFileMapping) ;
		
		if (ret.getStatus() == OperationCompletionStatus.STOPPED) {
			return ret ;
		}
		
		// then, we transfer the files using localPathDriveFileMapping
		if (statusReporter != null) {
			statusReporter.setCurrentProgress(0.0) ;
			statusReporter.setTotalProgress(0.0);
			statusReporter.setStatus("Transfering files...");
		}
		
		uploadFiles (ret, localPathDriveFileMapping, client, srcDir , overwrite, stopRequester, statusReporter) ;
		if (ret.getStatus() == OperationCompletionStatus.STOPPED) {
			return ret ;
		}
		
		if (statusReporter != null) {
			StringBuilder sb = new StringBuilder () ;
			sb.append("Complete!") ;
			if (ret.hasError()) {
				sb.append(" Errors occurred. ") ;
				sb.append(ret.getPathErrorMap().size()) ;
				sb.append(" files were not transferred...") ;
			} 
			if (ret.hasWarning()) {
				sb.append(" There are ") ;
				sb.append(ret.getPathWarningMap().size()) ;
				sb.append(" warnings...") ;
			}
			statusReporter.setStatus(sb.toString());
		}
		return ret ;
	}
}
