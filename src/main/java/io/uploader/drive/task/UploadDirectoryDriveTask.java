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

package io.uploader.drive.task;

import io.uploader.drive.drive.DriveDirectory;
import io.uploader.drive.drive.DriveOperations;
import io.uploader.drive.drive.DriveOperations.HasStatusReporter;
import io.uploader.drive.drive.DriveOperations.StopRequester;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.drive.Drive;

public class UploadDirectoryDriveTask extends DriveTask<DriveOperations.OperationResult> {

	private static final Logger logger = LoggerFactory.getLogger(UploadDirectoryDriveTask.class);
	
	private final String destDirTitle ;
	private final Path srcDir ;
	private final Drive service ;
	private final DriveDirectory destDir ;
	
	private final boolean overwrite ;
	
	public UploadDirectoryDriveTask(Drive service, DriveDirectory destDir, Path srcDir, boolean overwrite, StopRequester stopRequester, HasStatusReporter statusReporter) {
		super(stopRequester, statusReporter);
		if (srcDir == null || org.apache.commons.lang3.StringUtils.isEmpty(srcDir.toString())) {
			throw new IllegalArgumentException ("Ths source directory cannot be null") ;
		}
		if (service == null) {
			throw new IllegalArgumentException ("Ths drive cannot be null") ;
		}
		
		if (destDir == null || org.apache.commons.lang3.StringUtils.isEmpty(destDir.getTitle())) {
			this.destDirTitle = null ;
		} else {
			this.destDirTitle = destDir.getTitle() ;
		}
		this.destDir = destDir ;
		this.srcDir = srcDir ;
		this.service = service ;
		this.overwrite = overwrite ;
	}
	
	
	private boolean isSameTaskAs (UploadDirectoryDriveTask task) {
		return destDirTitle.equals(task.destDirTitle) && srcDir.equals(task.srcDir) ;
	}
	
	
	@Override
	public boolean isSameTaskAs(
			DriveTask<DriveOperations.OperationResult> task) {
		
		if (task == null || !(task instanceof UploadDirectoryDriveTask)) {
			return false;
		} else {
			return isSameTaskAs ((UploadDirectoryDriveTask)task) ;
		}
	}
	

	@Override
	public DriveOperations.OperationResult call() throws Exception {
		
		DriveOperations.OperationResult res = null ;
		try {
			res = DriveOperations.uploadDirectory (service, destDir, srcDir, overwrite, this.getStopRequester(), this.getStatusReporter()) ;
		} catch (Throwable e) {
			logger.error("Error occurred while task was being performed", e);
			throw new ExecutionException (e) ;
		}
		return res ;
	}
}
