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

package io.uploader.drive.drive.largefile;

import java.io.IOException;

import io.uploader.drive.config.HasConfiguration;
import io.uploader.drive.drive.DriveUtils.HasId;
import io.uploader.drive.drive.DriveUtils.HasMimeType;
import io.uploader.drive.util.FileUtils.InputStreamProgressFilter.StreamProgressCallback;

public class GDriveUpdater extends GDriveUpload {

	public GDriveUpdater(HasConfiguration config, HasId fileId,
			HasMimeType mimeType, String filename,
			StreamProgressCallback progressCallback) {
		super(config, fileId, mimeType, filename, progressCallback);
	}
	
	public String updateFile() throws IOException {
		return uploadFile (true) ;
	}
}
