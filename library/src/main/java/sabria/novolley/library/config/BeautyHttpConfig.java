package sabria.novolley.library.config;

/**
 * 这里关于的都是基本的网络请求的一些配置信息
 * @author 伟
 *
 */
public class BeautyHttpConfig {
	
	
	// HTTP-RequestMethod
	public interface HttpMethod {
        int GET = 0;
        int POST = 1;
    }
	//开启debug
	public static boolean DEBUG = true;
	//默认编码
	public static final String DEFAULT_ENCODING = "UTF-8";
	//Content-Type文件拓展名
	public final static String HEADER_CONTENT_TYPE = "Content-Type";
	//获取手机的CPU数目
	public static int DEFAULT_CORE_NUMS = Runtime.getRuntime().availableProcessors() + 1;
	//开启(CPU数目+1)个NetWorkThread
	public static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = DEFAULT_CORE_NUMS;
	
	//ByteArrayPool字节数组池中的byte[]大小
	public static int DEFAULT_BYTEARRAYPOOL_SIZE = 4096;
	//缓存目录名
	public static String DEFAULT_CACHE_DIR="beauty";
	
	//重试-超时5.0秒后算请求超时，重试之
	public static final int DEFAULT_TIMEOUT_MS = 5000;
	//重试-次数
	public static final int DEFAULT_MAX_RETRIES = 1;
	//超时时间倍数:backoff基于倍数型概念处理：第一次是5000，然后重试一次TIMEOUT是10000.
	 public static final float DEFAULT_BACKOFF_MULT = 1f;
	 
	 //在HTTP请求中，如果服务器也声明了对缓存时间的控制，那么是否优先使用服务器设置: 默认false
	public static boolean useServerControl = false;
	 //如果不使用服务器的缓存，我们定义的缓存URL有效时间:5分钟
    public static int cacheTime = 5;
}
