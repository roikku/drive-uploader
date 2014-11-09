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

import io.uploader.drive.drive.DriveDirectory;
import io.uploader.drive.drive.DriveOperations.OperationResult;

import java.nio.file.Path;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;

public class ErrorModel {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ErrorModel.class);
	
	private final Path srcPath;  
	private final DriveDirectory destDir; 
	private final Throwable err ; 
	private final OperationResult.HasWarning warn ; 
	
	public enum ErrorLevel {
		ERROR,
		WARNING,
	}
	
	private final StringProperty errLevelStr = new SimpleStringProperty ();
	private final StringProperty srcPathStr = new SimpleStringProperty ();
	private final StringProperty destDirStr = new SimpleStringProperty ();
	private final StringProperty errStr = new SimpleStringProperty ();

	public final static ImageView newWarningIcon () {
		return new ImageView (new Image(ErrorModel.class.getResourceAsStream("/icons/Warning16.png"))) ;
	}
	public final static ImageView newErrorIcon () {
		return new ImageView (new Image(ErrorModel.class.getResourceAsStream("/icons/Error16.png"))) ;
	}
	
	private ErrorModel(Path srcPath, DriveDirectory destDir, Throwable err, OperationResult.HasWarning warn) {
		super();
		this.srcPath = srcPath;
		this.destDir = destDir;
		this.err = err;
		this.warn = warn ;
		
		srcPathStr.set(srcPath.toString());
		destDirStr.set(destDir.getTitle());
	}
	
	
	public ErrorModel(Path srcPath, DriveDirectory destDir, Throwable err) {
		this(Preconditions.checkNotNull(srcPath), Preconditions.checkNotNull(destDir), Preconditions.checkNotNull(err), null);
		errStr.set(err.getMessage());
		errLevelStr.set(ErrorLevel.ERROR.toString());
	}
	
	
	public ErrorModel(Path srcPath, DriveDirectory destDir, OperationResult.HasWarning warn) {
		this(Preconditions.checkNotNull(srcPath), Preconditions.checkNotNull(destDir), null, Preconditions.checkNotNull(warn));
		errStr.set(warn.getWarningMessage());
		errLevelStr.set(ErrorLevel.WARNING.toString());
	}
	
	
	public Path getSrcPath() {
		return srcPath;
	}

	public DriveDirectory getDestDir() {
		return destDir;
	}

	public Throwable getErr() {
		return err;
	}
	
	public OperationResult.HasWarning getWarn () {
		return warn ;
	}


	public String getErrLevelStr() {
		return errLevelStr.get();
	}
	
	public StringProperty errLevelStrProperty() {
		return errLevelStr;
	}
	
	
	public String getSrcPathStr() {
		return srcPathStr.get();
	}
	
	public StringProperty srcPathStrProperty() {
		return srcPathStr;
	}
	
	public final void setSrcPathStr(String s) {
		srcPathStr.set(s);
	}
	
	
	public String getDestDirStr() {
		return destDirStr.get();
	}
	
	public StringProperty destDirStrProperty() {
		return destDirStr;
	}
	
	public final void setDestDirStr(String s) {
		destDirStr.set(s);
	}
	
	
	public String getErrStr() {
		return errStr.get();
	}
	
	public StringProperty errStrProperty() {
		return errStr;
	}
	
	public final void setErrStr(String s) {
		errStr.set(s);
	}
}
