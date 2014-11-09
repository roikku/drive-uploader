/*
 * Copyright 2014 Loic Merckel
 * Copyright (c) 2013 Google Inc.
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

/*
* 
* The original version of this file (i.e., the one that is copyrighted 2013 Google Inc.) 
* can  be found here:
*
*  https://code.google.com/p/google-api-java-client/source/browse/drive-cmdline-sample/src/main/java/com/google/api/services/samples/drive/cmdline/DriveSample.java?repo=samples
*  
*/

package io.uploader.drive;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.uploader.drive.auth.DriveUploaderAuthorizationCodeInstalledApp;
import io.uploader.drive.auth.webbrowser.Browser;
import io.uploader.drive.auth.webbrowser.SimpleBrowserImpl;
import io.uploader.drive.config.Configuration;
import io.uploader.drive.config.auth.AuthenticationSettingsImpl;
import io.uploader.drive.gui.MainWindow;
import io.uploader.drive.gui.dlg.MessageDialogs;
import io.uploader.drive.gui.dlg.MessageDialogs.Response;
import io.uploader.drive.util.Callback;
import io.uploader.drive.util.ObserverService;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.common.base.Preconditions;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class DriveUploader extends Application {

	private static final Logger logger = LoggerFactory.getLogger(DriveUploader.class);
	
	private static final boolean activateNativeLookAndFeelForMacOs = true ;
	
	public static boolean isMacOsX ()
	{
		if (!activateNativeLookAndFeelForMacOs) {
			return false ;
		}
		String os = System.getProperty("os.name") ;
		return (os != null && os.toLowerCase().contains("mac")) ;
	}
	
	/**
	 * Be sure to specify the name of your application. If the application name
	 * is {@code null} or blank, the application will log a warning. Suggested
	 * format is "MyCompany-ProductName/1.0".
	 */
	private static final String APPLICATION_NAME = Configuration.INSTANCE.getAppName() ;

	/** Directory to store user credentials. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(Configuration.INSTANCE.getDataStoreDirectory());

	/**
	 * Global instance of the {@link DataStoreFactory}. The best practice is to
	 * make it a single globally shared instance across your application.
	 */
	private static FileDataStoreFactory dataStoreFactory;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private static HttpTransport httpTransport;

	private static Drive client;
		
	private static final AppEvent appEvent = new AppEvent () ;

	
	public static void main(String[] args) {
		
    	if (isMacOsX ())
    	{
    		System.setProperty("apple.laf.useScreenMenuBar", "true");
    		System.setProperty("com.apple.mrj.application.apple.menu.about.name", Configuration.INSTANCE.getAppName());
    		try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
				logger.error("Error occurred while initializing the UI", e);
			}
    	}
    	
    	Platform.setImplicitExit(false);	
		
		// load the settings
    	String settingsFile = "driveuploader-settings.xml" ;
    	if (!new java.io.File (settingsFile).exists())
    		settingsFile = null ;
		try {
			Configuration.INSTANCE.load(settingsFile);
		} catch (ConfigurationException e) {
			logger.error("Error occurred while initializing the configuration", e);
		}
		
		try {
			// initialize the transport
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();

			// initialize the data store factory
			dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

		} catch (IOException e) {
			logger.error("Error occurred while initializing the drive", e);
			Platform.exit() ;
		} catch (Throwable t) {
			logger.error("Error occurred while initializing the drive", t);
			Platform.exit() ;
		}
    	
		launch(args);
	}

	private static void authorize(final Browser browser, final Callback<Credential> callback)  {
		try {
			// load client secrets
			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
					JSON_FACTORY,
					new InputStreamReader(AuthenticationSettingsImpl.getClientSecretJson()));
			if (clientSecrets.getDetails().getClientId().startsWith("Enter")
					|| clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
				System.out
						.println("Overwrite the src/main/resources/client_secrets.json file with the client secrets file "
								+ "you downloaded from the Quickstart tool or manually enter your Client ID and Secret "
								+ "from https://code.google.com/apis/console/?api=drive#project:94720202188 "
								+ "into src/main/resources/client_secrets.json");
				System.exit(1);
			}
			
			Configuration.INSTANCE.setAuthenticationSettingsImpl(new AuthenticationSettingsImpl (clientSecrets));
	
			// Set up authorization code flow.
			Set<String> scopes = new HashSet<String>();
			scopes.add(DriveScopes.DRIVE);
			// old api for large file support (where upload takes more than one hour)
			scopes.add("https://docs.google.com/feeds") ;
	
			final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
					httpTransport, JSON_FACTORY, clientSecrets, scopes)
					.setDataStoreFactory(dataStoreFactory)
					.setAccessType("offline").setApprovalPrompt("force").build();
			
			// authorize
			final VerificationCodeReceiver receiver = new LocalServerReceiver() ;

			ObserverService<Credential> service = new ObserverService<Credential>() {

				private volatile DriveUploaderAuthorizationCodeInstalledApp driveUploaderAuthorizationCodeInstalledApp = null ;
			
				@Override
				protected Task<Credential> createTask() {
					Task<Credential> task = new Task<Credential>() {

						@Override
						protected Credential call() throws Exception {

							driveUploaderAuthorizationCodeInstalledApp = new DriveUploaderAuthorizationCodeInstalledApp(
									flow, receiver, browser) ;
							Credential credential = driveUploaderAuthorizationCodeInstalledApp.authorize("user");
							return credential;
						}
					};
					return task ;
				}
			};

			service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

				@Override
				public void handle(WorkerStateEvent t) {
					logger.info("Login success") ;
					Credential ret = (Credential) t.getSource().getValue();
					callback.onSuccess(ret);
				}
			});
			
			appEvent.addObserver(service) ;
			service.start();

		} catch (Exception e) {
			callback.onFailure(e) ;
		}
	}
	
	
	@Override
	public void stop() throws Exception {
		logger.info("Stop!") ;
		super.stop();
	}


	@Override
	public void start(final Stage stage) throws Exception {
		
		Preconditions.checkNotNull(stage) ;
		
		try {
			stage.onCloseRequestProperty().set(new EventHandler<WindowEvent>() {
				public void handle(WindowEvent e) {
					logger.info("Close request") ;
		
					if (Response.NO == MessageDialogs.showConfirmDialog(stage, "Are you sure you want to close the application?", "Confirmation")) {
						e.consume();
						return ;
					}
					appEvent.exit () ;
					Platform.exit ();
					
					// TODO: 
					// Investigate why when we shutdown the authentication service
					// before the end of the process, the app hangs after closing the main window...
					// http://stackoverflow.com/questions/4425350/how-to-terminate-a-thread-blocking-on-socket-io-operation-instantly
					//if (client == null) {
						System.exit(0) ;
					//}
				}
			});
			
			final List<Integer> logoSizeList = Arrays.asList(16, 32, 64, 128, 256, 512) ;
			final String logoNameBase = "DriveUploader" ;
			final String logoNameExt = ".png" ;
			for (Integer size : logoSizeList) {
				StringBuilder logoName = new StringBuilder () ;
				logoName.append(logoNameBase) ;
				logoName.append(size) ;
				logoName.append(logoNameExt) ;
				Image logo = new Image(getClass().getResourceAsStream("/images/" + logoName)) ;
				stage.getIcons().add(logo);
			}
			
			//final Browser browser = new SimpleBrowser (new Stage (), null) ;
			final Browser browser = new SimpleBrowserImpl (stage, null) ;
			authorize (browser, new Callback<Credential> () {
	
				@Override
				public void onSuccess(Credential result)  {
					logger.info("Received credential") ;
					
					client = new Drive.Builder(httpTransport, JSON_FACTORY, result)
						.setApplicationName(APPLICATION_NAME).build();
					
					Configuration.INSTANCE.setCredential(result);
					
					//browser.close () ;
					try {
						MainWindow mainWindow = new MainWindow (client, stage, appEvent, Configuration.INSTANCE) ;
						mainWindow.show() ;					
					} catch (IOException e) {
						logger.error("Error occurred while creating the main window", e) ;
						Platform.exit() ;
					}
				}
	
				@Override
				public void onFailure(Throwable cause) {
					logger.error("Error occurred while authenticating", cause) ;
					Platform.exit() ;
				}}) ;
		}catch (Exception e) {
			Platform.exit() ;
			throw e ;
		}
	}
}
