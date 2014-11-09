/*
 *  Copyright 2014 Loic Merckel
 *  Copyright 2014 Dirk Boye 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
*
* The original version of this file (i.e., the one that is copyrighted 2014 Dirk Boye) 
* can  be found here:
*
*  https://github.com/dirkboye/GDriveUpload
*  
*  Massive changes have been made
*
*/


// https://code.google.com/p/gdata-java-client/
package io.uploader.drive.drive.largefile;

import io.uploader.drive.config.HasConfiguration;
import io.uploader.drive.config.proxy.HasProxySettings;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;

public class DriveAuth {
	
    private static final Logger logger = LoggerFactory.getLogger(DriveAuth.class);
    
    private final String clientId;
    private final String clientSecret;
    private String accessToken = "" ;
    private String tokenType = "" ;
    private String refreshToken = "" ;

    private final int maxRetries = 3;
    
    private final HasConfiguration config ;
    private final HasProxySettings proxySetting ;


    public DriveAuth(HasConfiguration config) throws IOException {
        this(true, config);
    }

    
    public DriveAuth(boolean useOldApi, HasConfiguration config) throws IOException {
    	super () ;
    	this.config = Preconditions.checkNotNull(config) ;
    	this.proxySetting = config.getHttpProxySettings() ;

        clientSecret = config.getAuthenticationSettings().getClientSecret();
        clientId = config.getAuthenticationSettings().getClientId();
        config.getAuthenticationSettings().getCallBackUrl();
        refreshToken = config.getCredential().getRefreshToken() ;

        int retry = 0;
        boolean tokensOK = false;
        while (!tokensOK && retry < maxRetries) {
            tokensOK = updateAccessToken();
            ++retry;
        }
        if (!tokensOK) {
        	logger.info("Authentication aborted after " + maxRetries + " retries.");
            throw new IllegalStateException () ;
        }
    }

    
    private CloseableHttpClient getHttpClient () {
    	return HttpClientUtils.getHttpClient(proxySetting) ;
    }
    

    public String getAccessToken() {
        return accessToken;
    }

    
    public String getAuthHeader() {
        return tokenType + " " + accessToken;
    }

    
    public boolean updateAccessToken() throws UnsupportedEncodingException, IOException {
        // If a refresh_token is set, this class tries to retrieve an access_token.
        // If refresh_token is no longer valid it resets all tokens to an empty string.
        if (refreshToken.isEmpty() || accessToken.isEmpty()) {
            accessToken = config.getCredential().getAccessToken();
            tokenType = "";
            refreshToken = config.getCredential().getRefreshToken();
        }
        logger.info("Updating access_token from Google");
        CloseableHttpClient httpclient = getHttpClient () ;
        HttpPost httpPost = new HttpPost("https://accounts.google.com/o/oauth2/token");
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("client_id", clientId));
        nvps.add(new BasicNameValuePair("client_secret", clientSecret));
        nvps.add(new BasicNameValuePair("refresh_token", refreshToken));
        nvps.add(new BasicNameValuePair("grant_type", "refresh_token"));
        BufferedHttpEntity postentity = new BufferedHttpEntity(new UrlEncodedFormEntity(nvps));
        httpPost.setEntity(postentity);
        CloseableHttpResponse response = httpclient.execute(httpPost);
        BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
        EntityUtils.consume(response.getEntity());
        boolean tokensOK = false;
        try {
            if (response.getStatusLine().getStatusCode() == 200 && entity != null) {
                String retSrc = EntityUtils.toString(entity);
                JSONObject result = new JSONObject(retSrc);
                accessToken = result.getString("access_token");
                tokenType = result.getString("token_type");
                tokensOK = true;
            }
        } finally {
            response.close();
        }
        httpclient.close();
        if (!tokensOK) {
            refreshToken = "";
            accessToken = "";
            tokenType = "";
        }
        return tokensOK;
    }
}
