package com.mikejaps.downloader.http;

import java.util.Map;

public class HttpResponse {
    private int code;
    private String body;
    private Map<String, Object> headers;

    public HttpResponse(int code, String body, Map<String, Object> headers) {
        this.code = code;
        this.body = body;
        this.headers = headers;
    }

    public int getCode() {
        return code;
    }

    public String getBody() {
        return body;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        return "HttpResponse{" +
                "code=" + code +
                ", body='" + body + '\'' +
                ", headers=" + headers +
                '}';
    }
}
