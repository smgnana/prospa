package au.prospa.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadResult {
	@JsonProperty("Error_Message__c")
	public String errorMessage = null;
	
	@JsonProperty("Total_Count__c")
	public int totalCount = 0;
	
	@JsonProperty("Success_Count__c")
	public int successCount = 0;
	
	@JsonProperty("Skipped_Count__c")
	public int skippedCount = 0;

	@JsonProperty("Status__c")
	public String status;
	
	public void populateStatus() {
		CallRecordUploadReportStatus status = null;
		
		if (errorMessage != null)
			status = CallRecordUploadReportStatus.ERROR;
		
		status =  totalCount == successCount ? CallRecordUploadReportStatus.FINISHED_COMPLETE
				: CallRecordUploadReportStatus.FINISHED_INCOMPLETE;
		
		this.status = status.getValue();
	}
	
	public static UploadResult error(String message){
		UploadResult result = new UploadResult();
		result.errorMessage = message;
		return result;
	}
}