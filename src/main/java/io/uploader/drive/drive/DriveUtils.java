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

// see https://developers.google.com/drive/v2/reference/
// see https://developers.google.com/drive/web/search-parameters

package io.uploader.drive.drive;

import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Children;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Insert;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.common.base.Preconditions;

import io.uploader.drive.config.Configuration;
import io.uploader.drive.config.HasConfiguration;
import io.uploader.drive.drive.largefile.GDriveUpdater;
import io.uploader.drive.drive.largefile.GDriveUploader;
import io.uploader.drive.drive.media.CustomDriveApiProgressListener;
import io.uploader.drive.drive.media.CustomProgressListener;
import io.uploader.drive.drive.media.MediaHttpUploader;
import io.uploader.drive.util.FileUtils.InputStreamProgressFilter;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriveUtils {

	private static final Logger logger = LoggerFactory.getLogger(DriveUtils.class);
	
	private DriveUtils() {
		super();
		throw new IllegalStateException();
	}

	
	public interface HasDescription {
		public abstract String getDescription () ;
	}
	
	
	public interface HasId {
		public abstract String getId () ;
	}
	
	
	public interface HasMimeType {
		public abstract String getMimeType () ; 
	}
	
	
	public static HasId newId (final String id) {
		return new HasId () {

			@Override
			public String getId() {
				return (id); 
			}} ;
	}
	
	
	public static HasId newId (final File file) {
		return new HasId () {

			@Override
			public String getId() {
				return ((file == null)?(null):(file.getId())) ;
			}} ;
	}
	
	
	public static HasMimeType newMineType (final String mineType) {
		return new HasMimeType () {

			@Override
			public String getMimeType() {
				return (mineType); 
			}} ;
	}
	
	
	public static HasMimeType newMineType (final File file) {
		return new HasMimeType () {

			@Override
			public String getMimeType() {
				return ((file == null)?(null):(file.getMimeType())) ;
			}} ;
	}
	
	
	private static final String mimeTypeDirectory = "application/vnd.google-apps.folder";
	
	private static final long largeFileMinimumSize = 30 * 1024 * 1024 ;
	
	public static File getFile(Drive service, HasId id) throws IOException {
		
		Preconditions.checkNotNull(service) ;
		Preconditions.checkNotNull(id) ;
		if (org.apache.commons.lang3.StringUtils.isEmpty(id.getId())) {
			throw new IllegalArgumentException () ;
		}
		return service.files().get(id.getId()).execute();
	}
	
	
	public static ChildList getChildren(Drive service, File file) throws IOException {
		
		Preconditions.checkNotNull(file) ;
		if (org.apache.commons.lang3.StringUtils.isEmpty(file.getId())) {
			throw new IllegalArgumentException () ;
		}
		Children.List request = service.children().list(file.getId());
		return request.execute();
	}
	
	
	public static boolean isDirectory(File file)
			throws IOException {
		return (file == null) ? (false) : (mimeTypeDirectory.equals(file.getMimeType())) ;
	}
	
	
	/**
	 * Insert new folder.
	 * 
	 * @param service
	 *            Drive API service instance.
	 * @param title
	 *            Title of the folder to insert.
	 * @param description
	 *            Description of the file to insert.
	 * @param parentId
	 *            Optional parent folder's ID.
	 * @return Inserted file metadata if successful, {@code null} otherwise.
	 * @throws IOException
	 */
	public static File insertDirectory(Drive service, String title,
			HasDescription description, HasId parentId)
			throws IOException {

		if (service == null
				|| org.apache.commons.lang3.StringUtils.isEmpty(title)) {
			throw new IllegalArgumentException();
		}

		File body = new File();
		body.setTitle(title);
		body.setDescription((description==null) ? (null) : (description.getDescription()));
		body.setMimeType(mimeTypeDirectory);

		// Set the parent folder.
		if (parentId != null) {
			if (org.apache.commons.lang3.StringUtils.isNotEmpty(parentId.getId())) {
				body.setParents(Arrays.asList(new ParentReference().setId(parentId.getId())));
			}
		}

		File file = service.files().insert(body).execute();
		return file;
	}
	

	/**
	 * Permanently delete a file, skipping the trash.
	 * 
	 * @param service
	 *            Drive API service instance.
	 * @param fileId
	 *            ID of the file to delete.
	 * @throws IOException
	 */
	public static void deleteFile(Drive service, String fileId) throws IOException {
		if (service == null || org.apache.commons.lang3.StringUtils.isEmpty(fileId)) {
			throw new IllegalArgumentException();
		}
		service.files().delete(fileId).execute();
	}
	
	
	/**
	 * Move a file to the trash.
	 * 
	 * @param service
	 *            Drive API service instance.
	 * @param fileId
	 *            ID of the file to trash.
	 * @return The updated file if successful, {@code null} otherwise.
	 * @throws IOException 
	 */
	public static File trashFile(Drive service, HasId fileId) throws IOException {
		if (service == null || fileId == null || org.apache.commons.lang3.StringUtils.isEmpty(fileId.getId())) {
			throw new IllegalArgumentException();
		}
		return service.files().trash(fileId.getId()).execute();
	}

	
	/**
	 * Find folders.
	 * 
	 * @param service
	 *            Drive API service instance.
	 * @param parentId
	 *            Optional parent folder's ID.
	 * @param maxResults
	 *            Optional maximum number of folders in the returned list.
	 * @return List of file metadatas.
	 * @throws IOException
	 */
	public static FileList findDirectories(Drive service,
			HasId parentId, Integer maxResults)
			throws IOException {
		return findDirectoriesWithTitle (service, null, parentId, maxResults) ;
	}
	
	
	/**
	 * Find folders.
	 * 
	 * @param service
	 *            Drive API service instance.
	 * @param title
	 *            Optional title of the folder.
	 * @param parentId
	 *            Optional parent folder's ID.
	 * @param maxResults
	 *            Optional maximum number of folders in the returned list.
	 * @return List of file metadatas.
	 * @throws IOException
	 */
	public static FileList findDirectoriesWithTitle(Drive service,
			String title, HasId parentId, Integer maxResults)
			throws IOException {

		Files.List request = service.files().list();
		if (maxResults != null && maxResults.intValue() > 0) {
			request = request.setMaxResults(maxResults);
		}
		StringBuilder query = new StringBuilder();
		if (org.apache.commons.lang3.StringUtils.isNotEmpty(title)) {
			query.append("title = '");
			query.append(escape(title)); 
			query.append("' and  ");
		}
		query.append("mimeType='");
		query.append(mimeTypeDirectory);
		query.append("' and trashed=false");
		if (parentId != null && org.apache.commons.lang3.StringUtils.isNotEmpty(parentId.getId())) {
			query.append(" and '");
			query.append(escape(parentId.getId()));
			query.append("' in parents");
		} else {
			query.append(" and '");
			query.append("root");
			query.append("' in parents");
		}
		
		logger.info("findDirectoriesWithTitle: " + query.toString()) ;
		
		request = request.setQ(query.toString());
		FileList files = request.execute();
		return files;
	}
	
	
	private static String escape (String str) {
		if (str == null) {
			return null ;
		}
		return str.replace("'", "\\'") ; //.replace("%", "\\%") ;
	}
	
	
	/**
	 * Find files.
	 * 
	 * @param service
	 *            Drive API service instance.
	 * @param title
	 *            Title of the file, including the extension.
	 * @param parentId
	 *            Optional parent folder's ID.
	 * @param mimeType
	 *            Optional MIME type of the file.
	 * @param maxResults
	 *            Optional maximum number of files in the returned list.
	 * @return List of file metadatas.
	 * @throws IOException
	 */
	public static FileList findFilesWithTitleAndMineType(Drive service,
			String title, HasId parentId, HasMimeType mineType, Integer maxResults)
			throws IOException {

		Files.List request = service.files().list();
		if (maxResults != null && maxResults.intValue() > 0) {
			request = request.setMaxResults(maxResults);
		}
		StringBuilder query = new StringBuilder();
		query.append("title = '");
		query.append(escape(title));
		query.append("'");
		if (mineType != null && org.apache.commons.lang3.StringUtils.isNotEmpty(mineType.getMimeType())) {
			query.append(" and mimeType='");
			query.append(mineType.getMimeType());
			query.append("'");
		}
		query.append(" and trashed=false");
		if (parentId != null && org.apache.commons.lang3.StringUtils.isNotEmpty(parentId.getId())) {
			query.append(" and '");
			query.append(escape(parentId.getId()));
			query.append("' in parents");
		} else {
			query.append(" and '");
			query.append("root");
			query.append("' in parents");
		}
		
		logger.info("findFilesWithTitleAndMineType: " + query.toString()) ;
		
		request = request.setQ(query.toString());
		FileList files = request.execute();
		return files;
	}

	
	/**
	 * Insert new file.
	 * 
	 * @param service
	 *            Drive API service instance.
	 * @param title
	 *            Title of the file to insert, including the extension.
	 * @param description
	 *            Description of the file to insert.
	 * @param parentId
	 *            Optional parent folder's ID.
	 * @param mimeType
	 *            MIME type of the file to insert.
	 * @param filename
	 *            Filename of the file to insert.
	 * @return Inserted file metadata if successful, {@code null} otherwise.
	 * @throws IOException
	 */
	public static File insertFile(Drive service, String title,
			HasDescription description, HasId parentId, HasMimeType mimeType,
			String filename, InputStreamProgressFilter.StreamProgressCallback progressCallback) throws IOException {
		
		return insertFile(Configuration.INSTANCE, service, title,
				description, parentId, mimeType,
				filename, progressCallback) ;
	}
	
	
	/**
	 * Insert new file.
	 * 
	 * @param config
	 *            Configuration object.
	 * @param service
	 *            Drive API service instance.
	 * @param title
	 *            Title of the file to insert, including the extension.
	 * @param description
	 *            Description of the file to insert.
	 * @param parentId
	 *            Optional parent folder's ID.
	 * @param mimeType
	 *            MIME type of the file to insert.
	 * @param filename
	 *            Filename of the file to insert.
	 * @return Inserted file metadata if successful, {@code null} otherwise.
	 * @throws IOException
	 */
	public static File insertFile(HasConfiguration config, Drive service, String title,
			HasDescription description, HasId parentId, HasMimeType mimeType,
			String filename, InputStreamProgressFilter.StreamProgressCallback progressCallback) throws IOException {

		if (service == null
				|| org.apache.commons.lang3.StringUtils.isEmpty(title)
				|| org.apache.commons.lang3.StringUtils.isEmpty(filename)) {
			throw new IllegalArgumentException();
		}

		final String type = (mimeType==null) ? (null) : (mimeType.getMimeType()) ;
		
		// File's metadata.
		File body = new File();
		body.setTitle(title);
		body.setDescription((description==null) ? (null) : (description.getDescription()));
		body.setMimeType(type);
		body.setOriginalFilename(filename);

		// Set the parent folder.
		if (parentId != null) {
			if (org.apache.commons.lang3.StringUtils.isNotEmpty(parentId.getId())) {
				body.setParents(Arrays.asList(new ParentReference().setId(parentId.getId())));
			}
		}

		// https://code.google.com/p/google-api-java-client/wiki/MediaUpload
		BasicFileAttributes attr = io.uploader.drive.util.FileUtils
				.getFileAttr(Paths.get(filename));
		boolean useMediaUpload = (attr != null && attr.size() > largeFileMinimumSize);
		
		boolean useOldApi = true ;
		boolean useCustomMediaUpload = true ;
		
		if (useMediaUpload) {
			File file = null ;
			
			// if large file, there exists a nasty bug in the new API which remains unresolved, 
			// therefore we currently need to rely on the old API (even though it has been deprecated...)
			// see: https://code.google.com/p/google-api-python-client/issues/detail?id=231
			if (useOldApi) {
				GDriveUploader upload = new GDriveUploader(config,
						title, description, parentId, mimeType, filename,
						progressCallback);

				String fileId = upload.uploadFile();
				Preconditions.checkState(org.apache.commons.lang3.StringUtils.isNotEmpty(fileId));
				// get the file from response
				file = getFile (service, newId(fileId)) ;
			}
			else
			{		
				logger.info("Media Upload is used for large files");
				java.io.File mediaFile = new java.io.File(filename);
				InputStreamContent mediaContent = new InputStreamContent(type,
						new BufferedInputStream(
								io.uploader.drive.util.FileUtils
										.getInputStreamWithProgressFilter(
												progressCallback, mediaFile.length(),
												new FileInputStream(mediaFile))));	
	
				mediaContent.setRetrySupported(true) ;
				// TODO: there seems to exist a bug when the size is set... (java.io.IOException: insufficient data written)
				// ...
				//mediaContent.setLength(mediaFile.length());
				mediaContent.setLength(-1);
				
				Drive.Files.Insert request = service.files().insert(body, mediaContent);
				
				if (useCustomMediaUpload) {
					MediaHttpUploader mediaHttpUploader = new MediaHttpUploader (
							mediaContent, 
							request.getAbstractGoogleClient().getRequestFactory().getTransport(), 
							request.getAbstractGoogleClient().getRequestFactory().getInitializer()) ;
					
					mediaHttpUploader.setDisableGZipContent(true) ;
					mediaHttpUploader.setProgressListener(new CustomProgressListener());

					HttpResponse response = mediaHttpUploader.upload(request.buildHttpRequestUrl());
					try {
						if (!response.isSuccessStatusCode()) {
							logger.error ("Error occurred while transferring the large file: " + response.getStatusMessage() + " (Status code: "+ response.getStatusCode()+ ")") ;
						} 
						file = response.parseAs(com.google.api.services.drive.model.File.class) ;
					} finally {
						response.disconnect();
					}
					
				} else {
					request.getMediaHttpUploader().setDisableGZipContent(true) ;
					request.getMediaHttpUploader().setProgressListener(new CustomDriveApiProgressListener()) ;
					request.setDisableGZipContent(true) ;
					file = request.execute();
				}
			}
			return file ;
		} else {
			// File's content.
			java.io.File fileContent = new java.io.File(filename);
			DriveFileContent mediaContent = new DriveFileContent(type,
					fileContent, progressCallback);
			Insert insert = service.files().insert(body, mediaContent) ;
			return insert.execute();
		}
	}
	
	
	/**
	 * Update an existing file's metadata and content.
	 * 
	 * @param service
	 *            Drive API service instance.
	 * @param fileId
	 *            ID of the file to update.
	 * @param newTitle
	 *            New title for the file.
	 * @param newDescription
	 *            New description for the file.
	 * @param newMimeType
	 *            New MIME type for the file.
	 * @return Updated file metadata if successful, {@code null} otherwise.
	 * @throws IOException
	 */
	public static File updateFile(Drive service, HasId fileId, String newTitle,
			HasDescription newDescription, HasMimeType newMimeType,
			String filename,
			InputStreamProgressFilter.StreamProgressCallback progressCallback)
			throws IOException {
		
		return updateFile (Configuration.INSTANCE, service, fileId, newTitle,
				newDescription, newMimeType, filename, progressCallback) ;	
	}
	
	
	/**
	 * Update an existing file's metadata and content.
	 * 
	 * @param config
	 *            Configuration object.
	 * @param service
	 *            Drive API service instance.
	 * @param fileId
	 *            ID of the file to update.
	 * @param newTitle
	 *            New title for the file.
	 * @param newDescription
	 *            New description for the file.
	 * @param newMimeType
	 *            New MIME type for the file.
	 * @return Updated file metadata if successful, {@code null} otherwise.
	 * @throws IOException
	 */	
	public static File updateFile(HasConfiguration config, Drive service, HasId fileId, String newTitle,
			HasDescription newDescription, HasMimeType newMimeType,
			String filename,
			InputStreamProgressFilter.StreamProgressCallback progressCallback)
			throws IOException {

		Preconditions.checkNotNull(fileId);
		if (org.apache.commons.lang3.StringUtils.isEmpty(fileId.getId())) {
			throw new IllegalArgumentException();
		}
		
		// First retrieve the file from the API.
		File file = service.files().get(fileId.getId()).execute();

		// File's new metadata.
		if (org.apache.commons.lang3.StringUtils.isNotEmpty(newTitle)) {
			file.setTitle(newTitle);
		}
		if (newDescription != null) {
			file.setDescription(newDescription.getDescription());
		}
		if (newMimeType != null) {
			file.setMimeType(newMimeType.getMimeType());
		}

		// if large file, the same bug as for uploading exists, therefore we currently need to rely on the old api
		// see: https://code.google.com/p/google-api-python-client/issues/detail?id=231
		boolean useOldApi = true ;
		boolean useMediaUpload = false ;
		
		DriveFileContent mediaContent = null ;
		if (org.apache.commons.lang3.StringUtils.isNotEmpty(filename)) {
			
			BasicFileAttributes attr = io.uploader.drive.util.FileUtils
					.getFileAttr(Paths.get(filename));
			useMediaUpload = (attr != null && attr.size() > largeFileMinimumSize);
					
			java.io.File fileContent = new java.io.File(filename);
			mediaContent = new DriveFileContent(file.getMimeType(), fileContent, progressCallback);
		}

		// Send the request to the API.
		File updatedFile = null ;
		if (useMediaUpload) {
			// update metadata
			logger.info("Update metadata");
			updatedFile = service.files().update(fileId.getId(), file).execute();
			
			// we need to upload the new media content
			logger.info("Update content");
			if (useOldApi) {
				GDriveUpdater upload = new GDriveUpdater(config, newId(updatedFile.getId()), newMimeType, filename, progressCallback) ;
				upload.updateFile();
			} else {
				// TODO:
				// ...
				throw new IllegalStateException ("Not implemented") ;
			}
			updatedFile = getFile (service, newId(updatedFile.getId())) ;
		} else {
			// update metadata, and content (if any) of small files
			if (mediaContent != null) {
				updatedFile = service.files().update(fileId.getId(), file, mediaContent).execute();
			} else {
				updatedFile = service.files().update(fileId.getId(), file).execute();
			}
		}
		return updatedFile;
	}
}
