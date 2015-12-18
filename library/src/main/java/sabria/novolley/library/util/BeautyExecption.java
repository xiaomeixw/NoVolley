package sabria.novolley.library.util;


import sabria.novolley.library.response.NetworkResponse;

/**
 * 提示异常
 * @author 伟
 *
 */
public class BeautyExecption extends Exception {
	
	private static final long serialVersionUID = -3595309961894680051L;
	
	public final NetworkResponse networkResponse;

	public BeautyExecption() {
		networkResponse = null;
	}

	public BeautyExecption(NetworkResponse response) {
		networkResponse = response;
	}

	public BeautyExecption(String exceptionMessage) {
		super(exceptionMessage);
		networkResponse = null;
	}

	public BeautyExecption(String exceptionMessage, NetworkResponse response) {
		super(exceptionMessage);
		networkResponse = response;
	}

	public BeautyExecption(String exceptionMessage, Throwable reason) {
		super(exceptionMessage, reason);
		networkResponse = null;
	}

	public BeautyExecption(Throwable cause) {
		super(cause);
		networkResponse = null;
	}
}
