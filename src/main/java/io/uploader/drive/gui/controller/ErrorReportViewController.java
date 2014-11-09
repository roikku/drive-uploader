package io.uploader.drive.gui.controller;

import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;

import io.uploader.drive.gui.model.ErrorModel;
import io.uploader.drive.gui.util.UiUtils;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;

public class ErrorReportViewController implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(ErrorReportViewController.class);
	
	@FXML private TableView<ErrorModel> tableErrorView;
	
	@FXML private TableColumn<ErrorModel, String> errLevelColumn;
	@FXML private TableColumn<ErrorModel, String> srcColumn;  
	@FXML private TableColumn<ErrorModel, String> destColumn;
	@FXML private TableColumn<ErrorModel, String> errColumn;  

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		
		srcColumn.setCellValueFactory(new PropertyValueFactory("srcPathStr")); 
		srcColumn.setCellFactory(multiLineTextColumnCellFactory);
		
		destColumn.setCellValueFactory(new PropertyValueFactory("destDirStr")); 
		destColumn.setCellFactory(multiLineTextColumnCellFactory);
		
		errColumn.setCellValueFactory(new PropertyValueFactory("errStr")); 
		errColumn.setCellFactory(multiLineTextColumnCellFactory);
		
		errLevelColumn.setCellValueFactory(new PropertyValueFactory("errLevelStr"));
		errLevelColumn.setCellFactory(errorLevelColumnCellFactory);
	}
	
	
	public void addErrors (Collection<ErrorModel> errs) {
		Preconditions.checkNotNull(tableErrorView) ;
		Preconditions.checkNotNull(errs) ;
		logger.info("Show erros");
		final ObservableList<ErrorModel> data = tableErrorView.getItems();
		for (ErrorModel err : errs) {
			data.add(err) ;
		}
	}
	

	@FXML
	protected void onClose(ActionEvent event) {
		UiUtils.closeDialog (event) ;
	}
	
	
    Callback<TableColumn<ErrorModel, String>, TableCell<ErrorModel, String>> multiLineTextColumnCellFactory = 
            new Callback<TableColumn<ErrorModel, String>, TableCell<ErrorModel, String>>() {

        @SuppressWarnings("rawtypes")
		@Override
        public TableCell call(final TableColumn param) {
            final TableCell cell = new TableCell() {

            	private Text text;
            	
                @SuppressWarnings("unchecked")
				@Override
                public void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        text = new Text(item.toString());
                        text.wrappingWidthProperty().bind(widthProperty());
                        setGraphic(text);
                    }
                }
            };
            return cell;
        }
    };
	
	
    Callback<TableColumn<ErrorModel, String>, TableCell<ErrorModel, String>> errorLevelColumnCellFactory = 
            new Callback<TableColumn<ErrorModel, String>, TableCell<ErrorModel, String>>() {

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
                        if (ErrorModel.ErrorLevel.ERROR.toString().equals(((String) item))) {
                        	setGraphic(ErrorModel.newErrorIcon());
                        } else if (ErrorModel.ErrorLevel.WARNING.toString().equals(((String) item))) {
                        	setGraphic(ErrorModel.newWarningIcon());
                        }
                        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    }
                }
            };
            cell.setAlignment(Pos.CENTER);
            return cell;
        }
    };
}
