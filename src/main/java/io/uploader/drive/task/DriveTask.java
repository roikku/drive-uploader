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

import io.uploader.drive.drive.DriveOperations.HasStatusReporter;
import io.uploader.drive.drive.DriveOperations.StopRequester;

import java.util.concurrent.Callable;

public abstract class DriveTask<T> implements Callable<T> {
	
	private final HasStatusReporter statusReporter ; 
	private final StopRequester stopRequester ; 
	
	public abstract boolean isSameTaskAs (DriveTask<T> task) ;
	
	public DriveTask (StopRequester stopRequester, HasStatusReporter statusReporter) {
		super () ;
		this.statusReporter = statusReporter ;
		this.stopRequester = stopRequester ;
	}

	public HasStatusReporter getStatusReporter() {
		return statusReporter;
	}

	public StopRequester getStopRequester() {
		return stopRequester;
	}
}
