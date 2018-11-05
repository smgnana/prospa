package au.prospa.ftp;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import au.prospa.domain.CallRecord;

@Service
public class FtpBatchDownloader implements Closeable {

	private String server;
	private int port = 22;
	private String user;
	private String password;

	private JSch jsch = new JSch();
	private Session session = null;
	private ChannelSftp sftpChannel = null;
	private boolean connected = false;

	private static final Logger logger = LoggerFactory.getLogger(FtpBatchDownloader.class);

	@Autowired
	public FtpBatchDownloader(@Value("${ftp.url}") String server, @Value("${ftp.user}") String user, 
			@Value("${ftp.pass}") String password) {
		this.server = server;
		this.user = user;
		this.password = password;
	}

	private void connect() {

		try {
			session = jsch.getSession(user, server, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			Channel channel = session.openChannel("sftp");
			channel.connect();
			sftpChannel = (ChannelSftp) channel;

		} catch (Exception e) {
			throw new RuntimeException("Issue in Login to FTP.", e);
		}

		connected = true;
	}

	public byte[] download(CallRecord record) {

		if (!connected) {
			connect();
		}

		InputStream stream = null;
		try {
			
			if (!record.nvmLinkExists()){
				logger.warn("No NVM link for record: " + record);
				return null;
			}
			
			ChannelSftp.LsEntry result = check(sftpChannel, record);
			if (result == null){
				return null;
			}
			
			record.ftpFileName = result.getFilename();
			
			logger.info("Downloading " + record.getFtpPath() + " as " + user);
			stream = sftpChannel.get(record.getFtpPath());

			return IOUtils.toByteArray(stream);
			// return new InputStreamResource(stream);

		} catch (Exception e) {
			logger.warn("Error when Downloading " + record + " as " + user, e);
			return null;

		} finally {

			try {
				if (stream != null){
					stream.close();
					logger.info("Finished downloading " + record);					
				}
			} catch (IOException e) {
				logger.warn("Issues in Stream close. Ignoring", e);
			}
		}
	}
	
	private ChannelSftp.LsEntry check(ChannelSftp sftpChannel, CallRecord record) throws SftpException {
		Vector<ChannelSftp.LsEntry> lsEntries = sftpChannel.ls(record.getFtpSearchString());
		if (lsEntries.isEmpty()){
			logger.warn("FTP file not found for NVM link in record: " + record);
			return null;
		}
		
		if (lsEntries.size() > 2){
			throw new IllegalStateException(lsEntries.size() + " files found for path " + record);
		}
		
		return lsEntries.firstElement();
	}

	@Override
	public void close() {
		try {
			sftpChannel.exit();
			session.disconnect();
		} catch (Exception e) {
			logger.warn("Issues doing FTP download cleanup. Ignoring", e);
		}
		connected = false;
	}
}
