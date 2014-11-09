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

import io.uploader.drive.drive.DriveOperations;
import io.uploader.drive.drive.DriveOperations.OperationResult;
import io.uploader.drive.drive.DriveOperations.OperationResult.HasWarning;
import io.uploader.drive.gui.dlg.ErrorReportDialog;
import io.uploader.drive.gui.dlg.MessageDialogs;
import io.uploader.drive.gui.dlg.TaskAddDialog;
import io.uploader.drive.gui.dlg.MessageDialogs.Response;
import io.uploader.drive.gui.factory.DriveTaskFactory;
import io.uploader.drive.gui.factory.DriveUiFactory;
import io.uploader.drive.gui.model.DriveTaskModel;
import io.uploader.drive.gui.model.ErrorModel;
import io.uploader.drive.gui.util.UiUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

public class DriveTaskPanelViewController implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(DriveTaskPanelViewController.class);
	
	@FXML private TableView<DriveTaskModel> tableTaskView;
	
	@FXML private TableColumn<DriveTaskModel, Double> progressTotalColumn;  
	@FXML private TableColumn<DriveTaskModel, Double> progressCurrentColumn;
	@FXML private TableColumn<DriveTaskModel, String> statusColumn;  
	@FXML private TableColumn<DriveTaskModel, String> optionsColumn;
	      
	private DriveTaskFactory taskFactory = null ;
	private DriveUiFactory driveUiFactory = null ;
	
	
	private void checkController () {
		if (taskFactory == null) {
			throw new IllegalStateException ("The DriveTaskFactory must be set before using the controller") ;
		}
		if (driveUiFactory == null) {
			throw new IllegalStateException ("The DriveUiFactory must be set before using the controller") ;
		}
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		progressTotalColumn.setCellValueFactory(new PropertyValueFactory("totalProgress"));  
		progressTotalColumn.setCellFactory(ProgressBarTableCell.<DriveTaskModel>forTableColumn());  
        
		progressCurrentColumn.setCellValueFactory(new PropertyValueFactory("currentProgress"));  
		progressCurrentColumn.setCellFactory(ProgressBarTableCell.<DriveTaskModel>forTableColumn());  
        
        statusColumn.setCellValueFactory(new PropertyValueFactory("status")); 
        
        optionsColumn.setCellValueFactory(new PropertyValueFactory("options")); 
        optionsColumn.setCellFactory(optionButtonColumnCellFactory);
	}
	
	
	private static class ResultCallback implements io.uploader.drive.util.Callback<DriveOperations.OperationResult> {

		private final DriveTaskModel taskModel ;
		
		public ResultCallback(DriveTaskModel taskModel) {
			super();
			this.taskModel = taskModel;
		}

		@Override
		public void onSuccess(OperationResult result) {
			Preconditions.checkNotNull(taskModel) ;
			if (result == null) {
				return ;
			}
			StringBuilder taskStr = new StringBuilder () ;
			taskStr.append("Uploaded '") ;
			taskStr.append(taskModel.getSrcDir()) ;
			taskStr.append("' into") ;
			taskStr.append(taskModel.getDriveDir().getTitle()) ;
			logger.info (taskStr.toString()) ;
			
			final List<ErrorModel> errList = new ArrayList<ErrorModel>() ;
			if (result.hasError()) {

				if (result.getPathErrorMap() != null) {
					for (Entry<Path, Throwable> entry : result.getPathErrorMap().entrySet()) {
						StringBuilder sb = new StringBuilder () ;
						sb.append("File '") ;
						sb.append(entry.getKey().toString()) ;
						sb.append("' was not uploaded.") ;
						if (entry.getValue() != null) {
							sb.append(" Error message: ") ;
							sb.append(entry.getValue().getMessage()) ;
						}
						logger.info(sb.toString());
						errList.add(new ErrorModel (entry.getKey(), taskModel.getDriveDir(), entry.getValue())) ;
					}
				}
			}
			if (result.hasWarning()) {

				if (result.getPathWarningMap() != null) {
					for (Entry<Path, HasWarning> entry : result.getPathWarningMap().entrySet()) {
						StringBuilder sb = new StringBuilder () ;
						sb.append("File '") ;
						sb.append(entry.getKey().toString()) ;
						sb.append("' was uploaded, but with warnings. ") ;
						if (entry.getValue() != null) {
							sb.append(" Warning message: ") ;
							sb.append(entry.getValue().getWarningMessage()) ;
						}
						logger.info(sb.toString());
						errList.add(new ErrorModel (entry.getKey(), taskModel.getDriveDir(), entry.getValue())) ;
					}
				}
			}
			if (!errList.isEmpty()) {

				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						try {
							ErrorReportDialog errDlg = new ErrorReportDialog(null, errList);
							errDlg.showDialog();
						} catch (IOException e) {
							logger.error("Error occurred while showing the errors list", e);
						}
					}
				});

			}
		}

		
		@Override
		public void onFailure(Throwable cause) {
			logger.error("Error occurred while task was being performed", cause);
		}
	}
	
	
	@FXML
	protected void addDriveTask(ActionEvent event) {
		checkController () ;
        final ObservableList<DriveTaskModel> data = tableTaskView.getItems();
        final Stage owner = UiUtils.getStage(event) ;
        try {
        	
        	io.uploader.drive.util.Callback<DriveTaskModel> callback = new io.uploader.drive.util.Callback<DriveTaskModel> () {

    			@Override
    			public void onSuccess(DriveTaskModel result) {

    				if (result == null) {
    					return ;
    				}
    				for (DriveTaskModel taskModel : data) {
    					if (!taskModel.isDone() && taskModel.hasTheSameTaskAs(result)) {
    						MessageDialogs.showMessageDialog(owner, "This task already exists", "Warning", MessageDialogs.MessageType.WARNING); 
    						return ;
    					}
    				}
    				data.add(result) ;
    				result.start(new ResultCallback (result)) ;
    			}

    			@Override
    			public void onFailure(Throwable cause) {
    				logger.error("Error occurred while adding a new task", cause) ;
    			}} ;
    			
			TaskAddDialog dlg = new TaskAddDialog (owner, callback, taskFactory, driveUiFactory) ;
			dlg.showDialog();
		} catch (IOException e) {
			logger.error("Error occurred while opening the task dialog", e);
		}
	}
	
	
	public void setTaskFactory (DriveTaskFactory taskFactory) {
		this.taskFactory = taskFactory ;
	}
	
	
	public void setDriveUiFactory (DriveUiFactory driveUiFactory) {
		this.driveUiFactory = driveUiFactory ;
	}
	
	
    Callback<TableColumn<DriveTaskModel, String>, TableCell<DriveTaskModel, String>> optionButtonColumnCellFactory = 
            new Callback<TableColumn<DriveTaskModel, String>, TableCell<DriveTaskModel, String>>() {

        @SuppressWarnings("rawtypes")
		@Override
        public TableCell call(final TableColumn param) {
            final TableCell cell = new TableCell() {

                @SuppressWarnings("unchecked")
				@Override
                public void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        final Button buttonStop = new Button("Stop");
                        setGraphic(buttonStop);
                        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                        
                        buttonStop.setOnAction(new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {
                            	param.getTableView().getSelectionModel().select(getIndex());
                                DriveTaskModel task = tableTaskView.getSelectionModel().getSelectedItem();
                            	param.getTableView().getSelectionModel().clearSelection();
                                if (task == null) {
                                	logger.info("No task to stop...") ;
                                	return ;
                                } else if (task.isDone()) {
                                	MessageDialogs.showMessageDialog(UiUtils.getStage(event), "The task is already done", "Information", MessageDialogs.MessageType.INFO);
                                	buttonStop.setDisable(true);
                                	return ;
                                }
                                else if (Response.NO == MessageDialogs.showConfirmDialog(UiUtils.getStage(event), "Are you sure you want to stop this task?", "Confirmation")) {
            						return ;
            					} else {
	                                logger.info("Stop task") ;
	                                task.stop() ;
	                                buttonStop.setDisable(true);
            					}
                            }
                        });
                    }
                }
            };
            cell.setAlignment(Pos.CENTER);
            return cell;
        }
    };
	
}
