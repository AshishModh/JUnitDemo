package com.liquidnet.pst.helm.wizard.logdownload;

import ats.frontend.login.AtsFrontendLogin;
import com.google.protobuf.Message;
import com.liquidnet.gpss.lib.base.MessageWrapper;
import com.liquidnet.gpss.lib.protobuf.PbufMessageWrapper;
import com.liquidnet.gpss.lib.protobuf.SingleThreadedMessagingServer;
import com.liquidnet.pst.helm.IProtoService;
import liquidnet.messages.AtsMemberAdmin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Clock;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;


@RunWith(MockitoJUnitRunner.class)
public class LogDownloadCallbacksServiceTest {
    private String requestKey = "USERUSER1DIR";

    private String OMSIRequestKey = "USERDIR";
    @Mock
    private SingleThreadedMessagingServer messagingServer;
    @Mock
    private IProtoService protoService;
    @Mock
    private Clock clock;
    private LogDownloadService logDownloadService;
    private ConcurrentMap<String, BlockingQueue<Message>> requestIdToResponseQueueMap = new ConcurrentHashMap<>();

    @Before
    public void test_init() {
        this.requestIdToResponseQueueMap.put(requestKey, new LinkedBlockingQueue<>());
        this.requestIdToResponseQueueMap.put(OMSIRequestKey, new LinkedBlockingQueue<>());
        logDownloadService = new LogDownloadService(requestIdToResponseQueueMap, protoService, 4000, "", clock);
    }

    @Test
    public void traderCallBackReceived() {
        LogDownloadCallbacksService classToTest = new LogDownloadCallbacksService(messagingServer, logDownloadService);
        AtsFrontendLogin.TraderButlerFileSystemEntry entries = AtsFrontendLogin.TraderButlerFileSystemEntry.newBuilder().setFileName("\\AppData\\Local\\Liquidnet").setDateTimeModified("09032022 19:04:03").setSize(249).setType("File").build();
        AtsFrontendLogin.TraderButlerRequest request = AtsFrontendLogin.TraderButlerRequest.newBuilder()
                .setActionType("DIR")
                .setFileName("FILENAME")
                .setMemberId("USER")
                .setUserId("USER1")
                .build();
        AtsFrontendLogin.TraderButlerResponse traderButlerResponse = AtsFrontendLogin.TraderButlerResponse.newBuilder().addButlerFileEntries(entries).setRequest(request).build();

        MessageWrapper.MessagingInfo<Message> messagingInfo = new PbufMessageWrapper.PbufMessagingInfo<>("XYZ", "XYZ", null, null);
        PbufMessageWrapper<Message> pbufMessageWrapper = new PbufMessageWrapper<>(messagingInfo, traderButlerResponse);
        classToTest.onMessage(pbufMessageWrapper);
        BlockingQueue<AtsFrontendLogin.TraderButlerResponse> messageQueue = (BlockingQueue) this.logDownloadService.getRequestIdToResponseQueueMap().get(requestKey);
        Assert.assertEquals(1, messageQueue.size());
    }

    @Test
    public void OMSICallBackReceived() {
        LogDownloadCallbacksService classToTest = new LogDownloadCallbacksService(messagingServer, logDownloadService);
        AtsMemberAdmin.OMSIButlerFileSystemEntry entries = AtsMemberAdmin.OMSIButlerFileSystemEntry.newBuilder().setFileName("\\AppData\\Local\\Liquidnet").setDateTimeModified("09032022 19:04:03").setSize(249).setType("File").build();
        AtsMemberAdmin.OMSIButlerRequest request = AtsMemberAdmin.OMSIButlerRequest.newBuilder()
                .setActionType("DIR")
                .setFileName("FILENAME")
                .setMemberId("USER")
                .setResendFromStart(1)
                .build();
        AtsMemberAdmin.OMSIButlerResponse OMSIButlerResponse = AtsMemberAdmin.OMSIButlerResponse.newBuilder().addButlerFileEntries(entries).setRequest(request).build();

        MessageWrapper.MessagingInfo<Message> messagingInfo = new PbufMessageWrapper.PbufMessagingInfo<>("XYZ", "XYZ", null, null);
        PbufMessageWrapper<Message> pbufMessageWrapper = new PbufMessageWrapper<>(messagingInfo, OMSIButlerResponse);
        classToTest.onMessage(pbufMessageWrapper);
        BlockingQueue<MessageWrapper<AtsMemberAdmin.OMSIButlerResponse>> messageQueue = (BlockingQueue) this.logDownloadService.getRequestIdToResponseQueueMap().get(OMSIRequestKey);
        Assert.assertEquals(1, messageQueue.size());
    }

    @Test
    public void requestKeyWithSpacesReceived() {
        LogDownloadCallbacksService classToTest = new LogDownloadCallbacksService(messagingServer, logDownloadService);
        AtsFrontendLogin.TraderButlerFileSystemEntry entries = AtsFrontendLogin.TraderButlerFileSystemEntry.newBuilder().setFileName("\\AppData\\Local\\Liquidnet").setDateTimeModified("09032022 19:04:03").setSize(249).setType("File").build();
        AtsFrontendLogin.TraderButlerRequest request = AtsFrontendLogin.TraderButlerRequest.newBuilder()
                .setActionType("DIR")
                .setFileName("FILENAME")
                .setMemberId("   USER   ")
                .setUserId("USER 1")
                .build();
        AtsFrontendLogin.TraderButlerResponse traderButlerResponse = AtsFrontendLogin.TraderButlerResponse.newBuilder().addButlerFileEntries(entries).setRequest(request).build();

        MessageWrapper.MessagingInfo<Message> messagingInfo = new PbufMessageWrapper.PbufMessagingInfo<>("XYZ", "XYZ", null, null);
        PbufMessageWrapper<Message> pbufMessageWrapper = new PbufMessageWrapper<>(messagingInfo, traderButlerResponse);
        classToTest.onMessage(pbufMessageWrapper);
        BlockingQueue<AtsFrontendLogin.TraderButlerResponse> messageQueue = (BlockingQueue) this.logDownloadService.getRequestIdToResponseQueueMap().get(requestKey);
        Assert.assertEquals(1, messageQueue.size());
    }

    @Test
    public void traderCallBackWithWrongResponseOrKeyMismatch() {
        LogDownloadCallbacksService classToTest = new LogDownloadCallbacksService(messagingServer, logDownloadService);
        AtsFrontendLogin.TraderButlerResponse traderButlerResponse = AtsFrontendLogin.TraderButlerResponse.getDefaultInstance();
        MessageWrapper.MessagingInfo<Message> messagingInfo = new PbufMessageWrapper.PbufMessagingInfo<>("XYZ", "XYZ", null, null);
        PbufMessageWrapper<Message> pbufMessageWrapper = new PbufMessageWrapper<>(messagingInfo, traderButlerResponse);
        classToTest.onMessage(pbufMessageWrapper);
        BlockingQueue<MessageWrapper<AtsFrontendLogin.TraderButlerResponse>> messageQueue = (BlockingQueue) this.logDownloadService.getRequestIdToResponseQueueMap().get(requestKey);
        Assert.assertEquals(0, messageQueue.size());
    }

    @Test
    public void omsiCallBackWithWrongResponseOrKeyMismatch() {
        LogDownloadCallbacksService classToTest = new LogDownloadCallbacksService(messagingServer, logDownloadService);
        AtsMemberAdmin.OMSIButlerResponse OMSIButlerResponse = AtsMemberAdmin.OMSIButlerResponse.getDefaultInstance();
        MessageWrapper.MessagingInfo<Message> messagingInfo = new PbufMessageWrapper.PbufMessagingInfo<>("XYZ", "XYZ", null, null);
        PbufMessageWrapper<Message> pbufMessageWrapper = new PbufMessageWrapper<>(messagingInfo, OMSIButlerResponse);
        classToTest.onMessage(pbufMessageWrapper);
        BlockingQueue<MessageWrapper<AtsMemberAdmin.OMSIButlerResponse>> messageQueue = (BlockingQueue) this.logDownloadService.getRequestIdToResponseQueueMap().get(OMSIRequestKey);
        Assert.assertEquals(0, messageQueue.size());
    }

    @Test
    public void omsiCallBackAreInQueue() {
        LogDownloadCallbacksService classToTest = new LogDownloadCallbacksService(messagingServer, logDownloadService);
        AtsMemberAdmin.OMSIButlerFileSystemEntry entries = AtsMemberAdmin.OMSIButlerFileSystemEntry.newBuilder().setFileName("\\AppData\\Local\\Liquidnet").setDateTimeModified("09032022 19:04:03").setSize(249).setType("File").build();
        AtsMemberAdmin.OMSIButlerRequest request = AtsMemberAdmin.OMSIButlerRequest.newBuilder()
                .setActionType("DIR")
                .setFileName("FILENAME")
                .setMemberId("USER")
                .setResendFromStart(1)
                .build();
        AtsMemberAdmin.OMSIButlerResponse OMSIButlerResponse = AtsMemberAdmin.OMSIButlerResponse.newBuilder().addButlerFileEntries(entries).setRequest(request).build();

        MessageWrapper.MessagingInfo<Message> messagingInfo = new PbufMessageWrapper.PbufMessagingInfo<>("XYZ", "XYZ", null, null);
        PbufMessageWrapper<Message> pbufMessageWrapper = new PbufMessageWrapper<>(messagingInfo, OMSIButlerResponse);
        classToTest.onMessage(pbufMessageWrapper);
        BlockingQueue<MessageWrapper<AtsMemberAdmin.OMSIButlerResponse>> messageQueue = (BlockingQueue) this.logDownloadService.getRequestIdToResponseQueueMap().get(OMSIRequestKey);
        Assert.assertEquals(1, messageQueue.size());
        //Second Message in Queue
        classToTest.onMessage(pbufMessageWrapper);
        Assert.assertEquals(2, messageQueue.size());
    }
}
