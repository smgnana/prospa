package au.prospa.service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import au.prospa.domain.CallRecord;

@Service
public class SalesforceCallRecordReader {

	private final String username;
	private final String password;
	private final String consumerKey;
	private final String consumerSecret;
	private final String tokenUrl;

	@Autowired
	public SalesforceCallRecordReader(@Value("${sf.username}") String username,
			@Value("${sf.password}") String password, @Value("${sf.consumerKey}") String consumerKey,
			@Value("${sf.consumerSecret}") String consumerSecret, @Value("${sf.tokenUrl}") String tokenUrl) {
		this.username = username;
		this.password = password;
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.tokenUrl = tokenUrl;
	}

    public List<CallRecord> getAllNew() {
    	try {
            final CloseableHttpClient httpclient = HttpClients.createDefault();

            final HttpPost post = createLoginRequest();
            final HttpResponse loginResponse = httpclient.execute(post);
            
            if (loginResponse.getStatusLine().getStatusCode() == 200){
                // parse
                final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

                final JsonNode loginResult = mapper.readValue(loginResponse.getEntity().getContent(), JsonNode.class);
                final String accessToken = loginResult.get("access_token").asText();
                final String instanceUrl = loginResult.get("instance_url").asText();

                // query contacts
                final URIBuilder builder = new URIBuilder(instanceUrl);
                builder.setPath("/services/data/v39.0/query/").setParameter("q", "SELECT Id, Name, NVM_URL__c FROM Call_Record__c");

                final HttpGet get = new HttpGet(builder.build());
                get.setHeader("Authorization", "Bearer " + accessToken);

                final HttpResponse queryResponse = httpclient.execute(get);

                final JsonNode queryResults = mapper.readValue(queryResponse.getEntity().getContent(), JsonNode.class);
                System.out.println(queryResults);
                return null;
            } else {
            	throw new RuntimeException("Issue in Login to Salesforce. Response: " + loginResponse);
            }
            
    	} catch (Exception e){
    		throw new RuntimeException(e);
    	}
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
}
