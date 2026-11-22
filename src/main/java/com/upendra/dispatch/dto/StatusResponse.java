package com.upendra.dispatch.dto;

/** Simple acknowledgement body: {"message": "...", "status": "success"}. */
public record StatusResponse(String message, String status) {

    public static StatusResponse success(String message) {
        return new StatusResponse(message, "success");
    }
}
