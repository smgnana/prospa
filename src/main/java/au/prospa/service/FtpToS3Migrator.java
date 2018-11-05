package au.prospa.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import au.prospa.domain.CallRecord;
import au.prospa.ftp.FtpBatchDownloader;
import au.prospa.service.s3.S3Uploader;
import au.prospa.service.salesforce.SalesforceCallRecordReader;
import au.prospa.service.salesforce.SalesforceCallRecordUpdater;

@Service
public class FtpToS3Migrator {

	private SalesforceCallRecordReader salesforceCallRecordReader;
	private FtpBatchDownloader ftpBatchDownloader;
	private S3Uploader s3Uploader;
	private SalesforceCallRecordUpdater salesforceCallRecordUpdater;

	@Autowired
	public FtpToS3Migrator(SalesforceCallRecordReader salesforceCallRecordReader, FtpBatchDownloader ftpBatchDownloader,
			S3Uploader s3Uploader, SalesforceCallRecordUpdater salesforceCallRecordUpdater) {
		this.salesforceCallRecordReader = salesforceCallRecordReader;
		this.ftpBatchDownloader = ftpBatchDownloader;
		this.s3Uploader = s3Uploader;
		this.salesforceCallRecordUpdater = salesforceCallRecordUpdater;
	}

	public void migrate() {
		// Get data from SF
		List<CallRecord> callRecords = salesforceCallRecordReader.getAllNew();

		for (CallRecord callRecord : callRecords) {
			// Download from FTP
			byte[] file = ftpBatchDownloader.download(callRecord);

			// Upload to S3
			s3Uploader.uploadFile(callRecord, file);
		}
		// Update S3 URL in Salesforce
		salesforceCallRecordUpdater.update(callRecords);

		System.out.println(callRecords);
	}
}
