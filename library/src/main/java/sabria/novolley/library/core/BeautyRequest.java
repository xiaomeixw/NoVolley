package sabria.novolley.library.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import sabria.novolley.library.config.BeautyHttpConfig;
import sabria.novolley.library.implement.BeautyDefaultRetryPolicy;
import sabria.novolley.library.inter.BeautyCacheInterface;
import sabria.novolley.library.inter.BeautyCallBack;
import sabria.novolley.library.queue.BeautyRequestQueue;
import sabria.novolley.library.response.BeautyResponse;
import sabria.novolley.library.response.NetworkResponse;
import sabria.novolley.library.util.BeautyExecption;
import sabria.novolley.library.util.IOUtil;


/**
 * 请求Request
 * 
 * 注意如下调用：
 * 1.请求头设置：重写getHeaders方法
 * //设置字符集为UTF-8,并采用gzip压缩传输
 * @Override  
   public Map<String, String> getHeaders() throws AuthFailureError{  
		Map<String, String> headers = new HashMap<String, String>();  
		headers.put("Charset", "UTF-8");  
		headers.put("Content-Type", "application/x-javascript");  
		headers.put("Accept-Encoding", "gzip,deflate");  
		return headers;  
   }  
   
   2.超时设置：重写getRetryPolicy方法
   
   @Override  
   public RetryPolicy getRetryPolicy()  {  
		RetryPolicy retryPolicy = new DefaultRetryPolicy(SOCKET_TIMEOUT, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);  
		return retryPolicy;  
   }  
   
   3.请求参数组装：重写getBody方法
   
   @Override  
   public byte[] getBody() throws AuthFailureError  {  
		return params == null ? super.getBody() : params.getBytes();  
   }  
   
   
 * 
 * 
 * 
 * @author 伟
 * 
 */
public class BeautyRequest implements Comparable<BeautyRequest> {

	// **********************************
	// 全部变量
	// **********************************
	// 所采用的Http-Mothod
	private final int mMethod;
	// URL地址
	private final String mUrl;
	// 实现的CallBack
	private BeautyCallBack mCallback;
	// post请求的参数
	private HashMap<String, String> mParams;

	// 请求Queue
	private BeautyRequestQueue mRequestQueue;

	// request的序列号
	private Integer mSequence;
	
	//请求重试策略
    private BeautyDefaultRetryPolicy mRetryPolicy;
	
	/**
	 * request优先级
	 */
	public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        IMMEDIATE
    }

	/**
	 * 控制参数变量
	 */
	// 是否启用缓存thread
	private Boolean mShouldCache = true;
	// 请求是否被取消
	private boolean mCanceled = false;
	// 请求是否已经被分发过了
	private boolean mResponseDelivered = false;
	// 本次请求的tag
	private Object mTag;
	// 缓存实体Entry
	private BeautyCacheInterface.Entry mCacheEntry = null;

	// **********************************
	// 构造函数
	// 构造函数中的四个参数向下继续传递的方式是通过request暴露出去的get方法来处理的
	// **********************************
	public BeautyRequest(int mMethod, String mUrl, BeautyCallBack mCallback) {
		this(mMethod, mUrl, null, mCallback);
	}
	
	public BeautyRequest(int mMethod, String mUrl, HashMap<String, String> mParams, BeautyCallBack mCallback) {
		this.mMethod = mMethod;
		this.mUrl = mUrl;
		this.mParams = mParams;
		this.mCallback = mCallback;
		setRetryPolicy(new BeautyDefaultRetryPolicy());
	}

	// **********************************
	// 对外暴露的方法
	// **********************************
	/**
	 * 获取所调用为何种方法
	 * 
	 * @return
	 */
	public int getmMethod() {
		return mMethod;
	}
	
	/**
	 * 获取post的请求参数
	 * 只有post请求才有
	 * 然后该参数会在 实现的stack中获取得到并拼接变成post参数
	 * 
	 * @return
	 */
	public HashMap<String,String> getmParams(){
		return mParams;
	}
	
	/**
	 * stack的子类从这里获取post请求的参数params
	 * @return 如果用户传递的params为null,那么会一直继续传递下去
	 */
	public byte[] getBody(){
		return IOUtil.assemblePostParam(getmParams());
	}
	
	/**
	 * 设置请求Content-Type文件拓展名
	 * application/x-www-form-urlencoded:窗体数据被编码为名称/值对
	 * @return
	 */
	public String getBodyContentType() {
        return "application/x-www-form-urlencoded; charset="+ BeautyHttpConfig.DEFAULT_ENCODING;
    }
	
	
	/**
	 * 请求头的重写
	 * @return
	 */
	public Map<String, String> getHeaders(){
        return Collections.emptyMap();
    }
	
	

	/**
	 * 获取url地址
	 * 
	 * @return url
	 */
	public String getUrl() {
		return mUrl;
	}
	
	/**
	 * 获取请求的callback回调
	 * @return
	 */
	public BeautyCallBack getCallback(){
		return mCallback;
	}

	/**
	 * 设置tag,作为取消请求的tag标识
	 * 
	 * @param tag
	 */
	public void setTag(Object tag) {
		mTag = tag;
	}

	public Object getTag() {
		return mTag;
	}
	
	/**
	 * 绑定request和requestQueue
	 * 
	 * @param requestQueue
	 */
	public void setRequestQueue(BeautyRequestQueue requestQueue) {
		mRequestQueue=requestQueue;
	}
	
	

	// **********************************
	// 请求结束了,不管请求是否成功或者失败
	// **********************************
	public void finish() {
		if(mRequestQueue !=null){
			mRequestQueue.finish(this);
		}
	}
	
	/**
	 * 给Request设置序列号
	 * @param sequenceNumber
	 */
	public void setSequence(int sequenceNumber) {
		mSequence=sequenceNumber;
	}
	public final int getSequence() {
        if (mSequence == null) {
            throw new IllegalStateException("getSequence called before setSequence");
        }
        return mSequence;
    }
	
	
	/**
	 * request中的cache-key
	 * 用url作为cachekey是最好的方案
	 */
	public String getCacheKey() {
		return getUrl();
	}
	
	
	/**
	 * 设置缓存实体
	 * 
	 * @param entry
	 */
	public void setCacheEntry(BeautyCacheInterface.Entry entry) {
		mCacheEntry=entry;
	}
	public BeautyCacheInterface.Entry getCacheEntry() {
        return mCacheEntry;
    }
	
	
	/**
	 * 设置请求为canceled取消
	 * 这样no callback才执行
	 */
	public void cancel() {
		mCanceled=true;
	}
	/**
	 * 请求是否已经取消了
	 * @return true 已经取消了
	 */
	public boolean isCanceled() {
        return mCanceled;
    }
	
	
	/**
	 * 是否启用缓存Thread
	 * @return
	 */
	public boolean shouldCache() {
		return mShouldCache;
	}
	
	public final void setShouldCache(boolean shouldCache) {
	     mShouldCache = shouldCache;
	}
	
	
	 public Priority getPriority() {
	      return Priority.NORMAL;
	 }
	
	
	/**
	 * 标记已经分发过了
	 */
	 public void markDelivered() {
	     mResponseDelivered = true;
	 }
	
	/**
	 * 请求是否已经有响应传输
	 * 
	 * @return true 表示这个请求request已经有响应response进行分发delivered了
	 */
	public boolean hasHadResponseDelivered() {
		return mResponseDelivered;
	}
	
	
	// 实现Comparable
		/**
	     * 用于线程优先级排序
	     */
		@Override
		public int compareTo(BeautyRequest other) {
			Priority left = this.getPriority();
	        Priority right = other.getPriority();
	        return left == right ? this.mSequence - other.mSequence : right
	                .ordinal() - left.ordinal();
		}
	
	
	
	
	
	
	//************************************
	// 该模块中核心的方法
	//*************************************

	/**
	 * 解析json 注意这里子类必须覆盖重写
	 * 
	 * @param networkResponse
	 */
	public BeautyResponse parseNetworkResponse(NetworkResponse networkResponse) {
		//TODO 这里没有做逻辑处理，导致返回null
		
		/*String parsed;
        try {
            parsed = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(networkResponse.data);
        }
        return BeautyResponse.success(parsed, HttpHeaderParser.parseCacheHeaders(networkResponse));
	        */
		
		return BeautyResponse.success(networkResponse.data,null);
		

	}


	/**
	 * 两个分发
	 * 外部通过request.的方法调用
	 */
	public void deliverSuccessResponse(BeautyResponse response){
		if(mCallback != null){
			mCallback.onSuccess(response.result);
			
		}
	}

	public void deliverError(BeautyExecption error) {
        if (mCallback != null) {
            int errorNo;
            String strMsg;
            if (error != null) {
                if (error.networkResponse != null) {
                	//我在每个error中其实都把NetworkResponse都放进去了,所以可以直接从里面拿到有关StatusCode信息
                    errorNo = error.networkResponse.statusCode;
                } else {
                    errorNo = -1;
                }
                //每一个请求我都用了一段string
                strMsg = error.getMessage();
            } else {
                errorNo = -1;
                strMsg = "unknow";
            }
            mCallback.onFail(errorNo, strMsg);
        }
    }
	
	/**
	 * Http请求完成(不论成功失败)
	 * 我做回调，来起到关闭progress的功能
	 */
	public void requestFinish() {
        mCallback.onSufFinish();
    }

	/**
	 * 异常
	 * @param error
	 * @return
	 */
	public BeautyExecption parseNetworkError(BeautyExecption error) {
		return error;
	}

	/**
	 * 重试策略的处理
	 */
	 public BeautyDefaultRetryPolicy getRetryPolicy() {
	      return mRetryPolicy;
	 }
	 public void setRetryPolicy(BeautyDefaultRetryPolicy retryPolicy) {
	      mRetryPolicy = retryPolicy;
	 }
	 public final int getTimeoutMs() {
	      return mRetryPolicy.getCurrentTimeout();
	 }

}
