package au.prospa.service.salesforce;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import au.prospa.domain.CallRecord;

@Service
public class SalesforceCallRecordUpdater {

	private final String username;
	private final String password;
	private final String consumerKey;
	private final String consumerSecret;
	private final String tokenUrl;

	private static final Logger logger = LoggerFactory.getLogger(SalesforceCallRecordUpdater.class);

	@Autowired
	public SalesforceCallRecordUpdater(@Value("${sf.username}") String username,
			@Value("${sf.password}") String password, @Value("${sf.consumerKey}") String consumerKey,
			@Value("${sf.consumerSecret}") String consumerSecret, @Value("${sf.tokenUrl}") String tokenUrl) {
		
		this.username = username;
		this.password = password;
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.tokenUrl = tokenUrl;
	}

    public void update(List<CallRecord> callRecords) {
    	try {
            final CloseableHttpClient httpclient = HttpClients.createDefault();

            final HttpPost loginPost = createLoginRequest();
            final HttpResponse loginResponse = httpclient.execute(loginPost);
            
            if (loginResponse.getStatusLine().getStatusCode() == 200){
            	
            	
                // parse
                final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

                final JsonNode loginResult = mapper.readValue(loginResponse.getEntity().getContent(), JsonNode.class);
                final String accessToken = loginResult.get("access_token").asText();
                final String instanceUrl = loginResult.get("instance_url").asText();

                // query contacts
                final URIBuilder builder = new URIBuilder(instanceUrl);
                builder.setPath("/services/data/v44.0/composite/batch");
                
                JsonNode node = mapper.createObjectNode();
                ArrayNode batchRequests = ((ObjectNode) node).putArray("batchRequests");
                
                for (CallRecord callRecord : callRecords) {
                	if (callRecord.s3Url == null){
                		continue;
                	}
                	
					JsonNode callRecordRequest = mapper.createObjectNode();
					((ObjectNode) callRecordRequest)
						.put("method", "PATCH")
						.put("url", "v44.0/sobjects/Call_Record__c/"+callRecord.id);
					
					JsonNode richInput = ((ObjectNode) callRecordRequest).putObject("richInput");
					((ObjectNode) richInput).put("AWS_S3_URL__c", callRecord.s3Url.toString());
					
					batchRequests.add(callRecordRequest);
				}
                
                if (batchRequests.size() <= 0){
                	logger.warn("No records to update in Salesforce at the end of the file upload");
                	return;
                }

                final HttpPost post = new HttpPost(builder.build());
                post.setHeader("Authorization", "Bearer " + accessToken);
                post.setHeader("Accept", "application/json");
                post.setHeader("Content-type", "application/json");
                
                logger.info("Updating Salesforce objects with S3 URLs: " + node.toString());
                
                StringEntity entity = new StringEntity(node.toString());
                post.setEntity(entity);

                CloseableHttpResponse response = httpclient.execute(post);
                logger.info("Updated Salesforce objects: " + response + " Detail: " + EntityUtils.toString(response.getEntity()));

                
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
