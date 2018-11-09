package au.prospa.domain;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({ "attributes" })
public class CallRecord {
	private static final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");

	@JsonProperty("Id")
	public String id;

	@JsonProperty("NVM_URL__c")
	public String nvmUrl;

	@JsonProperty("Created_Date__c")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	public Date createdDate;

	public String ftpFileName;

	public URL s3Url;

	public String uploadError;

	public String getFtpSearchString() {
		return new StringBuilder().append("/1171/").append(format.format(createdDate)).append("/*_")
				.append(nvmUrl.substring(nvmUrl.lastIndexOf('/') + 1)).append(".wav").toString();
	}

	public boolean nvmLinkExists() {
		return nvmUrl != null;
	}

	public String getFtpPath() {
		return new StringBuilder().append("/1171/").append(format.format(createdDate)).append("/").append(ftpFileName)
				.toString();
	}

	public String getS3Path() {
		return new StringBuilder().append(format.format(createdDate)).append("/").append(ftpFileName).toString();
	}

	@Override
	public String toString() {
		return "CallRecord [id=" + id + ", nvmUrl=" + nvmUrl + ", date=" + createdDate + ", ftpFileName=" + ftpFileName
				+ ", s3Url=" + s3Url + "]";
	}

	public boolean createdDateExists() {
		return createdDate != null;
	}

}
