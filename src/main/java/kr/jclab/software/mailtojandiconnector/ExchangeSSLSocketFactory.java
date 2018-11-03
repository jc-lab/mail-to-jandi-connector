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

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class ExchangeSSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory sslSocketFactory;
    private SocketFactory socketFactory;

    public ExchangeSSLSocketFactory()
    {
        try {
            socketFactory = SocketFactory.getDefault();

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] {
                            new EmptyTrustManager()
                    }
                    , null);
            sslSocketFactory = (SSLSocketFactory)context.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final class EmptyTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] cert, String authType)
                throws CertificateException {}

        public void checkServerTrusted(X509Certificate[] cert, String authType)
                throws CertificateException{}

        public X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }
    }

    public static SocketFactory getDefault() {
        return new ExchangeSSLSocketFactory();
    }

    @Override public Socket createSocket(Socket socket, String s, int i, boolean
            flag)throws IOException {
        return sslSocketFactory.createSocket(socket, s, i, flag);
    }

    @Override public Socket createSocket(InetAddress inaddr, int i, InetAddress
            inaddr1, int j)throws IOException {
        return socketFactory.createSocket(inaddr, i, inaddr1, j);
    }

    @Override public Socket createSocket(InetAddress inaddr, int i)throws
            IOException {
        return socketFactory.createSocket(inaddr, i);
    }

    @Override public Socket createSocket(String s, int i, InetAddress inaddr, int
            j)throws IOException {
        return socketFactory.createSocket(s, i, inaddr, j);
    }

    @Override public Socket createSocket(String s, int i)throws IOException {
        return socketFactory.createSocket(s, i);
    }

    @Override public Socket createSocket()throws IOException {
        return socketFactory.createSocket();
    }

    @Override public String[] getDefaultCipherSuites() {
        return sslSocketFactory.getSupportedCipherSuites();
    }

    @Override public String[] getSupportedCipherSuites() {
        return sslSocketFactory.getSupportedCipherSuites();
    }
}
