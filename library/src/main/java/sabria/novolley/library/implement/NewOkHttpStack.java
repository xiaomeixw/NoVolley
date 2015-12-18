package sabria.novolley.library.implement;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;



/**
 * 基于使用okhttp-urlconnection 来实现
 * @author 伟
 *
 */
public class NewOkHttpStack extends HurlStack {
	private final OkUrlFactory mFactory;

	public NewOkHttpStack() {
		this(new OkHttpClient());
	}

	public NewOkHttpStack(OkHttpClient client) {
		if (client == null) {
			throw new NullPointerException("Client must not be null.");
		}
		mFactory = new OkUrlFactory(client);
	}

	@Override
	protected HttpURLConnection createConnection(URL url) throws IOException {
		return mFactory.open(url);
	}
}