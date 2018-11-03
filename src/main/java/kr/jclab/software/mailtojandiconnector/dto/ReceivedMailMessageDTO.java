package kr.jclab.software.mailtojandiconnector.dto;

import java.io.IOException;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

public class ReceivedMailMessageDTO extends ReceivedMailSubjectDTO {
	public boolean content_isHtml = false;
	public String content_body = null;

	public ReceivedMailMessageDTO(Message msg) throws MessagingException, IOException {
		super(msg);
		Object content = msg.getContent();
		
		if (msg.isMimeType("multipart/*")) {
	        try {
		        MimeMultipart mimeMultipart = (MimeMultipart) msg.getContent();
				processMultipartMessage(mimeMultipart);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }else if(content instanceof String) {
	    	if(msg.isMimeType("text/html")){
	    		content_isHtml = true;
	    		content_body = (String)msg.getContent();
	    	}else{
	    		content_isHtml = false;
	    		content_body = (String)msg.getContent();
	    	}
	    }
	}
	
	private void processMultipartMessage(MimeMultipart mimeMultipart) throws MessagingException, IOException {
	    int count = mimeMultipart.getCount();
	    for (int i = 0; i < count; i++) {
	        BodyPart bodyPart = mimeMultipart.getBodyPart(i);
	        Object content = bodyPart.getContent();
	        if("attachment".equalsIgnoreCase(bodyPart.getDisposition()))
	        	this.hasAttachment = true;
	        if (content instanceof MimeMultipart){
	        	processMultipartMessage((MimeMultipart)bodyPart.getContent());
	        }else if(content instanceof String) {
		    	if(bodyPart.isMimeType("text/html")){
		    		content_isHtml = true;
		    		content_body = (String)bodyPart.getContent();
		    	}else{
		    		content_isHtml = false;
		    		content_body = (String)bodyPart.getContent();
		    	}
		    }
	    }
	}

}
