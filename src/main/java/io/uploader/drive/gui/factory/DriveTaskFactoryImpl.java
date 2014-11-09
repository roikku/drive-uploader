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

package io.uploader.drive.gui.factory;

import java.nio.file.Paths;
import java.util.Observable;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import io.uploader.drive.AppEvent;
import io.uploader.drive.drive.DriveDirectory;
import io.uploader.drive.drive.DriveOperations;
import io.uploader.drive.drive.DriveOperations.HasStatusReporter;
import io.uploader.drive.drive.DriveOperations.StopRequester;
import io.uploader.drive.task.DriveTask;
import io.uploader.drive.task.UploadDirectoryDriveTask;
import io.uploader.drive.util.ThreadUtils;

import com.google.api.services.drive.Drive;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class DriveTaskFactoryImpl implements DriveTaskFactory {

	final private static Logger logger = LoggerFactory.getLogger(DriveTaskFactoryImpl.class);
	
	private final int threadPoolSize = 3 ;
	
	private final Drive client;
	private ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(threadPoolSize));

	public DriveTaskFactoryImpl(Drive client) {
		super();
		this.client = client;
	}

	@Override
	public DriveTask<DriveOperations.OperationResult> buildUploadDirectoryDriveTask(
			DriveDirectory driveDirectory, String srcDirectory, boolean overwrite,
			StopRequester stopRequester, HasStatusReporter statusReporter) {
		
		return new UploadDirectoryDriveTask(client, driveDirectory,
				Paths.get(srcDirectory), overwrite, stopRequester, statusReporter);
	}

	@Override
	public HasStatusReporter buildStatusReporter(final DoubleProperty total,
			final DoubleProperty current, final StringProperty status) {

		return new DriveOperations.HasStatusReporter() {
			
			@Override
			public void setStatus(String str) {
				if (status == null) {
					return ;
				}
				status.set(str);
			}
			
			@Override
			public void setTotalProgress(double p) {
				if (total == null) {
					return ;
				}
				total.set(p);
			}

			@Override
			public void setCurrentProgress(double p) {
				if (current == null) {
					return ;
				}
				current.set(p);
			}
		};
	}

	@Override
	public ListeningExecutorService getExecutor () {
		return executor ;
	}

	@Override
	public StopRequester buildStopRequester(final BooleanProperty stopRequested) {

		return new DriveOperations.StopRequester() {
			
			@Override
			public boolean isStopRequested() {
				if (Thread.interrupted()) {
					logger.info("Thread has been interrupted") ;
					return true ;
				} else if (executor.isShutdown()) {
					logger.info("Executor has been shutdown") ;
					return true ;
				}
				return stopRequested.get();
			}
		};
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg != null) {
			if (arg instanceof AppEvent.Event) {
				if (((AppEvent.Event)arg) == AppEvent.Event.EXIT)
				{
					logger.info("Exit Event") ;
					shutdown() ;
				}
			}
		}
	}
	
	private void shutdown() {
		ThreadUtils.shutdownExecutor(executor) ;
	}
}
