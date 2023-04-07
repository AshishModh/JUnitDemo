package com.liquidnet.pst.helm.wizard.logdownload;

import ats.frontend.login.AtsFrontendLogin;
import com.google.protobuf.Message;
import com.liquidnet.gpss.lib.app.GpssApplicationException;
import com.liquidnet.gpss.lib.services.web.WebUtils;
import com.liquidnet.pst.helm.IProtoService;
import liquidnet.messages.AtsMemberAdmin;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ConcurrentReferenceHashMap;
import spark.Request;
import spark.Response;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class LogDownloadService {
    private static final Logger logger = LoggerFactory.getLogger(com.liquidnet.pst.helm.wizard.logdownload.LogDownloadService.class);
    private ConcurrentMap<String, BlockingQueue<Message>> requestIdToResponseQueueMap;
    private final IProtoService protoService;
    private int timeoutMs;
    private String saveLogHutPath;
    private Clock clock;
    public static final String TIMEOUT_MESSAGE_ERROR = "Timed out while waiting for response to request";
    public static final String GENERIC_TRADER_BUTLER_MESSAGE_ERROR = "Error occurred while sending trade butler request";
    public static final String WHILE_SHOWING_DIRECTORY_LIST_MESSAGE_ERROR = "Error occurred while showing directory list";
    public static final String UNABLE_TO_STORE_LOG_FILE_IN_DIRECTORY_MESSAGE_ERROR = "Error storing log file in logs directory";
    public static final String INTERRUPTED_LISTING_MESSAGE_ERROR = "Interrupted while waiting for message list response";
    public static final String INTERRUPTED_SAVE_LOG_MESSAGE_ERROR = "Interrupted while waiting to store file at log hut";
    public static final String YOUR_REQUEST_CANNOT_BE_FULFILLED_MESSAGE_ERROR = "Your request cannot be fulfilled at this time, please try again";
    public static final String PARSING_LOGS_DIRECTORY_LIST_MESSAGE_ERROR = "Error parsing logs directory list";
    public static final String WHILE_UNCOMPRESSING_GZIP_MESSAGE_ERROR = "Error occurred while uncompressing GZIP";
    public static final String RECEIVED_IS_0_OR_NULL_MESSAGE_ERROR = "File byte received is 0 or null";
    public static final String ACCESSING_OR_CREATING_DIRECTORY_FOR_LOG_HUT_MESSAGE_ERROR = "Error occurred while accessing or creating directory for log hut. Please contact Technical Product Support";
    public static final String VALIDATE_LOG_HUT_RESPONSE = "Invalid file request";
    public static final String LOG_HUT_DOWNLOAD_EXCEPTION_RESPONSE = "Error occurred during file download";


    public LogDownloadService(ConcurrentMap<String, BlockingQueue<Message>> requestIdToResponseQueueMap, IProtoService protoService, int timeoutMs, String saveLogHutPath, Clock clock) {
        this.requestIdToResponseQueueMap = requestIdToResponseQueueMap == null ? new ConcurrentReferenceHashMap<>() : requestIdToResponseQueueMap;
        this.protoService = protoService;
        this.timeoutMs = timeoutMs;
        this.saveLogHutPath = saveLogHutPath;
        this.clock = clock;
    }

    public LogDownloadWrapper butlerProcess(LogDownloadRequest logDownloadRequest) {
        if (requestIdToResponseQueueMap.containsKey(logDownloadRequest.getRequestKey())) {
            return new LogDownloadWrapper(Collections.emptyList(), YOUR_REQUEST_CANNOT_BE_FULFILLED_MESSAGE_ERROR);
        }
        return "DIR".equalsIgnoreCase(logDownloadRequest.getActionType()) ? listLogDirectory(logDownloadRequest) : logHutFileStore(logDownloadRequest);
    }

    private LogDownloadWrapper listLogDirectory(LogDownloadRequest logDownloadRequest) {
        try {
            logDownloadRequest.validateRequestActionType();
            butlerEntriesRequest(logDownloadRequest, 1);
            return new LogDownloadWrapper(getDirectoryListFromEntries(logDownloadRequest.getIsOMSI(), logDownloadRequest.getRequestKey()), "");
        } catch (GpssApplicationException e) {
            return new LogDownloadWrapper(Collections.emptyList(), e.getMessage());
        } catch (Exception e) {
            logger.error(WHILE_SHOWING_DIRECTORY_LIST_MESSAGE_ERROR + " {}", e);
            return new LogDownloadWrapper(Collections.emptyList(), WHILE_SHOWING_DIRECTORY_LIST_MESSAGE_ERROR);
        } finally {
            removeKey(logDownloadRequest.getRequestKey());
        }
    }

    private LogDownloadWrapper logHutFileStore(LogDownloadRequest logDownloadRequest) {
        try {
            logDownloadRequest.validateRequestActionType();
            butlerEntriesRequest(logDownloadRequest, 1);
            return new LogDownloadWrapper(Collections.emptyList(), "", saveLogHut(logDownloadRequest));
        } catch (GpssApplicationException e) {
            return new LogDownloadWrapper(Collections.emptyList(), e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new LogDownloadWrapper(Collections.emptyList(), UNABLE_TO_STORE_LOG_FILE_IN_DIRECTORY_MESSAGE_ERROR);
        } finally {
            removeKey(logDownloadRequest.getRequestKey());
        }
    }

    protected void butlerEntriesRequest(LogDownloadRequest logDownloadRequest, int isFromStart) throws GpssApplicationException {
        Message request;
        try {
            if (logDownloadRequest.getIsOMSI()) {
                request = AtsMemberAdmin.OMSIButlerRequest.newBuilder()
                        .setActionType(logDownloadRequest.getActionType())
                        .setFileName(logDownloadRequest.getFileName())
                        .setMemberId(logDownloadRequest.getMemberId())
                        .setResendFromStart(isFromStart)
                        .build();
            } else {
                request = AtsFrontendLogin.TraderButlerRequest.newBuilder()
                        .setActionType(logDownloadRequest.getActionType())
                        .setFileName(logDownloadRequest.getFileName())
                        .setMemberId(logDownloadRequest.getMemberId())
                        .setUserId(logDownloadRequest.getUserId())
                        .build();
            }
            if (!requestIdToResponseQueueMap.containsKey(logDownloadRequest.getRequestKey())) {
                this.requestIdToResponseQueueMap.put(logDownloadRequest.getRequestKey(), new LinkedBlockingQueue<>());
            }
            this.protoService.sendMessage(request, logDownloadRequest.getTopic());
        } catch (Exception e) {
            logger.error(GENERIC_TRADER_BUTLER_MESSAGE_ERROR + " {} ", e);
            throw new GpssApplicationException(GENERIC_TRADER_BUTLER_MESSAGE_ERROR);
        }
    }

    private List<LogDownload> getDirectoryListFromEntries(Boolean isOMSI, String requestKey) throws GpssApplicationException {
        try {
            BlockingQueue<Message> messageQueue = this.requestIdToResponseQueueMap.get(requestKey);
            Message message = messageQueue.poll((long) timeoutMs, TimeUnit.MILLISECONDS);
            if (!messageCheck(message)) {
                throw new GpssApplicationException(TIMEOUT_MESSAGE_ERROR);
            }
            if (isOMSI) {
                AtsMemberAdmin.OMSIButlerResponse omsiMessage = (AtsMemberAdmin.OMSIButlerResponse) message;
                if (omsiMessage.getButlerFileEntriesList().isEmpty()) {
                    byte[] fileBytes = getUncompressedBytes(omsiMessage.getFile().toByteArray());
                    return parseDirString(new String(fileBytes));
                }
                return omsiMessage.getButlerFileEntriesList().stream().map(obj -> new LogDownload(obj.getType(), obj.getFileName().replace("\\\\", "\\" + omsiMessage.getRequest().getFileName() + "\\"), obj.getDateTimeModified(), String.valueOf(obj.getSize()))).collect(Collectors.toList());
            } else {
                AtsFrontendLogin.TraderButlerResponse ln5Message = (AtsFrontendLogin.TraderButlerResponse) message;
                return ln5Message.getButlerFileEntriesList().stream().map(obj -> new LogDownload(obj.getType(), obj.getFileName().replace("\\\\", "\\" + ln5Message.getRequest().getFileName() + "\\"), obj.getDateTimeModified(), String.valueOf(obj.getSize()))).collect(Collectors.toList());
            }
        } catch (InterruptedException var5) {
            Thread.currentThread().interrupt();
            logger.error(INTERRUPTED_LISTING_MESSAGE_ERROR + " {}", var5);
            throw new GpssApplicationException(INTERRUPTED_LISTING_MESSAGE_ERROR);
        }
    }

    private String saveLogHut(LogDownloadRequest request) throws GpssApplicationException {
        Path fileFullPath = getValidPath(request);
        Boolean isLastPart;
        String result;
        try (FileOutputStream outputStream = new FileOutputStream(fileFullPath.toString(), true)) {
            do {
                BlockingQueue<Message> messageQueue = this.requestIdToResponseQueueMap.get(request.getRequestKey());
                Message message = messageQueue.poll((long) timeoutMs, TimeUnit.MILLISECONDS);
                if (!messageCheck(message)) {
                    throw new GpssApplicationException(TIMEOUT_MESSAGE_ERROR);
                }
                result = request.getIsOMSI() ? ((AtsMemberAdmin.OMSIButlerResponse) message).getResult() : ((AtsFrontendLogin.TraderButlerResponse) message).getResult();
                if (!"".equalsIgnoreCase(result)) {
                    logger.info("Error occurred at ATS end {}", result);
                    throw new GpssApplicationException(result);
                }
                byte[] fileBytes = getMessageBytes(request.getIsOMSI(), message);
                outputStream.write(getUncompressedBytes(fileBytes));
                isLastPart = request.getIsOMSI() ? ((AtsMemberAdmin.OMSIButlerResponse) message).getIsLastPart() : ((AtsFrontendLogin.TraderButlerResponse) message).getIsLastPart();
                if (!isLastPart) {
                    butlerEntriesRequest(request, 0);
                }
            } while (!isLastPart);
            logger.info("File successfully saved at log hut path : {}", fileFullPath);
            return fileFullPath.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(INTERRUPTED_SAVE_LOG_MESSAGE_ERROR + " {}", e);
            throw new GpssApplicationException(INTERRUPTED_SAVE_LOG_MESSAGE_ERROR);
        } catch (IOException e) {
            throw new GpssApplicationException("Error occurred during file creation");
        }
    }

    private byte[] getMessageBytes(Boolean isOMSI, Message message) throws GpssApplicationException {
        byte[] fileBytes = isOMSI ? ((AtsMemberAdmin.OMSIButlerResponse) message).getFile().toByteArray() : ((AtsFrontendLogin.TraderButlerResponse) message).getFile().toByteArray();
        if (fileBytes.length == 0) {
            logger.info(RECEIVED_IS_0_OR_NULL_MESSAGE_ERROR);
            throw new GpssApplicationException(RECEIVED_IS_0_OR_NULL_MESSAGE_ERROR);
        }
        return fileBytes;
    }

    public Path getValidPath(LogDownloadRequest request) throws GpssApplicationException {
        try {
            String fileName = FilenameUtils.getName(request.getFileName());
            Path fileFullPath = Paths.get(saveLogHutPath, request.getMemberId(), request.getIsOMSI() ? "OMSI" : request.getUserId(), DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss").withZone(this.clock.getZone()).format(Instant.now(this.clock)), fileName);
            if (!fileFullPath.getParent().toFile().exists()) Files.createDirectories(fileFullPath.getParent());
            return fileFullPath;
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new GpssApplicationException(ACCESSING_OR_CREATING_DIRECTORY_FOR_LOG_HUT_MESSAGE_ERROR);
        }
    }

    public Boolean messageCheck(Message message) {
        Boolean isEmpty = true;
        if (null == message) {
            logger.error(TIMEOUT_MESSAGE_ERROR);
            isEmpty = false;
        }
        return isEmpty;
    }

    private byte[] getUncompressedBytes(byte[] bytes) throws GpssApplicationException {
        try (GZIPInputStream gzInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream fileBytesOutStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzInputStream.read(buffer)) != -1) {
                fileBytesOutStream.write(buffer, 0, len);
            }
            return fileBytesOutStream.toByteArray();
        } catch (IOException ioEx) {
            logger.error(ioEx.getMessage());
            throw new GpssApplicationException(WHILE_UNCOMPRESSING_GZIP_MESSAGE_ERROR);
        }
    }

    public void downloadLogHut(Request req, Response res) {
        String logHutUrl = WebUtils.decodeParamString(req, "logHutUrl");
        String fileName = FilenameUtils.getName(logHutUrl);
        res.type("application/octet-stream");
        res.header("Content-Disposition", "attachment; filename=" + fileName);
        if (validateLogHutUrl(logHutUrl)) {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(logHutUrl));
                 BufferedOutputStream outputStream = new BufferedOutputStream(res.raw().getOutputStream())) {
                byte[] buf = new byte[1024];
                int length;
                while ((length = bis.read(buf)) != -1) {
                    outputStream.write(buf, 0, length);
                }
                outputStream.flush();
            } catch (IOException ioEx) {
                logger.error(ioEx.getMessage());
                res.header("File-Download-Error", LOG_HUT_DOWNLOAD_EXCEPTION_RESPONSE);
            }
        } else {
            res.header("File-Download-Error", VALIDATE_LOG_HUT_RESPONSE);
        }

    }

    private Boolean validateLogHutUrl(String logHutUrl) {
        if (logHutUrl != null && !"".equals(logHutUrl)) {
            Path savedLogHutPath = Paths.get(saveLogHutPath);
            Path logHutPath = Paths.get(logHutUrl);
            return logHutPath.startsWith(savedLogHutPath);
        }
        logger.info("URL provided is not log hut path.");
        return false;
    }

    private List<LogDownload>  parseDirString(String dirStr) throws GpssApplicationException {
        try {
            return Arrays.stream(dirStr.split(System.lineSeparator())).filter(obj -> obj.trim().length() > 10 && Pattern.compile(
                    "^\\d{2}/\\d{2}/\\d{4}$").matcher(obj.trim().substring(0, 10)).matches()).map(obj -> {
                String[] parts = Arrays.stream(obj.split(" "))
                        .filter(value ->
                                value != null && value.trim().length() > 0
                        )
                        .toArray(size -> new String[size]);
                return new LogDownload("<dir>".equalsIgnoreCase(parts[3]) ? "DIR" : "FILE", parts[4], String.join(" ", parts[0], parts[1], parts[2]),
                        "<dir>".equalsIgnoreCase(parts[3]) ? "0" : parts[3].replaceAll(",", ""));
            }).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(PARSING_LOGS_DIRECTORY_LIST_MESSAGE_ERROR + " {}", e);
            throw new GpssApplicationException(PARSING_LOGS_DIRECTORY_LIST_MESSAGE_ERROR);
        }
    }

    public Map<String, BlockingQueue<Message>> getRequestIdToResponseQueueMap() {
        return requestIdToResponseQueueMap;
    }

    public void removeKey(String requestKey) {
        if (requestIdToResponseQueueMap.containsKey(requestKey)) {
            this.requestIdToResponseQueueMap.remove(requestKey);
        }
    }
}
