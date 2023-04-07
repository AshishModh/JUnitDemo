package com.liquidnet.pst.helm.wizard.logdownload;

import com.liquidnet.gpss.lib.app.GpssApplicationException;
import com.liquidnet.gpss.lib.services.web.WebUtils;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import spark.Request;

@Getter
public class LogDownloadRequest {
    private String actionType;
    private String memberId;
    private String userId;
    private String fileName;
    private Boolean isOMSI = false;
    private String requestKey;
    private String topic;

    public LogDownloadRequest(Request req, String actionType) {
        this.actionType = actionType;
        this.memberId = WebUtils.decodeParamString(req, "memberid");
        this.userId = WebUtils.decodeParamString(req, "userid");
        this.fileName = WebUtils.decodeParamString(req, "filename");
        if (StringUtils.isBlank(this.userId) || "OMSI".equalsIgnoreCase(this.userId)) {
            this.userId = "";
            this.isOMSI = true;
        }
        this.requestKey = generateKeyForResponseMap(memberId, userId, actionType);
        if (this.isOMSI) {
            this.topic = String.join("_", "RemoteComponent_Butler", memberId).replaceAll("\\s", "");
        } else {
            this.topic = String.join("_", "RemoteComponent_Traders", memberId, userId).replaceAll("\\s", "");
        }
    }

    public static String generateKeyForResponseMap(String memberId, String userId, String actionType) {
        String key = String.join("", memberId, userId, actionType);
        key = key.replaceAll("\\s", "");
        return key;
    }

    public void validateRequestActionType() throws GpssApplicationException {
        if (!"DIR".equalsIgnoreCase(this.actionType) && !"GET".equalsIgnoreCase(this.actionType))
            throw new GpssApplicationException("Invalid action type.");
    }
}
