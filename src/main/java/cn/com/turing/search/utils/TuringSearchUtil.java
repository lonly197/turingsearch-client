package cn.com.turing.search.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;


public class TuringSearchUtil {

    private static Client client = null;
    private static Logger log = LoggerFactory.getLogger(TuringSearchUtil.class);

    public static void init(Client client) {
        TuringSearchUtil.client = client;
    }

    /**
     * 创建单个文档索引
     */
    public static boolean createIndex(String index, String type, String jsonSource) {
        IndexRequestBuilder indexRequestBuilder = client.prepareIndex(index, type);
        indexRequestBuilder.setSource(jsonSource);
        IndexResponse response = indexRequestBuilder.execute().actionGet();
        if (response.isCreated()) {
            log.info("create index successfully.the new index info: " + response.getId() + ", " + response.getIndex() + ", " + response.getType());
            return true;
        } else {
            return false;
        }
    }

    /**
     * 创建单个文档索引
     */
    public static void createIndex(String index, String type, Map<String, Object> docsMap) {
        String jsonString = JSON.toJSONString(docsMap);
        IndexRequestBuilder indexRequestBuilder = client.prepareIndex(index, type);
        indexRequestBuilder.setSource(jsonString);
        IndexResponse indexResponse = indexRequestBuilder.execute().actionGet();
        log.info("create index successfully.the new index info: " + indexResponse.getId() + ", " + indexResponse.getIndex() + ", " + indexResponse.getType());
    }

    /**
     * 批量导入索引
     */
    public static void bulkCreateIndex(String index, String type, List<JSONObject> objects, int bulkSize) {
        int batch = 1;
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        for (JSONObject object : objects) {
            System.out.println("bulkadd: " + object.toJSONString());
            bulkRequestBuilder.add(client.prepareIndex(index, type).setSource(object));
            batch++;
            if (batch >= bulkSize) {
                bulkRequestBuilder.execute().actionGet();
                batch = 1;
                bulkRequestBuilder = client.prepareBulk();
            }
        }
        if (bulkRequestBuilder.numberOfActions() > 0) {
            bulkRequestBuilder.execute().actionGet();
        }
    }

    /**
     * 通过文档id删除索引
     */
    public static void deleteIndex(String index, String type, String id) {
        log.info("delete document : " + index + "-" + type + "-" + id);
        DeleteRequestBuilder deleteRequestBuilder = client.prepareDelete(index, type, id);
        DeleteResponse response = deleteRequestBuilder.execute().actionGet();
        if (response.isFound()) {
            System.out.println("delete document " + id + "secussfully!");
        } else {
            System.out.println("no data to deleted");
        }
    }

    /**
     * 删除整个索引
     */
    public static void deleteIndex(String index) {
//		DeleteRequestBuilder deleteRequestBuilder = client.prepareDelete(index,type);
        client.delete(new DeleteRequest(index)).actionGet();
    }

    /**
     * 精确查询后，批量删除数据索引
     */
    public static void deleteIndexByQuery(String index, String type, String key, String keyValue, int size) throws InterruptedException {
        QueryBuilder query = QueryBuilders.termsQuery(key, keyValue);

        Scroll scroll = new Scroll(TimeValue.timeValueSeconds(3));

        SearchResponse searchResponse = client.prepareSearch(index)
                .setQuery(query)
                .setSearchType(SearchType.SCAN)
                .setScroll(scroll)
                .setFrom(0)
                .setSize(size).execute().actionGet();

        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();

        if (searchResponse.getHits().getHits().length != 0) {
            while (true) {
                searchResponse = client.prepareSearchScroll(searchResponse.getScrollId())
                        .setScroll(scroll).execute().actionGet();
                if (searchResponse.getHits().hits().length == 0) {
                    break;
                }

                for (SearchHit hit : searchResponse.getHits()) {
                    DeleteRequest deleteRequest = new DeleteRequest(index, hit.getType(), hit.getId());
//					System.out.println("add document id :" + hit.getId());
                    bulkRequestBuilder.add(deleteRequest);
                }
            }
            bulkRequestBuilder.execute().actionGet();

        } else {
            System.out.println("no data will be deleted!");
        }
    }

    public static GetResponse get(String index, String type, String id) {
//		GetRequest getReq = new GetRequest(index,type,id);
        GetResponse response = client.prepareGet(index, type, id).execute().actionGet();
        if (response.isExists()) {
            return response;
        } else {
            return null;
        }
    }

    /**
     * 更新单个文档索引信息
     */
    public static UpdateResponse updateIndex(String index, String type, String id, Map<String, Object> dataMap) {
        GetResponse getRes = get(index, type, id);
        if (getRes != null) {
            String sourceJson = getRes.getSourceAsString();
            JSONObject object = JSON.parseObject(sourceJson);
            for (Entry<String, Object> entry : dataMap.entrySet()) {
                if (object.containsKey(entry.getKey())) {
                    object.put(entry.getKey(), entry.getValue());
                }
            }
            UpdateResponse response = client.prepareUpdate(index, type, id).setDoc(object.toJSONString()).get();
            return response;
        } else {
            return null;
        }
    }

    /**
     * 批量更新
     */
    public static boolean bulkUpdateIndex(String index, String type, String[] id, Map<String, Object> dataMap) {
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        for (int i = 0; i < id.length; i++) {
            GetResponse getRes = get(index, type, id[i]);
            if (getRes != null) {
                String sourceJson = getRes.getSourceAsString();
                JSONObject object = JSON.parseObject(sourceJson);
                for (Entry<String, Object> entry : dataMap.entrySet()) {
                    if (object.containsKey(entry.getKey())) {
                        object.put(entry.getKey(), entry.getValue());
                    }
                }
                UpdateRequest update = new UpdateRequest();
                update.index(index);
                update.type(type);
                update.id(id[i]);
                update.doc(object.toJSONString());
                bulkRequestBuilder.add(update);
            }
        }
        BulkResponse response = bulkRequestBuilder.execute().actionGet();
        if (!response.hasFailures()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 按条件字段进行精确查询
     */
    public static SearchResponse termSearch(String index, String type, String field, String value) {
        QueryBuilder qb = QueryBuilders.termQuery(field, QueryParser.escape(value));
        SearchResponse searchResponse = client.prepareSearch(index)
                .setQuery(qb)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .execute().actionGet();
        return searchResponse;
    }

    /**
     * match 查询
     */
    public static SearchResponse matchSearch(String index, String type, String field, String value, int size) {
        QueryBuilder qb = QueryBuilders.matchQuery(field, value);
        SearchResponse searchResponse = client.prepareSearch(index)
                .setTypes(type)
                .setSize(size)
                .setQuery(qb)
                .execute().actionGet();
        return searchResponse;
    }

    /**
     * match 查询，指定最小评分和返回个数
     */
    public static SearchResponse matchSearch(String index, String type, String field, String value, int size, float minScore) {
        QueryBuilder qb = QueryBuilders.matchQuery(field, value);
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index).setTypes(type)
                //.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qb)
                .setFrom(0).setSize(size)
                .setExplain(true);

        SearchResponse response = searchRequestBuilder
                .setMinScore(minScore)
                .execute()
                .actionGet();
        return response;
    }

    /**
     * 类似于批量索引的操作，一次执行多个查询请求
     */
    public static MultiSearchResponse multiSearch(String index, Map<String, Object> requestMap) {
        List<QueryBuilder> queryBuilders = new ArrayList<QueryBuilder>();

        for (Entry<String, Object> map : requestMap.entrySet()) {
            QueryBuilder qb = QueryBuilders.matchQuery(map.getKey().toString(), map.getValue());
            queryBuilders.add(qb);
        }

        MultiSearchRequestBuilder multiSearch = client.prepareMultiSearch();
        for (QueryBuilder qbr : queryBuilders) {
            SearchRequestBuilder srb1 = client.prepareSearch(index);
            srb1.setQuery(qbr);
            multiSearch.add(srb1);
        }
        MultiSearchResponse multiSearchResponse = multiSearch.execute().actionGet();

        return multiSearchResponse;
    }

    /**
     * scroll
     */
    public static void scrollSearch(String index, String type, QueryBuilder queryBuilder, int size) {
        SearchResponse searchResponse = client.prepareSearch(index)
                .setTypes(type)
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(10000)) //10s 数据需要保存的时长
                .setQuery(queryBuilder)
                .setSize(size).execute().actionGet();

        while (true) {
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                System.out.println(hit.getId() + ", " + hit.getSourceAsString());
            }

            searchResponse = client.prepareSearchScroll(searchResponse.getScrollId())
                    .setScroll(new TimeValue(60000)).execute().actionGet();
            if (searchResponse.getHits().getHits().length == 0) {
                break;
            }
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                System.out.println(hit.getId() + ", " + hit.getSourceAsString());
            }
        }

    }

    /**
     * 关闭连接
     */
    public static void close() {
        client.close();
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
    }

    /**
     * 正则匹配搜索
     */
    public SearchResponse wildcardSearch(String index, String type, String field, String term, int size) {
        String wildcardValue = "*" + term + "*";
        QueryBuilder queryBuilder = wildcardQuery(field, wildcardValue);
        SearchResponse searchResponse = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(queryBuilder)
                .setSize(size)
                .execute().actionGet();
        return searchResponse;
    }

}
