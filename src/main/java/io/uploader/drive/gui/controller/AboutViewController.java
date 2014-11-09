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

import io.uploader.drive.config.Configuration;
import io.uploader.drive.gui.util.UiUtils;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AboutViewController implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(AboutViewController.class);

	@FXML Text titleField ;
	@FXML ImageView imgField ;
	@FXML Hyperlink link ;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		StringBuilder title = new StringBuilder () ;
		title.append(Configuration.INSTANCE.getAppName()) ;
		title.append(" (") ;
		title.append(Configuration.INSTANCE.getAppVersion()) ;
		title.append(")") ;
		
		titleField.setText(title.toString());
		imgField.setImage(new Image( getClass().getResourceAsStream("/images/DriveUploader64.png")));
	}
	
	@FXML
	protected void onLink(ActionEvent event) {
		
        URI uri = null ;
		try {
			uri = new URI("http://drive.uploader.io");
		} catch (URISyntaxException e) {
			logger.error("URL seems to be ill-formed", e);
		}
		open (uri) ;
	}
	
	private static void open(URI uri) {
		if (Desktop.isDesktopSupported()) 
		{
			try {
				Desktop.getDesktop().browse(uri);
			} catch (IOException e) {

			}
		} 
	}

	@FXML
	protected void onClose(ActionEvent event) {
		UiUtils.closeDialog (event) ;
	}
}
