package sabria.novolley.library.implement;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import sabria.novolley.library.config.BeautyHttpConfig;
import sabria.novolley.library.core.BeautyRequest;
import sabria.novolley.library.inter.BeautyHttpStackInter;

/**
 * API>9时启用的网络请求
 * 
 * HttpURLConnection 也就是这里的是默认带GZIP压缩的
 * 另一个是不带GZIP压缩的
 * 
 * @author 伟
 * 
 */
public class HurlStack implements BeautyHttpStackInter {

	// 接口
	private final UrlRewriter mUrlRewriter;
	// 对HTTPS进行加载安全通讯证书
	private final SSLSocketFactory mSslSocketFactory;

	public interface UrlRewriter {
		// 重写用于请求的URL
		public String rewriteUrl(String originalUrl);
	}

	/**
	 * 构造函数
	 */
	public HurlStack() {
		this(null);
	}

	public HurlStack(UrlRewriter urlRewriter) {
		this(urlRewriter, null);
	}

	public HurlStack(UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
		mUrlRewriter = urlRewriter;
		mSslSocketFactory = sslSocketFactory;
	}

	/**
	 * 发送HTTP请求
	 */
	@Override
	public HttpResponse executeRequest(BeautyRequest request, Map<String, String> cacheHeaders) throws IOException {
		//处理URL
		String url = createHttpURL(request, cacheHeaders);
		//参数设置
		HttpURLConnection connection = createHttpURLConnection(request, url);
		//请求头处理
		createHeaders(request, connection, cacheHeaders);
		//HTTP方法
		createHttpMethod(request, connection);
		//得到response
		BasicHttpResponse response = getHttpResponse(connection);
		handlerResponse(connection, response);
		return response;
	}

	private void handlerResponse(HttpURLConnection connection, BasicHttpResponse response) {
		response.setEntity(entityFromConnection(connection));
		for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
			if (header.getKey() != null) {
				String value = "";
				for (String v : header.getValue()) {
					value += (v + "; ");
				}
				Header h = new BasicHeader(header.getKey(), value);
				response.addHeader(h);
			}
		}
	}

	/**
	 * 从给定的HttpurlConnection创建HttpEntity
	 * 
	 * @param connection
	 * @return
	 */
	private HttpEntity entityFromConnection(HttpURLConnection connection) {
		BasicHttpEntity entity = new BasicHttpEntity();
		InputStream inputStream;
		try {
			inputStream = connection.getInputStream();
		} catch (IOException ioe) {
			inputStream = connection.getErrorStream();
		}
		entity.setContent(inputStream);
		entity.setContentLength(connection.getContentLength());
		entity.setContentEncoding(connection.getContentEncoding());
		entity.setContentType(connection.getContentType());
		return entity;
	}

	/**
	 * 获取BasicHttpResponse
	 * 
	 * @param connection
	 * @return
	 * @throws IOException
	 */
	private BasicHttpResponse getHttpResponse(HttpURLConnection connection) throws IOException {
		ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
		int responseCode = connection.getResponseCode();
		if (responseCode == -1) {
			throw new IOException("Could not retrieve response code from HttpUrlConnection.");
		}
		BasicStatusLine responseStatus = new BasicStatusLine(protocolVersion, connection.getResponseCode(), connection.getResponseMessage());
		BasicHttpResponse response = new BasicHttpResponse(responseStatus);
		return response;
	}

	/**
	 * 对URL地址进行处理
	 * 
	 * @param request
	 * @param cacheHeaders
	 * @return
	 */
	private String createHttpURL(BeautyRequest request, Map<String, String> cacheHeaders) throws IOException {
		// URL
		String url = request.getUrl();

		// 接口URL重写
		if (mUrlRewriter != null) {
			// 使用者实现接口
			String rewritten = mUrlRewriter.rewriteUrl(url);
			if (rewritten == null) {
				throw new IOException("URL blocked by rewriter: " + url);
			}
			url = rewritten;
		}
		return url;
	}

	/**
	 * 创建HttpURLConnection
	 * 
	 * @param request
	 * @param pUrl
	 * @return
	 * @throws IOException
	 */
	private HttpURLConnection createHttpURLConnection(BeautyRequest request, String pUrl) throws IOException {

		URL url = new URL(pUrl);
		HttpURLConnection connection = createConnection(url);

		// 参数设置
		int timeoutMs = request.getTimeoutMs();
		connection.setConnectTimeout(timeoutMs);
		connection.setReadTimeout(timeoutMs);
		connection.setUseCaches(false);
		connection.setDoInput(true);

		// HTTPS
		if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
			((HttpsURLConnection) connection).setSSLSocketFactory(mSslSocketFactory);
		}

		return connection;

	}
	
	/**
	 * 让子类复写的重要方法
	 * @param url
	 * @return
	 * @throws IOException
	 */
	protected HttpURLConnection createConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

	/**
	 * 请求头部信息
	 * 
	 * @param request
	 * @param connection
	 * @param cacheHeaders
	 */
	private void createHeaders(BeautyRequest request, HttpURLConnection connection, Map<String, String> cacheHeaders) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.putAll(request.getHeaders());
		map.putAll(cacheHeaders);
		for (String headerName : map.keySet()) {
			connection.addRequestProperty(headerName, map.get(headerName));
		}
	}

	/**
	 * 
	 * @param request
	 * @param connection
	 */
	private void createHttpMethod(BeautyRequest request, HttpURLConnection connection) throws IOException {
		switch (request.getmMethod()) {

		case BeautyHttpConfig.HttpMethod.GET:
			connection.setRequestMethod("GET");
			break;

		case BeautyHttpConfig.HttpMethod.POST:
			connection.setRequestMethod("POST");
			addBodyIfExists(request, connection);
			break;

		default:
			throw new IllegalStateException("Unknown method type.");
		}

	}

	/**
	 * 如果有body则添加
	 * 
	 * @param request
	 * @param connection
	 * @throws IOException
	 */
	private void addBodyIfExists(BeautyRequest request, HttpURLConnection connection) throws IOException {
		byte[] body = request.getBody();
		if (body != null) {
			connection.setDoOutput(true);
			connection.addRequestProperty(BeautyHttpConfig.HEADER_CONTENT_TYPE, request.getBodyContentType());
			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
			out.write(body);
			out.close();
		}
	}

}
