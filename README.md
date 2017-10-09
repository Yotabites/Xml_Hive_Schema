# Xml_Hive_Schema

Xml_Hive_Schema is a tool to create a hql file from XML file with it's attribute and element names as column name. It also points to the hbase table where the XML data data presents.

### Steps to Create jar: ###

1. Clone repository as ``` git clone https://github.com/Yotabites/Xml_Hive_Schema.git```
2. `mvn:package` will create a executable jar file.

### Usage: ###

This tool takes the below five inputs,

* **args1 - Input XML file path**
* **args2 - Start tag of XML file**
* **args3 - Hive table namebr**
* **args4 - HBase table name**
* **args5 - Column family name of HBase table**
* **args6 - Output hql file path (output file name to store query)**


```    java -jar XmlTransform-0.0.1-SNAPSHOT.jar input_xml start_tag hive_table_name hbase_table_name Column_Family output.hql```

**Example XML:**

```xml
<root>
    	<info>
    		<name></name>
    		<age></age>
    		<dob></dob>
    	</info>
    	<transaction></transaction>
</root> 
```

**Result:**

```sql
    CREATE EXTERNAL TABLE hive(key int,root_info_age string,
    root_info_name string,
    root_info_dob string,
    root_transaction string) 
    STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler' WITH 
    SERDEPROPERTIES 
    ('hbase.columns.mapping' = ':key,CF:ROOT.INFO.AGE,CF:ROOT.INFO.NAME,CF:ROOT.INFO.DOB,CF:ROOT.TRANSACTION')
    TBLPROPERTIES 
    ('hbase.table.name'='hbase');
```
with this query you can create table in hive and play with XML data present in HBase.

### Constraints: ###

1.	The XML file should be well-formed. The XML file with unclosed tag will not work.
2.	The column name in hive should not be more than 127 characters if it exceeds the parent element tag will be removed till it gets less than or equal to 127 characters.
3.	The repeated column names formed from XML will be ended with "_duplicate" text.   