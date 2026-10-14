package com.roqhrcl.sealed.error;

import com.roqhrcl.common.error.ErrorCode;

public enum SealedErrorCode implements ErrorCode {

    SEALED_NOT_FOUND(404, "SEALED를 찾을 수 없습니다"),
    SEALED_NOT_REVEALED(409, "아직 공개 시각이 아닙니다");

    private final int status;
    private final String message;

    SealedErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String message() {
        return message;
    }
}
