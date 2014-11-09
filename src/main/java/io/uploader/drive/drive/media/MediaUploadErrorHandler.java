/*
 * Copyright (c) 2014 Loic Merckel
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
*  http://code.google.com/p/google-api-java-client/source/checkout
*  package com.google.api.client.googleapis.media;
*  
*/

package io.uploader.drive.drive.media;

import com.google.api.client.http.HttpIOExceptionHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.util.Beta;
import com.google.api.client.util.Preconditions;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MediaUpload error handler handles an {@link IOException} and an abnormal HTTP
 * response by calling to {@link MediaHttpUploader#serverErrorCallback()}.
 *
 * @author Eyal Peled
 */
@Beta
class MediaUploadErrorHandler implements HttpUnsuccessfulResponseHandler,
		HttpIOExceptionHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(MediaUploadErrorHandler.class);
	
	private final AtomicInteger httpErrorCounter ;

	/** The uploader to callback on if there is a server error. */
	private final MediaHttpUploader uploader;

	/** The original {@link HttpIOExceptionHandler} of the HTTP request. */
	private final HttpIOExceptionHandler originalIOExceptionHandler;

	/**
	 * The original {@link HttpUnsuccessfulResponseHandler} of the HTTP request.
	 */
	private final HttpUnsuccessfulResponseHandler originalUnsuccessfulHandler;

	/**
	 * Constructs a new instance from {@link MediaHttpUploader} and
	 * {@link HttpRequest}.
	 */
	public MediaUploadErrorHandler(MediaHttpUploader uploader,
			HttpRequest request, AtomicInteger httpErrorCounter) {
		this.uploader = Preconditions.checkNotNull(uploader);
		originalIOExceptionHandler = request.getIOExceptionHandler();
		originalUnsuccessfulHandler = request.getUnsuccessfulResponseHandler();

		request.setIOExceptionHandler(this);
		request.setUnsuccessfulResponseHandler(this);
		
		this.httpErrorCounter = Preconditions.checkNotNull(httpErrorCounter) ;
	}

	@Override
	public boolean handleIOException(HttpRequest request, boolean supportsRetry)
			throws IOException {
		boolean handled = originalIOExceptionHandler != null
				&& originalIOExceptionHandler.handleIOException(request,
						supportsRetry);

		// TODO(peleyal): figure out what is best practice - call
		// serverErrorCallback only if I/O
		// exception was handled, or call it regardless
		if (handled) {
			try {
				uploader.serverErrorCallback();
			} catch (IOException e) {
				logger.warn("exception thrown while calling server callback", e);
			}
		}
		return handled;
	}

	private void exponentialBackoff(int n) {
		try {
			Thread.sleep((long) (Math.pow(2.0, n) * 1000 + Math.random() * 1000));
		} catch (InterruptedException e) {
			logger.error("Error occurred while sleeping", e);
		}
	}
	
	private final int maxRetry = 5 ;

	@Override
	public boolean handleResponse(HttpRequest request, HttpResponse response,
			boolean supportsRetry) throws IOException {
		boolean handled = originalUnsuccessfulHandler != null
				&& originalUnsuccessfulHandler.handleResponse(request,
						response, supportsRetry);

		// TODO(peleyal): figure out what is best practice - call
		// serverErrorCallback only if the
		// abnormal response was handled, or call it regardless
		
		int statusCode = response.getStatusCode() ;
		boolean retry = false ;
		
		StringBuilder sb = new StringBuilder () ;
		sb.append("Error ").append(statusCode).append(" (").append(response.getStatusMessage()).append(") occurred...") ;
		
		if (((statusCode < 600 && statusCode >= 500))
				&& httpErrorCounter.get() <= maxRetry) {
			// We need to resume the upload
			sb.append("attempting to resume the upload...") ;
			exponentialBackoff(httpErrorCounter.getAndIncrement()) ;
			retry = true ;
		} else if (statusCode != 308 && httpErrorCounter.getAndIncrement() < maxRetry) {
			// we try for other errors to resume the upload
			// (we do not need the exponentialBackoff)
			sb.append("attempting to resume the upload...") ;
			if (statusCode == 401) {

				// There is a nasty bug in the Drive API, which seems to be there for quite a while...
				// Basically after about one hour, the 401 error occurs, and there is not much we can do to resume
				// the upload...
				// https://code.google.com/p/google-api-python-client/issues/detail?id=231
				
				retry = false ;
			}
		}
		if (statusCode != 308) {
			logger.info(sb.toString());
		}
		if (handled && supportsRetry && retry /* && response.getStatusCode() / 100 == 5 */) {
			try {
				uploader.serverErrorCallback();
			} catch (IOException e) {
				logger.warn("exception thrown while calling server callback", e);
			}
		}
		return handled;
	}
}
