package sabria.novolley.library.inter;

import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.Map;

import sabria.novolley.library.core.BeautyRequest;


/**
 * HttpStack 处理 HTTP 请求，返回请求结果。
 * 
 * @author 伟
 * 
 */
public interface BeautyHttpStackInter {

	/**
	 * 处理 HTTP 请求，返回请求结果
	 * 
	 * implement的类中必须完成类似(HttpClient)
	 * 			HttpPost post = new HttpPost(URL);
	 *          addHeader
	 *          addParams
	 * 			HttpClient.execute(post);
	 * 
	 * @param request 一次实际请求集合
	 * @param cacheHeaders 缓存头addCacheHeaders(headers, request.getCacheEntry());
	 * @return HttpResponse
	 * @throws IOException
	 */
	public HttpResponse executeRequest(BeautyRequest request, Map<String, String> cacheHeaders) throws IOException;

}
