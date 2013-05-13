package org.scribe.model;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.scribe.exceptions.OAuthConnectionException;
import org.scribe.exceptions.OAuthException;

/**
 * Represents an HTTP Request object
 * 
 * @author Pablo Fernandez
 */
public class Request {
	private static RequestTuner NOOP = new RequestTuner() {
		@Override
		public void tune(Request _) {
		}
	};
	public static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";

	private String url;
	private Verb verb;
	private ParameterList querystringParams;
	private Map<String, String> headers;
	private HttpClient httpClient;
	private HttpUriRequest httpRequest;
	private String charset;
	private boolean connectionKeepAlive = false;
	private Long connectTimeout = null;
	private Long readTimeout = null;

	/**
	 * Creates a new Http Request
	 * 
	 * @param verb
	 *            Http Verb (GET, POST, etc)
	 * @param url
	 *            url with optional querystring parameters.
	 */
	public Request(Verb verb, String url) {
		this.verb = verb;
		this.url = url;
		this.querystringParams = new ParameterList();
		this.headers = new HashMap<String, String>();
	}

	/**
	 * Execute the request and return a {@link Response}
	 * 
	 * @return Http Response
	 * @throws RuntimeException
	 *             if the connection cannot be created.
	 */
	public Response send(RequestTuner tuner) {
		try {
			createConnection();
			return doSend(tuner);
		} catch (Exception e) {
			throw new OAuthConnectionException(e);
		}
	}

	public Response send() {
		return send(NOOP);
	}

	private void createConnection() throws IOException {
		String completeUrl = getCompleteUrl();
		if (httpClient == null) {
			httpClient = new DefaultHttpClient();
		}
		if (httpRequest == null) {
			httpRequest = convertRequest(verb, completeUrl);
			if (connectionKeepAlive) {
				httpRequest.setHeader("Connection", "Keep-Alive");
			} else {
				httpRequest.removeHeaders("Connection");
			}
		}
	}

	private HttpUriRequest convertRequest(Verb verb, String completeUrl) {
		if (Verb.GET == verb) {
			return new HttpGet(completeUrl);
		} else if (Verb.POST == verb) {
			return new HttpPost(this.url);
		} else if (Verb.PUT == verb) {
			return new HttpPut(this.url);
		} else if (Verb.DELETE == verb) {
			return new HttpDelete(this.url);
		} else if (Verb.HEAD == verb) {
			return new HttpHead(this.url);
		} else if (Verb.OPTIONS == verb) {
			return new HttpOptions(this.url);
		} else if (Verb.TRACE == verb) {
			return new HttpTrace(this.url);
		} else if (Verb.PATCH == verb) {
			return new HttpPatch(this.url);
		}
		throw new IllegalArgumentException(
				String.format("unkown verb:%s", verb));
	}

	/**
	 * Returns the complete url (host + resource + encoded querystring
	 * parameters).
	 * 
	 * @return the complete url.
	 */
	public String getCompleteUrl() {
		return querystringParams.appendTo(url);
	}

	Response doSend(RequestTuner tuner) throws IOException {
		if (connectTimeout != null) {
			httpClient.getParams().setParameter(
					CoreConnectionPNames.CONNECTION_TIMEOUT, connectTimeout);
		}
		if (readTimeout != null) {
			httpClient.getParams().setParameter(
					CoreConnectionPNames.SO_TIMEOUT, readTimeout);
		}
		addHeaders(httpRequest);
		if (verb.equals(Verb.PUT) || verb.equals(Verb.POST)) {
			addBody((HttpEntityEnclosingRequestBase) httpRequest,
					querystringParams.asFormUrlEncodedString());
		}
		tuner.tune(this);
		return new Response(httpClient, httpRequest);
	}

	void addHeaders(HttpUriRequest httpRequest) {
		for (String key : headers.keySet())
			httpRequest.setHeader(key, headers.get(key));
	}

	void addBody(HttpEntityEnclosingRequestBase httpRequest, String content)
			throws IOException {
		StringEntity entity = new StringEntity(content);
		entity.setContentType(DEFAULT_CONTENT_TYPE);
		httpRequest.setEntity(entity);
	}

	/**
	 * Add an HTTP Header to the Request
	 * 
	 * @param key
	 *            the header name
	 * @param value
	 *            the header value
	 */
	public void addHeader(String key, String value) {
		this.headers.put(key, value);
	}

	/**
	 * Add a QueryString parameter
	 * 
	 * @param key
	 *            the parameter name
	 * @param value
	 *            the parameter value
	 */
	public void addQuerystringParameter(String key, String value) {
		this.querystringParams.add(key, value);
	}

	/**
	 * Get a {@link ParameterList} with the query string parameters.
	 * 
	 * @return a {@link ParameterList} containing the query string parameters.
	 * @throws OAuthException
	 *             if the request URL is not valid.
	 */
	public ParameterList getQueryStringParams() {
		try {
			ParameterList result = new ParameterList();
			String queryString = new URL(url).getQuery();
			result.addQuerystring(queryString);
			result.addAll(querystringParams);
			return result;
		} catch (MalformedURLException mue) {
			throw new OAuthException("Malformed URL", mue);
		}
	}


	/**
	 * Obtains the URL of the HTTP Request.
	 * 
	 * @return the original URL of the HTTP Request
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Returns the URL without the port and the query string part.
	 * 
	 * @return the OAuth-sanitized URL
	 */
	public String getSanitizedUrl() {
		return url.replaceAll("\\?.*", "").replace("\\:\\d{4}", "");
	}

	/**
	 * Returns the HTTP Verb
	 * 
	 * @return the verb
	 */
	public Verb getVerb() {
		return verb;
	}

	/**
	 * Returns the connection headers as a {@link Map}
	 * 
	 * @return map of headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * Returns the connection charset. Defaults to {@link Charset}
	 * defaultCharset if not set
	 * 
	 * @return charset
	 */
	public String getCharset() {
		return charset == null ? Charset.defaultCharset().name() : charset;
	}

	/**
	 * Sets the connect timeout for the underlying {@link HttpURLConnection}
	 * 
	 * @param duration
	 *            duration of the timeout
	 * 
	 * @param unit
	 *            unit of time (milliseconds, seconds, etc)
	 */
	public void setConnectTimeout(int duration, TimeUnit unit) {
		this.connectTimeout = unit.toMillis(duration);
	}

	/**
	 * Sets the read timeout for the underlying {@link HttpURLConnection}
	 * 
	 * @param duration
	 *            duration of the timeout
	 * 
	 * @param unit
	 *            unit of time (milliseconds, seconds, etc)
	 */
	public void setReadTimeout(int duration, TimeUnit unit) {
		this.readTimeout = unit.toMillis(duration);
	}

	/**
	 * Set the charset of the body of the request
	 * 
	 * @param charsetName
	 *            name of the charset of the request
	 */
	public void setCharset(String charsetName) {
		this.charset = charsetName;
	}

	/**
	 * Sets whether the underlying Http Connection is persistent or not.
	 * 
	 * @see http
	 *      ://download.oracle.com/javase/1.5.0/docs/guide/net/http-keepalive
	 *      .html
	 * @param connectionKeepAlive
	 */
	public void setConnectionKeepAlive(boolean connectionKeepAlive) {
		this.connectionKeepAlive = connectionKeepAlive;
	}

	@Override
	public String toString() {
		return String.format("@Request(%s %s)", getVerb(), getUrl());
	}
}
