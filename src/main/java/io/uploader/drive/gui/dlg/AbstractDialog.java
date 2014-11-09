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

import com.google.common.base.Preconditions;

import io.uploader.drive.gui.util.UiUtils;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public abstract class AbstractDialog {

	private final Stage dialog = new Stage();
	private Scene scene = null ;
	private final Stage owner;
	
	public AbstractDialog (Stage owner) {
		super () ;
		this.owner = owner ;
		dialog.setMaxHeight(Double.MAX_VALUE);
		dialog.setMaxWidth(Double.MAX_VALUE);
	}
	
	protected void initStyle(StageStyle style) {
		dialog.initStyle(style);
	}
	
	protected void setTitle(String title) {
		dialog.setTitle(title);
	}
	
	protected void initOwner(Window owner) {
		dialog.initOwner(owner);
	}
	
	protected void initModality (Modality modality) {
		dialog.initModality(modality);
	}

	protected void setMinHeight(double d) {
		dialog.setMinHeight(d);
	}
	
	protected void setMinWidth(double d) {
		dialog.setMinWidth(d);
	}
	
	protected void setMaxHeight(double d) {
		dialog.setMaxHeight(d);
	}
	
	protected void setMaxWidth(double d) {
		dialog.setMaxWidth(d);
	}

	protected void setHeight(double d) {
		dialog.setHeight(d);
	}
	
	protected void setWidth(double d) {
		dialog.setWidth(d);
	}
	
	protected void setScene (Scene scene) {
		this.scene = scene ;
	}
	
	public final void showDialog() {
		Preconditions.checkNotNull(scene) ;
		dialog.setScene(scene);
		UiUtils.centerDialog(dialog, owner);
		dialog.show();
	}
}
