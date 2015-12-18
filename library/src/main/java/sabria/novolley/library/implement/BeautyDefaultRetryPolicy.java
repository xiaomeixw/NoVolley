package sabria.novolley.library.implement;


import sabria.novolley.library.config.BeautyHttpConfig;
import sabria.novolley.library.inter.BeautyRetryPolicyInter;
import sabria.novolley.library.util.BeautyExecption;

/**
 * 设置的自定义的重试策略
 *  retry(…) 函数中判断重试次数是否达到上限确定是否继续重试。
 * 
 * @author 伟
 * 
 */
public class BeautyDefaultRetryPolicy implements BeautyRetryPolicyInter {

	/**
	 * 表示当前重试的 timeout 时间，会以mBackoffMultiplier作为因子累计前几次重试的 timeout
	 */
	private int mCurrentTimeoutMs;
	/**
	 * 已经重试次数
	 */
	private int mCurrentRetryCount;
	/**
	 * 每次重试之前的 timeout 该乘以的因子
	 */
	private final float mBackoffMultiplier;
	
	//设定一个最大的重试count
	private final int mMaxNumRetries;

	public BeautyDefaultRetryPolicy() {
		this(BeautyHttpConfig.DEFAULT_TIMEOUT_MS, BeautyHttpConfig.DEFAULT_MAX_RETRIES, BeautyHttpConfig.DEFAULT_BACKOFF_MULT);
	}

	/**
	 * 
	 * @param initialTimeoutMs
	 *            多少时间后属于超时
	 * @param maxNumRetries
	 *            从外部指定的最大重试次数
	 * @param backoffMultiplier
	 *            重试间隔时常倍数 backoff基于倍数型概念处理：第一次是2500，然后重试一次TIMEOUT是5000
	 */
	public BeautyDefaultRetryPolicy(int initialTimeoutMs, int maxNumRetries, float backoffMultiplier) {
		this.mCurrentTimeoutMs=initialTimeoutMs;
		this.mMaxNumRetries=maxNumRetries;
		this.mBackoffMultiplier=backoffMultiplier;
	}
	
	@Override
	public int getCurrentTimeout() {
		return mCurrentTimeoutMs;
	}
	
	@Override
	public int getCurrentRetryCount() {
		return mCurrentRetryCount;
	}
	
	@Override
	public void retry(BeautyExecption error) throws BeautyExecption {
		
		mCurrentRetryCount++;
		mCurrentTimeoutMs+=(mCurrentTimeoutMs*mBackoffMultiplier);
		if(!hasAttemptRemaining()){
			throw error;
		}
		
	}
	
	/**
	 * 是否要继续
	 * @return true 尝试剩余
	 */
	protected boolean hasAttemptRemaining() {
        return mCurrentRetryCount <= mMaxNumRetries;
    }
	
	
	

}
