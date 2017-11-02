package cn.bgs.okhttpdemo;

import java.io.File;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * OKHttp3上传工具类
 */
public class Okhttp3Utlis {
    private static OkHttpClient ok = null;
    private static Okhttp3Utlis instance=null;
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
    private static File file;
    private static String imgpath;
    private static String imageName;

    private Okhttp3Utlis() {
        if (ok==null) {
            ok = new OkHttpClient();
            ok.newBuilder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS);
        }
    }

    /**
     *单例获取
     * */
    public static Okhttp3Utlis getInstance() {
        if (instance == null) {
            synchronized (Okhttp3Utlis.class) {
                if (instance == null) {
                    instance = new Okhttp3Utlis();
                }
            }
        }
        return instance;
    }


    /**
     * get 请求
     * @param url 地址
     * @param callBack
     */
    public void getRequest(String url, final Callback callBack) {
        Request request = buildRequest(url, null, HttpMethodType.GET);
        Call call = ok.newCall(request);
        call.enqueue(callBack);
    }

    /**
     * post 请求
     * @param url 地址
     * @param params  图片路径集合
     * @param callBack
     */
    public void postRequest(String url,  Map<String, String> params,final Callback callBack) {
        Request request = buildRequest(url, params, HttpMethodType.POST);
        Call call = ok.newCall(request);
        call.enqueue(callBack);
    }

    /**
     * 图片
     * @param url 地址
     * @param callback
     * @param file  图片路径集合
     * @param fileKey
     * @param params
     */
    public void postUploadSingleImage(String url, final File file, String fileKey, Map<String, String> params,Callback callback) {
        Param[] paramsArr = fromMapToParams(params);

        try {
            postAsyn(url, callback, file, fileKey, paramsArr);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 图片
     * @param url 地址
     * @param callback
     * @param files
     * @param fileKeys
     * @param params
     */
    public void postUploadMoreImages(String url, final  File[] files, String[] fileKeys, Map<String, String> params,Callback callback) {
        Param[] paramsArr = fromMapToParams(params);
        try {
            postAsyn(url, callback, files, fileKeys, paramsArr);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 单个文件上传请求  不带参数
     * @param url
     * @param file
     * @param fileKey
     * @param callback
     * @throws IOException
     */
    private void postAsyn(String url,File file, String fileKey, Callback callback) throws IOException {
        Request request = buildMultipartFormRequest(url, new File[]{file}, new String[]{fileKey}, null);
        Call call = ok.newCall(request);
        call.enqueue(callback);
    }

    /**
     * 单个文件上传请求 带参数
     * @param url
     * @param callback
     * @param file
     * @param fileKey
     * @param params
     * @throws IOException
     */
    private void postAsyn(String url, Callback callback, File file, String fileKey, Param... params) throws IOException {
        Request request = buildMultipartFormRequest(url, new File[]{file}, new String[]{fileKey}, params);
        Call call = ok.newCall(request);
        call.enqueue(callback);
    }

    /**
     * 多个文件上传请求 带参数
     * @param url
     * @param callback
     *
     * @param files
     * @param fileKeys
     * @param params
     * @throws IOException
     */
    private void postAsyn(String url, Callback callback, File[] files, String[] fileKeys, Param... params) throws IOException {
        Request request = buildMultipartFormRequest(url, files, fileKeys, params);
        Call call = ok.newCall(request);
        call.enqueue(callback);
    }


    //构造上传图片 Request
    private Request buildMultipartFormRequest(String url, File[] files, String[] fileKeys, Param[] params) {
        params = validateParam(params);
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        for (Param param : params) {
            builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + param.key + "\""),
                    RequestBody.create(MediaType.parse("image/png"), param.value));
        }
        if (files != null) {
            RequestBody fileBody = null;
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                String fileName = file.getName();
                fileBody = RequestBody.create(MediaType.parse(guessMimeType(fileName)), file);
                //TODO 根据文件名设置contentType
                builder.addPart(Headers.of("Content-Disposition",
                        "form-data; name=\"" + fileKeys[i] + "\"; filename=\"" + fileName + "\""),
                        fileBody);
            }
        }

        RequestBody requestBody = builder.build();
        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
    }


    private Param[] validateParam(Param[] params) {
        if (params == null)
            return new Param[0];
        else
            return params;
    }


    public static class Param {
        public Param() {
        }

        public Param(String key, String value) {
            this.key = key;
            this.value = value;
        }

        String key;
        String value;
    }



    private String guessMimeType(String path) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentTypeFor = fileNameMap.getContentTypeFor(path);
        if (contentTypeFor == null) {
            contentTypeFor = "application/octet-stream";
        }
        return contentTypeFor;
    }


    //创建 Request对象
    private Request buildRequest(String url, Map<String, String> params, HttpMethodType methodType) {

        Request.Builder builder = new Request.Builder();
        builder.url(url);
        if (methodType == HttpMethodType.GET) {
            builder.get();
        } else if (methodType == HttpMethodType.POST) {
            RequestBody requestBody = buildFormData(params);
            builder.post(requestBody);
        }
        return builder.build();
    }
    //构建请求所需的参数表单
    private RequestBody buildFormData(Map<String, String> params) {
        FormBody.Builder builder = new FormBody.Builder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }
    private Param[] fromMapToParams(Map<String, String> params) {
        if (params == null)
            return new Param[0];
        int size = params.size();
        Param[] res = new Param[size];
        Set<Map.Entry<String, String>> entries = params.entrySet();
        int i = 0;
        for (Map.Entry<String, String> entry : entries) {
            res[i++] = new Param(entry.getKey(), entry.getValue());
        }
        return res;
    }
    enum HttpMethodType {
        GET, POST
    }

}
