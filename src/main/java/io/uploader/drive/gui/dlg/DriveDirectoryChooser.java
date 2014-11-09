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

import io.uploader.drive.gui.controller.DriveDirectoryChooserViewController;
import io.uploader.drive.util.Callback;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

// https://gist.github.com/jewelsea/5174074

public class DriveDirectoryChooser extends AbstractDialog {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory
			.getLogger(DriveDirectoryChooser.class);

	public DriveDirectoryChooser(Stage owner, Drive service, Callback<File> callback) throws IOException {
		super(owner);

		if (service == null) {
			throw new IllegalArgumentException("The drive cannot be null");
		}
		
		initStyle(StageStyle.UTILITY);
		initOwner(owner);
		setTitle("Directory Chooser");
		initModality (Modality.WINDOW_MODAL );

		final FXMLLoader loader = new FXMLLoader(getClass().getResource(
				"/fxml/DriveDirectoryChooserView.fxml"));
		final Parent parent = (Parent) loader.load();
		final DriveDirectoryChooserViewController controller = loader
				.<DriveDirectoryChooserViewController> getController();

		controller.setDrive(service);
		controller.setCallback(callback);
		controller.loadTreeNode(null);

		/*
		parent.addEventFilter(MouseEvent.MOUSE_CLICKED,
				new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent t) {

						logger.info("Mouse event in tree");
					}
				});*/

		setMinHeight(300.0);
		setMinWidth(250.0);
		
		setHeight(410.0);
		setWidth(260.0);
		
		setScene(new Scene(parent));
	}
}
