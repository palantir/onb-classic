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

import com.palantir.isofilereader.isofilereader.GenericInternalIsoFile;
import com.palantir.isofilereader.isofilereader.IsoFileReader;
import com.palantir.isofilereader.isofilereader.udf.UdfFormatException;
import com.palantir.onb.LogStandard;
import com.palantir.onb.gui.GuiLog;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Servlet for the http server.
 */
@SuppressWarnings("StrictUnusedVariable")
public class Home extends HttpServlet {
    @Serial private static final long serialVersionUID = -1L;
    private String fileLoc = "";

    @SuppressWarnings("for-rollout:NullAway")
    private static LogStandard localLogger;

    private static boolean allowImageProcessing;

    /**
     * Get the servlet prepared.
     * @param config Servlet config
     * @throws ServletException if something bad happens
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        //We are using only loggerC with the http endpoint work around
        localLogger = new GuiLog(Integer.parseInt(getInitParameter("loggerC")));
        localLogger.setDates(false);
        fileLoc = getInitParameter("folder");
        allowImageProcessing = Boolean.parseBoolean(getInitParameter("imgprocessing"));
    }

    /**
     * HTTP Get functions for servlet.
     * @param req request coming in
     * @param resp servers response
     * @throws IOException errors thrown writing response
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        ServletOutputStream out = resp.getOutputStream();
        String request;
        if (req.getRequestURI() == null || req.getRequestURI().equals("/")) {
            request = "index.html";
        } else {
            request = req.getRequestURI().substring(1);
        }

        // Starts of a simple api, used for logging right now
        if (request.startsWith("api/")) {
            if ("api/log/console".equals(request)) {
                StringBuilder outputData = new StringBuilder();
                for (LogStandard.TimeStampedLog singleString : localLogger.getLogNDump()) {
                    outputData.append(singleString.getLog());
                    outputData.append("\n");
                }
                out.write(outputData.toString().getBytes(StandardCharsets.UTF_8));
                out.flush();
                out.close();
                return;
            }
            resp.sendError(404);
            return;
        }

        localLogger.report("", "", "", " - Read Request - For "
                + request, "/" + req.getRemoteAddr() + " - HTTP - " + "Read Request " + request, false);
        pullImageFile(req, resp);
    }

    private void pullImageFile(HttpServletRequest req, HttpServletResponse resp) {
        String fileToGet = req.getRequestURI();
        //I have seen several times redhat installer ask for something with multiple slashes
        fileToGet = checkForJump(fileToGet).replace("//", "/");
        fileToGet = URLDecoder.decode(fileToGet, StandardCharsets.UTF_8);
        String compressionPath = req.getQueryString();
        if (compressionPath != null) {
            compressionPath = compressionPath.replace("//", "/");
            //If a file has a + in it, this breaks getting the file
            //compressionPath = URLDecoder.decode(compressionPath, StandardCharsets.UTF_8);
        } else {
            Pattern pattern = Pattern.compile(".*\\/(.*\\.(iso|zip))(.*)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(fileToGet);
            if (matcher.find()) {
                fileToGet = matcher.group(1);
                compressionPath = matcher.group(3);
            }
        }

        String internalFileToGet = fileLoc + File.separatorChar + fileToGet;
        if (allowImageProcessing && compressionPath != null && !compressionPath.isEmpty()) {
            getInternalImageFile(req, resp, internalFileToGet, compressionPath);
        } else {
            //raw access mode
            localLogger.report("", "", "", "",
                    "/" + req.getRemoteAddr() + " - HTTP - " + "Pulling: " + fileToGet, false);
            File initialFile = new File(internalFileToGet);
            sendUncompressedData(initialFile, fileToGet, req, resp);
        }
    }

    private void getInternalImageFile(HttpServletRequest req, HttpServletResponse resp, String fileToGet,
                                      String passedCompressionPath) {
        String compressionPath = passedCompressionPath;
        localLogger.report("", "", "", "",
                "/" + req.getRemoteAddr() + " - HTTP - " + "Pulling: " + fileToGet + ":" + compressionPath, false);
        File initialFile = new File(fileToGet);
        if (compressionPath.startsWith("/")) {
            compressionPath = compressionPath.substring(1);
        }

        int dotIndex = fileToGet.lastIndexOf('.');
        String extension;
        if (dotIndex != -1) {
            extension = fileToGet.substring(dotIndex + 1);
            switch (extension) {
                case "iso" -> {
                    sendIsoData(initialFile, compressionPath, req, resp);
                    return;
                }
                case "zip" -> {
                    sendZipData(initialFile, compressionPath, req, resp);
                    return;
                }
                default ->
                        localLogger.simpleReport("Compressed file that is not zip or iso tried to "
                                + "direct load.", true);
            }
        }

        localLogger.simpleReport("File should be on disk and couldn't be located", true);
        send404(resp);

    }

    private String checkForJump(String loc) {
        if (loc.contains("..")) {
            //someone tried to go up a directory
            String editedLoc = loc.replaceAll("..", ".");
            localLogger.simpleReport("A url was given with a directory jump" + editedLoc, true);
            return editedLoc;
        } else {
            return loc;
        }
    }

    private void send404(HttpServletResponse resp) {
        try {
            resp.sendError(404);
        } catch (Exception e) {
            localLogger.simpleReport("Could not write 404." + e, true);
        }
    }

    /**
     * <a href="http://stackoverflow.com/questions/11715979/best-way-to-pipe-inputstream-to-outputstream">Connecting pipes.</a>
     * @param is input
     * @param os output
     * @throws IOException something went wrong
     */
    private void pipe(InputStream is, OutputStream os, int bufferSize) throws IOException {
        int tracking;

        final byte[] buffer = new byte[bufferSize];
        while ((tracking = is.read(buffer)) > -1) {
            os.write(buffer, 0, tracking); // Don't allow any extra bytes to creep in, final write
        }
        os.close();
    }

    @SuppressWarnings("ReadReturnValueIgnored")
    private void pipePart(InputStream is, OutputStream os, int bufferSize, int start, int end) throws IOException {
        int tracking;
        int read = 0;
        final byte[] buffer = new byte[bufferSize];

        is.skip(start);
        while ((tracking = is.read(buffer)) > -1 && read <= (end - start)) {
            if ((read + bufferSize) > (end - start)) {
                tracking = (end - start) - read + 1;
                os.write(buffer, 0, tracking); // Don't allow any extra bytes to creep in, final write
            } else {
                os.write(buffer, 0, tracking); // Don't allow any extra bytes to creep in, final write
            }
            read += tracking;
        }
        os.close();
    }

    private void selectFileType(String requestedFile, HttpServletResponse resp) {
        //Return correct file type, iPXE and some other systems get mad if this isnt correct
        int lastInd = requestedFile.lastIndexOf('.') + 1;
        resp.setContentType("text/plain");
        String sub = requestedFile.substring(lastInd);
        switch (sub) {
            case "zip" -> resp.setContentType("application/zip");
            case "svg" -> resp.setContentType("image/svg+xml");
            case "html" -> resp.setContentType("text/html");
            case "css" -> resp.setContentType("text/css");
            case "js" -> resp.setContentType("application/javascript");
            case "woff" -> resp.setContentType("application/x-font-woff");
        }
    }

    private void sendUncompressedData(File file, String requestedFile, HttpServletRequest req,
                                      HttpServletResponse resp) {
        localLogger.simpleReport("Pulling: " + file.getAbsolutePath(), false);
        if (!file.exists()) {
            send404(resp);
            return;
        }
        try (InputStream input = new FileInputStream(file)) {
            final ServletOutputStream out = resp.getOutputStream();
            selectFileType(requestedFile, resp);
            resp.setContentLengthLong(file.length());
            // Without the following line, if there is a ? in the url, the correct file name will not be sent, and
            // rhel install will fail even though the data is correct
            resp.addHeader("Content-Disposition",
                    "attachment; filename=\"" + file.getName().toLowerCase(Locale.ROOT) + "\"");
            String range = req.getHeader("range");
            // Installs of rhel and cent will not work if they request a range and the server does not respond with that
            // It will look like files are downloading fine, but that will be a lie
            if (range != null) {
                @SuppressWarnings("for-rollout:StringSplitter")
                String[] parts = range.split("=")[1].split("-");
                resp.setContentLengthLong((Integer.parseInt(parts[1]) - Integer.parseInt(parts[0]) + 1));
                pipePart(input, out, resp.getBufferSize(), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            } else {
                resp.setContentLengthLong(file.length());
                pipe(input, out, resp.getBufferSize());
            }
            pipe(input, out, resp.getBufferSize());
            out.flush();
            out.close();
        } catch (IOException e) {
            localLogger.simpleReport("Could not serve uncompressed file. " + e, true);
        }
    }

    private void sendZipData(File fileToGet, String compressionPath, HttpServletRequest req,
                             HttpServletResponse resp) {
        try (ZipFile zipFile = new ZipFile(fileToGet)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals(compressionPath)) {
                    resp.setContentLengthLong(entry.getSize());
                    @SuppressWarnings("for-rollout:StringSplitter")
                    String[] processRealName =
                            entry.getName().toLowerCase(Locale.ROOT).split("/");
                    //This could need range data sometime
                    resp.addHeader("Content-Disposition", "attachment; filename=\""
                            + processRealName[processRealName.length - 1] + "\"");
                    InputStream zipIn = zipFile.getInputStream(entry);
                    final ServletOutputStream out = resp.getOutputStream();
                    selectFileType(req.getRequestURI().toLowerCase(Locale.ROOT), resp);
                    pipe(zipIn, out, resp.getBufferSize());
                    out.flush();
                    out.close();
                    zipIn.close();
                    return;
                }
            }
        } catch (IOException e) {
            localLogger.simpleReport("Error reading zip file. " + e, true);
        }
    }

    @SuppressWarnings({"CyclomaticComplexity", "for-rollout:NarrowingCompoundAssignment"})
    private void sendIsoData(
            File getIsoImage, String passedCompressionPath, HttpServletRequest req, HttpServletResponse resp) {
        String compressionPath = passedCompressionPath;
        if (!getIsoImage.exists()) {
            localLogger.simpleReport("Could not find ISO file on disk, " + getIsoImage.getAbsolutePath(), true);
            send404(resp);
            return;
        }

        compressionPath = "/" + compressionPath; //Iso reading library expects paths to start with a /

        try (IsoFileReader discFs = new IsoFileReader(getIsoImage)) {
            GenericInternalIsoFile[] files = discFs.getAllFiles();
            Optional<GenericInternalIsoFile> generalFile = discFs.getSpecificFileByName(files, compressionPath);

            if (generalFile.isEmpty()) {
                localLogger.simpleReport("Could not find file from ISO. ", true);
                send404(resp);
                return;
            }

            // This means we found the file
            resp.addHeader("Content-Disposition",
                            "attachment; filename=\"" + generalFile.get().getFileName() + "\"");
            final InputStream isoInputStream = discFs.getFileStream(generalFile.get());
            final ServletOutputStream out = resp.getOutputStream();
            selectFileType(generalFile.get().getFileName(), resp);

            String range = req.getHeader("range");
            if (range != null) {
                // Feed part of the file
                @SuppressWarnings("for-rollout:StringSplitter")
                String[] parts = range.split("=")[1].split("-");
                long start = Integer.parseInt(parts[0]);
                long end = Integer.parseInt(parts[1]);
                if (generalFile.get().getSize() < end) {
                    // If past the end of the file is being requested, lower to the real end
                    end = generalFile.get().getSize();
                }
                // Without the +1 here, rhel and things will fail to install
                resp.setHeader("Content-Length", String.valueOf(end - start + 1));
                long skipped = isoInputStream.skip(start);
                if (skipped != start) {
                    localLogger.simpleReport("Failed to seek to right place?", true);
                }
                if (end == generalFile.get().getSize()) {
                    // Stream just goes to end, don't need anything fancy
                    isoInputStream.transferTo(out);
                } else {
                    // This stream stops before the end
                    long tracking;
                    int read = 0;
                    final byte[] buffer = new byte[resp.getBufferSize()];
                    while ((tracking = isoInputStream.read(buffer)) > -1 && read <= (end - start)) {
                        // Don't allow any extra bytes to creep in, final write
                        if ((read + resp.getBufferSize()) > (end - start)) {
                            tracking = (end - start) - read + 1;
                        }
                        out.write(buffer, 0, (int) tracking); // Don't allow any extra bytes to creep in, final write
                        read += tracking;
                    }
                }
            } else {
                //Feed the whole file
                resp.setHeader("Content-Length", String.valueOf(generalFile.get().getSize()));
                isoInputStream.transferTo(out);
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            localLogger.simpleReport("Could not get file from ISO. " + e, true);
            send404(resp);
        } catch (UdfFormatException e) {
            localLogger.simpleReport("Error parsing UDF data. " + e, true);
            send404(resp);
        }
    }
}
