package au.prospa.service.salesforce;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public abstract class SalesforceAuthService {

	private final String username;
	private final String password;
	private final String consumerKey;
	private final String consumerSecret;
	private final String tokenUrl;

	private static final Logger logger = LoggerFactory.getLogger(SalesforceAuthService.class);

	public SalesforceAuthService(String username, String password, String consumerKey, String consumerSecret,
			String tokenUrl) {

		this.username = username;
		this.password = password;
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.tokenUrl = tokenUrl;
	}

	protected boolean isSuccess(final HttpResponse response) {
		int statusCode = response.getStatusLine().getStatusCode();
		return statusCode >= 200 && statusCode < 300;
	}

	private HttpPost createLoginRequest() throws UnsupportedEncodingException {
		final List<NameValuePair> loginParams = new ArrayList<NameValuePair>();
		loginParams.add(new BasicNameValuePair("client_id", consumerKey));
		loginParams.add(new BasicNameValuePair("client_secret", consumerSecret));
		loginParams.add(new BasicNameValuePair("grant_type", "password"));
		loginParams.add(new BasicNameValuePair("username", username));
		loginParams.add(new BasicNameValuePair("password", password));

		final HttpPost post = new HttpPost(tokenUrl);
		post.setEntity(new UrlEncodedFormEntity(loginParams));
		return post;
	}

	protected Auth login() {
		try {
			final CloseableHttpClient httpclient = HttpClients.createDefault();

			final HttpPost loginPost = createLoginRequest();
			final HttpResponse loginResponse = httpclient.execute(loginPost);

			if (!isSuccess(loginResponse)) {
				logger.error("Issue in Login to Salesforce when saving Report object. Response: " + loginResponse
						+ " Detail: " + EntityUtils.toString(loginResponse.getEntity()));
				return new Auth();
			}

			// parse
			final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

			final JsonNode loginResult = mapper.readValue(loginResponse.getEntity().getContent(), JsonNode.class);

			final String accessToken = loginResult.get("access_token").asText();
			final String instanceUrl = loginResult.get("instance_url").asText();

			return new Auth(accessToken, instanceUrl);

		} catch (IOException e) {
			throw new RuntimeException("Issue in logging into Salesforce as " + username, e);
		}
	}

	public static class Auth {
		final String accessToken;
		final String instanceUrl;

		public Auth(String accessToken, String instanceUrl) {
			this.accessToken = accessToken;
			this.instanceUrl = instanceUrl;
		}

		public Auth() {
			this(null, null);
		}

		public boolean isError() {
			return this.accessToken == null;
		}
	}
}
