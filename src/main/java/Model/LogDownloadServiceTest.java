package com.liquidnet.pst.helm.wizard.logdownload;

import ats.frontend.login.AtsFrontendLogin;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.liquidnet.gpss.lib.app.GpssApplicationException;
import com.liquidnet.gpss.lib.base.GpssMessagingException;
import com.liquidnet.gpss.lib.base.MessageWrapper;
import com.liquidnet.gpss.lib.protobuf.PbufMessageWrapper;
import com.liquidnet.gpss.lib.protobuf.ProtoBufMessagePipe;
import com.liquidnet.gpss.lib.protobuf.SingleThreadedMessagingServer;
import com.liquidnet.pst.helm.IProtoService;
import com.liquidnet.pst.helm.ProtoService;
import liquidnet.messages.AtsMemberAdmin;
import lombok.SneakyThrows;
import org.awaitility.Duration;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import spark.Request;
import spark.Response;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPOutputStream;

import static com.liquidnet.pst.helm.wizard.logdownload.LogDownloadService.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LogDownloadServiceTest {
    private ConcurrentMap<String, BlockingQueue<Message>> requestIdToResponseQueueMapDir = new ConcurrentHashMap<>();
    private ConcurrentMap<String, BlockingQueue<Message>> requestIdToResponseQueueMapGet = new ConcurrentHashMap<>();
    @Mock
    private ProtoBufMessagePipe messagePipe;
    @Mock
    private IProtoService protoService;
    @Mock
    private SingleThreadedMessagingServer messagingServer;
    private String saveLogHutPath;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private static final int POLL_TIMEOUT = 1000;
    private static final int SHORT_POLL_TIMEOUT = 1;

    @Before
    public void test_init() throws IOException {
        saveLogHutPath = temporaryFolder.getRoot().getAbsolutePath();
    }

    @After
    public void teardown() {
        Mockito.reset(messagePipe, protoService, messagePipe);
        requestIdToResponseQueueMapDir.clear();
        requestIdToResponseQueueMapGet.clear();
    }

    @Test
    public void directoryListingCallBackNotReceived() throws GpssMessagingException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapDir, new ProtoService(messagePipe), SHORT_POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = createRequest("USER1");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "DIR");
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(0, butlerResponse.getButlerEntries().size());
        Assert.assertEquals(LogDownloadService.TIMEOUT_MESSAGE_ERROR, butlerResponse.getMessage());
    }

    @Test
    public void fileStoringCallBackNotReceived() throws GpssMessagingException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapDir, new ProtoService(messagePipe), SHORT_POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = createRequest("USER1");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "GET");
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(0, butlerResponse.getButlerEntries().size());
        Assert.assertEquals(LogDownloadService.TIMEOUT_MESSAGE_ERROR, butlerResponse.getMessage());
        Assert.assertFalse(requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void invalidActionTypeReceived() throws GpssMessagingException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapDir, new ProtoService(messagePipe), SHORT_POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = createRequest("USER1");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "XYZ");
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(0, butlerResponse.getButlerEntries().size());
        Assert.assertEquals("Invalid action type.", butlerResponse.getMessage());
        Assert.assertFalse(requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void traderCallBackReceived() throws GpssMessagingException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapDir, new ProtoService(messagePipe), POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = createRequest("USER1");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "DIR");
        OnMessageTraderResponse onMessageResponse = new OnMessageTraderResponse(new LogDownloadCallbacksService(messagingServer, classToTest), logDownloadRequest);
        onMessageResponse.start();
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(1, butlerResponse.getButlerEntries().size());
        Assert.assertEquals("", butlerResponse.getMessage());
        Assert.assertEquals("249", butlerResponse.getButlerEntries().get(0).getSize());
        Assert.assertEquals("09032022 19:04:03", butlerResponse.getButlerEntries().get(0).getDateTimeModified());
        Assert.assertEquals("\\AppData\\Local\\Liquidnet", butlerResponse.getButlerEntries().get(0).getFilePath());
        Assert.assertEquals("File", butlerResponse.getButlerEntries().get(0).getType());
        Assert.assertFalse(requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));

    }

    @Test
    public void omsiCallBackReceived() throws GpssMessagingException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapDir, new ProtoService(messagePipe), POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = createRequest("OMSI");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "DIR");
        OnMessageOMSIResponse onMessageResponse = new OnMessageOMSIResponse(new LogDownloadCallbacksService(messagingServer, classToTest), logDownloadRequest);
        onMessageResponse.start();
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(1, butlerResponse.getButlerEntries().size());
        Assert.assertEquals("", butlerResponse.getMessage());
        Assert.assertEquals("249", butlerResponse.getButlerEntries().get(0).getSize());
        Assert.assertEquals("09032022 19:04:03", butlerResponse.getButlerEntries().get(0).getDateTimeModified());
        Assert.assertEquals("\\AppData\\Local\\Liquidnet", butlerResponse.getButlerEntries().get(0).getFilePath());
        Assert.assertEquals("File", butlerResponse.getButlerEntries().get(0).getType());
        Assert.assertFalse(requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));

    }

    @Test
    public void omsiDirectoryBinaryCallBackReceived() throws GpssMessagingException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapDir, new ProtoService(messagePipe), POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = createRequest("OMSI");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "DIR");
        OnMessageOMSIBinaryDirResponse onMessageResponse = new OnMessageOMSIBinaryDirResponse(new LogDownloadCallbacksService(messagingServer, classToTest), ByteString.copyFrom(DummyByteArray(" Volume in drive C is OS\r\n" +
                " Volume Serial Number is C464-4E84\r\n" +
                "\r\n" +
                " Directory of C:\\liquidnet\\deployed\\logs\r\n" +
                "\r\n" +
                "10/28/2022  06:15 AM    <DIR>          .\r\n" +
                "10/24/2022  02:14 PM                97 \\xyz\\LogFile.log\r\n" +
                "              1 File(s)    173,699,303 bytes\r\n" +
                "               1 Dir(s)  256,627,949,568 bytes free\r\n")), logDownloadRequest);
        onMessageResponse.start();
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(2, butlerResponse.getButlerEntries().size());
        Assert.assertEquals("", butlerResponse.getMessage());
        Assert.assertEquals("\\xyz\\LogFile.log", butlerResponse.getButlerEntries().get(1).getFilePath().trim());
        Assert.assertFalse(requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void omsiDirectoryBinaryParsingDirFailed() throws GpssMessagingException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapDir, new ProtoService(messagePipe), POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = createRequest("OMSI");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "DIR");
        OnMessageOMSIBinaryDirResponse onMessageResponse = new OnMessageOMSIBinaryDirResponse(new LogDownloadCallbacksService(messagingServer, classToTest), ByteString.copyFrom(DummyByteArray(" Volume in drive C is OS\r\n" +
                " Volume Serial Number is C464-4E84\r\n" +
                "\r\n" +
                " Directory of C:\\liquidnet\\deployed\\logs\r\n" +
                "10/24/202202:14PM")), logDownloadRequest);
        onMessageResponse.start();
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(0, butlerResponse.getButlerEntries().size());
        Assert.assertEquals(PARSING_LOGS_DIRECTORY_LIST_MESSAGE_ERROR, butlerResponse.getMessage());
        Assert.assertFalse(requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void exceptionThrownWhenButlerRequestSend() {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapDir, protoService, SHORT_POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        classToTest = spy(classToTest);
        Request req = createRequest("USER1");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "DIR");
        doThrow(RuntimeException.class).when(protoService).sendMessage(Mockito.any(Message.class), Mockito.anyString());
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(0, butlerResponse.getButlerEntries().size());
        Assert.assertEquals(GENERIC_TRADER_BUTLER_MESSAGE_ERROR, butlerResponse.getMessage());
        Assert.assertFalse(requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void exceptionThrownWhenDirectoryListingResponse() throws GpssApplicationException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapDir, protoService, SHORT_POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        classToTest = spy(classToTest);
        Request req = createRequest("USER1");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "DIR");
        Mockito.doThrow(NullPointerException.class).when(classToTest).butlerEntriesRequest(any(), anyInt());
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(0, butlerResponse.getButlerEntries().size());
        Assert.assertEquals(WHILE_SHOWING_DIRECTORY_LIST_MESSAGE_ERROR, butlerResponse.getMessage());
        Assert.assertFalse(requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void exceptionThrownWhenFileStoringResponse() throws GpssApplicationException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapGet, protoService, SHORT_POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        classToTest = spy(classToTest);
        Request req = createRequest("USER1");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "GET");
        Mockito.doThrow(NullPointerException.class).when(classToTest).butlerEntriesRequest(any(), anyInt());
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(0, butlerResponse.getButlerEntries().size());
        Assert.assertEquals(UNABLE_TO_STORE_LOG_FILE_IN_DIRECTORY_MESSAGE_ERROR, butlerResponse.getMessage());
        Assert.assertFalse(requestIdToResponseQueueMapGet.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test()
    public void interruptedExceptionWhenDirectoryListingResponse() {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapDir, protoService, POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = createRequest("USER1");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "DIR");
        ButlerTask butlerTask = new ButlerTask(classToTest, logDownloadRequest);
        butlerTask.start();
        butlerTask.interrupt();
        await()
                .atMost(Duration.FIVE_HUNDRED_MILLISECONDS)
                .until(() -> requestIdToResponseQueueMapDir.get("USERUSER1DIR") == null);
        Assert.assertEquals(0, butlerTask.getButlerResponse().getButlerEntries().size());
        Assert.assertEquals(INTERRUPTED_LISTING_MESSAGE_ERROR, butlerTask.getButlerResponse().getMessage());
        Assert.assertFalse(requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void saveLogHutTraderSuccess() throws GpssMessagingException {
        Instant instantVal = Instant.parse("2022-02-15T18:35:24.00Z");
        Clock clock = Clock.fixed(instantVal, ZoneId.systemDefault());
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapGet, new ProtoService(messagePipe), POLL_TIMEOUT, saveLogHutPath, clock);
        Request req = createRequest("USER1");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "GET");
        OnMessageBinaryTraderResponse onMessageBinaryResponse = new OnMessageBinaryTraderResponse(new LogDownloadCallbacksService(messagingServer, classToTest), ByteString.copyFrom(DummyByteArray("This is Test data")), logDownloadRequest);
        onMessageBinaryResponse.start();
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals("", butlerResponse.getMessage());
        Path expected = Paths.get(saveLogHutPath, "USER", "USER1", DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss").withZone(clock.getZone()).format(Instant.now(clock)), "FILENAME");
        Assert.assertEquals(expected.toString(), butlerResponse.getLogHutUrl());
        Assert.assertFalse(requestIdToResponseQueueMapGet.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void saveLogHutOMSISuccess() throws GpssMessagingException {
        Instant instantVal = Instant.parse("2022-02-15T18:35:24.00Z");
        Clock clock = Clock.fixed(instantVal, ZoneId.systemDefault());
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapGet, new ProtoService(messagePipe), POLL_TIMEOUT, saveLogHutPath, clock);
        Request req = createRequest("OMSI");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "GET");
        OnMessageBinaryOMSIResponse onMessageBinaryResponse = new OnMessageBinaryOMSIResponse(new LogDownloadCallbacksService(messagingServer, classToTest), ByteString.copyFrom(DummyByteArray("This is test data")), logDownloadRequest);
        onMessageBinaryResponse.start();
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals("", butlerResponse.getMessage());
        Path expected = Paths.get(saveLogHutPath, "USER", "OMSI", DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss").withZone(clock.getZone()).format(Instant.now(clock)), "FILENAME");
        Assert.assertEquals(expected.toString(), butlerResponse.getLogHutUrl());
        Assert.assertFalse(requestIdToResponseQueueMapGet.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void saveLogHutOMSIFailedZeroFileByte() throws GpssMessagingException {
        Instant instantVal = Instant.parse("2022-02-15T18:35:24.00Z");
        Clock clock = Clock.fixed(instantVal, ZoneId.systemDefault());
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapGet, new ProtoService(messagePipe), POLL_TIMEOUT, saveLogHutPath, clock);
        Request req = createRequest("OMSI");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "GET");
        OnMessageBinaryOMSIResponse onMessageBinaryResponse = new OnMessageBinaryOMSIResponse(new LogDownloadCallbacksService(messagingServer, classToTest), ByteString.copyFrom(new byte[0]), logDownloadRequest);
        onMessageBinaryResponse.start();
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(RECEIVED_IS_0_OR_NULL_MESSAGE_ERROR, butlerResponse.getMessage());
        Assert.assertFalse(requestIdToResponseQueueMapGet.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void failedLogHutFailedUncompress() throws GpssMessagingException, IOException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapGet, new ProtoService(messagePipe), POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = createRequest("USER1");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "GET");
        OnMessageBinaryTraderResponse onMessageBinaryResponse = new OnMessageBinaryTraderResponse(new LogDownloadCallbacksService(messagingServer, classToTest), ByteString.copyFrom("DUMMY", "UTF8"), logDownloadRequest);
        onMessageBinaryResponse.start();
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(WHILE_UNCOMPRESSING_GZIP_MESSAGE_ERROR, butlerResponse.getMessage());
        Assert.assertEquals("", butlerResponse.getLogHutUrl());
        Assert.assertFalse(requestIdToResponseQueueMapGet.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void saveLogHutFilePathAccessIssue() throws GpssMessagingException, GpssApplicationException {
        Instant instantVal = Instant.parse("2022-02-15T18:35:24.00Z");
        Clock clock = Clock.fixed(instantVal, ZoneId.systemDefault());
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapGet, new ProtoService(messagePipe), POLL_TIMEOUT, "//XYZ", clock);
        classToTest = spy(classToTest);
        Request req = createRequest("OMSI");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "GET");
        OnMessageBinaryOMSIResponse onMessageBinaryResponse = new OnMessageBinaryOMSIResponse(new LogDownloadCallbacksService(messagingServer, classToTest), ByteString.copyFrom(DummyByteArray("This is test data")), logDownloadRequest);
        onMessageBinaryResponse.start();
        doThrow(new GpssApplicationException(ACCESSING_OR_CREATING_DIRECTORY_FOR_LOG_HUT_MESSAGE_ERROR)).when(classToTest).getValidPath(Mockito.any());
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(ACCESSING_OR_CREATING_DIRECTORY_FOR_LOG_HUT_MESSAGE_ERROR, butlerResponse.getMessage());
        Assert.assertFalse(requestIdToResponseQueueMapGet.containsKey(logDownloadRequest.getRequestKey()));

    }

    @Test
    public void downloadLogHutSuccess() throws GpssMessagingException, IOException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapGet, new ProtoService(messagePipe), SHORT_POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        Path logHutUrlPath = Paths.get(saveLogHutPath, "USER", "USER1", "2022-10-06 1000", "TEST.txt");
        if (!logHutUrlPath.getParent().toFile().exists()) Files.createDirectories(logHutUrlPath.getParent());
        String dummyString = "This is test data";
        Files.write(logHutUrlPath, dummyString.getBytes());
        Mockito.when(req.params("logHutUrl")).thenReturn(logHutUrlPath.toString());
        Mockito.when(res.raw()).thenReturn(httpServletResponse);
        DummyOutputStream dummyOutputStream = new DummyOutputStream();
        Mockito.when(httpServletResponse.getOutputStream()).thenReturn(dummyOutputStream);
        classToTest.downloadLogHut(req, res);
        Mockito.verify(res, Mockito.timeout(2000).times(1)).header(Mockito.anyString(),Mockito.anyString());
        Assert.assertEquals("This is test data", dummyOutputStream.getContent());
    }

    @Test
    public void downloadLogHutFail() throws GpssMessagingException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapGet, new ProtoService(messagePipe), SHORT_POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Path logHutUrlPath = Paths.get("D:\\", "USER", "USER1", "2022-10-06 1000");
        Mockito.when(req.params("logHutUrl")).thenReturn(logHutUrlPath.toString());
        ArgumentCaptor<String> headerResponse = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> headerValue = ArgumentCaptor.forClass(String.class);
        classToTest.downloadLogHut(req, res);
        Mockito.verify(res, Mockito.timeout(2000).times(2)).header(headerResponse.capture(), headerValue.capture());
        Assert.assertEquals("File-Download-Error", headerResponse.getAllValues().get(1));
        Assert.assertEquals(VALIDATE_LOG_HUT_RESPONSE, headerValue.getAllValues().get(1));
    }

    @Test
    public void downloadLogHutException() throws GpssMessagingException, IOException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapGet, new ProtoService(messagePipe), SHORT_POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Path logHutUrlPath = Paths.get(saveLogHutPath, "USER", "USER1", "2022-10-06 1000", "TEST.txt");
        if (!logHutUrlPath.getParent().toFile().exists()) Files.createDirectories(logHutUrlPath.getParent());
        Mockito.when(req.params("logHutUrl")).thenReturn(logHutUrlPath.toString());
        ArgumentCaptor<String> headerResponse = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> headerValue = ArgumentCaptor.forClass(String.class);
        classToTest.downloadLogHut(req, res);
        Mockito.verify(res, Mockito.timeout(2000).times(2)).header(headerResponse.capture(), headerValue.capture());
        Assert.assertEquals("File-Download-Error", headerResponse.getAllValues().get(1));
        Assert.assertEquals(LOG_HUT_DOWNLOAD_EXCEPTION_RESPONSE, headerValue.getAllValues().get(1));
    }

    @Test
    public void downloadLogHutFailWithBlankUrl() throws GpssMessagingException {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapGet, new ProtoService(messagePipe), SHORT_POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Mockito.when(req.params("logHutUrl")).thenReturn("");
        ArgumentCaptor<String> headerResponse = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> headerValue = ArgumentCaptor.forClass(String.class);
        classToTest.downloadLogHut(req, res);
        Mockito.verify(res, Mockito.timeout(2000).times(2)).header(headerResponse.capture(), headerValue.capture());
        Assert.assertEquals("File-Download-Error", headerResponse.getAllValues().get(1));
        Assert.assertEquals(VALIDATE_LOG_HUT_RESPONSE, headerValue.getAllValues().get(1));
    }

    @Test()
    public void interruptedExceptionWhenFileStoringResponse() {
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapDir, protoService, POLL_TIMEOUT, saveLogHutPath, Clock.systemDefaultZone());
        Request req = createRequest("USER1");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "GET");
        ButlerTask butlerTask = new ButlerTask(classToTest, logDownloadRequest);
        butlerTask.start();
        butlerTask.interrupt();
        await()
                .atMost(Duration.FIVE_HUNDRED_MILLISECONDS)
                .until(() -> requestIdToResponseQueueMapDir.get("USERUSER1GET") == null);
        Assert.assertEquals(0, butlerTask.getButlerResponse().getButlerEntries().size());
        Assert.assertEquals(INTERRUPTED_SAVE_LOG_MESSAGE_ERROR, butlerTask.getButlerResponse().getMessage());
        Assert.assertFalse(requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void whenKeyAlreadyExist() throws GpssMessagingException, IOException {
        requestIdToResponseQueueMapGet.put("USERUSER1GET", new LinkedBlockingQueue<>());
        Instant instantVal = Instant.parse("2022-02-15T18:35:24.00Z");
        Clock clock = Clock.fixed(instantVal, ZoneId.systemDefault());
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapGet, new ProtoService(messagePipe), SHORT_POLL_TIMEOUT, saveLogHutPath, clock);
        Request req = createRequest("USER1");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "GET");
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals(LogDownloadService.YOUR_REQUEST_CANNOT_BE_FULFILLED_MESSAGE_ERROR, butlerResponse.getMessage());
        Assert.assertTrue(requestIdToResponseQueueMapGet.containsKey(logDownloadRequest.getRequestKey()));
    }

    @Test
    public void fileResponseFailAtAtsEnd() throws GpssMessagingException {
        Instant instantVal = Instant.parse("2022-02-15T18:35:24.00Z");
        Clock clock = Clock.fixed(instantVal, ZoneId.systemDefault());
        LogDownloadService classToTest = new LogDownloadService(requestIdToResponseQueueMapGet, new ProtoService(messagePipe), POLL_TIMEOUT, saveLogHutPath, clock);
        Request req = createRequest("OMSI");
        LogDownloadRequest logDownloadRequest = new LogDownloadRequest(req, "GET");
        OnMessageBinaryOMSIFailedResponse onMessageBinaryResponse = new OnMessageBinaryOMSIFailedResponse(new LogDownloadCallbacksService(messagingServer, classToTest), ByteString.copyFrom(DummyByteArray("This is test data")), logDownloadRequest);
        onMessageBinaryResponse.start();
        LogDownloadWrapper butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        Assert.assertEquals("Error at ATS end.", butlerResponse.getMessage());
        Assert.assertFalse(requestIdToResponseQueueMapGet.containsKey(logDownloadRequest.getRequestKey()));
    }

    public class DummyOutputStream extends ServletOutputStream {
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            baos.write(b);
        }

        public String getContent() {
            return baos.toString();
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }
    }

    private class ButlerTask extends Thread {
        private LogDownloadWrapper butlerResponse;
        private LogDownloadService classToTest;
        private LogDownloadRequest logDownloadRequest;

        public LogDownloadWrapper getButlerResponse() {
            return butlerResponse;
        }

        public ButlerTask(LogDownloadService classToTest, LogDownloadRequest logDownloadRequest) {
            this.classToTest = classToTest;
            this.logDownloadRequest = logDownloadRequest;
        }

        @Override
        public void run() {
            butlerResponse = classToTest.butlerProcess(logDownloadRequest);
        }
    }

    private class OnMessageTraderResponse extends Thread {
        private LogDownloadCallbacksService logDownloadCallbacksService;
        private LogDownloadRequest logDownloadRequest;

        public OnMessageTraderResponse(LogDownloadCallbacksService logDownloadCallbacksService, LogDownloadRequest logDownloadRequest) {
            this.logDownloadCallbacksService = logDownloadCallbacksService;
            this.logDownloadRequest = logDownloadRequest;
        }

        @SneakyThrows
        @Override
        public void run() {
            AtsFrontendLogin.TraderButlerFileSystemEntry entry = AtsFrontendLogin.TraderButlerFileSystemEntry.newBuilder().setFileName("\\AppData\\Local\\Liquidnet").setDateTimeModified("09032022 19:04:03").setSize(249).setType("File").build();
            AtsFrontendLogin.TraderButlerRequest request = AtsFrontendLogin.TraderButlerRequest.newBuilder()
                    .setActionType(logDownloadRequest.getActionType())
                    .setFileName(logDownloadRequest.getFileName())
                    .setMemberId(logDownloadRequest.getMemberId())
                    .setUserId(logDownloadRequest.getUserId())
                    .build();
            AtsFrontendLogin.TraderButlerResponse traderButlerResponse = AtsFrontendLogin.TraderButlerResponse.newBuilder().addButlerFileEntries(entry).setRequest(request).build();
            MessageWrapper.MessagingInfo<Message> messagingInfo = new PbufMessageWrapper.PbufMessagingInfo<>("XYZ", "XYZ", null, null);
            PbufMessageWrapper<Message> pbufMessageWrapper = new PbufMessageWrapper<>(messagingInfo, traderButlerResponse);
            await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));
            logDownloadCallbacksService.onMessage(pbufMessageWrapper);
        }
    }


    private class OnMessageBinaryTraderResponse extends Thread {
        private LogDownloadCallbacksService logDownloadCallbacksService;
        private ByteString byteString;
        private LogDownloadRequest logDownloadRequest;

        public OnMessageBinaryTraderResponse(LogDownloadCallbacksService logDownloadCallbacksService, ByteString byteString, LogDownloadRequest logDownloadRequest) {
            this.logDownloadCallbacksService = logDownloadCallbacksService;
            this.byteString = byteString;
            this.logDownloadRequest = logDownloadRequest;
        }

        @SneakyThrows
        @Override
        public void run() {
            AtsFrontendLogin.TraderButlerFileSystemEntry entry = AtsFrontendLogin.TraderButlerFileSystemEntry.newBuilder().build();
            AtsFrontendLogin.TraderButlerRequest request = AtsFrontendLogin.TraderButlerRequest.newBuilder()
                    .setActionType(logDownloadRequest.getActionType())
                    .setFileName(logDownloadRequest.getFileName())
                    .setMemberId(logDownloadRequest.getMemberId())
                    .setUserId(logDownloadRequest.getUserId())
                    .build();

            AtsFrontendLogin.TraderButlerResponse traderButlerResponse = AtsFrontendLogin.TraderButlerResponse.newBuilder().addButlerFileEntries(entry).setFile(byteString).setRequest(request).setIsLastPart(true).build();
            MessageWrapper.MessagingInfo<Message> messagingInfo = new PbufMessageWrapper.PbufMessagingInfo<>("XYZ", "XYZ", null, null);
            PbufMessageWrapper<Message> pbufMessageWrapper = new PbufMessageWrapper<>(messagingInfo, traderButlerResponse);
            await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> requestIdToResponseQueueMapGet.containsKey(logDownloadRequest.getRequestKey()));
            logDownloadCallbacksService.onMessage(pbufMessageWrapper);
        }
    }

    private class OnMessageOMSIResponse extends Thread {
        private LogDownloadCallbacksService logDownloadCallbacksService;
        private LogDownloadRequest logDownloadRequest;

        public OnMessageOMSIResponse(LogDownloadCallbacksService logDownloadCallbacksService, LogDownloadRequest logDownloadRequest) {
            this.logDownloadCallbacksService = logDownloadCallbacksService;
            this.logDownloadRequest = logDownloadRequest;
        }

        @SneakyThrows
        @Override
        public void run() {
            AtsMemberAdmin.OMSIButlerFileSystemEntry entry = AtsMemberAdmin.OMSIButlerFileSystemEntry.newBuilder().setFileName("\\AppData\\Local\\Liquidnet").setDateTimeModified("09032022 19:04:03").setSize(249).setType("File").build();
            AtsMemberAdmin.OMSIButlerRequest request = AtsMemberAdmin.OMSIButlerRequest.newBuilder()
                    .setActionType(logDownloadRequest.getActionType())
                    .setFileName(logDownloadRequest.getFileName())
                    .setMemberId(logDownloadRequest.getMemberId())
                    .setResendFromStart(1)
                    .build();
            AtsMemberAdmin.OMSIButlerResponse OMSIButlerResponse = AtsMemberAdmin.OMSIButlerResponse.newBuilder().addButlerFileEntries(entry).setRequest(request).build();
            MessageWrapper.MessagingInfo<Message> messagingInfo = new PbufMessageWrapper.PbufMessagingInfo<>("XYZ", "XYZ", null, null);
            PbufMessageWrapper<Message> pbufMessageWrapper = new PbufMessageWrapper<>(messagingInfo, OMSIButlerResponse);
            await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));
            logDownloadCallbacksService.onMessage(pbufMessageWrapper);
        }
    }

    private class OnMessageOMSIBinaryDirResponse extends Thread {
        private LogDownloadCallbacksService logDownloadCallbacksService;
        private ByteString byteString;
        private LogDownloadRequest logDownloadRequest;

        public OnMessageOMSIBinaryDirResponse(LogDownloadCallbacksService logDownloadCallbacksService, ByteString byteString, LogDownloadRequest logDownloadRequest) {
            this.logDownloadCallbacksService = logDownloadCallbacksService;
            this.byteString = byteString;
            this.logDownloadRequest = logDownloadRequest;

        }

        @SneakyThrows
        @Override
        public void run() {
            AtsMemberAdmin.OMSIButlerRequest request = AtsMemberAdmin.OMSIButlerRequest.newBuilder()
                    .setActionType(logDownloadRequest.getActionType())
                    .setFileName(logDownloadRequest.getFileName())
                    .setMemberId(logDownloadRequest.getMemberId())
                    .setResendFromStart(1)
                    .build();
            AtsMemberAdmin.OMSIButlerResponse OMSIButlerResponse = AtsMemberAdmin.OMSIButlerResponse.newBuilder().setFile(byteString).setRequest(request).build();
            MessageWrapper.MessagingInfo<Message> messagingInfo = new PbufMessageWrapper.PbufMessagingInfo<>("XYZ", "XYZ", null, null);
            PbufMessageWrapper<Message> pbufMessageWrapper = new PbufMessageWrapper<>(messagingInfo, OMSIButlerResponse);
            await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> requestIdToResponseQueueMapDir.containsKey(logDownloadRequest.getRequestKey()));
            logDownloadCallbacksService.onMessage(pbufMessageWrapper);
        }
    }

    private class OnMessageBinaryOMSIResponse extends Thread {
        private LogDownloadCallbacksService logDownloadCallbacksService;
        private ByteString byteString;

        private LogDownloadRequest logDownloadRequest;

        public OnMessageBinaryOMSIResponse(LogDownloadCallbacksService logDownloadCallbacksService, ByteString byteString, LogDownloadRequest logDownloadRequest) {
            this.logDownloadCallbacksService = logDownloadCallbacksService;
            this.byteString = byteString;
            this.logDownloadRequest = logDownloadRequest;
        }

        @SneakyThrows
        @Override
        public void run() {
            AtsMemberAdmin.OMSIButlerFileSystemEntry entry = AtsMemberAdmin.OMSIButlerFileSystemEntry.newBuilder().setFileName("\\AppData\\Local\\Liquidnet").setDateTimeModified("09032022 19:04:03").setSize(249).setType("File").build();
            AtsMemberAdmin.OMSIButlerRequest request = AtsMemberAdmin.OMSIButlerRequest.newBuilder()
                    .setActionType(logDownloadRequest.getActionType())
                    .setFileName(logDownloadRequest.getFileName())
                    .setMemberId(logDownloadRequest.getMemberId())
                    .setResendFromStart(1)
                    .build();

            AtsMemberAdmin.OMSIButlerResponse OMSIButlerResponse = AtsMemberAdmin.OMSIButlerResponse.newBuilder().addButlerFileEntries(entry).setFile(byteString).setIsLastPart(false).setRequest(request).build();
            MessageWrapper.MessagingInfo<Message> messagingInfo = new PbufMessageWrapper.PbufMessagingInfo<>("XYZ", "XYZ", null, null);
            PbufMessageWrapper<Message> pbufMessageWrapper = new PbufMessageWrapper<>(messagingInfo, OMSIButlerResponse);
            await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> requestIdToResponseQueueMapGet.containsKey(logDownloadRequest.getRequestKey()));
            logDownloadCallbacksService.onMessage(pbufMessageWrapper);
            AtsMemberAdmin.OMSIButlerResponse OMSIButlerResponse2 = AtsMemberAdmin.OMSIButlerResponse.newBuilder().addButlerFileEntries(entry).setFile(byteString).setIsLastPart(true).setRequest(request).build();
            PbufMessageWrapper<Message> pbufMessageWrapper2 = new PbufMessageWrapper<>(messagingInfo, OMSIButlerResponse2);
            logDownloadCallbacksService.onMessage(pbufMessageWrapper2);
        }
    }

    private class OnMessageBinaryOMSIFailedResponse extends Thread {
        private LogDownloadCallbacksService logDownloadCallbacksService;
        private ByteString byteString;

        private LogDownloadRequest logDownloadRequest;

        public OnMessageBinaryOMSIFailedResponse(LogDownloadCallbacksService logDownloadCallbacksService, ByteString byteString, LogDownloadRequest logDownloadRequest) {
            this.logDownloadCallbacksService = logDownloadCallbacksService;
            this.byteString = byteString;
            this.logDownloadRequest = logDownloadRequest;
        }

        @SneakyThrows
        @Override
        public void run() {
            AtsMemberAdmin.OMSIButlerFileSystemEntry entry = AtsMemberAdmin.OMSIButlerFileSystemEntry.newBuilder().setFileName("\\AppData\\Local\\Liquidnet").setDateTimeModified("09032022 19:04:03").setSize(249).setType("File").build();
            AtsMemberAdmin.OMSIButlerRequest request = AtsMemberAdmin.OMSIButlerRequest.newBuilder()
                    .setActionType(logDownloadRequest.getActionType())
                    .setFileName(logDownloadRequest.getFileName())
                    .setMemberId(logDownloadRequest.getMemberId())
                    .setResendFromStart(1)
                    .build();
            AtsMemberAdmin.OMSIButlerResponse OMSIButlerResponse = AtsMemberAdmin.OMSIButlerResponse.newBuilder().addButlerFileEntries(entry).setFile(byteString).setIsLastPart(false).setResult("Error at ATS end.").setRequest(request).build();
            MessageWrapper.MessagingInfo<Message> messagingInfo = new PbufMessageWrapper.PbufMessagingInfo<>("XYZ", "XYZ", null, null);
            PbufMessageWrapper<Message> pbufMessageWrapper = new PbufMessageWrapper<>(messagingInfo, OMSIButlerResponse);
            await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> requestIdToResponseQueueMapGet.containsKey(logDownloadRequest.getRequestKey()));
            logDownloadCallbacksService.onMessage(pbufMessageWrapper);
            AtsMemberAdmin.OMSIButlerResponse OMSIButlerResponse2 = AtsMemberAdmin.OMSIButlerResponse.newBuilder().addButlerFileEntries(entry).setFile(byteString).setIsLastPart(true).setRequest(request).build();
            PbufMessageWrapper<Message> pbufMessageWrapper2 = new PbufMessageWrapper<>(messagingInfo, OMSIButlerResponse2);
            logDownloadCallbacksService.onMessage(pbufMessageWrapper2);
        }
    }

    @SneakyThrows
    public byte[] DummyByteArray(String data) {
        byte[] dataToCompress = data.getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(dataToCompress.length);
        GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
        zipStream.write(dataToCompress);
        zipStream.close();
        byteStream.close();
        return byteStream.toByteArray();
    }

    private static Request createRequest(String userId) {
        Request req = mock(Request.class);
        Mockito.when(req.params("memberid")).thenReturn("USER");
        Mockito.when(req.params("userid")).thenReturn(userId);
        Mockito.when(req.params("filename")).thenReturn("FILENAME");
        return req;
    }
}
