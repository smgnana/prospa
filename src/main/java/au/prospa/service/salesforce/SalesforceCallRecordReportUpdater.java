package au.prospa.service.salesforce;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPatch;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import au.prospa.domain.UploadResult;

@Service
public class SalesforceCallRecordReportUpdater extends SalesforceAuthService  {

	private static final Logger logger = LoggerFactory.getLogger(SalesforceCallRecordReportUpdater.class);

	@Autowired
	public SalesforceCallRecordReportUpdater(@Value("${sf.username}") String username,
			@Value("${sf.password}") String password, @Value("${sf.consumerKey}") String consumerKey,
			@Value("${sf.consumerSecret}") String consumerSecret, @Value("${sf.tokenUrl}") String tokenUrl) {

		super(username, password, consumerKey, consumerSecret, tokenUrl);
	}

	public void update(UploadResult result, String reportId) {
		try {
			Auth auth = login();

			// query contacts
			final URIBuilder builder = new URIBuilder(auth.instanceUrl);
			builder.setPath("/services/data/v44.0/sobjects/Call_Record_Upload_Report__c/" + reportId);

			final CloseableHttpClient httpclient = HttpClients.createDefault();
			final HttpPatch post = new HttpPatch(builder.build());
			post.setHeader("Authorization", "Bearer " + auth.accessToken);
			post.setHeader("Accept", "application/json");
			post.setHeader("Content-type", "application/json");

			result.populateStatus();
			final ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(result);

			logger.info("Update Report of final status: " + json);

			StringEntity entity = new StringEntity(json);
			post.setEntity(entity);

			final HttpResponse postResponse = httpclient.execute(post);

			if (!isSuccess(postResponse)) {
				logger.error("Issue in updating the Report after the upload. " + postResponse + " Detail: "
						+ EntityUtils.toString(postResponse.getEntity()));
			}

		} catch (Exception e) {
			logger.error("Issue in updating the Report after the upload. ", e);
		}
	}
}
