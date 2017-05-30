package cn.com.mixdata.app.spider.market;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;

import cn.com.mixdata.app.spider.HttpClient;
import cn.com.mixdata.app.spider.PropertiesReader;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author Administrator
 *
 */
public class ApplicationMarkets {
	private static final Logger logger = Logger.getLogger(ApplicationMarkets.class);
	private BufferedWriter bufferedWriter = null;
	public static Properties properties = new Properties();

	/**
	 * @param bufferedWriter
	 */
	public ApplicationMarkets(BufferedWriter bufferedWriter) {
		this.bufferedWriter = bufferedWriter;
	}

	/**
	 * 360手机助手市场分类逐页爬取
	 * @param catIds
	 */
	public void  qihuShouJiZhuShou(ArrayList<String> catIds){
		long currentTime = System.currentTimeMillis();
		String urlPart1 = "http://m.app.haosou.com/category/cat_request?page=";
		String urlPart2 = "&requestType=ajax&_t=" + currentTime;
		String urlPart3 = "&cid=";
		String urlPart4 = "&csid=";
		String urlPart5 = "&order=download";
		//爬取的总数量
		int totalAppSum = 0;

		for(String id: catIds){
			String[] nameIds = id.split("_");
			String cid;
			String csid;

			if(nameIds.length == 3){
				cid = nameIds[0];
				csid = nameIds[2];
			}else{
				logger.error("Error input: " + id);
				break;
			}

			int pageId = 0;
			int catAppSum = 0;

			while(true){
				pageId++;
				String url = urlPart1 + pageId + urlPart2 + urlPart3
						+ cid + urlPart4 + csid + urlPart5;
				logger.info(url);
				String[] result = HttpClient.get(url);
				JSONArray jsonArray = JSONArray.fromObject(result[1]);

				if(jsonArray.isEmpty()){
					break;
				}

				for(int i=0; i<jsonArray.size(); i++){
					JSONObject jsonObject = jsonArray.getJSONObject(i);

					String apkId = jsonObject.getString("apkid");
					String name  = jsonObject.getString("name");
					String versionName = jsonObject.getString("version_name");

					String rating = jsonObject.getString("rating");
					String marketName = jsonObject.getString("market_name");
					String categoryName = jsonObject.getString("category_name");

					String downCount = jsonObject.getString("download_times");
					String apkSize = jsonObject.getString("size");
					String apkMd5 = "";

					String apkURL = jsonObject.getString("down_url");
					String updateDate = "";
					int os = 0;

					String osVersion = "";
					String developer = "";
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String crawlingTime = simpleDateFormat.format(new Date());

					String desc = jsonObject.getString("brief").trim();

					StringBuffer stringBuffer = new StringBuffer();

					stringBuffer.append(apkId).append("\t");
					stringBuffer.append(name).append("\t");
					stringBuffer.append(versionName).append("\t");

					stringBuffer.append(rating).append("\t");
					stringBuffer.append(marketName).append("\t");
					stringBuffer.append(categoryName).append("\t");

					stringBuffer.append(downCount).append("\t");
					stringBuffer.append(apkSize).append("\t");
					stringBuffer.append(apkMd5).append("\t");

					stringBuffer.append(apkURL).append("\t");
					stringBuffer.append(updateDate).append("\t");
					stringBuffer.append(os).append("\t");

					stringBuffer.append(osVersion).append("\t");
					stringBuffer.append(developer).append("\t");
					stringBuffer.append(crawlingTime).append("\t");

					stringBuffer.append(desc).append("\n");

					try {
						bufferedWriter.write(stringBuffer.toString());
					} catch (IOException e) {
						logger.error(e);
					}

					totalAppSum++;
					catAppSum++;
				}
			}
			logger.info(nameIds[1] + ": " + catAppSum);
		}
		logger.info("Total applications: " + totalAppSum);
	}

	/**
	 * 关闭输出流
	 */
	public void writerClose(){
		try {
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException{
		properties = PropertiesReader.getProperties();

		String categoryUrl = "http://m.app.haosou.com/category/";
		String gamesUrl =  "http://m.app.haosou.com/category/request"
				+ "?page=1&requestType=ajax&_t=1486548465699&cid=4";
		//获取待爬取的分类ID
		ArrayList<String> idList = CategoryIdParser.getCategoryIds(categoryUrl);
		idList.addAll(CategoryIdParser.getGamesIds(gamesUrl));
		logger.info("idList: \n"+idList);

		//爬取数据输出路径
		String fileName = args[0];
		BufferedWriter bfWriter = new BufferedWriter(new FileWriter(fileName));

		ApplicationMarkets appMark = new ApplicationMarkets(bfWriter);

		try{
			if(!idList.isEmpty()){
				appMark.qihuShouJiZhuShou(idList);
			}else{
				logger.error("无法获取分类ID");
			}
		}finally{
			appMark.writerClose();
		}
	}

}
