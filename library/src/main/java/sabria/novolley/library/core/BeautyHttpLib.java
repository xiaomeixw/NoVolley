package sabria.novolley.library.core;

import java.util.HashMap;
import java.util.Map;

import sabria.novolley.library.config.BeautyHttpConfig;
import sabria.novolley.library.inter.BeautyCallBack;
import sabria.novolley.library.util.IOUtil;


/**
 * 1.以方法命名来区分get/post 2.都是暴露params的方式，然后针对get进行url拼接 3.
 * 
 * @author 伟
 * 
 */
public class BeautyHttpLib {

	public BeautyHttpLib() {
	}

	public BeautyRequest get(String url, String requestTag, BeautyCallBack callback) {
		return get(url, null, requestTag, callback);
	}

	/**
	 * 调用get请求方法
	 * 
	 * @param url
	 * @param params
	 * @param callback
	 * @return
	 */
	public BeautyRequest get(String url, HashMap<String, String> params, String requestTag, BeautyCallBack callback) {
		// get请求拼接URL地址
		if (params != null) {
			url += IOUtil.assembleGetURL(params);
		}
		BeautyRequest request = new BeautyRequest(BeautyHttpConfig.HttpMethod.GET, url, callback);
		request.setTag(requestTag);
		doRequest(request);
		return request;
	}

	public BeautyRequest post(String url, String requestTag, BeautyCallBack callback) {
		return post(url, null, requestTag, callback);
	}

	/**
	 * 调用post请求方法
	 * 
	 * @param url
	 * @param params
	 * @param callback
	 * @return
	 */
	public BeautyRequest post(String url, HashMap<String, String> params, String requestTag, BeautyCallBack callback) {
		// get请求拼接URL地址
		BeautyRequest request = new BeautyRequest(BeautyHttpConfig.HttpMethod.POST, url, params, callback);
		request.setTag(requestTag);
		doRequest(request);
		return request;
	}

	/**
	 * 带用content-type
	 * 
	 * @return
	 */
	public BeautyRequest post(String url, HashMap<String, String> params, final HashMap<String, String> heads, final String contentType, String requestTag, BeautyCallBack callback) {

		BeautyRequest request = new BeautyRequest(BeautyHttpConfig.HttpMethod.POST, url, params, callback) {

			/**
			 * 重写以实现对contentType的支持
			 */
			@Override
			public String getBodyContentType() {
				if (contentType != null) {
					return contentType;
				}
				return super.getBodyContentType();
			}

			/**
			 * 重写以实现对headers的支持
			 */
			@Override
			public Map<String, String> getHeaders() {
				if (heads != null) {
					return heads;
				}
				return super.getHeaders();
			}
		};

		request.setTag(requestTag);
		doRequest(request);

		return request;
	}

	/**
	 * 加入队列
	 * 
	 * @param request
	 */
	private void doRequest(BeautyRequest request) {
		BeautyHttpInit.getInstance().getRequestQueue().addRequest(request);
	}

}
