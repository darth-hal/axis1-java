/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.apache.axis.integration;

import java.io.File;
import java.net.ServerSocket;

import javax.xml.namespace.QName;

import org.apache.axis.context.EngineContext;
import org.apache.axis.context.ServiceContext;
import org.apache.axis.engine.AxisFault;
import org.apache.axis.engine.EngineRegistryFactory;
import org.apache.axis.transport.http.SimpleHTTPServer;

public class UtilServer {
    private static int count = 0;
    private static SimpleHTTPServer reciver;
    public static final int TESTING_PORT = 5555;
    public static final String FAILURE_MESSAGE = "Intentional Faliure";



    public static synchronized void deployService(ServiceContext service)
            throws AxisFault {
        reciver.getEngineReg().addService(service);
        reciver.getEngineReg().getEngineConfig().addService(service.getServiceConfig());
    }

    public static synchronized void unDeployService(QName service)
            throws AxisFault {
        reciver.getEngineReg().getEngineConfig().removeService(service);
    }

    public static synchronized void start() throws Exception {
        if (count == 0) {
            Class erClass = Class.forName("org.apache.axis.deployment.EngineRegistryFactoryImpl");
            EngineRegistryFactory erfac = (EngineRegistryFactory)erClass.newInstance();
            
            File file = new File("target/test-resources/samples");
            System.out.println(new File(file,"server.xml").exists());
            EngineContext er = erfac.createEngineRegistry(file.getAbsolutePath());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e1) {
                throw new AxisFault("Thread interuptted", e1);
            }

            

            ServerSocket serverSoc = null;
            serverSoc = new ServerSocket(Constants.TESTING_PORT);
            reciver = new SimpleHTTPServer(er,serverSoc);
            Thread thread = new Thread(reciver);
            thread.setDaemon(true);

            try {
                thread.start();
                System.out.print("Server started on port " + Constants.TESTING_PORT + ".....");
            } finally {

            }
        }
        count++;
    }

    public static synchronized void stop() {
        if (count == 1) {
            reciver.stop();
            count = 0;
            System.out.print("Server stopped .....");
        } else {
            count--;
        }
    }
}