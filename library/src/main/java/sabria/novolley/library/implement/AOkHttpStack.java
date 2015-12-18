package sabria.novolley.library.implement;

import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 使用的是旧版本的OKHttp-1.2版本,open方法已经失效了
 * @author 伟
 *
 */
public class AOkHttpStack extends HurlStack {
	
	
	
	private final OkHttpClient client;

	public AOkHttpStack() {
		this(new OkHttpClient());
	}

	public AOkHttpStack(OkHttpClient client) {
		if (client == null) {
			throw new NullPointerException("Client must not be null.");
		}
		this.client = client;
	}

	@Override
	protected HttpURLConnection createConnection(URL url) throws IOException {
		return null;
		//return client.open(url);
	}
}
