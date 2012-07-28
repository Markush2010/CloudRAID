/*
 * Copyright 2011 - 2012 by the CloudRAID Team
 * see AUTHORS for more details
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.dhbw_mannheim.cloudraid.sugarsync.impl.net.connector;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.dhbw_mannheim.cloudraid.config.ICloudRAIDConfig;
import de.dhbw_mannheim.cloudraid.config.exceptions.MissingConfigValueException;
import de.dhbw_mannheim.cloudraid.core.net.connector.IStorageConnector;

/**
 * The API wrapper for SugarSync.
 * 
 * @author Florian Bausch
 * 
 */
public class SugarSyncConnector implements IStorageConnector {

	private final static String APP_AUTH_URL = "https://api.sugarsync.com/app-authorization";
	private final static String AUTH_URL = "https://api.sugarsync.com/authorization";

	/**
	 * Creates an HTTPS connection with some predefined values
	 * 
	 * @param address
	 *            The address to connect to
	 * @param authToken
	 *            The authentication token
	 * @param method
	 *            The HTTP method
	 * 
	 * @return A preconfigured connection.
	 * @throws IOException
	 *             Thrown, if the connection cannot be established
	 */
	private static HttpsURLConnection getConnection(String address,
			String authToken, String method) throws IOException {
		System.out.println("getConnection: " + address);
		HttpsURLConnection con = (HttpsURLConnection) new URL(address)
				.openConnection();
		con.setRequestMethod(method);
		con.setRequestProperty("User-Agent", "CloudRAID");
		con.setRequestProperty("Accept", "*/*");
		con.setRequestProperty("Authorization", authToken);
		return con;
	}

	private String splitOutputDir = null;

	/**
	 * A reference to the current config;
	 */
	private ICloudRAIDConfig config = null;

	private String baseURL = null;

	private DocumentBuilder docBuilder;

	private String accessToken = "";

	private String accessKeyId, privateAccessKey, refreshToken, userURL;
	private int id = -1;

	private long expirationDate = 0L;

	/**
	 * A SimpleDateFormat for parsing the expiration date from SugarSync.
	 */
	private SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd'T'hhmmss.S Z");

	/**
	 * This HashMap contains name -> URL mappings, where name is the name of a
	 * resource and URL the regarding URL.
	 */
	private HashMap<String, String> urlCache = new HashMap<String, String>();

	@Override
	public boolean connect() {
		// Do not connect, if the current accessToken does not expire within the
		// next two minutes.
		if (this.expirationDate - 2L * 60 * 1000 > System.currentTimeMillis()) {
			return true;
		}
		try {
			// Get the Access Token
			HttpsURLConnection con = SugarSyncConnector.getConnection(
					SugarSyncConnector.AUTH_URL, "", "POST");
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type",
					"application/xml; charset=UTF-8");

			// Create authentication request
			String authReq = String.format(
					"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
							+ "<tokenAuthRequest><accessKeyId>%s"
							+ "</accessKeyId><privateAccessKey>%s"
							+ "</privateAccessKey><refreshToken>%s"
							+ "</refreshToken></tokenAuthRequest>",
					new Object[] { this.accessKeyId, this.privateAccessKey,
							this.refreshToken });

			con.connect();
			Document doc = null;
			try {
				con.getOutputStream().write(authReq.getBytes());
				this.accessToken = con.getHeaderField("Location");
				doc = this.docBuilder.parse(con.getInputStream());
				con.getInputStream().close();
			} finally {
				con.disconnect();
			}
			String expiration = doc.getElementsByTagName("expiration").item(0)
					.getTextContent().trim();
			expiration = expiration.substring(0, 23) + " "
					+ expiration.substring(23);
			this.expirationDate = this.sdf.parse(expiration.replace(":", ""))
					.getTime();
			this.userURL = doc.getElementsByTagName("user").item(0)
					.getTextContent().trim();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * This function initializes the {@link SugarSyncConnector} with the
	 * customer and application tokens. During the {@link #connect()} process
	 * various tokens are used. If {@link #connect()} returns <code>false</code>
	 * , this class has to be re-instantiated and initialized with proper
	 * credentials. </br>
	 * 
	 * The {@link ICloudRAIDConfig} must contain following keys:
	 * <ul>
	 * <li><code>connector.ID.username</li>
	 * <li><code>connector.ID.customer_secret</code></li>
	 * <li><code>connector.ID.accessKeyId</code></li>
	 * <li><code>connector.ID.privateAccessKey</code></li>
	 * </ul>
	 * 
	 * @param connectorid
	 *            The internal id of this connector.
	 * @param config
	 *            The reference to a running {@link ICloudRAIDConfig} service.
	 * 
	 * @throws InstantiationException
	 *             Thrown if not all required parameters are passed.
	 */
	@Override
	public IStorageConnector create(int connectorid, ICloudRAIDConfig config)
			throws InstantiationException {
		this.id = connectorid;
		this.config = config;
		String kUsername = String.format("connector.%d.username", this.id);
		String kPassword = String.format("connector.%d.password", this.id);
		String kAccessKey = String.format("connector.%d.accessKey", this.id);
		String kPrivateAccessKey = String.format(
				"connector.%d.privateAccessKey", this.id);
		String kRefresh = String.format("connector.%d.refreshToken", this.id);
		String kAppKey = String.format("connector.%d.appKey", this.id);
		try {
			this.splitOutputDir = this.config.getString("split.output.dir");
			if (!this.config.keyExists(kRefresh)) {
				if (this.config.keyExists(kUsername)
						&& this.config.keyExists(kPassword)
						&& this.config.keyExists(kAccessKey)
						&& this.config.keyExists(kPrivateAccessKey)
						&& this.config.keyExists(kAppKey)) {
					this.accessKeyId = this.config.getString(kAccessKey);
					this.privateAccessKey = this.config
							.getString(kPrivateAccessKey);
				} else {
					throw new InstantiationException(kUsername + ", "
							+ kPassword + ", " + kAccessKey + " and "
							+ kPrivateAccessKey
							+ " have to be set in the config!");
				}
				this.refreshToken = getRefreshToken(
						this.config.getString(kUsername),
						this.config.getString(kPassword),
						this.config.getString(kAppKey));
				if (this.refreshToken == null) {
					throw new InstantiationException(
							"Could not get SugarSync refresh token.");
				}
				this.config.put(kRefresh, this.refreshToken, true);
				this.config.save();
			} else {
				if (this.config.keyExists(kAccessKey)
						&& this.config.keyExists(kPrivateAccessKey)) {
					this.accessKeyId = this.config.getString(kAccessKey);
					this.privateAccessKey = this.config
							.getString(kPrivateAccessKey);
				} else {
					throw new InstantiationException(kAccessKey + " and "
							+ kPrivateAccessKey
							+ " have to be set in the config!");
				}
				this.refreshToken = config.getString(kRefresh);
			}
			this.docBuilder = null;
			try {
				this.docBuilder = DocumentBuilderFactory.newInstance()
						.newDocumentBuilder();
				this.docBuilder.setErrorHandler(null);
			} catch (ParserConfigurationException e) {
				throw new InstantiationException(e.getMessage());
			}
		} catch (MissingConfigValueException e) {
			throw new InstantiationException(e.getMessage());
		}
		return this;
	}

	/**
	 * Creates a file on SugarSync.
	 * 
	 * @param name
	 *            The file name.
	 * @param f
	 *            The file to be uploaded.
	 * @param parent
	 *            The URL to the parent.
	 * @throws IOException
	 *             Thrown, if no data can be read
	 * @throws SAXException
	 *             Thrown, if the content cannot be parsed
	 * @throws ParserConfigurationException
	 *             Thrown, if the content cannot be parsed
	 */
	private void createFile(String name, File f, String parent)
			throws IOException, SAXException, ParserConfigurationException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file><displayName>"
				+ name
				+ "</displayName><mediaType>"
				+ "application/pdf"
				+ "</mediaType></file>";
		HttpsURLConnection con = SugarSyncConnector.getConnection(parent,
				this.accessToken, "POST");
		con.setRequestProperty("Content-Type", "text/xml");
		con.setDoOutput(true);

		con.connect();
		try {
			con.getOutputStream().write(request.getBytes());
			con.getOutputStream().close();
			// Do not remove the following line.
			con.getResponseCode();
		} finally {
			con.disconnect();
		}

		String url = con.getHeaderField("Location");
		this.urlCache.put(name, url);

		con = SugarSyncConnector.getConnection(url + "/data", this.accessToken,
				"PUT");
		con.setDoOutput(true);
		con.setRequestProperty("Content-Type", "application/pdf");
		OutputStream os = null;
		InputStream is = null;
		try {
			con.connect();
			os = con.getOutputStream();
			is = new FileInputStream(f);
			int i;
			while ((i = is.read()) >= 0) {
				os.write(i);
			}
		} finally {
			try {
				if (os != null) {
					os.close();
				}
			} catch (IOException ignore) {
			}
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException ignore) {
			}
			// Do not remove the following line.
			con.getResponseCode();
			con.disconnect();
		}
	}

	@Override
	public boolean delete(String resource) {
		connect();
		boolean ret = performDelete(resource, String.valueOf(this.id));
		if (ret) {
			if (!performDelete(resource, "m")) {
				System.err
						.println("The data file has been removed. But unfortunately the meta data file has not been removed!");
			}
		}
		return ret;
	}

	@Override
	public void disconnect() {
	}

	/**
	 * Checks, if a file is in the specific folder on the SugarSync servers.
	 * 
	 * @param name
	 *            The file name.
	 * @param parent
	 *            The URL to the parent folder.
	 * @return The URL to the file, or null, if it could not be found.
	 * @throws SAXException
	 *             Thrown, if the content cannot be parsed
	 * @throws IOException
	 *             Thrown, if no data can be written
	 * @throws ParserConfigurationException
	 *             Thrown, if the content cannot be parsed
	 */
	private String findFileInFolder(String name) throws SAXException,
			IOException, ParserConfigurationException {
		String url = this.urlCache.get(name);
		if (url != null) {
			return url;
		}
		Document doc = null;
		String parent = this.getBaseUrl() + "/contents?type=file";
		HttpsURLConnection con = SugarSyncConnector.getConnection(parent,
				this.accessToken, "GET");
		con.setDoInput(true);

		// Build the XML tree.
		con.connect();
		try {
			doc = this.docBuilder.parse(con.getInputStream());
			con.getInputStream().close();
		} finally {
			con.disconnect();
		}
		NodeList nl = doc.getDocumentElement().getElementsByTagName("file");
		for (int i = 0; i < nl.getLength(); i++) {
			String displayName = ((Element) nl.item(i))
					.getElementsByTagName("displayName").item(0)
					.getTextContent();
			if (displayName.equalsIgnoreCase(name)) {
				url = ((Element) nl.item(i)).getElementsByTagName("ref")
						.item(0).getTextContent();
				this.urlCache.put(name, url);
				return url;
			}
		}
		return null;
	}

	@Override
	public InputStream get(String resource) {
		return performGet(resource, String.valueOf(this.id));
	}

	/**
	 * Loads and caches the URL to the 'Magic Briefcase' folder.
	 * 
	 * @return The URL to the 'Magic Briefcase' folder on SugarSync.
	 * @throws IOException
	 *             Thrown, if no connection can be established
	 * @throws SAXException
	 *             Thrown, if the content cannot be parsed
	 * @throws ParserConfigurationException
	 *             Thrown, if the content cannot be parsed
	 */
	private synchronized String getBaseUrl() throws IOException, SAXException,
			ParserConfigurationException {
		if (this.baseURL == null) {
			Document doc = null;
			HttpsURLConnection con = SugarSyncConnector.getConnection(
					this.userURL, this.accessToken, "GET");
			con.setDoInput(true);

			// Build the XML tree.
			con.connect();
			try {
				doc = this.docBuilder.parse(con.getInputStream());
				con.getInputStream().close();
			} finally {
				con.disconnect();
			}

			Element node = (Element) doc.getDocumentElement()
					.getElementsByTagName("webArchive").item(0);
			this.baseURL = node.getTextContent().trim();
		}
		return this.baseURL;
	}

	@Override
	public byte[] getMetadata(String resource, int size) {
		InputStream is = performGet(resource, "m");
		if (is == null) {
			return null;
		}
		BufferedInputStream bis = new BufferedInputStream(is);
		byte meta[] = new byte[size];
		Arrays.fill(meta, (byte) 0);
		try {
			bis.read(meta, 0, size);
		} catch (IOException ignore) {
			meta = null;
		} finally {
			try {
				bis.close();
			} catch (Exception ignore) {
			}
		}
		return meta;
	}

	private String getRefreshToken(String username, String password,
			String appKey) {
		try {
			// Get the Access Token
			HttpsURLConnection con = SugarSyncConnector.getConnection(
					SugarSyncConnector.APP_AUTH_URL, "", "POST");
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type",
					"application/xml; charset=UTF-8");

			// Create authentication request
			String authReq = String.format(
					"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<appAuthorization><username>%s"
							+ "</username><password>%s"
							+ "</password>\n\t<application>%s"
							+ "</application>\n\t<accessKeyId>%s"
							+ "</accessKeyId><privateAccessKey>%s"
							+ "</privateAccessKey></appAuthorization>",
					new Object[] { username, password, appKey,
							this.accessKeyId, this.privateAccessKey });

			con.connect();
			try {
				con.getOutputStream().write(authReq.getBytes());
				return con.getHeaderField("Location");
			} finally {
				con.disconnect();
			}
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Executes the actual deletion of a file.
	 * 
	 * @param resource
	 *            The resource name.
	 * @param extension
	 *            The extension of the file.
	 * @return true, if the deletion was successful; false, if not.
	 */
	private boolean performDelete(String resource, String extension) {
		resource += "." + extension;
		// Find URL of resource in parent directory
		try {
			String resourceURL = this.findFileInFolder(resource);
			if (!this.performDeleteResource(resource, resourceURL)) {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return true;
		}
		return true;
	}

	/**
	 * Actually deletes a resource on the given URL.
	 * 
	 * @param name
	 *            The name of the resource.
	 * @param resourceURL
	 *            The URL of the resource.
	 */
	private boolean performDeleteResource(String name, String resourceURL) {
		HttpsURLConnection con = null;
		try {
			con = SugarSyncConnector.getConnection(resourceURL,
					this.accessToken, "DELETE");
			con.setDoInput(true);
			con.connect();
			int respCode = con.getResponseCode();
			if (!(respCode == 404 || (respCode >= 200 && respCode <= 299))) {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
		this.urlCache.remove(name);
		return true;
	}

	/**
	 * Executes the actual get to the SugarSync servers.
	 * 
	 * @param resource
	 *            The resource name.
	 * @param extension
	 *            The extension of the resource.
	 * @return The InputStream that reads from the server.
	 */
	private InputStream performGet(String resource, String extension) {
		connect();
		resource += "." + extension;
		try {
			String resourceURL = this.findFileInFolder(resource);

			HttpsURLConnection con;
			con = SugarSyncConnector.getConnection(resourceURL + "/data",
					this.accessToken, "GET");
			con.setDoInput(true);

			return con.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Executes the actual update of a resource to the SugarSync servers.
	 * 
	 * @param resource
	 *            The resource name.
	 * @param extension
	 *            The extension of the resource.
	 * @return true, if the update was successful; false, if not.
	 */
	private boolean performUpdate(String resource, String extension) {
		resource += "." + extension;
		File f = new File(this.splitOutputDir + "/" + resource);
		int maxFilesize;
		try {
			maxFilesize = this.config.getInt("filesize.max", null);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		if (!f.exists()) {
			System.err.println("File does not exist");
		} else if (f.length() > maxFilesize) {
			System.err.println("File too big");
		} else {
			try {
				String resourceURL = this.findFileInFolder(resource);
				if (resourceURL != null) {
					System.err.println("The file already exists. DELETE it. "
							+ resourceURL);
					this.performDeleteResource(resource, resourceURL);
					this.createFile(resource, f, this.getBaseUrl());
					return true;
				} else {
					System.err.println("No file found for update.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * Executes the actual file upload to the SugarSync servers.
	 * 
	 * @param resource
	 *            The resource name.
	 * @param extension
	 *            The extension of the resource.
	 * @return true, if the upload was successful; false, if not.
	 */
	private boolean performUpload(String resource, String extension) {
		resource += "." + extension;
		File f = new File(this.splitOutputDir + "/" + resource);
		int maxFilesize;
		try {
			maxFilesize = this.config.getInt("filesize.max", null);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		if (!f.exists()) {
			System.err.println("File does not exist");
		} else if (f.length() > maxFilesize) {
			System.err.println("File too big");
		} else {
			try {
				String resourceURL = this.findFileInFolder(resource);
				if (resourceURL != null) {
					System.err.println("The file already exists. DELETE it. "
							+ resourceURL);
					return false;
				}
				this.createFile(resource, f, this.getBaseUrl());
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} catch (SAXException e) {
				e.printStackTrace();
				return false;
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean update(String resource) {
		connect();
		boolean ret = performUpdate(resource, String.valueOf(this.id));
		if (ret) {
			// Upload metadata after successful data update.
			ret = performUpdate(resource, "m");
			if (!ret) {
				// If the metadata could not be updated, remove the data file.
				delete(resource);
			}
		}
		return ret;
	}

	@Override
	public boolean upload(String resource) {
		connect();
		boolean ret = performUpload(resource, String.valueOf(this.id));
		if (ret) {
			// Upload metadata after successful data upload.
			ret = performUpload(resource, "m");
			if (!ret) {
				// If the metadata could not be uploaded, remove the data file.
				delete(resource);
			}
		}
		return ret;
	}
}
