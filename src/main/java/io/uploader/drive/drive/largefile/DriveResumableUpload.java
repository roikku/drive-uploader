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

package io.uploader.drive.drive.largefile;

import io.uploader.drive.config.proxy.HasProxySettings;
import io.uploader.drive.drive.DriveUtils;
import io.uploader.drive.drive.DriveUtils.HasDescription;
import io.uploader.drive.drive.DriveUtils.HasId;
import io.uploader.drive.drive.DriveUtils.HasMimeType;
import io.uploader.drive.util.FileUtils.InputStreamProgressFilter;

import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

public class DriveResumableUpload {

	private static final Logger logger = LoggerFactory.getLogger(DriveResumableUpload.class);
	
    private final DriveAuth auth;
    private final long fileSize;
    private final String location;
    private final URI uri;
    private final boolean useOldApi;
    private final HasProxySettings proxySetting ;

	public DriveResumableUpload(HasProxySettings proxySetting, DriveAuth auth,
			String uploadLocation, String title, HasDescription description,
			HasId parentId, HasMimeType mimeType, String filename,
			long fileSize,
			InputStreamProgressFilter.StreamProgressCallback progressCallback)
			throws IOException, URISyntaxException {
		
		this.auth = auth ; 
		
        this.fileSize = fileSize;
        this.useOldApi = true;
        this.proxySetting = proxySetting ;
        if (org.apache.commons.lang3.StringUtils.isEmpty(uploadLocation)) {
        	this.location = createResumableUpload(title, description, parentId, mimeType);
        } else {
        	this.location = uploadLocation;
        }
        Preconditions.checkState(StringUtils.isNotEmpty(this.location));
        URIBuilder urib = new URIBuilder(location);
        uri = urib.build();
        //logger.info("URI: " + uri.toASCIIString());
    }
	
	
	public DriveResumableUpload(HasProxySettings proxySetting, DriveAuth auth, String uploadLocation, DriveUtils.HasId fileId, HasMimeType mimeType, String filename,
			long fileSize,
			InputStreamProgressFilter.StreamProgressCallback progressCallback)
			throws IOException, URISyntaxException {
		
		this.auth = auth;

        this.useOldApi = true;
        this.fileSize = fileSize;
        this.proxySetting = proxySetting ;
        if (org.apache.commons.lang3.StringUtils.isEmpty(uploadLocation)) {
        	this.location = createResumableUploadUpdate(fileId, mimeType);
        } else {
        	this.location = uploadLocation;
        }
        Preconditions.checkState(StringUtils.isNotEmpty(this.location));
        URIBuilder urib = new URIBuilder(location);
        uri = urib.build();
        //logger.info("URI: " + uri.toASCIIString());
    }
	
	
    public String getFileId() throws IOException {
    	logger.info("Querying file id of completed upload...");
    	CloseableHttpClient httpclient = null ;
    	CloseableHttpResponse response = null ;
    	try {
	    	httpclient = getHttpClient () ;
	        BasicHttpRequest httpreq = new BasicHttpRequest("PUT", location);
	        httpreq.addHeader("Authorization", auth.getAuthHeader());
	        httpreq.addHeader("Content-Length", "0");
	        httpreq.addHeader("Content-Range", "bytes */" + getFileSizeString());
	        response = httpclient.execute(URIUtils.extractHost(uri), httpreq);
	        BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
	        EntityUtils.consume(response.getEntity());
	        String retSrc = EntityUtils.toString(entity);
	        if (useOldApi) {
	            // Old API will return XML!
	            JSONObject result = XML.toJSONObject(retSrc);
	            return result.getJSONObject("entry").getString("gd:resourceId").replace("file:", "") ;
	        } else {
	            JSONObject result = new JSONObject(retSrc);
	            return result.getString("id") ;
	        }
        } finally {
        	if (response != null) {
        		response.close();
        	}
        	if (httpclient != null) {
        		httpclient.close();
        	}
        }
    }
	

    public boolean checkMD5(String md5) throws IOException {
    	logger.info("Querying metadata of completed upload...");
    	
    	Preconditions.checkState(org.apache.commons.lang3.StringUtils.isNotEmpty(md5)) ;
    	
    	CloseableHttpClient httpclient = null ;
    	CloseableHttpResponse response = null ;
    	try {
	    	httpclient = getHttpClient () ;
	        BasicHttpRequest httpreq = new BasicHttpRequest("PUT", location);
	        httpreq.addHeader("Authorization", auth.getAuthHeader());
	        httpreq.addHeader("Content-Length", "0");
	        httpreq.addHeader("Content-Range", "bytes */" + getFileSizeString());
	        response = httpclient.execute(URIUtils.extractHost(uri), httpreq);
	        BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
	        EntityUtils.consume(response.getEntity());
	        String retSrc = EntityUtils.toString(entity);
	        String driveMd5 = null ;
	        if (useOldApi) {
	            // Old API will return XML!
	            JSONObject result = XML.toJSONObject(retSrc);
	            logger.info("id          : " + result.getJSONObject("entry").getString("gd:resourceId").replace("file:", ""));
	            logger.info("title       : " + result.getJSONObject("entry").getString("title"));
	            logger.info("link        : " + result.getJSONObject("entry").getJSONArray("link").getJSONObject(0).getString("href"));
	            logger.info("md5Checksum : " + result.getJSONObject("entry").getString("docs:md5Checksum"));
	            driveMd5 = result.getJSONObject("entry").getString("docs:md5Checksum") ;
	        } else {
	            JSONObject result = new JSONObject(retSrc);
	            logger.info("id          : " + result.getString("id"));
	            logger.info("title       : " + result.getString("title"));
	            logger.info("link        : " + result.getString("webContentLink"));
	            logger.info("md5Checksum : " + result.getString("md5Checksum"));
	            driveMd5 = result.getString("md5Checksum") ;
	        }
	        // verify the consistency of the md5 values
	        return md5.equals(driveMd5) ;
        } finally {
        	if (response != null) {
        		response.close();
        	}
        	if (httpclient != null) {
        		httpclient.close();
        	}
        }
    }
    

    public boolean updateAccessToken() throws UnsupportedEncodingException, IOException {
        return auth.updateAccessToken();
    }

    private String getFileSizeString() {
        return Long.toString(fileSize);
    }

    public String getLocation() {
        return location;
    }
    
    
    private CloseableHttpClient getHttpClient () {
    	return HttpClientUtils.getHttpClient(proxySetting) ;
    }
    

    public long getCurrentByte() throws IOException {
    	logger.info("Querying status of resumable upload...");
    	
    	CloseableHttpClient httpclient = null ;
    	CloseableHttpResponse response = null ;
    	long lastbyte = -1;
    	try {
	    	httpclient = getHttpClient () ;
	        BasicHttpRequest httpreq = new BasicHttpRequest("PUT", location);
	        httpreq.addHeader("Authorization", auth.getAuthHeader());
	        httpreq.addHeader("Content-Length", "0");
	        httpreq.addHeader("Content-Range", "bytes */" + getFileSizeString());
	        //logger.info(httpreq.toString());
	        response = httpclient.execute(URIUtils.extractHost(uri), httpreq);
	        @SuppressWarnings("unused")
			BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
	        EntityUtils.consume(response.getEntity());
            if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 201) {
                lastbyte = fileSize;
            }
            if (response.getStatusLine().getStatusCode() == 308) {
                if (response.getHeaders("Range").length > 0) {
                    String range = response.getHeaders("Range")[0].getValue();
                    String[] parts = range.split("-");
                    lastbyte = Long.parseLong(parts[1]) + 1;
                } else {
                    // nothing uploaded, but file is there to start upload!
                    lastbyte = 0;
                }
            }
            return lastbyte;
        } finally {
        	if (response != null) {
        		response.close();
        	}
        	if (httpclient != null) {
        		httpclient.close();
        	}
        }
    }
    

    public int uploadChunk(byte[] bytecontent, long start_range, int bytes_in_array) throws IOException {
    	
    	logger.info(String.format("% 5.1f%% complete. Uploading next chunk.", start_range*100.0/fileSize));
        String byterange = "bytes " + Long.toString(start_range) + "-" +
                Long.toString(start_range+bytes_in_array-1) + "/" + getFileSizeString();
        if (start_range+bytes_in_array-1 >= fileSize) {
        	logger.info("Trying to push more than remaining bytes. Aborting.");
        	throw new RuntimeException () ;
        }
        
    	CloseableHttpClient httpclient = null ;
    	CloseableHttpResponse response = null ;
    	int status_code = 420 ;
        try {
	        logger.info("Uploading " + byterange);
	        httpclient = getHttpClient () ;
	        HttpPut httpPut = new HttpPut(location);
	        httpPut.addHeader("Authorization", auth.getAuthHeader());
	        httpPut.addHeader("Content-Range", byterange);
	        if (bytes_in_array != bytecontent.length) {
	        	logger.info("Seems to be the last part of the file.");
	            byte[] contentpart = new byte[bytes_in_array];
	            for (int i=0; i<bytes_in_array;++i) {
	                contentpart[i] = bytecontent[i];
	            }
	            httpPut.setEntity(new ByteArrayEntity(contentpart));
	        } else {
	            httpPut.setEntity(new ByteArrayEntity(bytecontent));
	        }
	        response = httpclient.execute(httpPut);
	        @SuppressWarnings("unused")
			BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
	        EntityUtils.consume(response.getEntity());
	        status_code = response.getStatusLine().getStatusCode();
	        return status_code;
        } finally {
        	if (response != null) {
        		response.close();
        	}
        	if (httpclient != null) {
        		httpclient.close();
        	}
        }
    }

    // https://developers.google.com/gdata/docs/resumable_upload
    private String createResumableUpload(String title, HasDescription description,
			HasId parentId, HasMimeType mimeType) throws IOException {
    	
    	logger.info("Creating resumable upload...");
        String postUri = "https://www.googleapis.com/upload/drive/v2/files?uploadType=resumable";
        if (useOldApi) {
            postUri = "https://docs.google.com/feeds/upload/create-session/default/private/full?convert=false";
          	if (parentId != null 
        			&& org.apache.commons.lang3.StringUtils.isNotEmpty(parentId.getId())) {
           	
          		// https://developers.google.com/google-apps/documents-list/
          		postUri = "https://docs.google.com/feeds/upload/create-session/default/private/full"
          				+ "/folder%3A"
          				+ parentId.getId()
          				+ "/contents"
          				+ "?convert=false";
           	}
        } else {
        	// TODO: new api
        	// ...
        	throw new IllegalStateException ("Not implemented") ;
        }
        
    	CloseableHttpClient httpclient = null ;
    	CloseableHttpResponse response = null ;
        
    	try {
    		httpclient = getHttpClient () ;
	        HttpPost httpPost = new HttpPost(postUri);
	        httpPost.addHeader("Authorization", auth.getAuthHeader());
	        httpPost.addHeader("X-Upload-Content-Type", mimeType.getMimeType());
	        httpPost.addHeader("X-Upload-Content-Length", getFileSizeString());
	   
	        String entityString = new JSONObject().put("title", title).toString();
	        BasicHeader entityHeader = new BasicHeader(HTTP.CONTENT_TYPE, "application/json");
	        if (useOldApi) {
	        	
	        	StringBuilder sb = new StringBuilder () ;
	        	sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") ;
	        	sb.append("<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:docs=\"http://schemas.google.com/docs/2007\">") ;
	        	sb.append ("<category scheme=\"http://schemas.google.com/g/2005#kind\" term=\"http://schemas.google.com/docs/2007#document\"/>") ;
	        	// title
	        	sb.append ("<title>").append(title).append("</title>") ;
	        	// description
	        	if (description != null 
	        			&& org.apache.commons.lang3.StringUtils.isNotEmpty(description.getDescription())) {
	        		sb.append ("<docs:description>").append (description.getDescription()).append ("</docs:description>") ;
	        	}
	        	sb.append ("</entry>") ;            
	            entityString = sb.toString() ;
	            httpPost.addHeader("GData-Version","3");
	            entityHeader = new BasicHeader(HTTP.CONTENT_TYPE, "application/atom+xml");
	        } else {
	        	// TODO: new api
	        	// ...
	        	throw new IllegalStateException ("Not implemented") ;
	        }
	        StringEntity se = new StringEntity( entityString );
	        se.setContentType(entityHeader);
	        httpPost.setEntity(se);
	        //logger.info("Create Resumable: " + httpPost.toString());
	        response = httpclient.execute(httpPost);
	        @SuppressWarnings("unused")
			BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
	        EntityUtils.consume(response.getEntity());
	        String location = "";
	        if (response.getStatusLine().getStatusCode() == 200) {
	            location = response.getHeaders("Location")[0].getValue();
	            //logger.info("Location: " + location);
	        }
	        return location;
        } finally {
        	if (response != null) {
        		response.close();
        	}
        	if (httpclient != null) {
        		httpclient.close();
        	}
        }
    }
    
    
    private String getResumableUploadUpdateUri (DriveUtils.HasId fileId) throws IOException {
        String getUri = "https://www.googleapis.com/upload/drive/v2/files";
        if (useOldApi) {
        	StringBuilder sb = new StringBuilder () ;
        	sb.append ("https://docs.google.com/feeds/default/private/full/") ;
        	sb.append (fileId.getId()) ;
        	getUri = sb.toString() ;
        } else {
        	// TODO: new api
        	// ...
        	throw new IllegalStateException ("Not implemented") ;
        }
        
    	CloseableHttpClient httpclient = null ;
    	CloseableHttpResponse response = null ;
    	String putUri = null ;
        try {
	        httpclient = getHttpClient () ;
	        HttpGet httpGet = new HttpGet(getUri);
	        httpGet.addHeader("Authorization", auth.getAuthHeader());
	        httpGet.addHeader("GData-Version","3");
	        response = httpclient.execute(httpGet);
	        
    		BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
    		String contents = IOUtils.toString(entity.getContent(), "UTF8") ;
    		
    		String strBegin = "#resumable-edit-media" ;
    		String uriBegin = "href='" ;
    		String uriEnd = "'/>" ;
    		int index = contents.indexOf(strBegin) ;
    		if (index < 0) {
    			return null ;
    		}
    		contents = contents.substring(index + strBegin.length()) ;
    		
    		index = contents.indexOf(uriBegin) ;
    		if (index < 0) {
    			return null ;
    		}		
    		contents = contents.substring(index + uriBegin.length()) ;
    		
    		index = contents.indexOf(uriEnd) ;
    		if (index < 0) {
    			return null ;
    		}
    		contents = contents.substring(0, index) ;
            EntityUtils.consume(response.getEntity());
            putUri = contents ;
    		//logger.info("Upload update uri: " + putUri);
    		return putUri ;
        } finally {
        	if (response != null) {
        		response.close();
        	}
        	if (httpclient != null) {
        		httpclient.close();
        	}
        }
    }
    
    
    private String createResumableUploadUpdate(DriveUtils.HasId fileId, HasMimeType mimeType) throws IOException {
    	
    	logger.info("Creating update resumable upload...");
    	Preconditions.checkArgument(fileId != null);
    	Preconditions.checkArgument(StringUtils.isNotEmpty(fileId.getId()));

    	CloseableHttpClient httpclient = null ;
    	CloseableHttpResponse response = null ;
    	
    	// https://developers.google.com/google-apps/documents-list/#updatingchanging_documents_and_files

    	try {
	        String putUri = "https://www.googleapis.com/upload/drive/v2/files?uploadType=resumable";
	        if (useOldApi) {
	        	putUri = getResumableUploadUpdateUri (fileId) ;
	        	//putUri = "https://docs.google.com/feeds/upload/create-session/default/private/full/file%3A";
	        	//putUri = putUri + fileId.getId() ;
	        } else {
	        	// TODO: new api
	        	// ...
	        	throw new IllegalStateException ("Not implemented") ;
	        }
	    	httpclient = getHttpClient () ;
	        HttpPut httpPut = new HttpPut(putUri);
	        httpPut.addHeader("Authorization", auth.getAuthHeader());
	        httpPut.addHeader("If-Match", "*");
	        httpPut.addHeader("X-Upload-Content-Type", mimeType.getMimeType());
	        httpPut.addHeader("X-Upload-Content-Length", getFileSizeString());
	        httpPut.addHeader("GData-Version","3");
	        
	        //logger.info("Create Update Resumable Upload: " + httpPut.toString());
	        response = httpclient.execute(httpPut);
	        @SuppressWarnings("unused")
			BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
	        EntityUtils.consume(response.getEntity());
	        String location = "";
	        if (response.getStatusLine().getStatusCode() == 200) {
	            location = response.getHeaders("Location")[0].getValue();
	            //logger.info("Location: " + location);
	        }
	        return location;
        } finally {
        	if (response != null) {
        		response.close();
        	}
        	if (httpclient != null) {
        		httpclient.close();
        	}
        }
    }
}
