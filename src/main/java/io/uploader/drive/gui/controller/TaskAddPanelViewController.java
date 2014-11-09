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

package io.uploader.drive.gui.controller;

import io.uploader.drive.drive.DriveDirectoryImpl;
import io.uploader.drive.gui.dlg.DriveDirectoryChooser;
import io.uploader.drive.gui.dlg.MessageDialogs;
import io.uploader.drive.gui.factory.DriveTaskFactory;
import io.uploader.drive.gui.factory.DriveUiFactory;
import io.uploader.drive.gui.model.DriveTaskModel;
import io.uploader.drive.gui.util.UiUtils;
import io.uploader.drive.util.Callback;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.drive.model.File;

public class TaskAddPanelViewController implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(TaskAddPanelViewController.class);

	@FXML private TextField srcField ;
	@FXML private TextField destField ;
	
	@FXML private CheckBox checkBoxOverwrite ;
	
	private DriveTaskFactory taskFactory = null ;
	private DriveUiFactory driveUiFactory = null ;
	
	DriveDirectoryImpl drivedir = null ;

	private Callback<DriveTaskModel> callback = null;
	
	
	private void checkController () {
		if (taskFactory == null) {
			throw new IllegalStateException ("The DriveTaskFactory must be set before using the controller") ;
		}
		if (driveUiFactory == null) {
			throw new IllegalStateException ("The DriveUiFactory must be set before using the controller") ;
		}
		if (callback == null) {
			throw new IllegalStateException ("The Callback must be set before using the controller") ;
		}
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		srcField.setEditable(false);
		destField.setEditable(false);
		
		destField.setText("Backups");
		drivedir = new DriveDirectoryImpl (destField.getText()) ;
	}


	@FXML
	protected void onAdd(ActionEvent event) {

		String srcDir = srcField.getText().trim() ;
		String destDir = destField.getText().trim() ;
		boolean overwrite = checkBoxOverwrite.isSelected() ;
		
		if (StringUtils.isEmpty(srcDir)) {
			MessageDialogs.showMessageDialog(UiUtils.getStage(event), "A source folder must be specified", "Error", MessageDialogs.MessageType.ERROR);
			return ;
		} 
		if (StringUtils.isEmpty(destDir)) {
			MessageDialogs.showMessageDialog(UiUtils.getStage(event), "A destination folder must be specified", "Error", MessageDialogs.MessageType.ERROR);
			return ;
		} 
		
		// if overwrite ask confirmation
		if (overwrite) {
			if (MessageDialogs.Response.NO == MessageDialogs.showConfirmDialog(UiUtils.getStage(event), "Are you sure you want to overwrite existing files?", "Confirmation", MessageDialogs.MessageType.WARNING)) {
				return ;
			}
		}
		
      	DriveTaskModel driveTaskModel = new DriveTaskModel (taskFactory, drivedir, srcDir, overwrite) ;
      	callback.onSuccess(driveTaskModel) ;
      	
    	//close the dialog
    	UiUtils.closeDialog (event) ;
	}

	
	@FXML
	protected void onCancel(ActionEvent event) {
    	//close the dialog
		UiUtils.closeDialog (event) ;
	}
	
	@FXML
	protected void onSelectSourceDirectory(ActionEvent event) {

        DirectoryChooser dirChooser = new DirectoryChooser () ;
        dirChooser.setTitle("Select Directory to Upload");
        java.io.File dir = dirChooser.showDialog(UiUtils.getStage(event));
        if (dir != null) {
        	srcField.setText(dir.getPath());
        }
	}
	
	@FXML
	protected void onChangeDestinationDirectory(ActionEvent event) {

		checkController () ;
		DriveDirectoryChooser driveChooser = driveUiFactory
				.buildDriveDirectoryChooser(UiUtils.getStage(event), new Callback<File> () {

					@Override
					public void onSuccess(File result) {
						if (result != null) {
							destField.setText(result.getTitle());
							drivedir = DriveDirectoryImpl.newDriveDirectory(result.getTitle(), result.getId()) ;
						}
					}

					@Override
					public void onFailure(Throwable cause) {
						logger.error("Error occurred while selecting drive directory", cause);
					}});
		driveChooser.showDialog() ;
	}
	
	
	public void setTaskFactory (DriveTaskFactory taskFactory) {
		this.taskFactory = taskFactory ;
	}
	
	
	public void setDriveUiFactory (DriveUiFactory driveUiFactory) {
		this.driveUiFactory = driveUiFactory ;
	}
	
	
	public void setCallback (Callback<DriveTaskModel> callback) {
		this.callback = callback ;
	}
	
	//public void setTaskList (ObservableList<DriveTaskModel> taskList) {
	//	this.taskList = taskList ;
	//}
}
