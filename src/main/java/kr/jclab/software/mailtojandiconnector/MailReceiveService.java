/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.jclab.software.mailtojandiconnector;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import kr.jclab.software.mailtojandiconnector.dto.ReceivedMailMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import javax.mail.*;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MailReceiveService extends Thread {
    @Value("${app.mail.host}")
    private String mailHost;
    @Value("${app.mail.port}")
    private int mailPort;
    @Value("${app.mail.username}")
    private String mailUsername;
    @Value("${app.mail.password}")
    private String mailPassword;
    @Value("${app.mail.protocol}")
    private String mailProtocol;

    @Autowired
    private JandiNotifierService jandiNotifierService;

    private Store m_mailStore;
    private Object m_mailStoreLock = new Object();

    private AtomicInteger m_runState = new AtomicInteger(0);

    private MessageCountListener m_messageCountListener = new MessageCountListener() {
        @Override
        public void messagesAdded(MessageCountEvent e) {
            int index;
            Message[] messages = e.getMessages();
            System.out.println("messagesAdded: " + e.getType());
            for(index = 0; index < messages.length; index++) {
                try {
                    ReceivedMailMessageDTO mailMessageDTO = new ReceivedMailMessageDTO(messages[index]);
                    jandiNotifierService.notifyMessage(MailReceiveService.this, mailMessageDTO);
                } catch (MessagingException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        @Override
        public void messagesRemoved(MessageCountEvent e) {
            System.out.println("messagesRemoved: " + e.getType());
        }
    };

    public void reqStop() {
        try {
            m_mailStore.close();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        m_runState.set(2);
    }

    public void safeJoin() throws InterruptedException {
        if(m_runState.get() < 1)
            return ;
        while(m_runState.get() < 3) {
            this.join(100);
        }
    }

    public void prepare() throws Exception {
        Properties properties = new Properties();
        Session session;
        properties.setProperty("ssl.SocketFactory.provider", ExchangeSSLSocketFactory.class.getName());
        properties.setProperty("mail.imap.socketFactory.class", ExchangeSSLSocketFactory.class.getName());
        properties.setProperty("mail.pop3.socketFactory.class", ExchangeSSLSocketFactory.class.getName());
        properties.setProperty("mail.imaps.socketFactory.class", ExchangeSSLSocketFactory.class.getName());
        properties.setProperty("mail.pop3s.socketFactory.class", ExchangeSSLSocketFactory.class.getName());
        session = Session.getInstance(properties, null);
        m_mailStore = session.getStore(mailProtocol);
        m_runState.set(1);
    }

    @Override
    public void run() {
        while(m_runState.get() == 1) {
            try {
                IMAPFolder inboxFolder;
                Message[] messages;
                m_mailStore.connect(mailHost, mailPort, mailUsername, mailPassword);
                inboxFolder = (IMAPFolder) m_mailStore.getFolder("INBOX");
                inboxFolder.addMessageCountListener(m_messageCountListener);
                inboxFolder.open(Folder.READ_WRITE);

                List<ReceivedMailMessageDTO> mailList = new ArrayList<>();
                messages = inboxFolder.getMessages();
                for(Message message : messages) {
                    try {
                        ((IMAPMessage)message).setPeek(true);
                        if(!message.getFlags().contains(Flags.Flag.SEEN)) {
                            ReceivedMailMessageDTO mailMessageDTO = new ReceivedMailMessageDTO(message);
                            mailList.add(mailMessageDTO);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Collections.sort(mailList);
                Collections.reverse(mailList);

                try {
                    for (ReceivedMailMessageDTO item : mailList) {
                        jandiNotifierService.notifyMessage(MailReceiveService.this, item);
                    }
                }catch(RestClientException e) {
                    e.printStackTrace();
                }

                while(m_runState.get() == 1) {
                    inboxFolder.idle();
                }

                inboxFolder.removeMessageCountListener(m_messageCountListener);

                inboxFolder.close();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }

    public void onPushSuccess(int msgNum) {
        synchronized (m_mailStoreLock) {
            try {
                IMAPFolder imapFolder = (IMAPFolder) m_mailStore.getFolder("INBOX");
                Message message;
                imapFolder.open(Folder.READ_WRITE);
                message = imapFolder.getMessage(msgNum);
                message.setFlag(Flags.Flag.SEEN, true);
                imapFolder.close();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }
}
