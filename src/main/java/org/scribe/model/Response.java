package org.scribe.model;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.scribe.exceptions.OAuthException;
import org.scribe.utils.StreamUtils;

/**
 * Represents an HTTP Response.
 * 
 * @author Pablo Fernandez
 */
public class Response {
	private int code;
	private String message;
	private String body;
	private Map<String, String> headers;

	Response(HttpClient httpClient, HttpUriRequest httpRequest)
			throws IOException {
		try {
			HttpResponse response = httpClient.execute(httpRequest);
			HttpEntity entity = response.getEntity();

			code = response.getStatusLine().getStatusCode();
			message = response.getStatusLine().getReasonPhrase();

			headers = parseHeaders(response);

			body = StreamUtils.getStreamContents(entity.getContent());

			EntityUtils.consume(entity);
		} catch (UnknownHostException e) {
			throw new OAuthException(
					"The IP address of a host could not be determined.", e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}

	private Map<String, String> parseHeaders(HttpResponse response) {
		Map<String, String> headers = new HashMap<String, String>();

		for (Header header : response.getAllHeaders()) {
			headers.put(header.getName(), header.getValue());
		}
		return headers;
	}

	public boolean isSuccessful() {
		return getCode() >= 200 && getCode() < 400;
	}

	/**
	 * Obtains the HTTP Response body
	 * 
	 * @return response body
	 */
	public String getBody() {
		return body;
	}

	/**
	 * Obtains the HTTP status code
	 * 
	 * @return the status code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Obtains the HTTP status message. Returns <code>null</code> if the message
	 * can not be discerned from the response (not valid HTTP)
	 * 
	 * @return the status message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Obtains a {@link Map} containing the HTTP Response Headers
	 * 
	 * @return headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * Obtains a single HTTP Header value, or null if undefined
	 * 
	 * @param name
	 *            the header name.
	 * 
	 * @return header value or null.
	 */
	public String getHeader(String name) {
		return headers.get(name);
	}

}