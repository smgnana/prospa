package au.prospa.domain;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({ "attributes" })
public class CallRecord {
	@JsonProperty("Id")
	public String id;
	
	@JsonProperty("NVM_URL__c")
	public String nvmUrl;
	
	public String date = "2015/01/02";
	
	public String ftpFileName;

	public URL s3Url;

	public String getFtpSearchString() {
		return new StringBuilder().append("/1171/")
				.append(date)
				.append("/*_")
				.append(nvmUrl.substring(nvmUrl.lastIndexOf('/') + 1))
				.append(".wav")
				.toString();
	}

	public boolean nvmLinkExists() {
		return nvmUrl != null;
	}
	
	public String getFtpPath() {
		return new StringBuilder().append("/1171/")
				.append(date)
				.append("/")
				.append(ftpFileName)
				.toString();
	}
	
	public String getS3Path() {
		return new StringBuilder().append(date)
				.append("/")
				.append(ftpFileName)
				.toString();
	}

	@Override
	public String toString() {
		return "CallRecord [id=" + id + ", nvmUrl=" + nvmUrl + ", date=" + date + ", ftpFileName=" + ftpFileName
				+ ", s3Url=" + s3Url + "]";
	}
	
	
}
