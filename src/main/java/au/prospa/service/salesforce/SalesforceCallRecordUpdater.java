package au.prospa.service.salesforce;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import au.prospa.domain.UploadResult;

@Service
public class SalesforceCallRecordUpdater extends SalesforceAuthService {

	private static final Logger logger = LoggerFactory.getLogger(SalesforceCallRecordUpdater.class);

	@Autowired
	public SalesforceCallRecordUpdater(@Value("${sf.username}") String username,
			@Value("${sf.password}") String password, @Value("${sf.consumerKey}") String consumerKey,
			@Value("${sf.consumerSecret}") String consumerSecret, @Value("${sf.tokenUrl}") String tokenUrl) {

		super(username, password, consumerKey, consumerSecret, tokenUrl);
	}

	public UploadResult update(List<CallRecord> callRecords) {
		try {
			UploadResult result = new UploadResult();
			
			Auth auth = login();
			if (auth.isError()){
				result.errorMessage = "Issue when logging into Salesforce when updating Call Records";
				return result;
			}

			// query contacts
			final URIBuilder builder = new URIBuilder(auth.instanceUrl);
			builder.setPath("/services/data/v44.0/composite/batch");

			StringEntity entity = createBatchRequestBody(callRecords, result);

			final HttpPost post = new HttpPost(builder.build());
			post.setHeader("Authorization", "Bearer " + auth.accessToken);
			post.setHeader("Accept", "application/json");
			post.setHeader("Content-type", "application/json");
			post.setEntity(entity);

			final CloseableHttpClient httpclient = HttpClients.createDefault();
			CloseableHttpResponse response = httpclient.execute(post);
			
			if (!isSuccess(response)){
				String errorMessage = "Issue when updating Call Records in Salesforce. " + response + " Detail: "
						+ EntityUtils.toString(response.getEntity());;
				
				result.errorMessage = errorMessage;
				logger.error(errorMessage);
			}

			return result;
			
		} catch (Exception e) {
			throw new RuntimeException("Issue while updating Call Records", e);
		}
	}

	private StringEntity createBatchRequestBody(List<CallRecord> callRecords, final UploadResult result) throws UnsupportedEncodingException {
		
		final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT); 
		JsonNode node = mapper.createObjectNode();
		ArrayNode batchRequests = ((ObjectNode) node).putArray("batchRequests");

		result.totalCount = callRecords.size();

		for (CallRecord callRecord : callRecords) {
			JsonNode callRecordRequest = createCallRecordEntryToBatch(callRecord, result, mapper);
			batchRequests.add(callRecordRequest);
		}
		
		logger.info("Updating Salesforce objects with S3 URLs: " + node.toString());
		
		StringEntity entity = new StringEntity(node.toString());
		return entity;
	}

	private JsonNode createCallRecordEntryToBatch(CallRecord callRecord, final UploadResult result,
			final ObjectMapper mapper) {
		JsonNode callRecordRequest = mapper.createObjectNode();
		((ObjectNode) callRecordRequest).put("method", "PATCH").put("url",
				"v44.0/sobjects/Call_Record__c/" + callRecord.id);

		JsonNode richInput = ((ObjectNode) callRecordRequest).putObject("richInput");

		// ((ObjectNode) richInput).put("Uploaded_At__c", "NOW()");

		if (callRecord.s3Url != null) {
			((ObjectNode) richInput).put("Status__c", "Completed");
			((ObjectNode) richInput).put("AWS_S3_URL__c", callRecord.s3Url.toString());
			result.successCount++;
		}

		if (callRecord.uploadError != null) {
			((ObjectNode) richInput).put("Status__c", "Failed");
			((ObjectNode) richInput).put("Upload_Error__c", callRecord.uploadError);
			result.skippedCount++;
		}
		return callRecordRequest;
	}
}
