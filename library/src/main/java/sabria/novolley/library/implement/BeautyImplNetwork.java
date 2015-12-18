package sabria.novolley.library.implement;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.cookie.DateUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import sabria.novolley.library.config.BeautyHttpConfig;
import sabria.novolley.library.core.BeautyRequest;
import sabria.novolley.library.inter.BeautyHttpStackInter;
import sabria.novolley.library.inter.BeautyNetworkInter;
import sabria.novolley.library.response.NetworkResponse;
import sabria.novolley.library.util.BeautyExecption;
import sabria.novolley.library.util.BeautyLog;
import sabria.novolley.library.util.ByteArrayPool;
import sabria.novolley.library.util.IOUtil;


/**
 * 实现之BeautyNetworkInterface
 * 			发动request请求并返回NetworkResponse,静待request调用
 * @author 伟
 *
 */
public class BeautyImplNetwork implements BeautyNetworkInter {
	
	//****************************************
	//全局变量
	//*****************************************
	//处理请求的方案
	protected final BeautyHttpStackInter httpStack;
	//字节数组池
	protected final ByteArrayPool arrayPool;
	
	
	//****************************************
	//初始化构造参数
	//*****************************************
	public BeautyImplNetwork(BeautyHttpStackInter httpStack){
		this(httpStack, new ByteArrayPool(BeautyHttpConfig.DEFAULT_BYTEARRAYPOOL_SIZE));
	}
	
	public BeautyImplNetwork(BeautyHttpStackInter httpStack,ByteArrayPool arrayPool){
		this.httpStack=httpStack;
		this.arrayPool=arrayPool;
	}
	
	
	
	

	/**
	 * 这里会执行两个层级的业务逻辑：
	 * 1.传入自定义的需求HttpStack执行请求
	 * 2.将系统框架的HttpResponse转成含有信息的NetworkResponse
	 * 
	 * @return 这里执行返回的NetworkResponse 会 在 Request中parseNetworkResponse()方法中
	 * 会最终转化成为自定义的response类
	 */
	@Override
	public NetworkResponse executeRequestAndTransToHttpResponse(BeautyRequest request) throws BeautyExecption {
		
		//会一直调度式的执行
		while (true) {
			//服务端返回的HttpResponse对象
			HttpResponse httpResponse=null;
			//HttpResponse.getEntity.transToByte[]
			byte[] responseContents=null;
			//Map格式的响应头
			Map<String, String> responseHeaders = new HashMap<String, String>();
			try {
				 //获取缓存头部信息
				 Map<String, String> cacheHeaders = new HashMap<String, String>();
	             addCacheHeaders(cacheHeaders, request.getCacheEntry());
				 //HttpStack开始走发动请求的处理逻辑，并从服务端拿回HttpResponse
	             httpResponse = httpStack.executeRequest(request, cacheHeaders);
				 //获取statusCode
	             int statusCode=getStatusCode(httpResponse);
	             //将返回的header[]数组头转成map格式,这些信息都会放到NetworkResponse中
	             responseHeaders=convertHeaders(httpResponse.getAllHeaders());
	             //处理statusCode-302服务端无更新
	             if(statusCode==HttpStatus.SC_NOT_MODIFIED){
	     			return new NetworkResponse(HttpStatus.SC_NOT_MODIFIED,request.getCacheEntry().data, responseHeaders, true);
	     		 }
	             //判断httpResponse的getEntity是否为空
	             if(httpResponse.getEntity()!=null){
	            	 //不为空时，将Entity()转成byte[]数组
	            	 responseContents= IOUtil.entityToBytes(httpResponse.getEntity(), arrayPool);
	             }else{
	            	 //如果为空，这就是一个没有返回实体content内容的request
	            	 responseContents=new byte[0];
	             }
	             //处理statusCode为非200的情况
	             if (statusCode < 200 || statusCode > 299) {
						throw new IOException();
	             }
	             return new NetworkResponse(statusCode, responseContents, responseHeaders, false);
			//各种异常的形态处理
			} catch (SocketTimeoutException e) {
				attemptRetryOnException("socket", request, new BeautyExecption(new SocketTimeoutException("socket timeout")));
				//throw new BeautyExecption(new SocketTimeoutException("socket timeout"));
			}catch (ConnectTimeoutException e){
				 throw new BeautyExecption(new SocketTimeoutException("socket timeout"));
			}catch (MalformedURLException e) {
                throw new RuntimeException("Bad URL " + request.getUrl(), e);
            }catch(IOException e){
				int statusCode = 0;
				NetworkResponse networkResponse = null;
				if (httpResponse != null) {
					statusCode = httpResponse.getStatusLine().getStatusCode();
				} else {
					throw new BeautyExecption("NoConnection error", e);
				}
				BeautyLog.debug("Unexpected response code %d for %s", statusCode, request.getUrl());
				if (responseContents != null) {
					networkResponse = new NetworkResponse(statusCode, responseContents, responseHeaders, false);
					if (statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN) {
						throw new BeautyExecption("auth error");
					} else {
						throw new BeautyExecption("server error, Only throw ServerError for 5xx status codes.", networkResponse);
					}
				} else {
					throw new BeautyExecption(networkResponse);
				}
            }
		}
		
		
	}

	

	/**
	 * 把Header[]转成map集合
	 * @param allHeaders
	 * @return
	 */
	private  Map<String, String> convertHeaders(Header[] headers) {
		Map<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < headers.length; i++) {
            result.put(headers[i].getName(), headers[i].getValue());
        }
        return result;
	}

	/**
	 * 拿到statusCode
	 * @param httpResponse
	 */
	private  int getStatusCode(HttpResponse httpResponse) {
		return httpResponse.getStatusLine().getStatusCode();
	}

	/**
	 * 添加缓存头部信息
	 * @param headers
	 * @param entry
	 */
	private void addCacheHeaders(Map<String, String> headers, BeautyImplCache.Entry entry) {
		// 如果没有缓存实体的header,直接return,停止
		if (entry == null) {
			return;
		}
		//缓存tag
		if (entry.etag != null) {
			headers.put("If-None-Match", entry.etag);
		}
		//缓存响应时间
		if (entry.serverDate > 0) {
			Date refTime = new Date(entry.serverDate);
			headers.put("If-Modified-Since", DateUtils.formatDate(refTime));
		}
	}
	
	
	 private static void attemptRetryOnException(String logPrefix, BeautyRequest request,
	            BeautyExecption exception) throws BeautyExecption {
	        BeautyDefaultRetryPolicy retryPolicy = request.getRetryPolicy();
	        int oldTimeout = request.getTimeoutMs();
	        try {
	            retryPolicy.retry(exception);
	        } catch (BeautyExecption e) {
	            throw e;
	        }
	    }

}
