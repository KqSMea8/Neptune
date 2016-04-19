package org.qiyi.pluginlibrary.exception;

public class PluginStartupException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public int errorCode;

    public PluginStartupException(int errorCode, String msg) {
        super(msg);
        this.errorCode = errorCode;
    }

    public int getCode() {
        return errorCode;
    }

}
