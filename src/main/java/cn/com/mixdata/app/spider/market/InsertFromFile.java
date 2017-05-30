package cn.com.mixdata.app.spider.market;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

import cn.com.mixdata.app.spider.MySQLInsert;

public class InsertFromFile {

	private static Logger logger = Logger.getLogger(InsertFromFile.class);
	private static BufferedReader bufferedReader = null;

	public static void main(String[] args) {
		MySQLInsert mySQLInsert = new MySQLInsert();
		if(args.length == 1){
			//启动数据库连接
			mySQLInsert.getConn();
			String fileName = args[0];
			try {
				bufferedReader = new BufferedReader(new FileReader(fileName));
				String tmpLine = "";
				while ((tmpLine = bufferedReader.readLine()) != null){
					String[] appInfoList = tmpLine.split("\t", -1);
					if(appInfoList.length == 16){
						mySQLInsert.insertAppInfo(tmpLine);
					}else{
						logger.info("输入错误："+tmpLine);
					}
				}
			} catch (FileNotFoundException e) {
				logger.error("输入文件读取错误\n"+e);
			} catch (IOException e) {
				logger.error(e);
			}finally {
				//关闭 数据库连接
				mySQLInsert.closeConn();
			}
		}else{
			logger.info("请检查启动参数\n"
					+ "eg: java -jar cn.com.mixdata.app.spider.market.InsertFromFile 待插入到mysql的文件");
		}
	}

}
