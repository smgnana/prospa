package au.prospa.rest.controller;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import au.prospa.ftp.FtpDownloader;
import au.prospa.service.FtpToS3Migrator;

@RestController
public class FtpFetchController {
	
	@Autowired
	public FtpToS3Migrator migrator;

	@GetMapping(value = "/file/wav/{file}", produces = "audio/wav")
	public @ResponseBody ResponseEntity<InputStreamResource> getWavFile(@PathVariable("file") String filePath,
			@RequestHeader("Authorization") String authHeader) throws Exception {

		String credentials = authHeader.substring(authHeader.indexOf("Basic") + "Basic".length() + 1).trim();
		
		String credentialsDecoded = base64Decode(credentials);

		if (credentialsDecoded.indexOf(':') <= 0){
			throw new IllegalStateException("Malformed Org header");
		}
		
		String username = credentialsDecoded.substring(0, credentialsDecoded.indexOf(':'));
		String password = credentialsDecoded.substring(username.length() + 1);
		
		FtpDownloader client = new FtpDownloader("sftp12.contact-world.net", username, password);
		InputStreamResource stream = client.download(base64Decode(filePath));
		
		return new ResponseEntity<InputStreamResource>(stream, HttpStatus.OK);
	}

	private String base64Decode(String encoded) {
		return new String(Base64.getDecoder().decode(encoded));
	}
	
	@GetMapping("/job/ftp-to-s3/")
	public void startFtpToS3(){
		migrator.migrate();
	}
}
