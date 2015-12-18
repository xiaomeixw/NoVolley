package sabria.novolley.library.implement;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.util.Map;

import sabria.novolley.library.config.BeautyHttpConfig;
import sabria.novolley.library.core.BeautyRequest;
import sabria.novolley.library.inter.BeautyHttpStackInter;

/**
 * 具体的请求方案类
 * API小于9时建议调用
 * 
 * @author 伟
 * 
 */
public class HttpClientStack implements BeautyHttpStackInter {
	
	protected final HttpClient mClient;
	
	/**
	 * HttpClient 由外部来创建
	 * @param mClient
	 */
	public HttpClientStack(HttpClient mClient){
		this.mClient=mClient;
	}
	
	

	/**
	 * 实现实现类的执行请求的方法
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Override
	public HttpResponse executeRequest(BeautyRequest request, Map<String, String> cacheHeaders) throws IOException {
		//1. 创建一个请求httpRequest
		HttpUriRequest httpRequest = createHttpRequest(request, cacheHeaders);
		//2. 添加head头部信息
		addHeaders(httpRequest,cacheHeaders);
		addHeaders(httpRequest,request.getHeaders());
		//3. 设置一些参数
		setConfig(httpRequest,request);
		//4. 执行请求
		return mClient.execute(httpRequest);
	}

	
	
	/**
	 * 设置参数
	 */
	private void setConfig(HttpUriRequest httpRequest,BeautyRequest request) {
		HttpParams params = httpRequest.getParams();
		int timeoutMs = request.getTimeoutMs();
		//建立连接的超时设置：这定义了通过网络与服务器建立连接的超时时间。Httpclient包中通过一个异步线程去创建与服务器的socket连接，这就是该socket连接的超时时间，此处设置为5秒。
		HttpConnectionParams.setConnectionTimeout(params, 5000);
		//请求超时设置：这定义了Socket读数据的超时时间，即连接之后的指定时间内没有收到服务器发来的相应的数据包(从服务器获取响应数据需要等待的时间，此处设置为2.5秒)
		HttpConnectionParams.setSoTimeout(params, timeoutMs);
	}




	/**
	 * 添加头部信息
	 * @param httpRequest
	 * @param headers
	 */
	private void addHeaders(HttpUriRequest httpRequest, Map<String, String> headers) {
		for (String key : headers.keySet()) {
            httpRequest.setHeader(key, headers.get(key));
        }
	}



	/**
	 * HttpUriRequest的实现子类HttpGet 和 HttpPost new HttpPost(url) 或者new
	 * HttpGet(url)
	 * 
	 * @param request
	 * @param cacheHeaders
	 * @return
	 */
	private static HttpUriRequest createHttpRequest(BeautyRequest request, Map<String, String> cacheHeaders) {
		switch (request.getmMethod()) {
		case BeautyHttpConfig.HttpMethod.GET:
			return new HttpGet(request.getUrl());

		case BeautyHttpConfig.HttpMethod.POST:
			HttpPost httpPost = new HttpPost(request.getUrl());
			httpPost.addHeader(BeautyHttpConfig.HEADER_CONTENT_TYPE, request.getBodyContentType());
			// post放入参数(参数)
			setEntityIfNonEmptyBody(httpPost, request);
			return httpPost;
		default:
			throw new IllegalStateException("Unknown request method.");

		}

	}

	/**
	 * 对post的参数进行处理
	 * 
	 * @param httpPost
	 * @param request
	 */
	private static void setEntityIfNonEmptyBody(HttpPost httpPost, BeautyRequest request) {
		byte[] body = request.getBody();
		if (body != null) {
			ByteArrayEntity entity = new ByteArrayEntity(body);
			// 传递参数
			httpPost.setEntity(entity);
		}
	}

}
