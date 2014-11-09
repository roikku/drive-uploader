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

package io.uploader.drive.gui.util;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;

public class UiUtils {

	private UiUtils () { super () ; throw new IllegalStateException () ; } 
	
	
	public static void closeDialog (ActionEvent event) {
		Stage stage = UiUtils.getStage (event) ;
		if (stage == null) {
			throw new IllegalStateException () ;
		}
		if (stage.getOnCloseRequest() != null) {
			stage.getOnCloseRequest().handle(null) ;
		}
		stage.close();
	}
	
	
	public static void centerDialog (Stage dlg, Stage owner) {
		if (dlg == null) {
			throw new IllegalArgumentException () ;
		}
		if (owner == null) {
			dlg.centerOnScreen() ;
		} else {
			dlg.setX(owner.getX() + owner.getWidth() / 2 - dlg.getWidth() / 2);
			dlg.setY(owner.getY() + owner.getHeight() / 2 - dlg.getHeight() / 2);
		}
	}
	
	
	public static Stage getStage (ActionEvent event) {
		Node source = (Node) event.getSource();
		if (source == null) {
			return null ;
		} 
		if (source.getScene() == null) {
			return null ;
		}
		return (Stage) source.getScene().getWindow();
	}
	
	
	public static void setStageAppSize (Stage stage) {
		if (stage == null) {
			throw new IllegalArgumentException () ;
		}
		stage.setMinHeight(320.0);
		stage.setMinWidth(530.0);

    	stage.setWidth(850);
    	stage.setHeight(680);
	}
}
