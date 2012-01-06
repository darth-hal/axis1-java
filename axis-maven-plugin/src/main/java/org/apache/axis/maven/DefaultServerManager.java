/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.axis.maven;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axis.client.AdminClient;
import org.apache.axis.client.Call;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

public class DefaultServerManager implements ServerManager, LogEnabled {
    private final Map servers = new HashMap();
    
    private Logger logger;
    
    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    public void startServer(String jvm, String[] classpath, int port, File workDir, String[] wsddFiles) throws Exception {
        AdminClient adminClient = new AdminClient(true);
        adminClient.setTargetEndpointAddress(new URL("http://localhost:" + port + "/axis/services/AdminService"));
        Process process = Runtime.getRuntime().exec(new String[] {
                jvm,
                "-cp",
                StringUtils.join(classpath, File.pathSeparator),
                "org.apache.axis.transport.http.SimpleAxisServer",
                "-p",
                String.valueOf(port)
        }, null, workDir);
        servers.put(Integer.valueOf(port), new Server(process, adminClient));
        // TODO: need to set up stdout/stderr forwarding; otherwise the process will hang
        
        // Wait for server to become ready
        String versionUrl = "http://localhost:" + port + "/axis/services/Version";
        Call call = new Call(new URL(versionUrl));
        call.setOperationName(new QName(versionUrl, "getVersion"));
        for (int i=0; ; i++) {
            try {
                String result = (String)call.invoke(new Object[0]);
                logger.info("Server ready on port " + port + ": " + result.replace('\n', ' '));
                break;
            } catch (RemoteException ex) {
                if (i == 50) {
                    throw ex;
                }
            }
            Thread.sleep(200);
        }
        
        // Deploy services
        if (wsddFiles != null) {
            for (int i=0; i<wsddFiles.length; i++) {
                String wsddFile = wsddFiles[i];
                if (logger.isDebugEnabled()) {
                    logger.debug("Starting deployment of " + wsddFile);
                }
                String result = adminClient.process(wsddFile);
                if (logger.isDebugEnabled()) {
                    logger.debug("AdminClient result: " + result);
                }
                logger.info("Deployed " + wsddFile);
            }
        }
    }
    
    public void stopServer(int port) throws Exception {
        Server server = (Server)servers.remove(Integer.valueOf(port));
        server.getAdminClient().quit();
        server.getProcess().waitFor();
        logger.info("Server on port " + port + " stopped");
    }
}