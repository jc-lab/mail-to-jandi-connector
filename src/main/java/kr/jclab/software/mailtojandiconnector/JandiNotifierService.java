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

import kr.jclab.software.mailtojandiconnector.dto.JandiWebhookRequestDTO;
import kr.jclab.software.mailtojandiconnector.dto.ReceivedMailMessageDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;

@Service
public class JandiNotifierService {
    @Value("${app.jandi.webhook.url}")
    private String jandiWebhookUrl;

    public void notifyMessage(MailReceiveService mailReceiveService, ReceivedMailMessageDTO message) throws RestClientException {
        ClientHttpRequestFactory httpRequestFactory =  new HttpComponentsClientHttpRequestFactory();
        RestTemplate restTemplate = new RestTemplate(httpRequestFactory);
        HttpHeaders headers = new HttpHeaders();
        JandiWebhookRequestDTO requestData = new JandiWebhookRequestDTO();
        HttpEntity<JandiWebhookRequestDTO> entity;
        headers.set("Accept", "application/vnd.tosslab.jandi-v2+json");
        headers.setContentType(MediaType.APPLICATION_JSON);

        System.out.println("notiftyMessage : " + message.msgNum + " : " + message.sentDate.toString());

        requestData.body = message.subject;
        requestData.connectColor = "#10FF60";
        requestData.connectInfo = new ArrayList<>();

        {
            HashMap<String, Object> content = new HashMap<String, Object>();
            content.put("title", "Date");
            content.put("description", message.sentDate.toString());
            requestData.connectInfo.add(content);
        }
        {
            HashMap<String, Object> content = new HashMap<String, Object>();
            content.put("title", "Sender");
            content.put("description", message.sender);
            requestData.connectInfo.add(content);
        }
        {
            HashMap<String, Object> content = new HashMap<String, Object>();
            content.put("title", "내용");
            if(message.content_isHtml)
                content.put("description", cleanTagPerservingLineBreaks(message.content_body));
            else
                content.put("description", message.content_body);
            requestData.connectInfo.add(content);
        }

        entity = new HttpEntity<>(requestData, headers);
        restTemplate.exchange(jandiWebhookUrl, HttpMethod.POST, entity, String.class);

        mailReceiveService.onPushSuccess(message.msgNum);
    }

    public static String cleanTagPerservingLineBreaks(String html) {
        String result = "";
        if (html == null)
            return html;
        Document document = Jsoup.parse(html);
        document.outputSettings(new Document.OutputSettings().prettyPrint(false));// makes html() preserve linebreaks and
        // spacing
        document.select("br").append("\\n");
        document.select("p").prepend("\\n");
        return document.wholeText().replaceAll("\\\\n", "\n").trim();
    }
}
