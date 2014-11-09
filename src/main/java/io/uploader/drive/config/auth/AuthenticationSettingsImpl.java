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

package io.uploader.drive.config.auth;

import java.io.IOException;
import java.io.InputStream;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;

public class AuthenticationSettingsImpl implements HasAuthenticationSettings {

	private final GoogleClientSecrets clientSecrets ;
	
	public AuthenticationSettingsImpl(GoogleClientSecrets clientSecrets) {
		super();
		this.clientSecrets = clientSecrets;		
	}


	public static InputStream getClientSecretJson() throws IOException {
		InputStream in = AuthenticationSettingsImpl.class.getResourceAsStream("/client_secrets.json") ;
		if (in == null) {
			in = AuthenticationSettingsImpl.class.getResourceAsStream("/client_secrets_ori.json") ;
		}
		return in ;
	}
	
	
	@Override
	public synchronized String getClientId() {
		return clientSecrets.getDetails().getClientId() ;
	}

	
	@Override
	public synchronized String getClientSecret() {
		return clientSecrets.getDetails().getClientSecret() ;
	}
	

	@Override
	public String getCallBackUrl() {
		return clientSecrets.getDetails().getRedirectUris().get(0) ;
	}
}
