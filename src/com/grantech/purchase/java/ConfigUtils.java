package com.grantech.purchase.java;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * ConfigUtils
 */
public class ConfigUtils {
	static public String DEFAULT = "default.properties";
	static private ConfigUtils _instance;

	static public ConfigUtils getInstance() {
		if (_instance == null)
			_instance = new ConfigUtils();
		return _instance;
	}

	private Map<String, Properties> propertyList;

	public Properties load(String name) {
		if (this.propertyList == null)
			this.propertyList = new HashMap<>();

		if (this.propertyList.containsKey(name))
			return this.propertyList.get(name);

		// load and cache properties
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(name));
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.propertyList.put(name, properties);
		return properties;
	}

	public void save(String name) {
		if (this.propertyList.containsKey(name)) {
			System.out.print("Config " + name + " not found.");
			return;
		}
		try (OutputStream output = new FileOutputStream(name)) {
			Properties prop = this.propertyList.get(name);
			// save properties to project root folder
			prop.store(output, null);
			System.out.println(prop);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}