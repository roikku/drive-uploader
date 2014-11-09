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

package io.uploader.drive.gui.dlg;

import io.uploader.drive.gui.controller.TaskAddPanelViewController;
import io.uploader.drive.gui.factory.DriveTaskFactory;
import io.uploader.drive.gui.factory.DriveUiFactory;
import io.uploader.drive.gui.model.DriveTaskModel;
import io.uploader.drive.util.Callback;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskAddDialog extends AbstractDialog {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(TaskAddDialog.class);
	
	public TaskAddDialog(Stage owner, final Callback<DriveTaskModel> callback, DriveTaskFactory taskFactory, DriveUiFactory driveUiFactory) throws IOException {
		super(owner);

		initStyle(StageStyle.UTILITY);
		setTitle("Task Definition");
		
		final FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TaskAddPanelView.fxml"));
		final Parent parent = (Parent) loader.load();
		final TaskAddPanelViewController controller = loader
				.<TaskAddPanelViewController> getController();

		controller.setDriveUiFactory(driveUiFactory);
		controller.setTaskFactory(taskFactory);
		controller.setCallback(callback);

		setMinHeight(300.0);
		setMinWidth(400.0);
	
		setHeight(300.0);
		setWidth(600.0);
		
		initOwner(owner) ;
		
		setScene (new Scene(parent));
	}
}
