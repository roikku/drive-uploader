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
 

package io.uploader.drive.auth.webbrowser;

import io.uploader.drive.DriveUploader;
import io.uploader.drive.config.Configuration;
import io.uploader.drive.gui.controller.MainMenuController;
import io.uploader.drive.gui.util.UiUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class SimpleBrowserImpl implements Browser {
 
	final private static Logger logger = LoggerFactory.getLogger(SimpleBrowserImpl.class);
	
	private final Stage stage ;
	private final WebEngine webEngine ;
	
    public SimpleBrowserImpl(Stage stage, String url) throws IOException {

    	super () ;
    	
    	Preconditions.checkNotNull(stage) ;
    	
    	this.stage = stage ;
    	stage.setTitle("Drive Uploader - Authentication");
    	UiUtils.setStageAppSize(stage) ;
        Scene scene = new Scene(new Group());

        AnchorPane root = new AnchorPane () ;  

        final WebView browser = new WebView();
        webEngine = browser.getEngine();
        
        /*
        Menu settingsMenu = new Menu ("Settings") ;
        MenuItem proxy = new MenuItem ("Proxy") ;
        settingsMenu.getItems().add(proxy) ;
        MenuBar menuBar = new MenuBar () ;
        menuBar.getMenus().add(settingsMenu) ;
        proxy.setOnAction(new EventHandler<ActionEvent> () {

			@Override
			public void handle(ActionEvent event) {
				try {
					ProxySettingDialog dlg = new ProxySettingDialog (stage, Configuration.INSTANCE) ;
					dlg.showDialog();
				} catch (IOException e) {
					logger.error("Error occurred while opening the proxy setting dialog", e);
				}
			}});*/
        
		FXMLLoader mainMenuLoader = new FXMLLoader(getClass().getResource("/fxml/MainMenu.fxml")); 
		VBox mainMenuBar = (VBox)mainMenuLoader.load() ;
		AnchorPane.setTopAnchor(mainMenuBar, 0.0);
		AnchorPane.setLeftAnchor(mainMenuBar, 0.0);
		AnchorPane.setRightAnchor(mainMenuBar, 0.0);
		MainMenuController mainMenuController = mainMenuLoader.<MainMenuController>getController();
		mainMenuController.setOwner(stage) ;
		mainMenuController.setConfiguration(Configuration.INSTANCE) ;
		mainMenuController.hideAccountMenu(true);
             
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(browser);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        if (StringUtils.isNotEmpty(url)) {
        	goTo (url) ;
        }
                
        if (DriveUploader.isMacOsX()) {
        	AnchorPane.setTopAnchor(scrollPane, 5.0) ;
        } else {
        	AnchorPane.setTopAnchor(scrollPane, 35.0) ;
        }
        AnchorPane.setLeftAnchor(scrollPane, 5.0) ;
        AnchorPane.setRightAnchor(scrollPane, 5.0) ;
        AnchorPane.setBottomAnchor(scrollPane, 5.0) ;
        
        root.getChildren().add(mainMenuBar) ;
        root.getChildren().addAll(scrollPane);
        scene.setRoot(root);
        
        stage.setScene(scene);
    }
    
    
    @Override
    public void goTo (String url) {
    	
    	logger.info ("url: " + url) ;
    	
        String tmp = toURL(url);
        if (tmp == null) {
            tmp = toURL("http://" + url);
        }
        webEngine.load(tmp);
    }
    
    
    @Override
    public void show () {
    	stage.show() ;
    }
    
    
	@Override
	public void close() {
		stage.close() ;
	}
    
    
    public void stage () {
    	stage.showAndWait() ;
    }
    
    
    private static String toURL(String str) {
        try {
            return new URL(str).toExternalForm();
        } catch (MalformedURLException e) {
        	logger.error("Error occurred while loading the specified url.", e);
        	return null ;
        }
    }
}

