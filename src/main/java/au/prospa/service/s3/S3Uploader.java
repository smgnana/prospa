package au.prospa.service.s3;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

import au.prospa.domain.CallRecord;

@Service
public class S3Uploader {
	private Logger logger = LoggerFactory.getLogger(S3Uploader.class);

	@Autowired
	private AmazonS3 s3client;

	@Value("${s3.bucket}")
	private String bucketName;

	public void uploadFile(CallRecord record, byte[] file) {

		if (file == null) {
			return;
		}

		try {
			InputStream fis = new ByteArrayInputStream(file);

			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(file.length);
			metadata.setContentType("audio/wav");
			metadata.setCacheControl("public, max-age=31536000");

			String s3Path = record.getS3Path();
			s3client.putObject(new PutObjectRequest(bucketName, s3Path, fis, metadata));
			s3client.setObjectAcl(bucketName, s3Path, CannedAccessControlList.PublicRead);

			URL s3Url = s3client.getUrl(bucketName, s3Path);
			logger.info("Uploaded to " + record.id + " S3 " + s3Url);
			record.s3Url = s3Url;

		} catch (AmazonServiceException ase) {
			logger.info("Caught an AmazonServiceException from PUT requests, rejected reasons:");
			logger.info("Error Message:    " + ase.getMessage());
			logger.info("HTTP Status Code: " + ase.getStatusCode());
			logger.info("AWS Error Code:   " + ase.getErrorCode());
			logger.info("Error Type:       " + ase.getErrorType());
			logger.info("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			logger.info("Caught an AmazonClientException: ");
			logger.info("Error Message: " + ace.getMessage());
		}
	}
}
