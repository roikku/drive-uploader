package io.uploader.drive.gui.controller;

import io.uploader.drive.config.HasConfiguration;
import io.uploader.drive.config.proxy.HasProxySettings;
import io.uploader.drive.config.proxy.Proxy;
import io.uploader.drive.gui.dlg.MessageDialogs;
import io.uploader.drive.gui.util.UiUtils;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class ProxySettingViewController  implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(ProxySettingViewController.class);

    @FXML TextField httpUserField ;
    @FXML PasswordField httpPwdField ;
    @FXML TextField httpHostField ;
    @FXML TextField httpPortField ;
    
    @FXML TextField httpsUserField ;
    @FXML PasswordField httpsPwdField ;
    @FXML TextField httpsHostField ;
    @FXML TextField httpsPortField ;
    
    @FXML CheckBox httpActivatedCheckBox ;
    @FXML CheckBox httpsActivatedCheckBox ;
    
    private HasConfiguration config = null ;
    
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
		
	}
	
	@FXML
	protected void onCancel(ActionEvent event) {
    	//close the dialog
		UiUtils.closeDialog (event) ;
	}
	
	@FXML
	protected void onSetProxy(ActionEvent event) {
		Preconditions.checkNotNull(config) ;
		
		int httpPort = 8080 ;
		int httpsPort = 8080 ;
		try {
			httpPort = Integer.valueOf(httpPortField.getText().trim()) ;
			httpsPort = Integer.valueOf(httpsPortField.getText().trim()) ;
		} catch (NumberFormatException e) {
			logger.error("Error occurred while converting text to number");
			MessageDialogs.showConfirmDialog(UiUtils.getStage(event), "The ports must be a number", "Error") ;
			return ;
		}

		Proxy httpProxy = new Proxy.Builder("http")
				.setActivated(httpActivatedCheckBox.isSelected())
				.setHost(httpHostField.getText().trim())
				.setPassword(httpPwdField.getText().trim())
				.setUsername(httpUserField.getText().trim()).setPort(httpPort)
				.build();
		config.updateProxy(httpProxy);

		Proxy httpsProxy = new Proxy.Builder("https")
				.setActivated(httpsActivatedCheckBox.isSelected())
				.setHost(httpsHostField.getText().trim())
				.setPassword(httpsPwdField.getText().trim())
				.setUsername(httpsUserField.getText().trim()).setPort(httpsPort)
				.build();
		config.updateProxy(httpsProxy);

		MessageDialogs
				.showMessageDialog(
						UiUtils.getStage(event),
						"The application needs to be restarted for the new settings to be effective",
						"Information", MessageDialogs.MessageType.INFO);

    	//close the dialog
		UiUtils.closeDialog (event) ;
	}
	
	
	public void setConfiguration (HasConfiguration config) {
		Preconditions.checkNotNull(config) ;
		this.config = config ;

		HasProxySettings httpProxy = config.getHttpProxySettings() ;
		if (httpProxy != null) {
			httpActivatedCheckBox.setSelected(httpProxy.isActive());
			httpHostField.setText(httpProxy.getHost());
			httpPwdField.setText(httpProxy.getPassword());
			httpUserField.setText(httpProxy.getUsername());
			httpPortField.setText(String.valueOf(httpProxy.getPort()));
		}
		
		HasProxySettings httpsProxy = config.getHttpsProxySettings() ;
		if (httpProxy != null) {
			httpsActivatedCheckBox.setSelected(httpsProxy.isActive());
			httpsHostField.setText(httpsProxy.getHost());
			httpsPwdField.setText(httpsProxy.getPassword());
			httpsUserField.setText(httpsProxy.getUsername());
			httpsPortField.setText(String.valueOf(httpsProxy.getPort()));
		}
	}
}
