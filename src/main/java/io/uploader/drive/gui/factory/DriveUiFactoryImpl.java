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

import io.uploader.drive.gui.dlg.DriveDirectoryChooser;
import io.uploader.drive.util.Callback;

import java.io.IOException;

import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

public class DriveUiFactoryImpl implements DriveUiFactory {

	private static final Logger logger = LoggerFactory.getLogger(DriveUiFactoryImpl.class);
	
	private final Drive service;
	
	public DriveUiFactoryImpl(Drive service) {
		super();
		this.service = service;
	}

	@Override
	public DriveDirectoryChooser buildDriveDirectoryChooser(Stage owner, Callback<File> callback) {
		try {
			return new DriveDirectoryChooser (owner, service, callback) ;
		} catch (IOException e) {
			logger.error("Error occurred while creating drive directory chooser dialog", e) ;
			if (callback != null) {
				callback.onFailure(e) ;
			}
		}
		return null ;
	}
}
