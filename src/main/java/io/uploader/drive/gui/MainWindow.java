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

package io.uploader.drive.gui;


import java.io.IOException;

import io.uploader.drive.AppEvent;
import io.uploader.drive.DriveUploader;
import io.uploader.drive.config.HasConfiguration;
import io.uploader.drive.gui.controller.DriveTaskPanelViewController;
import io.uploader.drive.gui.controller.MainMenuController;
import io.uploader.drive.gui.factory.DriveTaskFactoryImpl;
import io.uploader.drive.gui.factory.DriveUiFactoryImpl;
import io.uploader.drive.gui.util.UiUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.drive.Drive;

public class MainWindow {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
	
	private final Stage stage ;
	
	public MainWindow(Drive client, Stage stage, AppEvent appEvent, HasConfiguration config) throws IOException {
		super();
		if (client == null) {
			throw new IllegalArgumentException () ;
		}
		this.stage = stage;
		
		stage.setTitle("Drive Uploader");
		
		AnchorPane mainFrame = (AnchorPane)FXMLLoader.load(getClass().getResource("/fxml/MainFrame.fxml"));
		Scene scene = new Scene(mainFrame);
		
		FXMLLoader mainMenuLoader = new FXMLLoader(getClass().getResource("/fxml/MainMenu.fxml")); 
		VBox mainMenuBar = (VBox)mainMenuLoader.load() ;
		AnchorPane.setTopAnchor(mainMenuBar, 0.0);
		AnchorPane.setLeftAnchor(mainMenuBar, 0.0);
		AnchorPane.setRightAnchor(mainMenuBar, 0.0);

		FXMLLoader driveTaskPanelViewLoader = new FXMLLoader(getClass().getResource("/fxml/DriveTaskPanelView.fxml")); 
		Pane mainPanel = (Pane)driveTaskPanelViewLoader.load();
		AnchorPane.setTopAnchor(mainPanel, (DriveUploader.isMacOsX()) ? (10.0) : (30.0));
		AnchorPane.setLeftAnchor(mainPanel, 10.0);
		AnchorPane.setRightAnchor(mainPanel, 10.0);
		AnchorPane.setBottomAnchor(mainPanel, 10.0);
		
		((AnchorPane) scene.getRoot()).getChildren().addAll(mainMenuBar, mainPanel);
		
		UiUtils.setStageAppSize(stage) ;
		
		final DriveTaskFactoryImpl taskFactory = new DriveTaskFactoryImpl (client) ;
		appEvent.addObserver(taskFactory) ;
		
		DriveTaskPanelViewController driveTaskPanelViewController = driveTaskPanelViewLoader.<DriveTaskPanelViewController>getController();
		driveTaskPanelViewController.setTaskFactory(taskFactory);
		driveTaskPanelViewController.setDriveUiFactory(new DriveUiFactoryImpl (client)) ;
		
		MainMenuController mainMenuController = mainMenuLoader.<MainMenuController>getController();
		mainMenuController.setOwner(stage) ;
		mainMenuController.setConfiguration(config) ;
		
		stage.setScene(scene);
	}

	public void show () {
		stage.show() ;
	}
}
