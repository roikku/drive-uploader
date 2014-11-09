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

import java.io.IOException;

import io.uploader.drive.config.HasConfiguration;
import io.uploader.drive.gui.controller.ProxySettingViewController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxySettingDialog extends AbstractDialog {
	
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ProxySettingDialog.class);
	
	
	public ProxySettingDialog (Stage owner, HasConfiguration config) throws IOException {
		super (owner) ;

		initStyle(StageStyle.UTILITY);
		setTitle("Proxy Setting");
		initOwner(owner);
		initModality (Modality.WINDOW_MODAL );
		
		final FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProxySettingView.fxml"));
		final Parent parent = (Parent) loader.load();
		final ProxySettingViewController controller = loader.<ProxySettingViewController> getController();

		controller.setConfiguration(config);
		
		setMinHeight(300.0);
		setMinWidth(400.0);
	
		setHeight(280.0);
		setWidth(720.0);

		setScene(new Scene(parent));
	}
}
