package au.prospa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FtpToS3Migrator {
	
	private SalesforceCallRecordReader salesforceCallRecordReader;

	@Autowired
	public FtpToS3Migrator(SalesforceCallRecordReader salesforceCallRecordReader) {
		this.salesforceCallRecordReader = salesforceCallRecordReader;
	}

	public void migrate(){
		// Get data from SF
		salesforceCallRecordReader.getAllNew();
		// 
	}
}
