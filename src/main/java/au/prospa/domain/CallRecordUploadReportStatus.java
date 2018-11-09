package au.prospa.domain;

public enum CallRecordUploadReportStatus {
	ERROR("Error"), FINISHED_COMPLETE("Finished Complete"), FINISHED_INCOMPLETE("Finished Incomplete");
	
	private final String value;

	private CallRecordUploadReportStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
