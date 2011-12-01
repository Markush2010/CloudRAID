package de.dhbw.mannheim.cloudraid.net.connector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.activation.MimetypesFileTypeMap;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.dhbw.mannheim.cloudraid.util.Config;

public class SugarSyncConnector implements IStorageConnector {

	private String token = "";
	private String username, password, accessKeyId, privateAccessKey;
	private DocumentBuilder docBuilder;
	private final static String AUTH_URL = "https://api.sugarsync.com/authorization";
	private final static String USER_INFO_URL = "https://api.sugarsync.com/user";

	private String baseURL = null;

	/**
	 * Does some stuff with the SugarSync API
	 * 
	 * @param username
	 * @param password
	 * @param accessKeyId
	 * @param privateAccessKey
	 */
	public SugarSyncConnector(String username, String password,
			String accessKeyId, String privateAccessKey) {
		this.password = password;
		this.username = username;
		this.accessKeyId = accessKeyId;
		this.privateAccessKey = privateAccessKey;
		docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			docBuilder.setErrorHandler(null);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * try {
		 * 
		 * // Get the Access Token HttpsURLConnection con = null; String
		 * sugarsyncAccess = ""; InputStream is = null;
		 * 
		 * // Get user information: // url = new //
		 * URL("https://api.sugarsync.com/workspace/:sc:2099477:0"); // url =
		 * new URL("https://api.sugarsync.com/user/"); // url = new //
		 * URL("https://api.sugarsync.com/user/2099477/folders/contents"); //
		 * url = new //
		 * URL("https://api.sugarsync.com/folder/:sc:2099477:2/contents"); con =
		 * SugarSyncConnector .getConnection(
		 * "https://api.sugarsync.com/folder/:sc:2099477:202_91974608/contents",
		 * sugarsyncAccess, "GET"); con.setDoInput(true); is =
		 * con.getInputStream(); int i;
		 * System.out.println("SugarSync response for your request:"); while ((i
		 * = is.read()) >= 0) System.out.print((char) i);
		 * System.out.println("\n");
		 * 
		 * // Get a file System.out.println("Get a file..."); con =
		 * SugarSyncConnector .getConnection(
		 * "https://api.sugarsync.com/file/:sc:2099477:202_91974881/data",
		 * sugarsyncAccess, "GET"); con.setDoInput(true);
		 * con.setAllowUserInteraction(false);
		 * 
		 * is = null; is = con.getInputStream();
		 * 
		 * File f = new File("/tmp/sugarFile.pdf"); FileOutputStream fos = null;
		 * fos = new FileOutputStream(f);
		 * 
		 * byte[] inputBytes = new byte[02000]; int readLength; while
		 * ((readLength = is.read(inputBytes)) >= 0) { fos.write(inputBytes, 0,
		 * readLength); } System.out.println("Done."); } catch (Exception e) {
		 * e.printStackTrace(); return; }
		 */
	}

	/**
	 * Creates an HTTPS connection with some predefined values
	 * 
	 * @return A preconfigured connection.
	 */
	private static HttpsURLConnection getConnection(String address,
			String authToken, String method) throws IOException {
		HttpsURLConnection con = (HttpsURLConnection) new URL(address)
				.openConnection();
		con.setRequestMethod(method);
		con.setRequestProperty("User-Agent", "CloudRAID");
		con.setRequestProperty("Accept", "*/*");
		con.addRequestProperty("Authorization", authToken);

		return con;
	}

	/**
	 * Connects to the SugarSync cloud service.
	 * 
	 * @param service
	 * @return true, if the service could be connected, false, if not.
	 */
	@Override
	public boolean connect(String service) {
		try {
			// Get the Access Token
			HttpsURLConnection con = SugarSyncConnector.getConnection(AUTH_URL,
					"", "POST");
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type",
					"application/xml; charset=UTF-8");

			// Create authentication request
			String authReq = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<authRequest>";
			authReq += "\n\t<username>" + username
					+ "</username>\n\t<password>" + password + "</password>";
			authReq += "\n\t<accessKeyId>" + accessKeyId + "</accessKeyId>";
			authReq += "\n\t<privateAccessKey>" + privateAccessKey
					+ "</privateAccessKey>";
			authReq += "\n</authRequest>";
			con.getOutputStream().write(authReq.getBytes());

			this.token = con.getHeaderField("Location");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Puts a resource to the SugarSync folder.
	 * 
	 * @param resource
	 *            The path (relative to /tmp) to the file to upload.
	 * @return true, if it could be uploaded.
	 */
	@Override
	public boolean put(String resource) {
		File f = new File("/tmp/" + resource);
		if (f.length() > Config.MAX_FILE_SIZE) {
			System.err.println("File too big");
		} else if (!f.exists()) {
			System.err.println("File does not exist");
		} else {
			try {
				String parent;
				if (resource.contains("/"))
					parent = this.getResourceURL(resource.substring(0,
							resource.lastIndexOf("/") + 1), true);
				else
					parent = this.getResourceURL("", true);
				// HttpsURLConnection con;
				// String resourceURL = this.findFileInFolder(
				// resource.substring(resource.lastIndexOf("/") + 1),
				// parent);
				// con = SugarSyncConnector.getConnection(resourceURL + "/data",
				// this.token, "GET");
				// con.setDoInput(true);

				this.createFile(
						resource.substring(resource.lastIndexOf("/") + 1), f,
						parent);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Gets a resource (file)
	 * 
	 * @param the
	 *            path on the SugarSync system.
	 * @return Either an InputStream, if resource could be found, or
	 *         <code>null</code>, if the resource could not be found.
	 */
	@Override
	public InputStream get(String resource) {
		try {
			String parent;
			if (resource.contains("/"))
				parent = this.getResourceURL(
						resource.substring(0, resource.lastIndexOf("/") + 1),
						false);
			else
				parent = this.getResourceURL("", false);
			HttpsURLConnection con;
			String resourceURL = this.findFileInFolder(
					resource.substring(resource.lastIndexOf("/") + 1), parent
							+ "/contents?type=file");
			con = SugarSyncConnector.getConnection(resourceURL + "/data",
					this.token, "GET");
			con.setDoInput(true);
			return con.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Permanently deletes a file from the SugarSync servers.
	 * 
	 * @param resource
	 *            The resource to be deleted.
	 * @return true, if the resource could be deleted, false, if not.
	 */
	@Override
	public boolean delete(String resource) {
		try {
			String parent;
			if (resource.contains("/"))
				parent = this.getResourceURL(
						resource.substring(0, resource.lastIndexOf("/") + 1),
						false);
			else
				parent = this.getResourceURL("", false);
			HttpsURLConnection con;
			String resourceURL = this.findFileInFolder(
					resource.substring(resource.lastIndexOf("/") + 1), parent
							+ "/contents?type=file");
			con = SugarSyncConnector.getConnection(resourceURL, this.token,
					"DELETE");
			System.out.println(con.getResponseCode() + ": "
					+ con.getResponseMessage());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public String post(String resource, String parent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] options(String resource) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String head(String resource) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Runs recursively through the folders in 'Magic Briefcase' to find the
	 * specified folder.
	 * 
	 * @param resource
	 *            The folder to be found.
	 * @param createResource
	 *            Create missing folders.
	 * @return The URL to the folder.
	 */
	private String getResourceURL(String resource, boolean createResource) {
		try {
			String folder = this.getBaseUrl();
			System.out.println(folder);
			while (resource.contains("/")) {
				String parent = folder;
				folder += "/contents?type=folder";
				String nextName = resource.substring(0, resource.indexOf("/"));
				System.out.println(resource);

				folder = this.findFolderInFolder(nextName, folder);

				resource = resource.substring(resource.indexOf("/") + 1);
				if (createResource && folder == null) {
					this.createFolder(nextName, parent);
					folder = this.findFolderInFolder(nextName, parent
							+ "/contents?type=folder");
				}
			}
			return folder;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Checks, if a folder is in the specific folder on the SugarSync servers.
	 * 
	 * @param name
	 *            The folder name.
	 * @param parent
	 *            The URL to the parent folder.
	 * @return The URL to the file, or null, if it could not be found.
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private String findFolderInFolder(String name, String parent)
			throws ParserConfigurationException, SAXException, IOException {
		HttpsURLConnection con = SugarSyncConnector.getConnection(parent,
				this.token, "GET");
		con.setDoInput(true);

		// Build the XML tree.
		Document doc;
		doc = docBuilder.parse(con.getInputStream());
		NodeList nl = doc.getDocumentElement().getElementsByTagName(
				"collection");
		for (int i = 0; i < nl.getLength(); i++) {
			String displayName = ((Element) nl.item(i))
					.getElementsByTagName("displayName").item(0)
					.getTextContent();
			if (displayName.equalsIgnoreCase(name)) {
				return ((Element) nl.item(i)).getElementsByTagName("ref")
						.item(0).getTextContent();
			}
		}
		return null;
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
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private String findFileInFolder(String name, String parent)
			throws SAXException, IOException, ParserConfigurationException {
		HttpsURLConnection con = SugarSyncConnector.getConnection(parent,
				this.token, "GET");
		con.setDoInput(true);

		// Build the XML tree.
		Document doc;
		doc = docBuilder.parse(con.getInputStream());
		NodeList nl = doc.getDocumentElement().getElementsByTagName("file");
		for (int i = 0; i < nl.getLength(); i++) {
			String displayName = ((Element) nl.item(i))
					.getElementsByTagName("displayName").item(0)
					.getTextContent();
			if (displayName.equalsIgnoreCase(name)) {
				return ((Element) nl.item(i)).getElementsByTagName("ref")
						.item(0).getTextContent();
			}
		}
		return null;
	}

	/**
	 * Loads and caches the URL to the 'Magic Briefcase' folder.
	 * 
	 * @return The URL to the 'Magic Briefcase' folder on SugarSync.
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	private String getBaseUrl() throws IOException, SAXException,
			ParserConfigurationException {
		if (baseURL == null) {
			HttpsURLConnection con = SugarSyncConnector.getConnection(
					USER_INFO_URL, this.token, "GET");
			con.setDoInput(true);

			// Build the XML tree.
			Document doc = docBuilder.parse(con.getInputStream());
			Element node = (Element) doc.getDocumentElement()
					.getElementsByTagName("syncfolders").item(0);
			String folder = node.getTextContent().trim();

			this.baseURL = this.findFolderInFolder("Magic Briefcase", folder);
		}
		return this.baseURL;
	}

	/**
	 * Creates a folder on SugarSync.
	 * 
	 * @param name
	 *            The name of the folder.
	 * @param parent
	 *            The URL to the parent folder.
	 * @throws IOException
	 */
	private void createFolder(String name, String parent) throws IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<folder>" + "\t<displayName>" + name + "</displayName>"
				+ "</folder>";
		HttpsURLConnection con = SugarSyncConnector.getConnection(parent,
				this.token, "POST");
		con.setRequestProperty("Content-Type", "text/xml");
		con.setDoOutput(true);
		con.setDoInput(true);
		con.getOutputStream().write(request.getBytes());
		InputStream is = con.getInputStream();
		int i;
		while ((i = is.read()) >= 0) {
			System.out.print((char) i);
		}
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
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	private void createFile(String name, File f, String parent)
			throws IOException, SAXException, ParserConfigurationException {
		String mime = new MimetypesFileTypeMap().getContentType(f);
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><file><displayName>"
				+ name
				+ "</displayName><mediaType>"
				+ mime
				+ "</mediaType></file>";
		HttpsURLConnection con = SugarSyncConnector.getConnection(parent,
				this.token, "POST");
		con.setRequestProperty("Content-Type", "text/xml");
		con.setDoOutput(true);
		con.getOutputStream().write(request.getBytes());
		InputStream is = con.getInputStream();
		System.out.println(con.getResponseCode() + ": "
				+ con.getResponseMessage());

		String file = this.findFileInFolder(name, parent
				+ "/contents?type=file")
				+ "/data";

		con = SugarSyncConnector.getConnection(file, this.token, "PUT");
		con.setDoOutput(true);
		con.setRequestProperty("Content-Type", mime);
		OutputStream os = con.getOutputStream();

		is = new FileInputStream(f);
		int i;
		while ((i = is.read()) >= 0) {
			os.write(i);
		}
		System.out.println(con.getResponseCode() + ": "
				+ con.getResponseMessage());
	}

	public static void main(String[] args) {
		try {
			if (args.length != 5) {
				System.err
						.println("usage: username password accessKey privateAccessKey resource");
				System.out
						.println("example for 'resource': 'Sample Documents/SugarSync QuickStart Guide.pdf'");
				return;
			}
			SugarSyncConnector ssc = new SugarSyncConnector(args[0], args[1],
					args[2], args[3]);
			ssc.connect("");

			// InputStream is = ssc.get(args[4]);
			// File f = new File("/tmp/" + args[4]);
			// f.getParentFile().mkdirs();
			// FileOutputStream fos = new FileOutputStream(f);
			//
			// byte[] inputBytes = new byte[02000];
			// int readLength;
			// while ((readLength = is.read(inputBytes)) >= 0) {
			// fos.write(inputBytes, 0, readLength);
			// }
			System.out.println("Done.");
			ssc.put(args[4]);
			System.out.println("Uploading done.");
			ssc.delete(args[4]);
			System.out.println("Deleting done.");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
}