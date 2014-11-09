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

package io.uploader.drive.util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class ThreadUtils {

	private static final Logger logger = LoggerFactory.getLogger(ThreadUtils.class);
	
	private ThreadUtils () { super () ; throw new IllegalStateException () ; }
	
	public static void shutdownExecutor(ExecutorService executor) {
		Preconditions.checkNotNull(executor) ;
		logger.info("Shutdown executors") ;
		executor.shutdown() ;
		try {
			executor.awaitTermination(2, TimeUnit.SECONDS) ;
		} catch (InterruptedException e) {
			logger.error("Error occurred while awaiting threads termination", e) ;
		}
		List<Runnable> notStarted = executor.shutdownNow() ;
		if (!notStarted.isEmpty()) {
			logger.info("Unstarted tasks stopped (" + notStarted.size() + " tasks in total)") ;
		}
	}
}
