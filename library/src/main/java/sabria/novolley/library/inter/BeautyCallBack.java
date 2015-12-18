package sabria.novolley.library.inter;

/**
 * 定义四个必须的回调 onSuccess 和 onFailure 是必须实现的 onPreStart 和 onFinish 为覆盖重写
 * 
 * @author 伟
 * 
 */
public abstract class BeautyCallBack {

	/**
	 * 请求成功,特指200
	 * 它的回调实在deliverResponse分发中完成
	 * @param data
	 */
	public abstract void onSuccess(byte[] data);

	/**
	 * 它的回调实在分发中完成
	 * @param statusCode
	 * @param messsage
	 */
	public abstract void onFail(int statusCode, String messsage);

	/**
	 * 开启progress
	 * 它的回调地址是在addRequest操作时最先执行
	 */
	public void onPreStart() {
	}
	/**
	 * 关闭progress
	 * 它的回调实在分发中完成
	 */
	public void onSufFinish() {

	}

}
