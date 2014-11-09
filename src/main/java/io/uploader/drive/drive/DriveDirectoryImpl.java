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


public class DriveDirectoryImpl implements DriveDirectory {

	private final String title ;
	private final String id  ;
	
	public DriveDirectoryImpl (String title) {
		this (title, null) ;
	}
	
	public DriveDirectoryImpl (String title, String id) {
		super () ;
		if (org.apache.commons.lang3.StringUtils.isEmpty(title)) {
			throw new IllegalArgumentException () ;
		}
		this.title = title ;
		this.id = id ;
	}
	
	public static DriveDirectoryImpl newDriveDirectory (String title, String id) {
		return new DriveDirectoryImpl (title, id) ;
	}
	
	public static DriveDirectoryImpl newDriveDirectory (String title) {
		return new DriveDirectoryImpl (title) ;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getTitle() {
		return title;
	}
}
