package com.mikejaps.downloader.http;


import android.util.Log;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpManager {
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient okHttpClient;

    private static HttpManager httpManager;

    private SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory ssfFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new SdkTrustManager()}, new SecureRandom());
            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ssfFactory;
    }

    private HttpManager() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        /*builder.sslSocketFactory(createSSLSocketFactory());
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });*/
        builder.connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS);
        okHttpClient = builder.build();
    }

    public static HttpManager getInstance() {
        synchronized (HttpManager.class) {
            if (httpManager == null) {
                httpManager = new HttpManager();
            }
        }
        return httpManager;
    }

    public void postAsync(String url, HttpCallBack callBack) {
        postAsync(url, null, callBack);
    }

    public void postAsync(String url, Map<String, String> headers, HttpCallBack callBack) {
        postAsync(url, headers, null, callBack);
    }

    public void postAsync(String url, Map<String, String> headers, String requestBody, HttpCallBack callBack) {
        Request request = postRequest(url, headers, requestBody);
        commAsync(request, callBack);
    }

    public HttpResponse postSync(String url) throws IOException {
        return postSync(url, null);
    }

    public HttpResponse postSync(String url, Map<String, String> headers) throws IOException {
        return postSync(url, headers, null);
    }

    public HttpResponse postSync(String url, Map<String, String> headers, String requestBody) throws IOException {
        Log.d("HttpManager", requestBody);
        Request request = postRequest(url, headers, requestBody);
        Response response = commonSync(request);
        if (response == null) {
            return null;
        }
        Headers responseHeaders = response.headers();
        Map<String, Object> httpHeaders = new HashMap<>();

        for (int i = 0; i < responseHeaders.size(); i++) {
            httpHeaders.put(responseHeaders.name(i), responseHeaders.value(i));
        }

        return new HttpResponse(response.code(), response.body().string(), httpHeaders);
    }

    public void getAsync(String url, HttpCallBack callBack) {
        getAsync(url, null, callBack);
    }

    public void getAsync(String url, Map<String, String> headers, HttpCallBack callBack) {
        Request request = getRequest(url, headers);
        commAsync(request, callBack);
    }

    public HttpResponse getSync(String url) throws IOException {
        return getSync(url, null);
    }

    public HttpResponse getSync(String url, Map<String, String> headers) throws IOException {
        Request request = getRequest(url, headers);

        Response response = okHttpClient.newCall(request).execute();

        if (response == null) {
            return null;
        }

        Headers responseHeaders = response.headers();
        Map<String, Object> httpHeaders = new HashMap<>();

        for (int i = 0; i < responseHeaders.size(); i++) {
            httpHeaders.put(responseHeaders.name(i), responseHeaders.value(i));
        }
        return new HttpResponse(response.code(), response.body().string(), httpHeaders);
    }

    private Request getRequest(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder()
                .url(url);
        if (headers != null)
            headers.forEach((String key, String value) -> {
                builder.addHeader(key, value);
            });
        return builder.build();
    }

    private Request postRequest(String url, Map<String, String> headers, String requestBody) {
        RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, requestBody);
        Request.Builder builder = new Request.Builder()
                .post(body)
                .url(url);
        if (headers != null)
            headers.forEach((String key, String value) -> {
                builder.addHeader(key, value);
            });
        return builder.build();
    }

    private void commAsync(Request request, HttpCallBack callBack) {
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callBack != null) callBack.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Headers responseHeaders = response.headers();
                int code = response.code();
                Map<String, Object> httpHeaders = new HashMap<>();
                for (int i = 0; i < responseHeaders.size(); i++) {
                    httpHeaders.put(responseHeaders.name(i), responseHeaders.value(i));
                }
                if (code == 200 && callBack != null)
                    callBack.onSuccess(new HttpResponse(code, response.body().string(), httpHeaders));
                else if (callBack != null)
                    callBack.onError(new Exception(response.body().string()));
            }
        });
    }

    private Response commonSync(Request request) throws IOException {
        Response response = okHttpClient.newCall(request).execute();
        return response;
    }
    public Call asyncCall(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        return okHttpClient.newCall(request);
    }
    public Response syncResponse(String url, long start, long end) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                //Range 请求头格式Range: bytes=start-end
                .addHeader("Range", "bytes=" + start + "-" + end)
                .build();
        return okHttpClient.newCall(request).execute();
    }


}
