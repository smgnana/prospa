package au.prospa.ftp;

import java.io.InputStream;

import org.springframework.core.io.InputStreamResource;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class FtpDownloader {

	private String server;
	private int port = 22;
	private String user;
	private String password;

	public FtpDownloader(String server, String user, String password) {
		this.server = server;
		this.user = user;
		this.password = password;
	}

	public InputStreamResource download(String path) throws Exception {
		JSch jsch = new JSch();
		Session session = null;
		InputStream stream = null;
		ChannelSftp sftpChannel = null;
		
		try {
			session = jsch.getSession(user, server, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			Channel channel = session.openChannel("sftp");
			channel.connect();
			sftpChannel = (ChannelSftp) channel;

			stream = sftpChannel.get(path);
			System.out.println("Downloading "+ path +" as " + user);
			
			//return IOUtils.toByteArray(stream);
			return new InputStreamResource(stream);

		} finally {
			System.out.println("Finished downloading "+ path);
			
			stream.close();
			sftpChannel.exit();
			session.disconnect();
		}
	}
}
