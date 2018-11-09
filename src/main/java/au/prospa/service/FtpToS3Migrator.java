package au.prospa.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import au.prospa.domain.CallRecord;
import au.prospa.domain.UploadResult;
import au.prospa.ftp.FtpBatchDownloader;
import au.prospa.service.s3.S3Uploader;
import au.prospa.service.salesforce.SalesforceCallRecordReader;
import au.prospa.service.salesforce.SalesforceCallRecordReportUpdater;
import au.prospa.service.salesforce.SalesforceCallRecordUpdater;

@Service
public class FtpToS3Migrator {

	private SalesforceCallRecordReader salesforceCallRecordReader;
	private FtpBatchDownloader ftpBatchDownloader;
	private S3Uploader s3Uploader;
	private SalesforceCallRecordUpdater salesforceCallRecordUpdater;
	private SalesforceCallRecordReportUpdater salesforceCallRecordReportUpdater;
	
	private static final Logger logger = LoggerFactory.getLogger(FtpToS3Migrator.class);

	@Autowired
	public FtpToS3Migrator(SalesforceCallRecordReader salesforceCallRecordReader, FtpBatchDownloader ftpBatchDownloader,
			S3Uploader s3Uploader, SalesforceCallRecordUpdater salesforceCallRecordUpdater,
			SalesforceCallRecordReportUpdater salesforceCallRecordReportUpdater) {
		
		this.salesforceCallRecordReader = salesforceCallRecordReader;
		this.ftpBatchDownloader = ftpBatchDownloader;
		this.s3Uploader = s3Uploader;
		this.salesforceCallRecordUpdater = salesforceCallRecordUpdater;
		this.salesforceCallRecordReportUpdater = salesforceCallRecordReportUpdater;
	}

	@Async
	public void migrate(String reportId) {

		UploadResult result = null;
		try {
			// Get data from SF
			List<CallRecord> callRecords = salesforceCallRecordReader.getAllNew();

			for (CallRecord callRecord : callRecords) {
				// Download from FTP
				byte[] file = ftpBatchDownloader.download(callRecord);

				// Upload to S3
				s3Uploader.uploadFile(callRecord, file);
			}

			ftpBatchDownloader.close();

			// Update S3 URL in Salesforce
			result = salesforceCallRecordUpdater.update(callRecords);

		} catch (Exception e) {
			logger.error("The S3 Upload Process failed", e);
			result = UploadResult.error(e.getMessage());

		} finally {
			salesforceCallRecordReportUpdater.update(result, reportId);

		}
	}
}
