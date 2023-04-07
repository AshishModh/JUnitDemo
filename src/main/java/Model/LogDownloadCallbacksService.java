package com.liquidnet.pst.helm.wizard.logdownload;

import ats.frontend.login.AtsFrontendLogin;
import com.google.protobuf.Message;
import com.liquidnet.gpss.lib.base.MessageWrapper;
import com.liquidnet.gpss.lib.base.MessagingServer;
import com.liquidnet.gpss.lib.protobuf.BasicCallback;
import liquidnet.messages.AtsMemberAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class LogDownloadCallbacksService extends BasicCallback<Message> {
    private static final Logger logger = LoggerFactory.getLogger(com.liquidnet.pst.helm.wizard.logdownload.LogDownloadCallbacksService.class);
    private final LogDownloadService logDownloadService;

    public LogDownloadCallbacksService(final MessagingServer<Message> messagingServer,
                                       final LogDownloadService logDownloadService) {
        super(messagingServer);
        this.logDownloadService = logDownloadService;
    }

    @Override
    public void onMessage(final MessageWrapper<Message> messageWrapper) {
        String requestKey = "";
        if (messageWrapper.getMessage() instanceof AtsFrontendLogin.TraderButlerResponse) {
            AtsFrontendLogin.TraderButlerRequest re = ((AtsFrontendLogin.TraderButlerResponse) messageWrapper.getMessage()).getRequest();
            requestKey = LogDownloadRequest.generateKeyForResponseMap(re.getMemberId(), re.getUserId(), re.getActionType());
            logger.info("Ln5 message received against request key: {}", requestKey);

        } else if (messageWrapper.getMessage() instanceof AtsMemberAdmin.OMSIButlerResponse) {
            AtsMemberAdmin.OMSIButlerRequest re = ((AtsMemberAdmin.OMSIButlerResponse) messageWrapper.getMessage()).getRequest();
            requestKey = LogDownloadRequest.generateKeyForResponseMap(re.getMemberId(), re.getUserId(), re.getActionType());
            logger.info("OMSI message received against request key: {}", requestKey);
        }
        if (this.logDownloadService.getRequestIdToResponseQueueMap().containsKey(requestKey)) {
            BlockingQueue<Message> messageQueue = this.logDownloadService.getRequestIdToResponseQueueMap().get(requestKey);
            messageQueue.add(messageWrapper.getMessage());
        } else {
            logger.info("No request found against response having key {}", requestKey);
        }
    }
}
