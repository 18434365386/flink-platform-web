package com.flink.streaming.web.utils;

import com.flink.streaming.web.common.SystemConstants;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;


@Slf4j
public class HttpClientToolUtils {
  //CHECKSTYLE:OFF

  /**
   * ????????????
   *
   * @return
   * @throws NoSuchAlgorithmException
   * @throws KeyManagementException
   */
  public static SSLContext createIgnoreVerifySSL()
      throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext sc = SSLContext.getInstance("TLSv1.2");

    // ????????????X509TrustManager?????????????????????????????????????????????????????????
    X509TrustManager trustManager = new X509TrustManager() {
      @Override
      public void checkClientTrusted(
          java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString)
          throws CertificateException {
      }

      @Override
      public void checkServerTrusted(
          java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString)
          throws CertificateException {
      }

      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
      }
    };

    sc.init(null, new TrustManager[]{trustManager}, null);
    return sc;
  }

  public static String doPost(String url, Map<String, String> paramMap, Map<String, String> headMap)
      throws IOException {
    return doPost(url, paramMap, headMap, null, null, SystemConstants.CODE_UTF_8);
  }

  public static String doPost(String url, Map<String, String> paramMap, Map<String, String> headMap,
      int connectTimeout) throws IOException {
    return doPost(url, paramMap, headMap, null, null, SystemConstants.CODE_UTF_8, connectTimeout);
  }

  public static String doPost(String url, Map<String, String> paramMap, Map<String, String> headMap,
      String body, String mimeType, String charset)
      throws IOException {
    return doPost(url, paramMap, headMap, body, mimeType, charset, 0);
  }

  /**
   * ????????????
   *
   * @param url            ????????????
   * @param paramMap       ????????????
   * @param headMap        ???????????????
   * @param body           ??????body
   * @param mimeType       MIME type
   * @param charset        ??????
   * @param connectTimeout ????????????
   * @return
   * @throws NoSuchAlgorithmException
   * @throws KeyManagementException
   * @throws IOException
   * @throws ClientProtocolException
   */

  public static String doPost(String url, Map<String, String> paramMap, Map<String, String> headMap,
      String body, String mimeType, String charset,
      int connectTimeout) throws IOException {
    if (StringUtils.isBlank(charset)) {
      charset = SystemConstants.CODE_UTF_8;
    }
    String result = "";
    // ?????????????????????????????????https??????
    SSLContext sslcontext = null;
    try {
      sslcontext = createIgnoreVerifySSL();
    } catch (KeyManagementException | NoSuchAlgorithmException e) {
      log.error(e.getMessage());
      return null;
    }
    // ????????????http???https???????????????socket?????????????????????
    Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
        .<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.INSTANCE)
        .register("https", new SSLConnectionSocketFactory(sslcontext)).build();
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
        socketFactoryRegistry);
    // HttpClients.custom().setConnectionManager(connManager);

    // ??????????????????httpclient??????
    // CloseableHttpClient client =
    // HttpClients.custom().setConnectionManager(connManager).build();
    if (connectTimeout <= 0) {
      connectTimeout = 8000;
    }
    // ????????????
    HttpHost proxy = null;
    if (headMap != null && headMap.containsKey("caohua_proxy_ip") && headMap
        .containsKey("caohua_proxy_port")) {
      String proxyIp = headMap.get("caohua_proxy_ip");
      int proxyPort = Integer.parseInt(headMap.get("caohua_proxy_port"));
      proxy = new HttpHost(proxyIp, proxyPort);
    }
    RequestConfig defaultRequestConfig = null;
    defaultRequestConfig = RequestConfig.custom().setConnectTimeout(connectTimeout)
        .setSocketTimeout(connectTimeout).setProxy(proxy).build();

    // ??????????????????httpclient??????
    HttpClientBuilder builder = HttpClients.custom().setConnectionManager(connManager);
    builder.setDefaultRequestConfig(defaultRequestConfig);
    CloseableHttpClient client = builder.build();
    // ??????post??????????????????
    HttpPost httpPost = new HttpPost(url);
    if (StringUtils.isNotBlank(body)) {
      HttpEntity entity = new StringEntity(body, ContentType.create(mimeType, charset));
      httpPost.setEntity(entity);
    }
    // ????????????
    if (paramMap != null) {
      List<NameValuePair> nvps = new ArrayList<NameValuePair>();
      for (Entry<String, String> entry : paramMap.entrySet()) {
        nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
      }
      // ??????????????????????????????
      try {
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, charset));
      } catch (UnsupportedEncodingException e) {
        log.error(e.getMessage());
        return null;
      }
    }
    // ??????header??????
    if (headMap != null) {
      for (Entry<String, String> entry : headMap.entrySet()) {
        if (StringUtils.contains(entry.getKey(), "caohua_proxy")) {
          continue;
        }
        httpPost.setHeader(entry.getKey(), entry.getValue());
      }
    }
    try {
      // ??????????????????????????????????????????????????????
      CloseableHttpResponse response = client.execute(httpPost);
      // ??????????????????
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        result = EntityUtils.toString(entity, charset);
      }
      EntityUtils.consume(entity);
      response.close();
    } catch (ClientProtocolException e) {
      log.error(e.getMessage());
    }
    return result;
  }

  public static String doGet(String url) throws IOException {
    return doGet(url, null, null, SystemConstants.CODE_UTF_8, 0);
  }

  public static String doGet(String url, int connectTimeout) throws IOException {
    return doGet(url, null, null, SystemConstants.CODE_UTF_8, connectTimeout);
  }

  public static String doGet(String url, Map<String, String> paramMap, Map<String, String> headMap)
      throws IOException {
    return doGet(url, paramMap, headMap, SystemConstants.CODE_UTF_8, 0);
  }

  public static String doGet(String url, Map<String, String> paramMap, Map<String, String> headMap,
      int connectTimeout) throws IOException {
    return doGet(url, paramMap, headMap, SystemConstants.CODE_UTF_8, connectTimeout);
  }

  public static String doGet(String url, Map<String, String> paramMap, Map<String, String> headMap,
      String charset, int connectTimeout,
      String cookieSpec) throws IOException {
    String result = "";
    // ?????????????????????????????????https??????
    SSLContext sslcontext = null;
    try {
      sslcontext = createIgnoreVerifySSL();
    } catch (KeyManagementException | NoSuchAlgorithmException e) {
      log.error(e.getMessage());
      return null;
    }

    // ????????????http???https???????????????socket?????????????????????
    Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
        .<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.INSTANCE)
        .register("https", new SSLConnectionSocketFactory(sslcontext)).build();
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
        socketFactoryRegistry);
    // HttpClients.custom().setConnectionManager(connManager);

    if (connectTimeout <= 0) {
      connectTimeout = 8000;
    }
    // ????????????
    HttpHost proxy = null;
    if (headMap != null && headMap.containsKey("caohua_proxy_ip") && headMap
        .containsKey("caohua_proxy_port")) {
      String proxyIp = headMap.get("caohua_proxy_ip");
      int proxyPort = Integer.parseInt(headMap.get("caohua_proxy_port"));
      proxy = new HttpHost(proxyIp, proxyPort);
    }
    RequestConfig defaultRequestConfig = null;
    if (StringUtils.isNotBlank(cookieSpec)) {
      defaultRequestConfig = RequestConfig.custom().setSocketTimeout(connectTimeout)
          .setConnectTimeout(connectTimeout).setCookieSpec(cookieSpec)
          .setProxy(proxy).build();
    } else {
      defaultRequestConfig = RequestConfig.custom().setSocketTimeout(connectTimeout)
          .setConnectTimeout(connectTimeout).setProxy(proxy).build();
    } // ??????????????????httpclient??????
    HttpClientBuilder builder = HttpClients.custom().setConnectionManager(connManager);
    builder.setDefaultRequestConfig(defaultRequestConfig);
    CloseableHttpClient client = builder.build();

    if (paramMap != null) {
      StringBuffer sbf = new StringBuffer();
      sbf.append(url);
      if (url.contains("?")) {
        sbf.append("&");
      } else {
        sbf.append("?");
      }
      int n = 0;
      for (Entry<String, String> entry : paramMap.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        sbf.append(key).append("=").append(value);
        if (n < paramMap.size() - 1) {
          sbf.append("&");
        }
        n++;
      }
      url = sbf.toString();
    }
    // ??????get??????????????????
    HttpGet httpGet = new HttpGet(url);
    // ??????header??????
    if (headMap != null) {
      for (Entry<String, String> entry : headMap.entrySet()) {
        if (StringUtils.contains(entry.getKey(), "caohua_proxy")) {
          continue;
        }
        httpGet.setHeader(entry.getKey(), entry.getValue());
      }
    }

    try {
      // ??????????????????????????????????????????????????????
      CloseableHttpResponse response = client.execute(httpGet);
      // ??????????????????
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        result = EntityUtils.toString(entity, charset);
      }
      EntityUtils.consume(entity);
      response.close();
    } catch (ClientProtocolException e) {
      e.printStackTrace();
      log.error(e.getMessage());
    }

    return result;
  }

  public static String doGet(String url, Map<String, String> paramMap, Map<String, String> headMap,
      String charset, int connectTimeout)
      throws IOException {
    return doGet(url, paramMap, headMap, SystemConstants.CODE_UTF_8, connectTimeout, null);
  }

  public static Map<String, Object> doGetFull(String url, Map<String, String> paramMap,
      Map<String, String> headMap, String charset,
      int connectTimeout) throws IOException {
    charset = StringUtils.isBlank(charset) ? SystemConstants.CODE_UTF_8 : charset;
    Map<String, Object> retMap = new HashMap<String, Object>();
    String result = "";
    // ?????????????????????????????????https??????
    SSLContext sslcontext = null;
    try {
      sslcontext = createIgnoreVerifySSL();
    } catch (KeyManagementException | NoSuchAlgorithmException e) {
      log.error(e.getMessage());
      return null;
    }

    // ????????????http???https???????????????socket?????????????????????
    Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
        .<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.INSTANCE)
        .register("https", new SSLConnectionSocketFactory(sslcontext)).build();
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
        socketFactoryRegistry);
    // HttpClients.custom().setConnectionManager(connManager);

    // setConnectTimeout?????????????????????????????????????????????
    // setConnectionRequestTimeout????????????connect Manager??????Connection
    // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????
    // setSocketTimeout?????????????????????????????????????????????????????? ?????????????????????????????????????????????????????????????????????????????????????????????
    if (connectTimeout == 0) {
      connectTimeout = 8000;
    }
    // ????????????
    HttpHost proxy = null;
    if (headMap != null && headMap.containsKey("caohua_proxy_ip") && headMap
        .containsKey("caohua_proxy_port")) {
      String proxyIp = headMap.get("caohua_proxy_ip");
      int proxyPort = Integer.parseInt(headMap.get("caohua_proxy_port"));
      proxy = new HttpHost(proxyIp, proxyPort);
    }
    CookieStore cookieStore = new BasicCookieStore();
    RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(connectTimeout)
        .setConnectTimeout(connectTimeout).setProxy(proxy)
        .build();
    // ??????????????????httpclient??????
    HttpClientBuilder builder = HttpClients.custom().setConnectionManager(connManager);
    builder.setDefaultRequestConfig(defaultRequestConfig);
    CloseableHttpClient client = builder.setDefaultCookieStore(cookieStore).build();

    if (paramMap != null) {
      StringBuffer sbf = new StringBuffer();
      sbf.append(url).append("?");
      int n = 0;
      for (Entry<String, String> entry : paramMap.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        sbf.append(key).append("=").append(value);
        if (n < paramMap.size() - 1) {
          sbf.append("&");
        }
        n++;
      }
      url = sbf.toString();
    }
    // ??????get??????????????????
    HttpGet httpGet = new HttpGet(url);
    // ??????header??????
    if (headMap != null) {
      for (Entry<String, String> entry : headMap.entrySet()) {
        if (StringUtils.contains(entry.getKey(), "caohua_proxy")) {
          continue;
        }
        httpGet.setHeader(entry.getKey(), entry.getValue());
      }
    }

    try {
      // ??????????????????????????????????????????????????????
      CloseableHttpResponse response = client.execute(httpGet);
      // ??????????????????
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        result = EntityUtils.toString(entity, charset);
      }
      EntityUtils.consume(entity);
      List<Cookie> cookies = cookieStore.getCookies();
      Header[] headers = response.getAllHeaders();
      retMap.put("cookies", cookies);
      retMap.put("headers", headers);
      retMap.put("statusLine", response.getStatusLine());
      response.close();
    } catch (ClientProtocolException e) {
      log.error(e.getMessage());
    }
    retMap.put("result", result);
    return retMap;
  }

  public static Map<String, Object> doPostFull(String url, Map<String, String> paramMap,
      Map<String, String> headMap, String body, String mimeType,
      String charset) throws IOException {
    return doPostFull(url, paramMap, headMap, body, mimeType, charset, 0);
  }

  public static Map<String, Object> doPostFull(String url, Map<String, String> paramMap,
      Map<String, String> headMap, String body, String mimeType,
      String charset, int connectTimeout, String cookieSpec) throws IOException {
    charset = StringUtils.isBlank(charset) ? SystemConstants.CODE_UTF_8 : charset;
    Map<String, Object> retMap = new HashMap<String, Object>();
    String result = "";
    // ?????????????????????????????????https??????
    SSLContext sslcontext = null;
    try {
      sslcontext = createIgnoreVerifySSL();
    } catch (KeyManagementException | NoSuchAlgorithmException e) {
      log.error(e.getMessage());
      return null;
    }

    // ????????????http???https???????????????socket?????????????????????
    Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
        .<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.INSTANCE)
        .register("https", new SSLConnectionSocketFactory(sslcontext)).build();
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
        socketFactoryRegistry);
    // HttpClients.custom().setConnectionManager(connManager);
    // setSocketTimeout?????????????????????????????????????????????????????? ?????????????????????????????????????????????????????????????????????????????????????????????
    if (connectTimeout == 0) {
      connectTimeout = 8000;
    }
    // ????????????
    HttpHost proxy = null;
    if (headMap != null && headMap.containsKey("caohua_proxy_ip") && headMap
        .containsKey("caohua_proxy_port")) {
      String proxyIp = headMap.get("caohua_proxy_ip");
      int proxyPort = Integer.parseInt(headMap.get("caohua_proxy_port"));
      proxy = new HttpHost(proxyIp, proxyPort);
    }
    CookieStore cookieStore = new BasicCookieStore();
    RequestConfig defaultRequestConfig = null;
    if (StringUtils.isNotBlank(cookieSpec)) {
      defaultRequestConfig = RequestConfig.custom().setSocketTimeout(connectTimeout)
          .setCookieSpec(cookieSpec).setProxy(proxy).build();
    } else {
      defaultRequestConfig = RequestConfig.custom().setSocketTimeout(connectTimeout).setProxy(proxy)
          .build();
    }

    // ??????????????????httpclient??????
    HttpClientBuilder builder = HttpClients.custom().setConnectionManager(connManager);
    builder.setDefaultRequestConfig(defaultRequestConfig);
    CloseableHttpClient client = builder.setDefaultCookieStore(cookieStore).build();

    // ??????post??????????????????
    HttpPost httpPost = new HttpPost(url);
    if (StringUtils.isNotBlank(body)) {
      HttpEntity entity = new StringEntity(body, ContentType.create(mimeType, charset));
      httpPost.setEntity(entity);
    }
    // ????????????
    if (paramMap != null) {
      List<NameValuePair> nvps = new ArrayList<NameValuePair>();
      for (Entry<String, String> entry : paramMap.entrySet()) {
        nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
      }
      // ??????????????????????????????
      try {
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, charset));
      } catch (UnsupportedEncodingException e) {
        log.error(e.getMessage());
        return null;
      }
    }
    // ??????header??????
    if (headMap != null) {
      for (Entry<String, String> entry : headMap.entrySet()) {
        if (StringUtils.contains(entry.getKey(), "caohua_proxy")) {
          continue;
        }
        httpPost.setHeader(entry.getKey(), entry.getValue());
      }
    }
    try {
      // ??????????????????????????????????????????????????????
      CloseableHttpResponse response = client.execute(httpPost);
      // ??????????????????
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        result = EntityUtils.toString(entity, charset);
      }
      EntityUtils.consume(entity);
      List<Cookie> cookies = cookieStore.getCookies();
      Header[] headers = response.getAllHeaders();
      retMap.put("cookies", cookies);
      retMap.put("headers", headers);

      response.close();
    } catch (ClientProtocolException e) {
      log.error(e.getMessage());
    }
    retMap.put("result", result);
    return retMap;
  }

  public static Map<String, Object> doPostFull(String url, Map<String, String> paramMap,
      Map<String, String> headMap, String body, String mimeType,
      String charset, int connectTimeout) throws IOException {
    return doPostFull(url, paramMap, headMap, body, mimeType, charset, connectTimeout, null);
  }

  /**
   * ??????SSL????????????
   */
  @SuppressWarnings("deprecation")
  public static SSLConnectionSocketFactory createSSLConnSocketFactory() {
    SSLConnectionSocketFactory sslsf = null;
    try {
      X509TrustManager xtm = new X509TrustManager() { // ??????TrustManager
        public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }
      };
      SSLContext ctx = SSLContext.getInstance("SSL");
      ctx.init(null, new TrustManager[]{xtm}, null);
      sslsf = new SSLConnectionSocketFactory(ctx, new X509HostnameVerifier() {
        @Override
        public boolean verify(String arg0, SSLSession arg1) {
          return true;
        }

        @Override
        public void verify(String host, SSLSocket ssl) throws IOException {
        }

        @Override
        public void verify(String host, X509Certificate cert) throws SSLException {
        }

        @Override
        public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
        }
      });
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    }
    return sslsf;
  }

  /**
   * ??????cookie?????????
   *
   * @param cookiemap
   * @return
   * @author ?????????
   * @date 2018???1???10??? ??????5:09:58
   * @version V1.0
   */
  public static String buildCookie(Map<String, String> cookiemap) {
    if (cookiemap == null) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    for (String key : cookiemap.keySet()) {
      sb.append(key).append("=").append(cookiemap.get(key)).append("; ");
    }
    return sb.toString();
  }

  /**
   * ??????cookie?????????
   *
   * @param cookiemap
   * @return
   * @author ?????????
   * @date 2018???1???10??? ??????5:09:58
   * @version V1.0
   */
  public static String buildCookieByMap(Map<String, Cookie> cookiemap) {
    if (cookiemap == null) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    for (Cookie cookie : cookiemap.values()) {
      sb.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
    }
    return sb.toString();
  }

  /**
   * ??????cookie?????????
   *
   * @return
   * @author ?????????
   * @date 2018???1???10??? ??????5:09:58
   * @version V1.0
   */
  public static String buildCookie(List<Cookie> cookies) {
    if (cookies == null) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    for (Cookie cookie : cookies) {
      sb.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
    }
    return sb.toString();
  }

  /**
   * ??????cookie????????????cookie map
   *
   * @param cookies
   * @return
   * @author ?????????
   * @date 2018???1???12??? ??????11:07:18
   * @version V1.0
   */
  public static Map<String, Cookie> buildCookieMap(List<Cookie> cookies) {
    if (cookies == null) {
      return null;
    }
    Map<String, Cookie> map = new HashMap<String, Cookie>();
    for (Cookie cookie : cookies) {
      map.put(cookie.getName(), cookie);
    }
    return map;
  }

  /**
   * ?????????????????????????????????cookie map
   *
   * @param cookies
   * @return
   * @author ?????????
   * @date 2018???1???12??? ??????11:07:18
   * @version V1.0
   */
  public static Map<String, String> buildCookieStrMap(Map<String, Cookie> cookies) {
    if (cookies == null) {
      return null;
    }
    Map<String, String> map = new HashMap<String, String>();
    for (Cookie cookie : cookies.values()) {
      map.put(cookie.getName(), cookie.getValue());
    }
    return map;
  }

  /**
   * ?????????????????????????????????cookie map
   *
   * @param cookies
   * @return
   * @author ?????????
   * @date 2018???1???12??? ??????11:07:18
   * @version V1.0
   */
  public static Map<String, String> buildCookieStrMap(List<Cookie> cookies) {
    if (cookies == null) {
      return null;
    }
    Map<String, String> map = new HashMap<String, String>();
    for (Cookie cookie : cookies) {
      map.put(cookie.getName(), cookie.getValue());
    }
    return map;
  }

  /**
   * ??????cookie map??????cookie??????
   *
   * @param cookies
   * @return
   * @author ?????????
   * @date 2018???1???12??? ??????11:07:18
   * @version V1.0
   */
  public static List<Cookie> buildCookieList(Map<String, Cookie> cookiemap) {
    if (cookiemap == null) {
      return null;
    }
    List<Cookie> cookies = new ArrayList<Cookie>();
    for (Cookie cookie : cookiemap.values()) {
      cookies.add(cookie);
    }
    return cookies;
  }

  public static String getUrlParamByName(String url, String param) {
    Map<String, String> params = getParamByUrl(url);
    return params.get(param);
  }

  public static Map<String, String> getParamByUrl(String url) {
    Map<String, String> map = new HashMap<>();
    List<NameValuePair> list = null;
    if (StringUtils.isBlank(url)) {
      return map;
    }
    try {
      list = URLEncodedUtils.parse(new URI(url), SystemConstants.CODE_UTF_8);
    } catch (URISyntaxException e) {
      log.error("HttpClientToolUtils.getParamByUrl.error.??????url??????");
    }
    for (NameValuePair pair : list) {
      map.put(pair.getName(), pair.getValue());
    }
    return map;
  }

  public static String doPut(String url, Map<String, String> cookieMap, String paramJsonStr) {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPut httpPut = new HttpPut(url);
    RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000)
        .setConnectionRequestTimeout(35000).setSocketTimeout(60000).build();
    httpPut.setConfig(requestConfig);

    for (String key : cookieMap.keySet()) {
      httpPut.setHeader(key, cookieMap.get(key));
    }

    CloseableHttpResponse httpResponse = null;
    try {
      List<NameValuePair> list = new ArrayList<NameValuePair>();
      httpPut.setEntity(new StringEntity(paramJsonStr, ContentType.APPLICATION_JSON));
      httpResponse = httpClient.execute(httpPut, HttpClientContext.create());
      HttpEntity entity = httpResponse.getEntity();
      String result = EntityUtils.toString(entity);
      return result;
    } catch (ClientProtocolException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if (httpResponse != null) {
        try {
          httpResponse.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      if (null != httpClient) {
        try {
          httpClient.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }
//CHECKSTYLE:ON
}

