package sabria.novolley.library.response;

import org.apache.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * NetworkResponse 是 Response的上一层级 封装了网络请求响应的 StatusCode，Headers 和 Body Request的
 * parseNetworkResponse(…) 方法入参 将其会转发成为response
 * 
 * @author 伟
 * 
 */
public class NetworkResponse {
	
	/** HTTP的状态码*/
    public final int statusCode;

    /** response的cacheEntity数据 */
    public final byte[] data;

    /** Response请求头 */
    public final Map<String, String> headers;

    /** 判断是否返回304证明服务端未修改过内容 true表示就是返回的是304 statusCodeTrue if the server returned a 304 (Not Modified). */
    public final boolean notModified;

	public NetworkResponse(byte[] data) {
		this(HttpStatus.SC_OK, data, Collections.<String, String> emptyMap(), false);
	}

	public NetworkResponse(byte[] data, Map<String, String> headers) {
		this(HttpStatus.SC_OK, data, headers, false);
	}

	public NetworkResponse(int statusCode, byte[] data, Map<String, String> headers, boolean notModified) {
		this.statusCode = statusCode;
		this.data = data;
		this.headers = headers;
		this.notModified = notModified;
	}



}
