package com.sonix.util;

import com.tqltech.tqlpencomm.bean.Dot;
import com.tqltech.tqlpencomm.bean.ElementCode;
import com.tqltech.tqlpencomm.bean.PenStatus;

public final class Events {
    private Events() {
    }

    public static final class DeviceConnecting {
        public String name;
        public String address;

        public DeviceConnecting(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }

    public static final class DeviceConnected {

    }

    public static final class DeviceDisconnected {
    }

    public static final class ReceiveDeviceName {
        public String name;

        public ReceiveDeviceName(String name) {
            this.name = name;
        }
    }

    public static final class ReceiveDeviceBattery {
        public int battery;
        public boolean warned;

        public ReceiveDeviceBattery(int battery, boolean warned) {
            this.battery = battery;
            this.warned = warned;
        }
    }

    public static final class ReceiveElementCode {
        public ElementCode code;
        public long index;

        public ReceiveElementCode(ElementCode code, long index) {
            this.code = code;
            this.index = index;
        }
    }

    public static final class ReceiveInvalidCode {
        public byte[] data_invalid;

        public ReceiveInvalidCode(byte[] data_invalid) {
            this.data_invalid = data_invalid;
        }
    }

    public static final class ReadElementCodeDot{
        public String elementCode;
        public long index;
        public ReadElementCodeDot(String elementCode, long index) {
            this.elementCode = elementCode;
            this.index = index;
        }
    }

    public static final class ReceiveDot {

        public Dot dot;
        public boolean b;

        public ReceiveDot(Dot dot,boolean b) {
            this.dot = dot;
            this.b = b;
        }

        @Override
        public String toString() {
            return "ReceiveDot{" +
                    "dot=" + dot +
                    '}';
        }
    }
    public static final class ReceiveDotErrorPage {

        public Dot dot;
        public boolean b;

        public ReceiveDotErrorPage(Dot dot,boolean b) {
            this.dot = dot;
            this.b = b;
        }

        @Override
        public String toString() {
            return "ReceiveDot{" +
                    "dot=" + dot +
                    '}';
        }
    }


    public static final class ReceiveOffline {
        public int offlineNum;

        public ReceiveOffline(int offlineNum) {
            this.offlineNum = offlineNum;
        }
    }

    public static final class ReceiveOfflineProgress {
        public boolean finished;
        public int progress;

        public ReceiveOfflineProgress(boolean finished, int progress) {
            this.finished = finished;
            this.progress = progress;
        }
    }

    public static final class onReceiveMcuReply {

        public int index;
        public onReceiveMcuReply(int index) {
            this.index = index;
        }
    }

    public static final class onStartMcuUpgrade {

        public int index;
        public onStartMcuUpgrade(int index) {
            this.index = index;
        }
    }

    public static final class ReceiveOfflineDeleteStatus {
        public boolean isSucceed;

        public ReceiveOfflineDeleteStatus(boolean isSucceed) {
            this.isSucceed = isSucceed;
        }
    }

    public static final class ReceiveOfflineDataTransferResponse {
        public boolean isSucceed;

        public ReceiveOfflineDataTransferResponse(boolean isSucceed) {
            this.isSucceed = isSucceed;
        }
    }

    public static final class ReceiveStatus {
        public PenStatus penStatus;

        public ReceiveStatus(PenStatus penStatus) {
            this.penStatus = penStatus;
        }
    }

    public static final class ReceiveSetNameResponse {
        public boolean isSuccess;

        public ReceiveSetNameResponse(boolean isSuccess) {
            this.isSuccess = isSuccess;
        }
    }


    public static final class ReceiveError {
        public String message;

        public ReceiveError(String message) {
            this.message = message;
        }
    }

    public static final class ReceiveFlash {
        public int message;

        public ReceiveFlash(int message) {
            this.message = message;
        }
    }

    public static class ReceiveBuzzes {
        public boolean buzzerBuzzes;
        public ReceiveBuzzes(boolean buzzerBuzzes) {
            this.buzzerBuzzes = buzzerBuzzes;
        }
    }
}
