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

import io.uploader.drive.DriveUploader;
import io.uploader.drive.config.HasConfiguration;
import io.uploader.drive.gui.dlg.AboutDialog;
import io.uploader.drive.gui.dlg.MessageDialogs;
import io.uploader.drive.gui.dlg.MessageDialogs.Response;
import io.uploader.drive.gui.dlg.ProxySettingDialog;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainMenuController implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(MainMenuController.class);

	@FXML
	private MenuBar menuBar;
	
	@FXML
	private Menu fileMenu ;
	
	@FXML
	private Menu accountMenu ;
	
	private Stage owner ;
	private HasConfiguration config ;

	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		menuBar.setFocusTraversable(true);
		menuBar.setUseSystemMenuBar(true);
		
		if (DriveUploader.isMacOsX()) {
			fileMenu.setVisible(false);
		}
	}

	
	@FXML
	private void onProxySetting (final ActionEvent event) {
		try {
			ProxySettingDialog dlg = new ProxySettingDialog (owner, config) ;
			dlg.showDialog();
		} catch (IOException e) {
			logger.error("Error occurred while opening the proxy setting dialog", e);
		}
	}
	
	
	@FXML
	private void onForgetAccount(final ActionEvent event) {

		Preconditions.checkNotNull(config) ;
		if (Response.YES == MessageDialogs.showConfirmDialog(owner, "Are you sure?", "Confirmation")) {
			StringBuilder sb = new StringBuilder () ;
			sb.append(config.getDataStoreDirectory()) ;
			sb.append("StoredCredential") ;
			try {
				if (Files.deleteIfExists(Paths.get(sb.toString()))) {
					MessageDialogs.showMessageDialog(owner, "You will be required to login again the next time you launch the application", "Notification", MessageDialogs.MessageType.INFO) ;
				}
			} catch (IOException e) {
				logger.error("Error occurred while removing account data", e) ;
			}		
		}
	}

	
	@FXML
	private void onAbout(final ActionEvent event) {
		showAboutDialog () ;
	}
	
	
	private void showAboutDialog () {
		AboutDialog dlg;
		try {
			dlg = new AboutDialog (owner);
			dlg.showDialog();
		} catch (IOException e) {
			logger.error("Error occurred while opening the About dialog", e);
		}
	}
	
	
	@FXML
	private void onExit(final ActionEvent event) {
		if (owner != null) {
			owner.fireEvent(new WindowEvent(owner, WindowEvent.WINDOW_CLOSE_REQUEST)); 
		} else {
			// should not happen!
			logger.warn("The owner of the main menu has not been set");
			System.exit(0);
		}
	}
	
	
	@FXML
	private void handleKeyInput(final InputEvent event) {
		if (event instanceof KeyEvent) {
			final KeyEvent keyEvent = (KeyEvent) event;
			if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.A) {
				showAboutDialog () ;
			}
		}
	}
	
	
	public void setOwner (Stage owner) {
		this.owner = owner ;
	}
	
	
	public void setConfiguration (HasConfiguration config) {
		this.config = config ;
	}
	
	
	public void hideAccountMenu (boolean b) {
		accountMenu.setVisible(!b);
	}
}
