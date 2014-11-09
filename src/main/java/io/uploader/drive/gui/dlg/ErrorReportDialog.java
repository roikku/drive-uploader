package io.uploader.drive.gui.dlg;

import io.uploader.drive.gui.controller.ErrorReportViewController;
import io.uploader.drive.gui.model.ErrorModel;

import java.io.IOException;
import java.util.Collection;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErrorReportDialog extends AbstractDialog {

	public ErrorReportDialog (Stage owner, Collection<ErrorModel> errs) throws IOException {
		super (owner) ;
		
		Preconditions.checkNotNull(errs);
		
		initStyle(StageStyle.UTILITY);
		setTitle("Error Report");
		initOwner(owner);
		initModality (Modality.WINDOW_MODAL );
		
		final FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ErrorReportView.fxml"));
		final Parent parent = (Parent) loader.load();
		final ErrorReportViewController controller = loader.<ErrorReportViewController> getController();

		controller.addErrors(errs);
		
		setMinHeight(300.0);
		setMinWidth(400.0);
	
		setHeight(500.0);
		setWidth(750.0);
		
		setScene(new Scene(parent));
	}
}
