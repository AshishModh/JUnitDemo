package com.liquidnet.pst.helm.wizard.logdownload;

import com.liquidnet.gpss.lib.services.web.JsonTransformer;
import com.liquidnet.pst.lib.services.web.Routes;

import java.util.HashMap;

import static spark.Spark.get;

public class LogDownloadRoutes extends Routes {
    private LogDownloadService logDownloadService;
    private static final String LOG_DOWNLOAD_WIZARD_URL = "/api/helm/wizard/log_download";

    protected LogDownloadRoutes(LogDownloadService logDownloadService) {
        this.logDownloadService = logDownloadService;
    }

    public void registerRoutes() {

        get(LOG_DOWNLOAD_WIZARD_URL, (req, res) -> {
            res.type("application/json");
            return logDownloadService.butlerProcess(new LogDownloadRequest(req, "DIR"));
        }, new JsonTransformer());

        get(LOG_DOWNLOAD_WIZARD_URL + "/saveloghut", (req, res) -> {
            res.type("application/json");
            return logDownloadService.butlerProcess(new LogDownloadRequest(req, "GET"));
        }, new JsonTransformer());

        get(LOG_DOWNLOAD_WIZARD_URL + "/loghutdownload", (req, res) -> {
            logDownloadService.downloadLogHut(req, res);
            return new HashMap<>();
        }, Object::toString);

    }
}
