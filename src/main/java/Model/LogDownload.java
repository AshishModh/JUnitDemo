package com.liquidnet.pst.helm.wizard.logdownload;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LogDownload {
    private final String type;
    private final String filePath;
    private final String dateTimeModified;
    private final String size;
}
