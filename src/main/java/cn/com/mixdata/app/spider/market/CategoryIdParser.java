package cn.com.mixdata.app.spider.market;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import cn.com.mixdata.app.spider.HttpClient;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class CategoryIdParser {

	private static Logger logger = Logger.getLogger(CategoryIdParser.class);

	/**
	 * 获取url的查询参数
	 * @param url
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
		Map<String, String> query_pairs = new LinkedHashMap<String, String>();
		String query = url.getQuery();
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
		}
		return query_pairs;
	}

	/**
	 * 获取360手机助手应用市场的应用分类名称和ID
	 * @param rootUrl 应用市场按类别查询入口 eg:http://m.app.haosou.com/category/
	 * @return [3_name_id, ...]
	 * @throws IOException
	 */
	public static ArrayList<String> getCategoryIds(String rootUrl) throws IOException{
		ArrayList<String> categoryIds = new ArrayList<>();

		//		Document doc = Jsoup.connect(rootUrl).get();
		String[] result = HttpClient.get(rootUrl);
		Document doc = Jsoup.parse(result[1], rootUrl);
		Elements softLists = doc.getElementsByClass("lists-soft");

		Elements categoryList = softLists.get(0).getElementsByTag("li");
		//		System.out.println("类别数量： " + categoryList.size());
		logger.info("类别数量： " + categoryList.size());

		for(int i=0; i<categoryList.size(); i++){
			//			System.out.println(categoryList.get(i).attr("abs:data-href"));
			logger.info(categoryList.get(i).attr("abs:data-href"));

			String link = categoryList.get(i).attr("abs:data-href");
			try{
				URL url = new URL(link.replaceAll("amp;", ""));
				HashMap<String, String> queryPairs = (HashMap<String, String>)splitQuery(url);
				String catNameID = "3" + "_" + queryPairs.get("cat_name") +"_"+ queryPairs.get("csid");
				//			System.out.println(catNameID);
				logger.info(catNameID);

				categoryIds.add(catNameID);
			}catch(Exception e){
				logger.error(e);
			}

		}

		return categoryIds;
	}

	/**
	 * 获取360手机助手应用市场的游戏分类名称和游戏分类ID
	 * @param rootUrl eg:http://m.app.haosou.com/category/request?page=1&requestType=ajax&_t=1486548465699&cid=4
	 * @return [4_name_id,...]
	 * @throws IOException
	 */
	public static ArrayList<String> getGamesIds(String rootUrl) throws IOException{
		ArrayList<String> gameIds = new ArrayList<>();
		//		Document doc = Jsoup.connect(rootUrl).get();
		String[] result = HttpClient.get(rootUrl);
		Document doc = Jsoup.parse(result[1], rootUrl);
		String body = doc.body().text();

		JSONArray jsonArray = JSONArray.fromObject(body);
		//		System.out.println(jsonArray.size());
		logger.info(jsonArray.size());
		for(int i=0; i<jsonArray.size(); i++){
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			String name = jsonObject.getString("name");
			String url = jsonObject.getString("url");

			HashMap<String, String> queryPairs = (HashMap<String, String>)splitQuery(new URL(url));
			String gameNameID = "4" + "_" + name +"_"+ queryPairs.get("csid");
			//			System.out.println(gameNameID);
			logger.info(gameNameID);

			gameIds.add(gameNameID);
		}

		return gameIds;
	}


	public static void main(String[] args) throws IOException{
		ArrayList<String> categoryIds = new ArrayList<>();
		//		ArrayList<String> gameIds = new ArrayList<>();

		String urlString="http://m.app.haosou.com/category/request"
				+ "?page=1&requestType=ajax&_t=1486548465699&cid=4";

		categoryIds = getGamesIds(urlString);

		System.out.println(categoryIds);
	}
}
