package com.sonix.oidbluetooth.bean;

public class BitErrorBean {

    private String originalData, formatData;

    public BitErrorBean(String originalData, String formatData) {
        this.originalData = originalData;
        this.formatData = formatData;
    }

    public String getOriginalData() {
        return originalData;
    }

    public void setOriginalData(String originalData) {
        this.originalData = originalData;
    }

    public String getFormatData() {
        return formatData;
    }

    public void setFormatData(String formatData) {
        this.formatData = formatData;
    }
}
