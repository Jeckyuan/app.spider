package cn.com.mixdata.app.spider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Logger;


public class PropertiesReader {
	private static Logger logger = Logger.getLogger(PropertiesReader.class);

	private static Properties properties = new Properties();

	public static Properties getProperties(){
		ClassLoader classLoader = PropertiesReader.class.getClassLoader();
		InputStream in = classLoader.getResourceAsStream("conf.properties");
		try {
			properties.load(in);
			in.close();
		}catch (IOException e) {
			logger.error("读取配置文件失败");
			logger.error(e);
		}
		return properties;
	}

	public static void main(String[] args){
		Properties prop = getProperties();
		Iterator<String> it=prop.stringPropertyNames().iterator();
		while(it.hasNext()){
			String key=it.next();
			System.out.println(key+":"+prop.getProperty(key));
		}

		//		System.out.println(SimpleDateFormat.getDateInstance().format(new Date()));
	}
}
