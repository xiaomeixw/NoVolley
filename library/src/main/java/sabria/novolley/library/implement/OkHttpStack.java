package sabria.novolley.library.implement;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request.Builder;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import sabria.novolley.library.config.BeautyHttpConfig;
import sabria.novolley.library.core.BeautyRequest;
import sabria.novolley.library.inter.BeautyHttpStackInter;


/**
 * 基于OKhttpStack
 * 
 * @author 伟
 * 
 */
public class OkHttpStack implements BeautyHttpStackInter {
	
	
	 private final OkHttpClient mClient;
	
	
	public OkHttpStack(OkHttpClient mClient) {
		this.mClient=mClient;
    }

	/**
	 * 发送请求
	 */
	@Override
	public HttpResponse executeRequest(BeautyRequest request, Map<String, String> cacheHeaders) throws IOException {
		// 1.创建build
		Builder builder = setBuild(request);

		// 2.处理请求头
		setHead(request, cacheHeaders,builder);
		
		// 3.处理post请求的参数
		setParams(request, cacheHeaders,builder);
		
		// 4.发动请求
		Response okhttpResponse = setExecute(builder);

		return null;
		//return okhttpResponse;
	}

	
	

	/**
	 * 创建builder
	 * @param request
	 * @return
	 */
	private Builder setBuild(BeautyRequest request) {
		Builder builder = new Builder();
		builder.url(request.getUrl());
		return builder;
	}

	/**
	 * 设置OKhttp的请求头
	 * 
	 * @param request
	 * @param cacheHeaders
	 */
	private static void setHead(BeautyRequest request, Map<String, String> cacheHeaders,Builder builder) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.putAll(cacheHeaders);
		map.putAll(request.getHeaders());

		for (String headerName : map.keySet()) {
			//for迭代调用builder.header(key,value)加入header参数
			builder.header(headerName, map.get(headerName));
		}
		
	}
	
	
	/**
	 * 设置post参数
	 * @param request
	 * @param cacheHeaders
	 * @param builder
	 */
	private void setParams(BeautyRequest request, Map<String, String> cacheHeaders, Builder builder) {
		switch (request.getmMethod()) {
		case BeautyHttpConfig.HttpMethod.GET:
			builder.get();
			break;
			
		case BeautyHttpConfig.HttpMethod.POST:
			byte[] body = request.getBody();
			if(body!=null){
				builder.post(null);
			}else{
				//这里必须关联okio-lib包
				builder.post(RequestBody.create(MediaType.parse(request.getBodyContentType()), body));
			}
			
			break;

		default:
			 throw new IllegalStateException("Unknown method type.");
		}
	}
	
	
	/**
	 * 执行
	 * @param builder
	 * @return 
	 */
	private Response setExecute(Builder builder) throws IOException{
		if(null!=mClient){
			Response okhttpResponse = mClient.newCall(builder.build()).execute();
			int responseCode = okhttpResponse.code();
			if(responseCode==-1){
				throw new IOException("Could not retrieve response code from HttpUrlConnection.");
			}
			return okhttpResponse;
		}
		return null;
	}

}
