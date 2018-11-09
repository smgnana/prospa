package au.prospa.service.salesforce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import au.prospa.domain.CallRecord;

@Service
public class SalesforceCallRecordReader extends SalesforceAuthService {

	private static final Logger logger = LoggerFactory.getLogger(SalesforceCallRecordReader.class);

	@Autowired
	public SalesforceCallRecordReader(@Value("${sf.username}") String username,
			@Value("${sf.password}") String password, @Value("${sf.consumerKey}") String consumerKey,
			@Value("${sf.consumerSecret}") String consumerSecret, @Value("${sf.tokenUrl}") String tokenUrl) {
		
		super(username, password, consumerKey, consumerSecret, tokenUrl);
	}

    public List<CallRecord> getAllPending() {
    	try {
    		List<CallRecord> records = new ArrayList<>();
    		
    		Auth auth = login();
    		if (auth.isError()){
    			return records;
    		}
    		
            // query contacts
            final URIBuilder builder = new URIBuilder(auth.instanceUrl);
            builder.setPath("/services/data/v44.0/query/").setParameter("q", "SELECT Id, NVM_URL__c, Created_Date__c FROM Call_Record__c WHERE Status__c = 'Pending'");

            final HttpGet get = new HttpGet(builder.build());
            get.setHeader("Authorization", "Bearer " + auth.accessToken);

            final CloseableHttpClient httpclient = HttpClients.createDefault();
            final HttpResponse queryResponse = httpclient.execute(get);

            loadCallRecords(queryResponse, records);

            return records;
            
    	} catch (Exception e){
    		throw new RuntimeException("Issue when getting the CallRecord List from Salesforce", e);
    	}
    }

	private void loadCallRecords(final HttpResponse queryResponse, List<CallRecord> records)
			throws IOException, JsonParseException, JsonMappingException, JsonProcessingException {
		
		final ObjectMapper mapper = new ObjectMapper();
		final JsonNode queryResults = mapper.readValue(queryResponse.getEntity().getContent(), JsonNode.class);
		List<String> recordIds = new ArrayList<>();
		for (JsonNode recordNode : queryResults.findPath("records")){
			CallRecord record = mapper.treeToValue(recordNode, CallRecord.class);
			records.add(record);
			recordIds.add(record.id);
		}
		
		logger.info(records.size() + " was found to process: " + recordIds);
	}
}
