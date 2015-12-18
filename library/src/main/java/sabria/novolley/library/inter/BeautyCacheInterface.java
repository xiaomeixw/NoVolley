package sabria.novolley.library.inter;

import java.util.Collections;
import java.util.Map;



/**
 * 缓存接口，代表了一个可以获取请求结果，存储请求结果的缓存。
 * 实体表示缓存的内容
 * @author xiongwei
 *
 */
public interface BeautyCacheInterface {

	//*************************************
	//通用型方法
	//*************************************
	
	/**
	 * 初始化缓存
	 */
	public void initialize();
	/**
	 * 存入一个请求的缓存实体
	 * @param key
	 * @param entry
	 */
	public void put(String key, Entry entry);
	/**
	 * 通过 key 获取请求的缓存实体
	 * @param key
	 * @return
	 */
	public Entry get(String key);
	/**
	 * 移除指定的缓存实体
	 * @param key
	 */
	public void remove(String key);
	/**
	 * 清空缓存
	 */
	public void clear();
	/**
	 * 让一个缓存过期
	 * @param key
	 * @param fullExpire
	 */
	public void invalidate(String key, boolean fullExpire);
	
	
	/**
	 * 缓存实体
	 * @author 伟
	 * 请求返回的数据（Body 实体）
	 *byte[] data ：请求返回的数据（Body 实体）
	 *String etag Http ：响应首部中用于缓存新鲜度验证的 ETag
	 *long serverDate Http ：响应首部中的响应产生时间
	 *long ttl ：缓存的过期时间
	 *long softTtl ：缓存的新鲜时间
	 *Map<String, String> responseHeaders ：响应的 Headers
	 *boolean isExpired() ：判断缓存是否过期，过期缓存不能继续使用
	 *boolean refreshNeeded() ：判断缓存是否新鲜，不新鲜的缓存需要发到服务端做新鲜度的检测
	 */
	public static class Entry {
		
        public byte[] data;

        public String etag;

        public long serverDate;

        public long ttl;

        public long softTtl;

        public Map<String, String> responseHeaders = Collections.emptyMap();

        /** True if the entry is expired. */
        public boolean isExpired() {
            return this.ttl < System.currentTimeMillis();
        }

        /** True if a refresh is needed from the original data source. */
        public boolean refreshNeeded() {
            return this.softTtl < System.currentTimeMillis();
        }
		
		

	}
	
	
	

}
