package cn.com.systex.search.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.MultiSearchResponse.Item;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cn.com.turing.search.utils.TsClient;
import cn.com.turing.search.utils.TuringSearchUtil;

public class ElasticApiTest {

    public static void main(String[] args) {
        String cluster = "systexelastic";
        String[] address = new String[]{"10.201.26.111", "10.201.26.112"};
        int port = 9300;
        String index = "guangfa-mem-20150619";
        String type = "test";
        TsClient tsClient = new TsClient(cluster, address, port);
        Client client = tsClient.transportClient();
        TuringSearchUtil.init(client);

        //test matchSearch
        SearchResponse searchResponse = TuringSearchUtil.termSearch(index, type, "LINEAL_NAME", "徐五十");
        printData(searchResponse);

        //test get
        GetResponse getRes = TuringSearchUtil.get(index, type, "8552");
        if (getRes != null) {
            System.out.println("index:" + getRes.getIndex() + " type:" + getRes.getType() + " source: " + getRes.getSourceAsString());
        } else {
            System.out.println("document doesn' exist!");
        }

        //test multisearch
        Map<String, Object> requestMap = new HashMap<String, Object>();
        requestMap.put("CADD1", "汕尾柯子岭景云路");
        requestMap.put("LINEAL_NAME", "孙三十八");
        MultiSearchResponse multiSearchResponse = TuringSearchUtil.multiSearch(index, requestMap);
        System.out.println("multiSearch:");
        for (Item item : multiSearchResponse.getResponses()) {
            printData(item.getResponse());
        }

        //test bulkindex
        List<JSONObject> dataList = new ArrayList<JSONObject>();
        String[] firstName = {"完颜", "公孙", "百叶", "姑苏", "徐"};
        String[] name = {"爱一", "西二", "北三", "东四", "六无", "七器", "剩九", "花八"};
        for (Item item : multiSearchResponse.getResponses()) {
            SearchResponse response = item.getResponse();
            for (SearchHit hit : response.getHits().getHits()) {
                String source = hit.getSourceAsString();
                JSONObject object = JSON.parseObject(source);
                Random random = new Random(5);
                Random random2 = new Random(8);
                object.put("LINEAL_NAME", firstName[random.nextInt(4)] + name[random2.nextInt(7)]);
//				System.out.println("data: "+object);
                dataList.add(object);
            }
        }
        TuringSearchUtil.bulkCreateIndex(index, type, dataList, 100);

        //test deleteIndexByQuery
        try {
            TuringSearchUtil.deleteIndexByQuery(index, type, "LINEAL_NAME", "徐五十", 10);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //delete document
        TuringSearchUtil.deleteIndex(index, type, "1169");
        //update document
        Map<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put("CADD1", "茂名柯子岭景云路123459999943号");
        TuringSearchUtil.updateIndex(index, type, "8552", dataMap);

        //test scrollSearch
        QueryBuilder qb = QueryBuilders.matchQuery("LINEAL_NAME", "百叶西二");
        TuringSearchUtil.scrollSearch(index, type, qb, 1000);
        TuringSearchUtil.close();
    }

    public static void printData(SearchResponse searchResponse) {
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits.getHits()) {
            System.out.println(hit.getId() + "-" + hit.getIndex() + "-" + hit.getType() + "-" + hit.getSourceAsString() + "-" + hit.getScore());
        }
    }
}
