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

import io.uploader.drive.AppEvent;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Service;

public abstract class ObserverService <T> extends Service <T> implements Observer {

	private static final Logger logger = LoggerFactory.getLogger(ObserverService.class);
	
	public ObserverService () {
		this (5) ;
	}
	
	public ObserverService (int threadPoolSize) {
		super () ;
		if (threadPoolSize <= 0) {
			throw new IllegalArgumentException () ;
		}
		setExecutor(Executors.newFixedThreadPool(threadPoolSize)) ;
	}
	
	@Override
	public void update(Observable o, Object arg) {

		if (arg != null) {
			if (arg instanceof AppEvent.Event) {
				if (((AppEvent.Event)arg) == AppEvent.Event.EXIT)
				{
					logger.info("Exit Event") ;
					cancel() ;
					Executor exe = getExecutor() ;
					if (exe != null && exe instanceof ExecutorService) {
						ThreadUtils.shutdownExecutor((ExecutorService) exe) ;
					}
				}
			}
		}
	}
}
