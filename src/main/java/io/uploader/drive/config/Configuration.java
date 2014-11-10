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

package io.uploader.drive.config;


import io.uploader.drive.config.auth.AuthenticationSettingsImpl;
import io.uploader.drive.config.auth.HasAuthenticationSettings;
import io.uploader.drive.config.proxy.HasProxySettings;
import io.uploader.drive.config.proxy.Proxy;
import io.uploader.drive.config.proxy.ProxySettingsImpl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;

public enum Configuration implements HasConfiguration {
	INSTANCE ;
	
	final private Logger logger = LoggerFactory.getLogger(Configuration.class);
	
	private final String defaultXmlPath ; 
	
	private final String appName = "Drive Uploader" ;
	private final String appVersion = "1.0" ;
	
	@Override
	public String getAppName ()
	{
		return appName ;
	}
	
	@Override
	public String getAppVersion ()
	{
		return appVersion ;
	}
	
	@Override
	public String getDataStoreDirectory() {
		String app = appName.toLowerCase().replaceAll(" ", "") ; 
		StringBuilder sb = new StringBuilder ();
		sb.append(System.getProperty("user.home")) ;
		sb.append(File.separator) ;
		sb.append(".") ;
		sb.append(app) ;
		sb.append(File.separator) ;
		sb.append(appVersion) ;
		sb.append(File.separator) ;
		sb.append("store") ;
		sb.append(File.separator) ;
		return sb.toString() ;
	}
	
	
	@Override
	public String getTmpDirectory() {
		String app = appName.toLowerCase().replaceAll(" ", "") ; 
		StringBuilder sb = new StringBuilder ();
		sb.append(System.getProperty("user.home")) ;
		sb.append(File.separator) ;
		sb.append(".") ;
		sb.append(app) ;
		sb.append(File.separator) ;
		sb.append(appVersion) ;
		sb.append(File.separator) ;
		sb.append("tmp") ;
		sb.append(File.separator) ;
		File dir = new File (sb.toString()) ;
		if (!dir.exists()) {
			dir.mkdirs() ;
		}
		return sb.toString() ;
	}
	
	
	private Configuration ()
	{
		String app = appName.toLowerCase().replaceAll(" ", "") ; 
		StringBuilder sb = new StringBuilder ();
		sb.append(System.getProperty("user.home")) ;
		sb.append(File.separator) ;
		sb.append(".") ;
		sb.append(app) ;
		sb.append(File.separator) ;
		sb.append(appVersion) ;
		sb.append(File.separator) ;
		sb.append(app) ;
		sb.append("-settings.xml") ;
		defaultXmlPath = sb.toString() ;
	}
	
	private volatile XMLConfiguration config = null ;
	
	private final ProxySettingsImpl httpProxySettings = new ProxySettingsImpl ("proxy.http", 80, "http") ;
	private final ProxySettingsImpl httpsProxySettings = new ProxySettingsImpl ("proxy.https", 443, "https") ;
	
	private Credential credential = null ;
	private AuthenticationSettingsImpl authenticationSettingsImpl = null ;

	public void load (String xmlPath) throws ConfigurationException
	{
		String settingFilePath = (xmlPath == null || xmlPath.isEmpty()) ? (defaultXmlPath) : (xmlPath) ;
		
		logger.debug("Load setting file {}.", settingFilePath);
		
		checkConfigFile (settingFilePath) ;
		try{
			config = new XMLConfiguration(settingFilePath);
		}
		catch (ConfigurationException e) {
			logger.error("Error occurred while opening the settings file", e);
		}
		if (config == null)
			return ;
		// TODO: when the commom-configuration 2.0 will be released
		// set the synchronizer
		//config.setSynchronizer(new ReadWriteSynchronizer());
		config.setThrowExceptionOnMissing(false);
		httpProxySettings.setConfig(config);
		httpsProxySettings.setConfig(config);
		
		setProxy () ;
	}
	
	
	private void checkConfigFile (String path)
	{
		try 
		{			
			File file = new File (path) ;
			if (!file.exists())
			{
				File parent = file.getParentFile() ;
				if (parent != null)
					parent.mkdirs() ;
				file.createNewFile() ;
				PrintWriter writer = new PrintWriter(file.getPath(), "UTF-8");
				writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?><settings></settings>" );
				writer.close();	
			}
		} 
		catch (IOException | SecurityException | NullPointerException e) 
		{
			logger.error("Error occurred while creating the settings file", e);
		}	
	}
	
	
	public void setAuthenticationSettingsImpl (AuthenticationSettingsImpl authenticationSettingsImpl) {
		this.authenticationSettingsImpl = authenticationSettingsImpl ;
	}
	
	
	@Override
	public HasAuthenticationSettings getAuthenticationSettings() {
		return authenticationSettingsImpl;
	}
	
	
	public void setCredential (Credential credential) {
		this.credential = credential ;
	}
	
	
	@Override
	public Credential getCredential() {
		return credential;
	}
	
	
	@Override
	public HasProxySettings getHttpProxySettings ()
	{
		return httpProxySettings ;
	}
	
	
	@Override
	public HasProxySettings getHttpsProxySettings ()
	{
		return httpsProxySettings ;
	}
	
		
	@Override
	public void updateProxy(Proxy newProxy) {
		
		try 
		{
			if ("http".equalsIgnoreCase(newProxy.getProtocol()))
			{
				httpProxySettings.update(newProxy);
				setProxySystemProperty (httpProxySettings, "http") ;
			}
			else if ("https".equalsIgnoreCase(newProxy.getProtocol()))
			{
				httpsProxySettings.update(newProxy);
				setProxySystemProperty (httpsProxySettings, "https") ;
			}
			else
			{
				logger.info("Unsupported proxy protocol: {}.", newProxy.getProtocol());
			}
		} 
		catch (ConfigurationException e) 
		{
			logger.error("Error occurred while updating the proxy settings", e);
		}
	}

	
	private static void setProxySystemProperty (HasProxySettings proxySettings, String prot)
	{
		if (proxySettings == null)
			return ;
		if (prot == null || prot.isEmpty())
			return ;
		
		prot = prot.toLowerCase() ;
		
    	if (proxySettings.isActive())
    	{
    		String host = proxySettings.getHost() ;
    		String user = proxySettings.getUsername() ;
    		String pwd = proxySettings.getPassword() ;
    		int port = proxySettings.getPort() ;
    		
    		if (host != null)
    			System.setProperty(prot + ".proxyHost", host);
	    	System.setProperty(prot + ".proxyPort", String.valueOf(port));
	    	if (user != null)
	    		System.setProperty(prot + ".proxyUser", user);
	    	if (pwd != null)
	    		System.setProperty(prot + ".proxyPassword", pwd);
    	}
	}

	
	
    private void setProxy ()
    {    	
    	setProxySystemProperty (httpProxySettings, "http") ;
    	setProxySystemProperty (httpsProxySettings, "https") ;
    	    	
		Authenticator.setDefault(new Authenticator() {
		    @Override
		    protected PasswordAuthentication getPasswordAuthentication() 
		    {
		        if (getRequestorType() == RequestorType.PROXY) 
		        {
		            String prot = getRequestingProtocol().toLowerCase();
		            
		            String host = System.getProperty(prot + ".proxyHost", "");
		            String port = System.getProperty(prot + ".proxyPort", "80");
		            String user = System.getProperty(prot + ".proxyUser", "");
		            String password = System.getProperty(prot + ".proxyPassword", "");

		            if (getRequestingHost().equalsIgnoreCase(host)) 
		            {
		                if (Integer.parseInt(port) == getRequestingPort()) 
		                    return new PasswordAuthentication(user, password.toCharArray());  
		            }
		        }
		        return null;
		    }  
		});
    }
}
