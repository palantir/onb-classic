/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.onb.http;

import com.palantir.onb.LogStandard;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

/**
 * Runnable to start the web server.
 */
public class ApacheHttp implements Runnable {
        @SuppressWarnings("for-rollout:NullAway")
        private static LogStandard localLogger;

    private int logger;
    private int startPort;
    private File folderLoc;

    private final boolean allowExtraction;

    /**
     * Entry point to start http server.
     * @param passedLoc location to host files out of
     * @param passedPort port to run on
     */
    public ApacheHttp(File passedLoc, int passedPort, boolean allowExtraction) {
        folderLoc = passedLoc;
        startPort = passedPort;
        this.allowExtraction = allowExtraction;
    }

    /**
     * Set up a logger for the apache server.
     * @param passedLogger logger to use
     */
    public void setLocalLogger(LogStandard passedLogger) {
        localLogger = passedLogger;
    }

    /**
     * Set the logger level.
     * @param logger int for the logger lever
     */
    public void setLogger(int logger) {
        this.logger = logger;
    }

    /**
     * Change HTTP port.
     * @param startPort which port to start on
     */
    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    /**
     * Override current folder location.
     * @param folderLoc file of folder location
     */
    public void setFolderLoc(File folderLoc) {
        this.folderLoc = folderLoc;
    }

    /**
     * Looping thread that contains the Apache server.
     */
    @Override
    public void run() {
        boolean shutdown = false;
        final Tomcat tomcat = new Tomcat();
        tomcat.setPort(startPort);

        final Context ctx = tomcat.addContext("/", folderLoc.getAbsolutePath());

        try {
            final Wrapper home = Tomcat.addServlet(ctx, "homeServlet", "com.palantir.onb.http.Home");
            home.addInitParameter("folder", folderLoc.getAbsolutePath());

            // Different loggers need different startup info
            home.addInitParameter("loggerC", Integer.toString(localLogger.getLogConsole()));
            home.addInitParameter("loggerF", Integer.toString(localLogger.getLogFile()));
            home.addInitParameter("logger", Integer.toString(logger));
            home.addInitParameter("imgprocessing", Boolean.toString(allowExtraction));

            ctx.addServletMappingDecoded("/", "homeServlet");
            localLogger.simpleReport(
                    "HTTP - Starting server on port " + startPort + ". Responding on all interfaces.", false);
            localLogger.report(
                    "",
                    "",
                    "",
                    "HTTP - configuring app with basedir: " + folderLoc.getAbsolutePath(),
                    "HTTP - configuring app with basedir: " + folderLoc.getAbsolutePath(),
                    false);

            tomcat.setSilent(true);

            // Below is commented out code that works for starting with HTTPS, one day that will be made to work
            final Connector setupConnector = tomcat.getConnector();
            setupConnector.setProperty("maxKeepAliveRequests", "1");
            // setupConnector.setSecure(true);
            setupConnector.setScheme("http");
            // setupConnector.setAttribute("keyAlias", "tomcat");
            // setupConnector.setAttribute("keystorePass", "palantir");
            // setupConnector.setAttribute("keystoreFile", "../ssl/localkeystore.jks");
            // setupConnector.setAttribute("clientAuth", "false");
            // setupConnector.setAttribute("sslProtocol", "TLS");
            // setupConnector.setAttribute("SSLEnabled", true);

            tomcat.setConnector(setupConnector);

            tomcat.start();
            /*
             * This is a big hack. Servlets run in self-contained threads, so to talk to them each
             * servlet has a buffer, then I hit that buffer...
             */
            URL localUrl = null;
            try {
                localUrl = new URL("http://localhost:" + startPort + "/api/log/console");
            } catch (final MalformedURLException e2) {
                localLogger.simpleReport(e2.toString(), true);
                return;
            }

            while (!shutdown) {
                try {
                    final URLConnection lc = localUrl.openConnection();
                    final BufferedReader in =
                            new BufferedReader(new InputStreamReader(lc.getInputStream(), StandardCharsets.UTF_8));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        localLogger.simpleReport(inputLine, false);
                    }
                    in.close();
                } catch (final IOException e1) {
                    localLogger.simpleReport(e1.toString(), true);
                }

                try {
                    Thread.sleep(500);
                } catch (final InterruptedException e) {
                    tomcat.stop();
                    tomcat.destroy();
                    shutdown = true;
                }
            }
        } catch (final LifecycleException e1) {
            localLogger.simpleReport(e1.toString(), true);
        }
        localLogger.simpleReport("Shutting down HTTP on " + startPort, false);
    }
}
