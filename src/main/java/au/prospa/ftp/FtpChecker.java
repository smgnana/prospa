package au.prospa.ftp;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.Vector;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class FtpChecker {

	private String server;
	private int port = 22;
	private String user;
	private String password;
	
	DateTimeFormatter df1 = DateTimeFormat.forPattern("dd/MM/yyyy");
	DateTimeFormatter df2 = DateTimeFormat.forPattern("yyyy/MM/dd");
	
	public static void main(String[] args) throws Exception {
		new FtpChecker(args[0], args[1], args[2]).find(new File("Input.csv"), new File("Output.csv"));
	}

	public FtpChecker(String server, String user, String password) {
		this.server = server;
		this.user = user;
		this.password = password;
	}

	public void find(File source, File destination) throws Exception {
		System.out.println(source.getAbsolutePath());
		JSch jsch = new JSch();
		Session session = null;
		ChannelSftp sftpChannel = null;
		Reader in = null;
		
		Writer writer = Files.newBufferedWriter(destination.toPath());
		CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("ID", "CreatedDate", "URL__c", "FoundInFtp__c", "FtpPath__c", "FtpFileSize__c", "FtpSearchString__c"));
		
		try {
			session = jsch.getSession(user, server, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			Channel channel = session.openChannel("sftp");
			channel.connect();
			sftpChannel = (ChannelSftp) channel;
			
			in = new FileReader(source);
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
			
			int count = 0;
			
			for (CSVRecord record : records) {
				count=count+1;
			    String sfId = record.get("Call Record: ID");
			    String createdDate = record.get("Created Date");
			    String url = record.get("NVM URL");
			    //System.out.println(sfId + createdDate + url);
			    
			    String path = resolvePath(createdDate, url);
			    
			    
			    FtpResult found = check(sftpChannel, path);
			    csvPrinter.printRecord(sfId, createdDate, url, found.found, found.fileName, found.size, found.queryString);
			    System.out.println(path+"Checking file no ==>"+count+"Salesforce Id ==>"+sfId);
			}
			System.out.println("Completed...");

		} finally {
			in.close();
			csvPrinter.flush();
			csvPrinter.close();
			sftpChannel.exit();
			session.disconnect();
		}
	}

	private String resolvePath(String createdDate, String url) throws ParseException {
		DateTime parsedDate = df1.parseDateTime(createdDate);
		
		return new StringBuilder().append("/1171/")
				.append(df2.print(parsedDate))
				.append("/*_")
				.append(url.substring(url.lastIndexOf('/') + 1))
				.append(".wav")
				.toString();
	}

	private FtpChecker.FtpResult check(ChannelSftp sftpChannel, String path) throws SftpException {
		Vector<ChannelSftp.LsEntry> lsEntries = sftpChannel.ls(path);
		if (lsEntries.isEmpty()){
			return new FtpChecker.FtpResult(false, null, null, path);
		}
		
		if (lsEntries.size() > 2){
			throw new IllegalStateException(lsEntries.size() + " files found for path " + path);
		}
		
		FtpChecker.FtpResult result = null;
		for (LsEntry lsEntry : lsEntries) {
//			System.out.println(lsEntry.getFilename());
//			System.out.println(lsEntry.getAttrs().getSize());
			result = new FtpChecker.FtpResult(true, lsEntry.getFilename(), lsEntry.getAttrs().getSize(), path);
		}
		return result;
	}
	
	static class FtpResult{
		boolean found;
		String fileName;
		Long size;
		String queryString;
		
		public FtpResult(boolean found, String fileName, Long size, String queryString) {
			this.found = found;
			this.fileName = fileName;
			this.size = size;
			this.queryString = queryString;
		}
		
	}
}
