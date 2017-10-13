package com.cloud.search.serviceconsumer.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author chandrakant_bagade
 * 
 *         This application acts as consumer for search service. This class
 *         demonstrate how to parse and use service.
 *
 */

public class Consumer extends HttpServlet {

	private static final long serialVersionUID = -1576693692514023580L;
	private static String user = null;
	private static String password = null;
	private static String url = null;

	/**
	 * When the application is binded to service , the VCAP_SERVICE
	 * environmental variable can be used to get URL and credentials for service
	 * 
	 * This method parse the VCAP_SERVICE, to get URL and credentials
	 */

	private static void parseService() {
		String methodInfo = "parseService () ";
		System.out.println(methodInfo + " , entered ");
		try {

			String VCAP_SERVICES = System.getenv("VCAP_SERVICES");
			if (VCAP_SERVICES != null) {
				// When running in Bluemix, the VCAP_SERVICES env var will have
				// the
				// credentials for all bound/connected services

				JsonObject obj = (JsonObject) new JsonParser()
						.parse(VCAP_SERVICES);
				Entry<String, JsonElement> serviceEntry = null;
				Set<Entry<String, JsonElement>> entries = obj.entrySet();
				for (Entry<String, JsonElement> eachEntry : entries) {
					if (eachEntry.getKey().toLowerCase()
							.contains("search-service")) {
						serviceEntry = eachEntry;
						break;
					}
				}
				if (serviceEntry == null) {
					throw new RuntimeException(
							"Could not find search-service key in VCAP_SERVICES env variable");
				}

				obj = (JsonObject) ((JsonArray) serviceEntry.getValue()).get(0);

				obj = (JsonObject) obj.get("credentials");

				user = obj.get("user").getAsString();
				password = obj.get("password").getAsString();
				url = obj.get("uri").getAsString();

				System.out.println(methodInfo + " user " + user);
				System.out.println(methodInfo + " password " + password);
				System.out.println(methodInfo + " url " + url);

			} else {
				System.out
						.println("VCAP_SERVICES env var doesn't exist: running locally.");
			}

		} catch (Exception e) {
			System.out.println("There is issue while pasring service");
		}

	}

	/**
	 * checks if user , password , url is set after parsing of VACP_SERVICE env
	 * variable
	 * 
	 * @return boolean , true is service is properly parsed
	 */
	private static boolean isServiceParsed() {
		if (user != null && !user.trim().equals("") && password != null
				&& !password.trim().equals("") && url != null
				&& !url.trim().equals("")) {
			return true;
		}

		parseService();

		if (user == null || user.trim().equals("") || password == null
				|| password.trim().equals("") || url == null
				|| url.trim().equals("")) {
			return false;
		}

		return true;
	}

	/**
	 * get the request content
	 * 
	 * @param request
	 *            HttpServletRequest
	 * @return String , content from request
	 */
	private static StringBuffer getRequestContent(HttpServletRequest request) {
		StringBuffer content = new StringBuffer();
		String aContentLine = null;
		try {
			BufferedReader reader = request.getReader();
			while ((aContentLine = reader.readLine()) != null)
				content.append(aContentLine);
		} catch (Exception e) {
			System.out.println("data reading error");
		}

		return content;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 * 
	 * this method will GET the content from search service
	 * 
	 * The method should be called with content as request content. User can
	 * optionally supply key as query parameter , to get content of specified
	 * key
	 */
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		String methodInfo = "doGet () ";
		response.setContentType("text/plain;charset=utf-8");

		if (!isServiceParsed()) {
			System.out.println(methodInfo
					+ " , looks like error with pasrsing binding.");
			response.setStatus(500);
			response.getWriter().println(
					"There is issue parsing binded service.");
			return;

		}

		String key = request.getParameter("key");
		System.out.println(methodInfo + " , key " + key);

		StringBuffer content = getRequestContent(request);

		if (content == null || content.length() == 0) {
			System.out
					.println(methodInfo
							+ " , bad request , returning since content is not passed.");
			response.setStatus(400);
			response.getWriter().println(
					"Please send proper content to search.");
			return;
		}

		String serviceResponse[] = getSearchContent(key, content.toString());
		response.setStatus(Integer.parseInt(serviceResponse[0]));
		response.getWriter().println(serviceResponse[1]);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doDelete(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 * 
	 * The method should be called with content as request content. User can
	 * optionally supply key as query parameter , to delete content of specified
	 * key
	 */

	@Override
	protected void doDelete(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		String methodInfo = "doDelete() ";
		response.setContentType("text/plain;charset=utf-8");

		if (!isServiceParsed()) {
			System.out.println(methodInfo
					+ " , looks like error with pasrsing binding.");
			response.setStatus(500);
			response.getWriter().println(
					"There is issue parsing binded service.");
			return;

		}

		String key = request.getParameter("key");
		System.out.println(methodInfo + " , key " + key);

		StringBuffer content = getRequestContent(request);

		if (content == null || content.length() == 0) {
			System.out
					.println(methodInfo
							+ " , bad request , returning since content is not passed.");
			response.setStatus(400);
			response.getWriter().println(
					"Please send proper content to delete.");
			return;
		}

		String serviceResponse[] = deleteContent(key, content.toString());
		response.setStatus(Integer.parseInt(serviceResponse[0]));
		response.getWriter().println(serviceResponse[1]);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 * 
	 * this method should be called with content as request content. The content
	 * is POST to search service to index.
	 */

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		String methodInfo = "doPost() ";
		response.setContentType("text/plain;charset=utf-8");

		if (!isServiceParsed()) {
			System.out.println(methodInfo
					+ " , looks like error with pasrsing binding.");
			response.setStatus(500);
			response.getWriter().println(
					"There is issue parsing binded service.");
			return;

		}

		System.out.println(methodInfo + " , called");

		StringBuffer content = getRequestContent(request);

		if (content == null || content.length() == 0) {
			System.out
					.println(methodInfo
							+ " , bad request , returning since content is not passed.");
			response.setStatus(400);
			response.getWriter()
					.println("Please send proper content to index.");
			return;
		}

		String[] serviceResponse = indexSearchContent(content.toString());

		response.setStatus(Integer.parseInt(serviceResponse[0]));
		response.getWriter().println(serviceResponse[1]);

	}

	// helper method to index content. This method will POST content to search
	// service for indexing
	private static String[] indexSearchContent(String content) {

		String methodInfo = "indexSearchContent () :: ";

		System.out.println(methodInfo + " content " + content);
		String[] finalResponse = new String[2];

		try {

			URL svcUrl = new URL(url);

			System.out.println(methodInfo + " svcUrl " + svcUrl);
			HttpURLConnection connection = (HttpURLConnection) svcUrl
					.openConnection();
			connection.setRequestMethod("PUT");

			String credentials = user + ":" + password;

			byte[] authEncBytes = Base64.encodeBase64(credentials.getBytes());

			String authStringEnc = new String(authEncBytes);
			connection.setRequestProperty("Authorization", "Basic "
					+ authStringEnc);

			connection.setDoOutput(true);
			byte[] outputInBytes = content.getBytes("UTF-8");
			OutputStream os = connection.getOutputStream();
			os.write(outputInBytes);
			os.close();

			StringBuilder connectionResponse = new StringBuilder();
			int responseCode = connection.getResponseCode();
			System.out.println(methodInfo + " responseCode " + responseCode);

			finalResponse[0] = responseCode + "";

			if (responseCode == 200 || responseCode == 201) {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						connection.getInputStream()));
				String inputLine;

				while ((inputLine = in.readLine()) != null) {
					connectionResponse.append(inputLine);
				}
				in.close();

				finalResponse[1] = connectionResponse.toString();

			} else {

				finalResponse[1] = "There is some issue indexing content";
			}

		} catch (Exception e) {

			e.printStackTrace();
		}
		System.out.println(methodInfo + " done ");
		return finalResponse;
	}

	// helper method to get search content. This method call search service to
	// get search content
	private static String[] getSearchContent(String key, String content) {
		String methodInfo = "getSearchContent () :: ";

		System.out.println(methodInfo + " key " + key + " content [" + content
				+ "]");
		String[] finalResponse = new String[2];

		try {

			URL svcUrl = null;
			if (!(key == null || key.trim().equals(""))) {
				svcUrl = new URL(url + "?key=" + key + "&content=" + content);
			} else {
				svcUrl = new URL(url + "?content=" + content);
			}

			System.out.println(methodInfo + " , url " + svcUrl);

			HttpURLConnection connection = (HttpURLConnection) svcUrl
					.openConnection();
			connection.setRequestMethod("GET");

			String credentials = user + ":" + password;

			byte[] authEncBytes = Base64.encodeBase64(credentials.getBytes());

			String authStringEnc = new String(authEncBytes);
			connection.setRequestProperty("Authorization", "Basic "
					+ authStringEnc);

			StringBuilder connectionResponse = new StringBuilder();
			int responseCode = connection.getResponseCode();
			System.out.println(methodInfo + " responseCode " + responseCode);

			finalResponse[0] = responseCode + "";
			if (responseCode == 200 || responseCode == 201) {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						connection.getInputStream()));
				String inputLine;

				while ((inputLine = in.readLine()) != null) {
					connectionResponse.append(inputLine);
				}

				finalResponse[1] = connectionResponse.toString();

			} else {

				finalResponse[1] = "There is some issue getting content";
			}

		} catch (Exception e) {

			e.printStackTrace();
		}
		return finalResponse;
	}

	// helper method to delete search content. This method call search service
	// to
	// delete search content
	private static String[] deleteContent(String key, String content) {
		String methodInfo = "deleteContent () :: ";

		System.out.println(methodInfo + " key " + key + " content " + content);
		String[] finalResponse = new String[2];

		try {

			URL svcUrl = null;
			if (!(key == null || key.trim().equals(""))) {
				svcUrl = new URL(url + "?key=" + key + "&content=" + content);
			} else {
				svcUrl = new URL(url + "?content=" + content);
			}

			System.out.println(methodInfo + " , url " + svcUrl);
			HttpURLConnection connection = (HttpURLConnection) svcUrl
					.openConnection();
			connection.setRequestMethod("DELETE");

			String credentials = user + ":" + password;

			byte[] authEncBytes = Base64.encodeBase64(credentials.getBytes());

			String authStringEnc = new String(authEncBytes);
			connection.setRequestProperty("Authorization", "Basic "
					+ authStringEnc);

			StringBuilder connectionResponse = new StringBuilder();
			int responseCode = connection.getResponseCode();
			System.out.println(methodInfo + " responseCode " + responseCode);

			finalResponse[0] = responseCode + "";
			if (responseCode == 200 || responseCode == 201) {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						connection.getInputStream()));
				String inputLine;

				while ((inputLine = in.readLine()) != null) {
					connectionResponse.append(inputLine);
				}

				finalResponse[1] = connectionResponse.toString();

			} else {

				finalResponse[1] = "There is some issue getting content";
			}

		} catch (Exception e) {

			e.printStackTrace();
		}
		return finalResponse;
	}

}
