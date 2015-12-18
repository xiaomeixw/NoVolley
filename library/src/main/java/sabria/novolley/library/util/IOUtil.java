package sabria.novolley.library.util;

import android.content.Context;
import android.util.Log;

import org.apache.http.HttpEntity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sabria.novolley.library.config.BeautyHttpConfig;

public class IOUtil {

	/**
	 * 将HttpEntity 装成 byte[]
	 * 
	 * @param entity
	 * @return
	 * @throws IOException
	 * @throws BeautyExecption
	 */
	public static byte[] entityToBytes(HttpEntity entity, ByteArrayPool mPool) throws IOException, BeautyExecption {
		PoolingByteArrayOutputStream bytes = new PoolingByteArrayOutputStream(mPool, (int) entity.getContentLength());
		byte[] buffer = null;
		try {
			InputStream in = entity.getContent();
			if (in == null) {
				new BeautyExecption("service-error:IOUtil-entityToBytes");
			}
			buffer = mPool.getBuf(1024);
			int count;
			while ((count = in.read(buffer)) != -1) {
				bytes.write(buffer, 0, count);
			}
			return bytes.toByteArray();
		} finally {
			try {
				// 使用consumeContent()方法关闭InputStream并且释放资源
				entity.consumeContent();
			} catch (IOException e) {
				// 继续捕consumeContent()方法执行时的错误catch
				Log.v("IOUtil-entityToBytes", "Error occured when calling consumingContent");
			}
			mPool.returnBuf(buffer);
			bytes.close();
		}
	}

	/**
	 * 初始化存储的文件目录
	 * 
	 *	通过Context.getExternalFilesDir()方法可以获取到 SDCard/Android/data/你的应用的包名/files/ 目录，一般放一些长时间保存的数据
	 *	通过Context.getExternalCacheDir()方法可以获取到 SDCard/Android/data/你的应用包名/cache/目录，一般存放临时缓存数据
     *
	 *	如果使用上面的方法，当你的应用在被用户卸载后，SDCard/Android/data/你的应用的包名/ 这个目录下的所有文件都会被删除，不会留下垃圾信息。
	 *	而且上面二个目录分别对应 设置->应用->应用详情里面的”清除数据“与”清除缓存“选项
	 * 
	 * @param context
	 */
	public static File initCachePath(Context context) {
		File cache = context.getExternalCacheDir();
		if (cache == null) {
			cache = context.getCacheDir();
		}
		File cacheDir = new File(cache, BeautyHttpConfig.DEFAULT_CACHE_DIR);
		return cacheDir;
	}

	/**
	 * 组装get方法的URL
	 * 
	 * @return
	 */
	public static String assembleGetURL(HashMap<String, String> params) {
		StringBuilder result = new StringBuilder();
		boolean isFirst = true;
		for (ConcurrentHashMap.Entry<String, String> entry : params.entrySet()) {
			if (!isFirst) {
				result.append("&");
			} else {
				result.append("?");
				isFirst = false;
			}
			result.append(entry.getKey());
			result.append("=");
			result.append(entry.getValue());
		}
		return result.toString();
	}

	/**
	 * 传入post的params-HashMap 装成 byte[]
	 * 
	 * @param params
	 * @return
	 */
	public static byte[] assemblePostParam(HashMap<String, String> params) {
		//如果参数不为null,同时参数的格式必须大于0
		if (params != null && params.size() > 0) {
			//将HashMap转成byte[]
			return encodeParameters(params, getParamsEncoding());
		}
		//继续null值传递
		return null;
	}

	/**
	 * UTF-8格式
	 * 
	 * @return
	 */
	private static String getParamsEncoding() {
		return BeautyHttpConfig.DEFAULT_ENCODING;
	}

	/**
	 * Converts <code>params</code> into an application/x-www-form-urlencoded
	 * encoded string.
	 */
	private static byte[] encodeParameters(Map<String, String> params, String paramsEncoding) {
		StringBuilder encodedParams = new StringBuilder();
		try {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
				encodedParams.append('=');
				encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
				encodedParams.append('&');
			}
			return encodedParams.toString().getBytes(paramsEncoding);
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
		}
	}

}
