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

package io.uploader.drive.gui.model;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.uploader.drive.drive.DriveDirectory;
import io.uploader.drive.drive.DriveOperations;
import io.uploader.drive.drive.DriveOperations.HasStatusReporter;
import io.uploader.drive.drive.DriveOperations.StopRequester;
import io.uploader.drive.gui.factory.DriveTaskFactory;
import io.uploader.drive.task.DriveTask;
import io.uploader.drive.util.Callback;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;

public class DriveTaskModel {

	private static final Logger logger = LoggerFactory.getLogger(DriveTaskModel.class);
	
	private final DoubleProperty totalProgress = new SimpleDoubleProperty ();
	private final DoubleProperty currentProgress = new SimpleDoubleProperty ();
	private final StringProperty status = new SimpleStringProperty ("Waiting...");
	private final DriveTask<DriveOperations.OperationResult> driveTask;
	private ListenableFuture<DriveOperations.OperationResult> result = null ;

	private final DriveDirectory driveDir ;
	private final String srcDir ;
	
	private final BooleanProperty stopRequested = new SimpleBooleanProperty (false);
	
	private final DriveTaskFactory taskFactory ;
	
	public DriveTaskModel (DriveTaskFactory taskFactory, DriveDirectory driveDir, String srcDir, boolean overwrite) {
		super () ;
		this.taskFactory = taskFactory ;
		HasStatusReporter statusReporter = taskFactory.buildStatusReporter(totalProgress, currentProgress, status) ;
		StopRequester stopRequester = taskFactory.buildStopRequester (stopRequested) ;
		this.driveTask = taskFactory.buildUploadDirectoryDriveTask(driveDir, srcDir, overwrite, stopRequester, statusReporter) ;
		
		this.driveDir = driveDir ;
		this.srcDir = srcDir ;
	}
	
	public boolean hasTheSameTaskAs (DriveTaskModel model) {
		if (model == null) {
			return false ;
		}
		return driveTask.isSameTaskAs(model.driveTask) ;
	}
	
	public synchronized DriveOperations.OperationResult getResult () throws InterruptedException, ExecutionException {
		if (result == null) {
			return null ;
		}
		return result.get() ;
	}
	
	public synchronized void start (final Callback<DriveOperations.OperationResult> callback) {
		//if (driveTask == null) {
		//	throw new IllegalStateException () ;
		//}
		Preconditions.checkNotNull(driveTask) ;
		result = taskFactory.getExecutor().submit(driveTask) ;
		Futures.addCallback(result, new FutureCallback<DriveOperations.OperationResult>() {

			public void onSuccess(DriveOperations.OperationResult result) {
				logger.info("Task is finished.");
				if (callback != null) {
					callback.onSuccess(result);
				}
			}

			public void onFailure(Throwable thrown) {
				logger.error("Error occurred while task was being performed", thrown);
				if (callback != null) {
					callback.onFailure(thrown);
				}
			}
		});
	}
	
	public synchronized boolean isDone () {
		//Preconditions.checkNotNull(result) ;
		return ((result == null)? (false) : (result.isDone())) ;
	}
	
	public void stop () {
		stopRequested.set(true);
	}
	
	public double getTotalProgress() {
		return totalProgress.get();
	}
	
	public DoubleProperty totalProgressProperty() {
		return totalProgress;
	}
	
	public final void setTotalProgress(double p) {
		totalProgress.set(p);
	}

	public final void setCurrentProgress(double p) {
		currentProgress.set(p);
	}
	
	public double getCurrentProgress() {
		return currentProgress.get();
	}
	
	public DoubleProperty currentProgressProperty() {
		return currentProgress;
	}
	
	public String getStatus() {
		return status.get();
	}
	
	public StringProperty statusProperty() {
		return status;
	}
	
	public final void setStatus(String s) {
		status.set(s);
	}
	
	public boolean getStopRequested() {
		return stopRequested.get();
	}
	
	public BooleanProperty stopRequestedProperty() {
		return stopRequested;
	}
	
	public final void setStopRequested(boolean b) {
		stopRequested.set(b);
	}

	public DriveTask<DriveOperations.OperationResult> getDriveTask() {
		return driveTask;
	}

	public DriveDirectory getDriveDir() {
		return driveDir;
	}

	public String getSrcDir() {
		return srcDir;
	}
}
