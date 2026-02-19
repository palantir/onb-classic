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

package com.palantir.onb;

import com.palantir.onb.http.ApacheHttp;
import com.palantir.onb.pxe.PxeService;
import com.palantir.onb.tftp.TftpService;
import com.palantir.onb.types.BootRules;
import java.io.File;
import java.io.IOException;

/**
 * Manager that controls all the threads.
 */
public final class Manager {
    private BootRules overrides = new BootRules();

        @SuppressWarnings("for-rollout:NullAway")
        private static Logging logger;

    private String rulesToLoad = "";

    private PxeService coreHandler;
    private Thread intPort67Start;
    private PxeService backup;
    private Thread intPort4011Start;
    private Runnable tftpProcess;
    private Thread tftpThread;
    private ApacheHttp httpProcess;
    private Thread httpThread;

        @SuppressWarnings("for-rollout:NullAway")
        private BootRules fileLoaded = null;

        @SuppressWarnings("for-rollout:NullAway")
        Manager() {}

    public static Logging getManagerLogger() {
        return logger;
    }

    public static void setLogger(Logging logger) {
        Manager.logger = logger;
    }

    /**
     * Command line argument to override interface was given.
     *
     * @param newInts - new interfaces to use
     */
    void overrideInterfaces(String[] newInts) {
        overrides.setLastInterfaces(newInts);
    }

    /**
     * Command line argument to override the PxeService setting was given.
     * @param setting - setting
     */
    void overrideStartPxe(boolean setting) {
        if (setting) {
            overrides.setEnablePxe((byte) 2);
        } else {
            overrides.setEnablePxe((byte) 1);
        }
    }

    /**
     * Command line argument to override the Tftp setting was given.
     * @param setting - setting
     */
    void overrideStartTftp(boolean setting) {
        if (setting) {
            overrides.setEnableTftp((byte) 2);
        } else {
            overrides.setEnableTftp((byte) 1);
        }
    }

    /**
     * Command line argument to override the Http setting was given.
     * @param setting - setting
     */
    void overrideStartHttp(boolean setting) {
        if (setting) {
            overrides.setEnableHttp((byte) 2);
        } else {
            overrides.setEnableHttp((byte) 1);
        }
    }

    /**
     * Command line argument to override the console logging level.
     * @param loglevel - log level
     */
    public void overrideLogLevelConsole(int loglevel) {
        overrides.setLastConsoleLogLvl(loglevel);
        logger.setLogConsole(loglevel);
    }

    /**
     * Command line argument to override the file logging level.
     * @param loglevel - log level
     */
    public void overrideLogLevelFile(int loglevel) {
        overrides.setLastFileLogLvl(loglevel);
        logger.setLogFile(loglevel);
    }

    /**
     * Command line argument to override the file log location.
     *
     * @param loc - Location to put log
     */
    public void overrideLogLocation(String loc) {
        overrides.setLastFileLogLoc(loc);
        logger.setLogFileLocation(loc);
    }

    /**
     * Override main operations folder.
     *
     * @param loc - Location of new folder
     */
    void overrideTftpFolder(String loc) {
        overrides.getTftpSettingSet().setRootFs(new File(loc));
    }

    void setRulesToLoad(String rulesLoc) {
        rulesToLoad = rulesLoc;
    }

    /**
     * Attempt to load rules.
     *
     * @return False is a failure to load rules
     */
    boolean loadRules() {
        if (rulesToLoad.isEmpty()) {
            logger.simpleReport("No rule file given, assuming rules.onr, in program folder.", false);
            rulesToLoad = "rules.onr";
        }
        final BootRulesParser ruleParser = new BootRulesParser(logger);
        String loadedRules = ruleParser.loadRulesFile(rulesToLoad);
        if (loadedRules.isEmpty()) {
            return false;
        }
        fileLoaded = ruleParser.loadRules(loadedRules);
        return fileLoaded != null;
    }

    void switchToLoadedRules() {
        logger.setLogConsole(fileLoaded.getLastConsoleLogLvl());
        logger.setLogFile(fileLoaded.getLastFileLogLvl());
        logger.setLogFileLocation(fileLoaded.getLastFileLogLoc());
    }

    void combineRules() {
        if (overrides.getEnablePxe() > 0) {
            fileLoaded.setEnablePxe(overrides.getEnablePxe());
        }

        if (overrides.getEnableTftp() > 0) {
            fileLoaded.setEnableTftp(overrides.getEnableTftp());
        }

        if (overrides.getEnableHttp() > 0) {
            fileLoaded.setEnableHttp(overrides.getEnableHttp());
        }

        if (overrides.getLastFileLogLvl() >= 0) {
            fileLoaded.setLastFileLogLvl(overrides.getLastFileLogLvl());
        }

        if (overrides.getLastConsoleLogLvl() >= 0) {
            fileLoaded.setLastConsoleLogLvl(overrides.getLastConsoleLogLvl());
        }

        if (!overrides.getLastFileLogLoc().isEmpty()) {
            fileLoaded.setLastFileLogLoc(overrides.getLastFileLogLoc());
        }

        if (overrides.getLastInterfaces() != null) {
            fileLoaded.setLastInterfaces(overrides.getLastInterfaces());
        }

        if (overrides.getTftpSettingSet().getRootFs() != null
                && overrides.getTftpSettingSet().getRootFs().equals(fileLoaded.getTftpSettingSet().getRootFs())) {
            fileLoaded.setTftpSettingSet(overrides.getTftpSettingSet());
        }
    }

    void startServices() {
        if (!fileLoaded.getTftpSettingSet().getRootFs().exists()) {
            logger.simpleReport("TFTP folder appears to not exist", true);
            return;
        }

        initDhcpThreads("0.0.0.0", logger);
        if (fileLoaded.getEnablePxe() == 2) {
            startPxeWithLogger(logger);
        } else {
            logger.simpleReport("Settings have PxeService disabled", false);
        }

        if (fileLoaded.getEnableTftp() == 2) {
            startTftpWithLogger(logger);
        } else {
            logger.simpleReport("Settings have TftpService disabled", false);
        }

        if (fileLoaded.getEnableHttp() == 2) {
            startHttpWithLogger(logger);
        } else {
            logger.simpleReport("Settings have HTTP disabled", false);
        }
    }

    public void startPxeWithLogger(LogStandard passedLogger) {
        passedLogger.simpleReport(
                "Starting OpenNetBoot Core Server - Logging Level; Console: "
                        + passedLogger.getLogConsole() + " , File: " + passedLogger.getLogFile()
                        + ", " + passedLogger.getLogFileLocation(),
                false);
        startThreads();
    }

    public boolean startTftpWithLogger(LogStandard passedLogger) {
        if (fileLoaded.getTftpSettingSet().getRootFs() != null) {
            tftpProcess = new TftpService(fileLoaded.getTftpSettingSet(), passedLogger);
            tftpThread = new Thread(tftpProcess);
            tftpThread.start();
        } else {
            logger.simpleReport("Can not find TftpService folder settings", true);
            return false;
        }
        return true;
    }

    public void startHttpWithLogger(LogStandard passedLogger) {
        if (fileLoaded.getTftpSettingSet().getRootFs() != null) {
            httpProcess = new ApacheHttp(
                    fileLoaded.getTftpSettingSet().getRootFs(), fileLoaded.getHttpPort(),
                    fileLoaded.isAllowIsoExtracting());
            httpProcess.setLocalLogger(passedLogger);
            httpProcess.setLogger(0);
            httpThread = new Thread(httpProcess);
            httpThread.start();
        } else {
            logger.simpleReport("Can not find HTTP folder settings", true);
        }
    }

    public void initDhcpThreads(String listenerIp, LogStandard passedLogger) {
        coreHandler = new PxeService();
        coreHandler.setLogger(passedLogger);
        coreHandler.setNicName(fileLoaded.getLastInterfaces());
        coreHandler.setListenerAddress(listenerIp);
        coreHandler.setListerPort(67);
        coreHandler.setBootRules(fileLoaded);
        intPort67Start = new Thread(coreHandler);
        intPort67Start.setName("DHCP - Port 67");

        backup = new PxeService();
        backup.setLogger(passedLogger);
        backup.setNicName(fileLoaded.getLastInterfaces());
        backup.setListenerAddress(listenerIp);
        backup.setListerPort(4011);
        backup.setBootRules(fileLoaded);
        intPort4011Start = new Thread(backup);
        intPort4011Start.setName("DHCP - Port 4011");
    }

    /**
     * Starts the listener threads, this has to be started only once, I have to bind to all listen
     * addresses with Java Then the Intname variable is a comma delimited list of interface names to
     * listen on.
     *
     */
    private void startThreads() {
        intPort67Start.start();

        intPort4011Start.start();
    }

    public boolean dhcp67Up() {
        return intPort67Start != null && intPort67Start.isAlive();
    }

    public boolean dhcp4011Up() {
        return intPort4011Start != null && intPort4011Start.isAlive();
    }

    public boolean tftpUp() {
        return tftpThread != null && tftpThread.isAlive();
    }

    public boolean httpUp() {
        return httpThread != null && httpThread.isAlive();
    }

    public int httpPort() {
        return fileLoaded.getHttpPort();
    }

    public BootRules getRunningRules() {
        return fileLoaded;
    }

    public void bounceThreadsWithInterfaceLimits() {
        if (flip67()) {
            flip67();
        }
        if (flip4011()) {
            flip4011();
        }
    }

    private boolean stopThread(Thread passedThread) {
        if (passedThread.isAlive()) {
            passedThread.interrupt();
        }
        int waitAttempts = 10;
        while (waitAttempts >= 0) {
            if (passedThread.isAlive()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    logger.simpleReport(e.toString(), true);
                }
                waitAttempts--;
            } else {
                break;
            }
        }
        if (waitAttempts == -1) {
            logger.simpleReport("Could not stop threads (" + passedThread.getName() + ")", true);
            return false;
        }
        return true;
    }

    public boolean flip67() {
        if (intPort67Start != null && intPort67Start.isAlive()) {
            return stopThread(intPort67Start);
        } else {
            coreHandler.setNicName(fileLoaded.getLastInterfaces());
            intPort67Start = new Thread(coreHandler);
            intPort67Start.setName("DHCP - Port 67");
            intPort67Start.start();
            return true;
        }
    }

    public boolean flip4011() {
        if (intPort4011Start != null && intPort4011Start.isAlive()) {
            return stopThread(intPort4011Start);
        } else {
            backup.setNicName(fileLoaded.getLastInterfaces());
            intPort4011Start = new Thread(backup);
            intPort4011Start.setName("DHCP - Port 4011");
            intPort4011Start.start();
            return true;
        }
    }

    public boolean flipTftp() {
        if (tftpThread != null && tftpThread.isAlive()) {
            return stopThread(tftpThread);
        } else {
            return startTftp("TftpService - ");
        }
    }

    private boolean startTftp(String name) {
        tftpThread = new Thread(tftpProcess);
        tftpThread.setName("DHCP - Port " + name);
        tftpThread.start();
        return true;
    }

    public boolean flipHttp() {
        if (httpThread != null && httpThread.isAlive()) {
            return stopThread(httpThread);
        } else {
            return startHttp("Http - ");
        }
    }
    private boolean startHttp(String name) {
        httpProcess.setStartPort(fileLoaded.getHttpPort());
        httpProcess.setFolderLoc(fileLoaded.getTftpSettingSet().getRootFs());
        httpThread = new Thread(httpProcess);
        httpThread.setName(name);
        httpThread.start();
        return true;
    }

        @SuppressWarnings("for-rollout:NullAway")
        public boolean saveSettingsFile() {
        String rulesData = null;
        try {
            rulesData = fileLoaded.saveRules();
        } catch (IOException e) {
            logger.simpleReport(e.toString(), true);
        }
        return fileLoaded.saveFile(rulesData, rulesToLoad);
    }

    public Logging getLogger() {
        return logger;
    }
}
