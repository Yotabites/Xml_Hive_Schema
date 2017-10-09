package com.yotabites.xml2hive;

import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public class XmlTransform {
	public static void main(String[] args) {
		try {
			// Input xml file path
			File inputFile = new File(args[0]);
			// Input hql file path
			File outFile = new File(args[5]);

			outFile.createNewFile();
			FileWriter writer = new FileWriter(outFile);

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();
			NodeList childNodes = doc.getChildNodes();
			childNodes = doc.getElementsByTagName(args[1]); // start tag of XML file
															
			Set<String> flatten_set = new HashSet<String>();
			Map<String, String> modification_track = new HashMap<String, String>();
			printChildNodes(null, childNodes, flatten_set);
			writer.close();

			int i = 0;
			String column_dup = "";
			String columns = "";
			String column_map = "";
			String col1 = "";
			String col2 = "";

			String hbase_table = args[3];// hbase table name which has the XML file data
			String column_family = args[4];// hbase column family name
			
			for (String col : flatten_set) {

				col1 = col.replaceAll("/", "_").replaceAll("@", "");
				col2 = col.replaceAll("/", ".").replaceAll("@", "");
				column_dup = getkey(col1, 1);

				while (modification_track.containsValue(column_dup)) {
					column_dup = column_dup + "_duplicate";
					column_dup = getkey(column_dup, 1);
				}
				columns = columns + column_dup + " string,\n";
				column_map = column_map + column_family + ":" + col2.toUpperCase() + ",";

				i++;
				System.out.println("Adding row #:" + i);
				modification_track.put(col1, column_dup);
			}

			System.out.println("Done creating table query file with " + i + " columns!!!");

			String table_name = args[2];// hive table name
			// String db_name = "XML_DB";
			StringBuilder column_build = new StringBuilder(column_map);
			column_build = column_build.replace(column_build.lastIndexOf(","), column_build.lastIndexOf(",") + 1, "");
			column_map = column_build.toString();

			column_build = new StringBuilder(columns);
			column_build = column_build.replace(column_build.lastIndexOf(","), column_build.lastIndexOf(",") + 1, "");
			columns = column_build.toString();
			String create_query = "CREATE EXTERNAL TABLE " + table_name + "(key int," + columns
					+ ") STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler' WITH SERDEPROPERTIES ('hbase.columns.mapping' = ':key,"
					+ column_map + "') TBLPROPERTIES ('hbase.table.name'='" + hbase_table + "');";

			FileWriter writer1 = new FileWriter(outFile);
			writer1.write(create_query);
			writer1.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* if same attribute name found from xml this function is used to modify the
	 * name to append "_duplicate" to the column name 
	 */
	
	public static String modify_dup(String str, Map<String, String> map) {

		if (map.containsValue(str)) {
			str = str + "_duplicate";
			str = getkey(str, 1);
			str = modify_dup(str, map);
			return str;
		} else {
			return str;
		}
	}

	public static String getkey(String str, int lastN) {
		/*
		 * Hive wont support the column name to have more than 127 characters.
		 * So if the columns names that generated form XML is more than 127
		 * characters then the parent element name are pruned till the name
		 * becomes less than 127 characters.
		 */
		if (str.length() >= 127) {
			String[] split = str.split("_");
			String dummyStr = "";
			int j = (split.length <= lastN) ? 0 : lastN;
			boolean ifFirst = true;
			for (int i = j; i < split.length; i++) {
				if (ifFirst) {
					dummyStr = split[i];
					ifFirst = false;

				} else {
					dummyStr = dummyStr + "_" + split[i];
				}
			}
			if (dummyStr.length() >= 127 && dummyStr.contains("_")) {

				dummyStr = getkey(dummyStr, lastN);
			} else if (dummyStr.length() >= 127 && dummyStr.indexOf("_") != 1) {
				dummyStr = dummyStr.substring(dummyStr.length() - 127, dummyStr.length());
			}
			return dummyStr;
		} else {
			return str;
		}
	}

	public static void printChildNodes(String parentKey, NodeList childNodes, Set<String> writer) throws IOException {

		for (int temp = 0; temp < childNodes.getLength(); temp++) {
			Node nNode = childNodes.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;
				String childKey = eElement.getTagName();

				String newKey = ((null == parentKey) ? childKey : parentKey + "/" + childKey);
				NodeList elementChildNodes = eElement.getChildNodes();
				NamedNodeMap attribute = eElement.getAttributes();
				if (attribute.getLength() > 0) {
					for (int i = 0; i < attribute.getLength(); i++) {
						writer.add(newKey + "/@" + attribute.item(i).getNodeName());
					}
				}
				int childNodes1 = elementChildNodes.getLength();
				if (childNodes1 == 1 || (null == eElement.getFirstChild())) {
					writer.add(newKey);
				}
				printChildNodes(newKey, elementChildNodes, writer);
			}
		}
	}
}
