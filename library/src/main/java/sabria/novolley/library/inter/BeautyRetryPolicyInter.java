package sabria.novolley.library.inter;


import sabria.novolley.library.util.BeautyExecption;

/**
 * 重试策略
 * @author 伟
 *
 */
public interface BeautyRetryPolicyInter {
	
	/**
	 * 获取当前请求用时（用于Log）
	 * @return
	 */
	public  int getCurrentTimeout();
	
	/**
	 * 获取已经重试的次数（用于Log）
	 * @return
	 */
	public int getCurrentRetryCount();
	
	/**
	 * 确定是否重试，参数为这次异常的具体信息。
	 * 在请求异常时此接口会被调用，可在此函数实现中抛出传入的异常表示停止重试。
	 * @param e
	 * @throws BeautyExecption
	 */
	public void retry(BeautyExecption e) throws BeautyExecption;

}
