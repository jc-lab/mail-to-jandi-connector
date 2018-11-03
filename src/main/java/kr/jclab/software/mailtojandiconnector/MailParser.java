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

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailParser {
    //1. 찾아낼 패턴 지정
    private static String pattern = "=\\?(.*?)\\?(.*?)\\?(.*?)\\?=";

    public static String utf8Decode(String source, boolean parseRawLine)
    {
        StringBuilder buffer = new StringBuilder();

        if(parseRawLine) {
            //2. default charset 지정
            String charsetMain = "UTF-8";
            String charsetSub = "B";

            Pattern r = Pattern.compile(pattern);
            Matcher matcher = r.matcher(source);
            //3. 내용 찾아서 decoding

            parseRawLine = false;

            while (matcher.find()) {
                byte[] textBytesArr = null;
                parseRawLine = true;
                charsetMain = matcher.group(1).toUpperCase();
                charsetSub = matcher.group(2);
                if("B".equalsIgnoreCase(charsetSub))
                {
                    try {
                        textBytesArr = (Base64.getDecoder()).decode(matcher.group(3));
                        buffer.append(new String(textBytesArr, Charset.forName(charsetMain)));
                    }catch(IllegalArgumentException e) {
                    }
                }else if("Q".equalsIgnoreCase(charsetSub))
                {
                    int i = 0;
                    String[] arr = matcher.group(3).toLowerCase().split("=");
                    textBytesArr = new byte[arr.length];
                    for(String hexByte : arr) {
                        int d = 0;
                        if(hexByte.length() >= 2)
                        {
                            for(int j=0; j<2; j++) {
                                char c = hexByte.charAt(j);
                                d <<= 4;
                                if(c >= '0' && c <= '9')
                                    d |= (c - '0') & 0xFF;
                                if(c >= 'a' && c <= 'f')
                                    d |= (c - 'a' + 10) & 0xFF;
                            }
                            textBytesArr[i++] = (byte)d;
                        }
                    }
                    buffer.append(new String(textBytesArr, 0, i, Charset.forName(charsetMain)));
                }
            }
        }

        /*
        if(!parseRawLine) {
        	try {
        		buffer.append(MimeUtility.decodeText(new String(source.getBytes("8859_1"), "utf-8")));
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
        */
        //4. decoding할 게 없다면 그대로 반환
        if(buffer.toString().isEmpty()){
            buffer.append(source);
        }
        return buffer.toString();
    }

}

