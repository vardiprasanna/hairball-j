package com.oath.gemini.merchant;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.time.TimeTCPClient;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tong on 10/1/2017
 */
@Slf4j
public class ClosableFTPClient implements Closeable, AutoCloseable {
    public static String username, password, host;
    private static int connectionTimeout;
    private static long remoteTimeDrifting = 0;
    private FTPSClient ftp = new FTPSClient(false); // TLS explicit

    static {
        Configuration config = AppConfiguration.getConfig();

        username = config.getString("ftp.username");
        password = config.getString("ftp.password");
        host = config.getString("ftp.host");
        connectionTimeout = config.getInt("ftp.connection.timeout", 10000);

        // Compute the time compensation between the remote and the local hosts due to the different zone and the eon
        TimeTCPClient timeClient = new TimeTCPClient();

        try {
            timeClient.setConnectTimeout(connectionTimeout);
            timeClient.connect(host);
            remoteTimeDrifting = (timeClient.getTime() - System.currentTimeMillis());
        } catch (Exception e) {
            remoteTimeDrifting = TimeTCPClient.SECONDS_1900_TO_1970 * 1000;
            log.error("Unable to fetch FTP server's current time, and the time zone difference is therefore not considered", e);
        } finally {
            try {
                timeClient.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Copy a local file to the FTP server
     * 
     * @param fromFile - a full pathname
     * @param toFile - an optional base file name if it differs from its from-file
     */
    public void copyTo(String fromFile, String toFile) throws Exception {
        File file = new File(fromFile);

        // setup FPT connection
        connect();

        if (StringUtils.isBlank(toFile)) {
            // Store a file in the root directory
            Path path = Paths.get(file.toURI()).normalize();
            Path fileName = path.getFileName();
            toFile = fileName.toString();
        } else {
            // Create and change to the working directory. If the toFile does not start with the root "/", the
            // directories will be created in a current working directory.
            String[] dirs = toFile.split("/");
            if (dirs != null && dirs.length > 1) {
                for (int i = 0; i < dirs.length - 1; i++) {
                    String dir = dirs[i];

                    if (!ftp.changeWorkingDirectory(dir)) {
                        if (StringUtils.isBlank(dir)) {
                            ftp.makeDirectory(dir);
                            ftp.changeWorkingDirectory(dir);
                        }
                    }
                }
            }
        }

        try (InputStream input = new FileInputStream(file)) {
            if (!this.ftp.storeFile(toFile, input)) {
                log.error("Failed to ftp the file '{}'", fromFile);
                throw new Exception("Failed to ftp the file");
            }
        }
    }

    /**
     * Return true if a FTP file's timestamp is within the freshness threshold
     */
    public boolean isFresh(long remoteTime, long threshold) {
        long localizedRemoteTime = remoteTime - remoteTimeDrifting;
        return (System.currentTimeMillis() - localizedRemoteTime < threshold);
    }

    /**
     * Return true if a given file exists on the FTP server
     * 
     * @param fileName - a full pathname
     */
    public boolean exits(String fileName) throws Exception {
        return find(fileName) != null;
    }

    /**
     * Return a given file if it exists in the FTP server; otherwise null
     */
    public FTPFile find(String fileName) throws Exception {
        int pathIdx = fileName.lastIndexOf('/');
        String baseName = (pathIdx == -1 ? fileName : fileName.substring(pathIdx + 1));
        FTPFile[] baseNames = listFiles(fileName);

        if (ArrayUtils.isEmpty(baseNames)) {
            return null;
        }

        Optional<FTPFile> ftpFile = Arrays.stream(baseNames).filter(f -> f.getName().endsWith(baseName)).findFirst();
        return ftpFile.isPresent() ? ftpFile.get() : null;
    }

    /**
     * List all files that share the same parent directory as the given file
     */
    private FTPFile[] listFiles(String fileName) throws Exception {
        // setup FPT connection
        connect();

        int pathIdx = fileName.lastIndexOf('/');
        if (pathIdx == -1) {
            return ftp.listFiles();
        } else {
            fileName = fileName.substring(0, pathIdx);
            return ftp.listFiles(fileName);
        }
    }

    private void connect() throws Exception {
        ftp.setConnectTimeout(connectionTimeout);
        ftp.connect(host);

        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            log.error("not able to connect the ftp host: {}", host);
            throw new Exception("Exception in connecting to FTP Server");
        }

        ftp.login(username, password);
        ftp.setFileType(FTP.ASCII_FILE_TYPE);
        ftp.setCharset(Charset.forName("UTF-8"));
        ftp.enterLocalPassiveMode();
        ftp.setKeepAlive(true);
    }

    @Override
    public void close() throws IOException {
        if (ftp != null) {
            ftp.logout();
            ftp.disconnect();
        }
    }
}
