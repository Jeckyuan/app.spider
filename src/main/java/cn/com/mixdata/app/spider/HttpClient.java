package cn.com.mixdata.app.spider;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * HttpClient工具类
 *
 * @return
 */
public class HttpClient {

	static final int timeOut = 2 * 15 * 1000;
	private static String ip = "";
	private static int port = 0;

	private static CloseableHttpClient httpClient = null;
	private final static Object syncLock = new Object();
	public static Logger logger = Logger.getLogger(HttpClient.class);

	private static void config(HttpRequestBase httpRequestBase) {
		// 设置Header等
		httpRequestBase.setHeader("User-Agent", "Mozilla/5.0");
		// httpRequestBase.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		// httpRequestBase.setHeader("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");// "en-US,en;q=0.5");
		// httpRequestBase.setHeader("Accept-Charset",  "ISO-8859-1,utf-8,gbk,gb2312;q=0.7,*;q=0.7");
		String ips=PropertiesReader.getProperties().getProperty("ip");
		String ports=PropertiesReader.getProperties().getProperty("port");
		if(ips != null && ports != null){
			ip = ips;
			port = Integer.parseInt(ports);
		}

		if(!ip.equals("") && port!=0){
			//			HttpHost proxy = new HttpHost("10.1.2.2", 21);
			//			HttpHost proxy = new HttpHost("10.1.3.110", 21);
			//		HttpHost proxy = new HttpHost("0.0.0.0", 21);
			HttpHost proxy = new HttpHost(ip, port);
			// 配置请求的超时设置
			RequestConfig requestConfig = RequestConfig.custom()
					.setConnectionRequestTimeout(timeOut)
					.setConnectTimeout(timeOut)
					.setSocketTimeout(timeOut)
					.setProxy(proxy)
					.build();
			httpRequestBase.setConfig(requestConfig);
			//			logger.info("爬虫代理已设置为 "+ip+":"+port);
		}else{
			RequestConfig requestConfig = RequestConfig.custom()
					.setConnectionRequestTimeout(timeOut)
					.setConnectTimeout(timeOut).setSocketTimeout(timeOut).build();
			httpRequestBase.setConfig(requestConfig);
			//			logger.info("爬虫代理未设置 ");
		}

	}

	/**
	 * 获取HttpClient对象
	 *
	 * @return
	 * @create 2015年12月18日
	 */
	public static CloseableHttpClient getHttpClient(String url) {
		String hostname = url.split("/")[2];
		int port = 80;
		if (hostname.contains(":")) {
			String[] arr = hostname.split(":");
			hostname = arr[0];
			port = Integer.parseInt(arr[1]);
		}
		if (httpClient == null) {
			synchronized (syncLock) {
				if (httpClient == null) {
					httpClient = createHttpClient(200, 40, 100, hostname, port);
				}
			}
		}
		return httpClient;
	}

	/**
	 * 创建HttpClient对象
	 *
	 * @return
	 * @create 2015年12月18日
	 */
	public static CloseableHttpClient createHttpClient(int maxTotal,
			int maxPerRoute, int maxRoute, String hostname, int port) {
		//连接池设置
		ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
		LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
		Registry<ConnectionSocketFactory> registry = RegistryBuilder
				.<ConnectionSocketFactory> create()
				.register("http", plainsf)
				.register("https", sslsf)
				.build();
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
		// 将最大连接数增加
		cm.setMaxTotal(maxTotal);
		// 将每个路由基础的连接增加
		cm.setDefaultMaxPerRoute(maxPerRoute);
		//		HttpHost httpHost = new HttpHost(hostname, port);
		// 将目标主机的最大连接数增加
		//		cm.setMaxPerRoute(new HttpRoute(httpHost), maxRoute);

		// 请求重试处理
		HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
			@Override
			public boolean retryRequest(IOException exception,
					int executionCount, HttpContext context) {
				if (executionCount >= 5) {// 如果已经重试了5次，就放弃
					return false;
				}
				if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
					return true;
				}
				if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
					return false;
				}
				if (exception instanceof InterruptedIOException) {// 超时
					return false;
				}
				if (exception instanceof UnknownHostException) {// 目标服务器不可达
					return false;
				}
				if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
					return false;
				}
				if (exception instanceof SSLException) {// SSL握手异常
					return false;
				}

				HttpClientContext clientContext = HttpClientContext
						.adapt(context);
				HttpRequest request = clientContext.getRequest();
				// 如果请求是幂等的，就再次尝试
				// treat those request methods defined as idempotent by RFC-2616 as safe to retry automatically:
				//GET, HEAD, PUT, DELETE, OPTIONS, and TRACE
				if (!(request instanceof HttpEntityEnclosingRequest)) {
					return true;
				}
				return false;
			}
		};

		//设置重定向处理方式，限制重定向
		RedirectStrategy redirectStrategy = new RedirectStrategy() {
			@Override
			public boolean isRedirected(HttpRequest arg0,
					HttpResponse arg1, HttpContext arg2)
							throws ProtocolException {

				return false;
			}
			@Override
			public HttpUriRequest getRedirect(HttpRequest arg0,
					HttpResponse arg1, HttpContext arg2)
							throws ProtocolException {

				return null;
			}
		};

		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(cm)
				.setRetryHandler(httpRequestRetryHandler)
				.setRedirectStrategy(redirectStrategy)
				.build();

		return httpClient;
	}

	private static void setPostParams(HttpPost httpost,
			Map<String, Object> params) {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		Set<String> keySet = params.keySet();
		for (String key : keySet) {
			nvps.add(new BasicNameValuePair(key, params.get(key).toString()));
		}
		try {
			httpost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * POST请求URL获取内容
	 *
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static String post(String url, Map<String, Object> params) throws Exception {
		HttpPost httppost = new HttpPost(url);
		config(httppost);
		setPostParams(httppost, params);
		CloseableHttpResponse response = null;
		try {
			response = getHttpClient(url).execute(httppost, HttpClientContext.create());
			HttpEntity entity = response.getEntity();
			String result = EntityUtils.toString(entity, "utf-8");
			EntityUtils.consume(entity);
			return result;
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (response != null)
					response.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * GET请求URL获取内容
	 *
	 * @param url
	 * @return
	 */
	public static String[] get(String url) {
		String[] rs =  new String[2];
		HttpGet httpget = new HttpGet(url);
		config(httpget);
		CloseableHttpResponse response = null;
		try {
			response = getHttpClient(url).execute(httpget, HttpClientContext.create());
			String status = ""+response.getStatusLine().getStatusCode();
			rs[0] = status;
			HttpEntity entity = response.getEntity();
			//            String result = EntityUtils.toString(entity, "gb2312");
			String result = EntityUtils.toString(entity, "utf-8");
			rs[1] = result;
			EntityUtils.consume(entity);
			return rs;
		} catch (IOException e) {
			logger.error("Connection error: "+url);
			logger.error(e);
		} finally {
			try {
				if (response != null)
					response.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}
		return rs;
	}

	public static void main(String[] args) {
		// URL列表数组
		String[] urisToGet = {
				"http://mp.weixin.qq.com/s?__biz=MzI2NzMxOTY4NA==&mid=2247484446&idx=1&sn=8d3c4865fdac5c9487092c45c94648b0&chksm=ea81e4f5ddf66de3a90e08d0d2864cc0125478f49835307e8893b827b9044a840cc8696af7a6&mpshare=1&scene=1&srcid$",
				"http://mp.weixin.qq.com/s?__biz=MjM5Njc3NDQ0Mg==&mid=2667120999&idx=1&sn=437783a5ba0df5a01af3fa21ae0f6495&chksm=bde9be228a9e373417cee08ef3c326da94239f030e54ffa5d23259468761a187218e7e3618a4&mpshare=1&scene=2&srcid=1113GHaI6kK39slJwAmpbYLZ&from=timeline&isappinstalled=0&key=cde9f53f8128acbd67acdd59e62a2cf07610cab760a2508b2ee8e86d4e377502aaee30cd014f1372818d0db90eceece9&ascene=2&uin=MTcxNzQ5MzUwMg%3D%3D&devicetype=android-22&version=2603163b&nettype=cmnet&pass_ticket=EJ5V9DwW2jmPpf14tUG6NDxN$a",
				"http://mp.weixin.qq.com/s?__biz=MzA3Njg2ODcxNw==&mid=2671748390&idx=3&sn=a91b7f0b1f05773e50fae80277db2c2d&chksm=8591b824b2e631322361a7b5e56f7307df572bf36071c1452b933f382f52208eecffc77b050e&mpshare=1&scene=2&srcid=1024W5sG55YEURTGDX9fLmuQ&from=timeline&isappinstalled=0&key=c3acc508db720376b569ae8eef0acac045bfa0176823ca92962a14082207ac8024287f0b43e2335c2332cd781dd5358f&ascene=2&uin=MjU4MjY0MzExMA%3D%3D&devicetype=android-19&version=26031933&nettype=cmnet&pass_ticket=jET0vPkpG79xPO6CRylxn%2Fg%ac",
		"http://mp.weixin.qq.com/s?__biz=MzI4NDE3Nzc2OA==&mid=2650114934&idx=1&sn=d4f1788633caf17dcfd2102d2c61b478&chksm=f3fe4f71c489c6672369ae70b03ea3310df6cae2f0c76308cd3f7dff9b1297ab440c83610232&mpshare=1&scene=1&srcid=10291zHaTjkTyLQkyhqj7Mwu&from=groupmessage&isappinstalled=0&key=cde9f53f8128acbde0df0076da5ccbba394081838ceaee964530be2e994afcc063ab76e37621521f164f0989ff31312b&ascene=1&uin=MjIxMTcxMTMyMQ%3D%3D&devicetype=android-23&version=26031b31&nettype=3gnet&pass_ticket=IisGERHk%2BkG21%2Fci%abc" };

		long start = System.currentTimeMillis();
		try {
			int pagecount = urisToGet.length;
			ExecutorService executors = Executors.newFixedThreadPool(pagecount);
			CountDownLatch countDownLatch = new CountDownLatch(pagecount);
			for (int i = 0; i < pagecount; i++) {
				//            	String ul = URLEncoder.encode(urisToGet[i]);
				HttpGet httpget = new HttpGet(urisToGet[i].replaceAll("%.?$", ""));
				//            	HttpGet httpget = new HttpGet(ul);
				config(httpget);
				// 启动线程抓取
				executors.execute(new GetRunnable(urisToGet[i], countDownLatch));
			}
			countDownLatch.await();
			executors.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			System.out.println("线程" + Thread.currentThread().getName() + ","
					+ System.currentTimeMillis() + ", 所有线程已完成，开始进入下一步！");
		}

		long end = System.currentTimeMillis();
		System.out.println("consume -> " + (end - start));
	}

	static class GetRunnable implements Runnable {
		private CountDownLatch countDownLatch;
		private String url;

		public GetRunnable(String url, CountDownLatch countDownLatch) {
			this.url = url;
			this.countDownLatch = countDownLatch;
		}

		@Override
		public void run() {
			try {
				System.out.println(HttpClient.get(url));
			} finally {
				countDownLatch.countDown();
			}
		}
	}
}