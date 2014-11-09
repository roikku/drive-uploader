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

import io.uploader.drive.drive.DriveUtils;
import io.uploader.drive.gui.dlg.MessageDialogs;
import io.uploader.drive.gui.util.UiUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class DriveDirectoryChooserViewController implements Initializable {

	private static final Logger logger = LoggerFactory
			.getLogger(DriveDirectoryChooserViewController.class);

	@FXML
	private TreeView<File> driveTreeView;

	private Drive drive = null;
	private io.uploader.drive.util.Callback<File> callback = null ;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		driveTreeView.setShowRoot(false);
		driveTreeView.setEditable(false);
		driveTreeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		driveTreeView
				.setCellFactory(new Callback<TreeView<File>, TreeCell<File>>() {
					@Override
					public TreeCell<File> call(TreeView<File> p) {
						return new TreeCell<File>() {

							@Override
							protected void updateItem(File file, boolean empty) {
								super.updateItem(file, empty);

								if (empty) {
									setText(null);
									setGraphic(null);
								} else {
									Node graphic  = new ImageView(new Image(getClass().getResourceAsStream("/icons/folder.png")));
									setText(getItem() == null ? "" : getItem().getTitle());
			                        setGraphic(graphic);
			                        setContentDisplay(ContentDisplay.LEFT);
								}
							}
						};
					}
				});

	}
	
	@FXML
	protected void onCancel(ActionEvent event) {
		UiUtils.closeDialog (event) ;
	}
	
	@FXML
	protected void onSelect(ActionEvent event) {
		checkController() ;
		TreeItem<File> selected = driveTreeView.getSelectionModel().getSelectedItem() ;
		if (selected == null) {
			MessageDialogs.showMessageDialog(UiUtils.getStage(event), "A directory must be selected", "Error", MessageDialogs.MessageType.ERROR);
			return ;
		} else {
			callback.onSuccess(selected.getValue());
			UiUtils.closeDialog(event);
		}
	}

	private void checkController() {
		if (drive == null) {
			throw new IllegalStateException(
					"The Drive must be set before using the controller");
		}
		if (callback == null) {
			throw new IllegalStateException(
					"The callback must be set before using the controller");
		}
	}

	public void loadTreeNode(File node) {

		checkController();

		if (node == null) {
			TreeItem<File> root = createNode (null);
			try {
				FileList dirs = DriveUtils.findDirectories(drive, null, null);
				for (File dir : dirs.getItems()) {
					root.getChildren().add(createNode(dir));
				}
				driveTreeView.setRoot(root);
			} catch (IOException e) {
				logger.error("Error occurred while loading the tree view", e);
			}
		}
	}
	
	
	private TreeItem<File> createNode (File dir) {
		TreeItem<File> treeItem = null ;
		if (dir == null) {
			treeItem = new TreeItem<File>();
			treeItem.setExpanded(true);
		} else {
			treeItem = new TreeItem<File>(dir) {

				private Boolean isLeaf = null ;
				
				@Override
				public boolean isLeaf() {
					if (isLeaf != null) {
						return isLeaf.booleanValue() ;
					}
					try {
						FileList children = DriveUtils.findDirectories (drive, DriveUtils.newId(dir.getId()), Integer.valueOf(1)) ;
						isLeaf = Boolean.valueOf(children.getItems() == null || children.getItems().isEmpty()) ;
						return isLeaf.booleanValue() ;
					} catch (IOException e) {
						logger.error("Error occurred while getting children list", e);
					}
					return true;
				}} ;
			treeItem.setExpanded(false);
		}
		treeItem.addEventHandler(TreeItem.branchExpandedEvent (), new EventHandler<TreeModificationEvent<File>>() {

			@Override
			public void handle(TreeModificationEvent<File> arg0) {
				if (TreeItem.branchExpandedEvent ().equals(arg0.getEventType())) {
					logger.info("Branch expanded");
					TreeItem<File> node = arg0.getTreeItem() ;
					File nodeFile = node.getValue() ;
					if (node.getChildren().isEmpty()) {
						try {
							FileList children = DriveUtils.findDirectories (drive, DriveUtils.newId(nodeFile.getId()), null) ;
							if (children != null && children.getItems() != null) {
						        for (File child : children.getItems()) {
						        	node.getChildren().add(createNode(child));
						        }
							}
						} catch (IOException e) {
							logger.error("Error occurred while expanding the tree", e);
						}
					}
				}
			}
		});
		return treeItem ;
	}
	

	public void setDrive(Drive drive) {
		this.drive = drive;
	}

	public void setCallback (io.uploader.drive.util.Callback<File> callback) {
		this.callback = callback ;
	}
}
