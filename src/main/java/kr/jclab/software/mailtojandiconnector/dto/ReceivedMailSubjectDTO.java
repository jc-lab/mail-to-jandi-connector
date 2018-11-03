package kr.jclab.software.mailtojandiconnector.dto;

import kr.jclab.software.mailtojandiconnector.MailParser;

import java.io.IOException;
import java.util.Date;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

public class ReceivedMailSubjectDTO implements Comparable<ReceivedMailSubjectDTO> {
	public int msgNum;
	public String folderName;
	public String sender;
	public String subject;
	public String time;
	public Flags flags;
	public boolean hasAttachment = false;
	public Date sentDate;
	
	public ReceivedMailSubjectDTO(Message msg) throws MessagingException {
		StringBuilder sb = new StringBuilder();
		for(Address address : msg.getFrom())
		{
			if(sb.length() > 0)
				sb.append("\n");
			sb.append(MailParser.utf8Decode(address.toString(), true));
		}
		this.msgNum = msg.getMessageNumber();
		this.folderName = msg.getFolder().getName();
		this.sender = sb.toString();
		this.subject = MailParser.utf8Decode(msg.getSubject(), false);
		this.sentDate = msg.getSentDate();
		this.time = msg.getSentDate().toString();
		this.flags = msg.getFlags();

		if (msg.isMimeType("multipart/*")) {
	        try {
		        MimeMultipart mimeMultipart = (MimeMultipart) msg.getContent();
				processMultipartMessage(mimeMultipart);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}
	
	private void processMultipartMessage(MimeMultipart mimeMultipart) throws MessagingException, IOException {
	    int count = mimeMultipart.getCount();
	    for (int i = 0; i < count; i++) {
	        BodyPart bodyPart = mimeMultipart.getBodyPart(i);
	        if("attachment".equalsIgnoreCase(bodyPart.getDisposition()))
	        	this.hasAttachment = true;
	        if (bodyPart.getContent() instanceof MimeMultipart){
	        	processMultipartMessage((MimeMultipart)bodyPart.getContent());
	        }
	    }
	}
	
	public boolean isAnswered() {
		return (this.flags.contains(Flags.Flag.ANSWERED));
	}
	
	public boolean isFlagged() {
		return (this.flags.contains(Flags.Flag.FLAGGED));
	}
	
	public boolean isDrafted() {
		return (this.flags.contains(Flags.Flag.DRAFT));
	}
	
	public boolean isDeleted() {
		return (this.flags.contains(Flags.Flag.DELETED));
	}
	
	public boolean isSeen() {
		return (this.flags.contains(Flags.Flag.SEEN));
	}

	@Override
	public int compareTo(ReceivedMailSubjectDTO compareObj) {
		return -this.sentDate.compareTo(compareObj.sentDate);
	}

}
