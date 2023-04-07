package com.liquidnet.pst.helm.wizard.logdownload;

import com.google.common.collect.ImmutableList;
import lombok.Getter;

import java.util.Collection;

@Getter
public class LogDownloadWrapper {
    private final ImmutableList<LogDownload> butlerEntries;
    private final String message;
    private final String logHutUrl;

    public LogDownloadWrapper(Collection<LogDownload> butlerEntries, String message, String logHutUrl) {
        this.butlerEntries = ImmutableList.copyOf(butlerEntries);
        this.message = message;
        this.logHutUrl = logHutUrl;
    }

    public LogDownloadWrapper(Collection<LogDownload> butlerEntries, String message) {
        this(butlerEntries, message, "");
    }
}
