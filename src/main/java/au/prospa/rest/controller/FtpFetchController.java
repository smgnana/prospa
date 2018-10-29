package au.prospa.rest.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FtpFetchController {
	
	@GetMapping(value = "/file/test",produces = "audio/wav")
	public @ResponseBody byte[] getTest() throws IOException {
		File file = new File(getClass().getClassLoader().getResource("test.wav").getFile());
		return Files.readAllBytes(file.toPath());
	}
}
