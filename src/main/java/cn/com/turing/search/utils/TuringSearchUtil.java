package cn.com.turing.search.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import cn.com.turing.search.utils.TuringSearchOption.SearchLogic;


/**
 * /TuringSearch的工具
 */
public class TuringSearchUtil {

    private static Client client = null;
    private static Logger log = LoggerFactory.getLogger(TuringSearchUtil.class);

    /**
     * 初始化client
     */
    public static void init() {
        if (client == null) {
            client = getClient();
        }
    }

    /**
     * 初始化client
     *
     * @param esClient 实例化的client对象
     */
    public static void init(Client esClient) {
        if (client == null) {
            client = esClient;
        }
    }

    /**
     * 获取TuringSearch连接客户端
     */
    public static Client getClient() {
        /* 获取TS配置 */
        TsConfig config = TsConfig.getIntance();
        TsClient client = new TsClient(config.getClusterName(), config.getHosts(), Integer.parseInt(config.getPort()));
        return client.transportClient();
    }

    /**
     * 创建单个文档索引
     *
     * @param index      索引名
     * @param type       类型
     * @param jsonSource 封装了文档的Json对象
     */
    public static boolean insertDoc(String index, String type, String jsonSource) {
        IndexRequestBuilder indexRequestBuilder = client.prepareIndex(index, type);
        indexRequestBuilder.setSource(jsonSource);
        IndexResponse response = indexRequestBuilder.execute().actionGet();
        if (response.isCreated()) {
            log.info("create index successfully.the new index info: " + response.getId() + ", " + response.getIndex()
                    + ", " + response.getType());
            return true;
        } else {
            return false;
        }
    }

    /**
     * 创建单个文档索引
     *
     * @param index   索引名
     * @param type    类型
     * @param docsMap 封装了文档的Map对象
     */
    public static void insertDoc(String index, String type, Map<String, Object> docsMap) {
        String jsonString = JSON.toJSONString(docsMap);
        IndexRequestBuilder indexRequestBuilder = client.prepareIndex(index, type);
        indexRequestBuilder.setSource(jsonString);
        IndexResponse indexResponse = indexRequestBuilder.execute().actionGet();
        log.info("create index successfully.the new index info: " + indexResponse.getId() + ", "
                + indexResponse.getIndex() + ", " + indexResponse.getType());
    }

    /**
     * 创建单个索引，分配索引ID
     *
     * @param index      索引名
     * @param type       类型
     * @param id         文档ID
     * @param jsonString 封装了文档的Json对象
     */
    public static boolean insertDoc(String index, String type, String id, String jsonString) {
        IndexRequestBuilder indexRequestBuilder = client.prepareIndex(index, type, id);
        indexRequestBuilder.setSource(jsonString);
        IndexResponse response = indexRequestBuilder.execute().actionGet();
        if (response.isCreated()) {
            log.info("create index successfully.the new index info: " + response.getId() + ", " + response.getIndex()
                    + ", " + response.getType());
            return true;
        } else {
            return false;
        }
    }

    /**
     * 创建单个索引，分配索引ID
     */
    public static boolean insertDoc(String index, String type, String id, Map<String, Object> docsMap) {
        String jsonString = JSON.toJSONString(docsMap);
        IndexRequestBuilder indexRequestBuilder = client.prepareIndex(index, type, id);
        indexRequestBuilder.setSource(jsonString);
        IndexResponse response = indexRequestBuilder.execute().actionGet();
        if (response.isCreated()) {
            log.info("create index successfully.the new index info: " + response.getId() + ", " + response.getIndex()
                    + ", " + response.getType());
            return true;
        } else {
            return false;
        }
    }

    /**
     * 批量导入索引
     */
    public static boolean bulkInsertDocs(String index, String type, List<JSONObject> objects, int bulkSize) {
        int batch = 1;
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        boolean flag = false;
        for (JSONObject object : objects) {
            bulkRequestBuilder.add(client.prepareIndex(index, type).setSource(object));
            batch++;
            if (batch >= bulkSize) {
                BulkResponse bulkResponse = bulkRequestBuilder.execute().actionGet();
                if (!bulkResponse.hasFailures()) {
                    flag = true;
                } else {
                    log.error(bulkResponse.buildFailureMessage());
                }
                batch = 1;
                bulkRequestBuilder = client.prepareBulk();
            }
        }
        if (bulkRequestBuilder.numberOfActions() > 0) {
            BulkResponse bulkResponse = bulkRequestBuilder.execute().actionGet();
            if (!bulkResponse.hasFailures()) {
                flag = true;
            } else {
                log.error(bulkResponse.buildFailureMessage());
            }
        }
        return flag;
    }

    /**
     * 通过文档id删除索引
     */
    public static void deleteDocs(String index, String type, String id) {
        log.info("delete document : " + index + "-" + type + "-" + id);
        DeleteRequestBuilder deleteRequestBuilder = client.prepareDelete(index, type, id);
        DeleteResponse response = deleteRequestBuilder.execute().actionGet();
        if (response.isFound()) {
            log.info("delete document " + id + "secussfully!");
        } else {
            log.warn("no data to deleted");
        }
    }

    /**
     * term查询后，批量删除数据索引
     */
    public static void deleteDocsByQuery(String index, String type, String key, String keyValue, int size)
            throws InterruptedException {
        QueryBuilder query = QueryBuilders.termsQuery(key, keyValue);

        Scroll scroll = new Scroll(TimeValue.timeValueSeconds(3));

        SearchResponse searchResponse = client.prepareSearch(index).setQuery(query).setSearchType(SearchType.QUERY_THEN_FETCH)
                .setScroll(scroll).setFrom(0).setSize(size).execute().actionGet();

        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();

        if (searchResponse.getHits().getHits().length != 0) {
            while (true) {
                searchResponse = client.prepareSearchScroll(searchResponse.getScrollId()).setScroll(scroll).execute()
                        .actionGet();
                if (searchResponse.getHits().hits().length == 0) {
                    break;
                }

                for (SearchHit hit : searchResponse.getHits()) {
                    DeleteRequest deleteRequest = new DeleteRequest(index, hit.getType(), hit.getId());
                    bulkRequestBuilder.add(deleteRequest);
                }
            }
            bulkRequestBuilder.execute().actionGet();

        } else {
            log.warn("no data will be deleted!");
        }
    }

    /**
     * 根据多字段查询结果进行批量删除
     *
     * @param index      索引名
     * @param type       类型
     * @param contentMap 封装了字段和查询值的Map对象
     */
    public static boolean bulkDeleteDocs(String index, String type, Map<String, Object[]> contentMap) {
        try {
            QueryBuilder queryBuilder = createQueryBuilder(contentMap, SearchLogic.must);
            SearchResponse searchResponse = client.prepareSearch(index).setQuery(queryBuilder)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).execute().actionGet();
            BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();// 创建bulk请求
            for (SearchHit hit : searchResponse.getHits()) {
                System.out.println("ID:" + hit.getId());
                DeleteRequest deleteRequest = new DeleteRequest(index, hit.getType(), hit.getId());
                bulkRequestBuilder.add(deleteRequest);
            }
            bulkRequestBuilder.execute().actionGet();
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return false;
    }

    /**
     * 根据index、type、id获取文档
     *
     * @param index 索引名称
     * @param type  type名
     * @param id    文档ID
     */
    public static GetResponse getDoc(String index, String type, String id) {
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
    public static UpdateResponse updateDoc(String index, String type, String id, Map<String, Object> dataMap) {
        GetResponse getRes = getDoc(index, type, id);
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
    public static boolean bulkUpdateDocs(String index, String type, QueryBuilder query, Map<String, Object> dataMap) {
        SearchResponse searchResponse = client.prepareSearch(index).setTypes(type).setQuery(query).execute()
                .actionGet();
        SearchHits searchHits = searchResponse.getHits();
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        for (SearchHit hit : searchHits.getHits()) {
            GetResponse getRes = getDoc(index, type, hit.getId());
            if (getRes != null) {
                String sourceJson = getRes.getSourceAsString();

                JSONObject object = JSON.parseObject(sourceJson);
                for (Entry<String, Object> entry : dataMap.entrySet()) {
                    object.put(entry.getKey(), entry.getValue());// 更新数据
                }
                UpdateRequest update = new UpdateRequest();
                update.index(index);
                update.type(type);
                update.id(hit.getId());
                update.doc(object.toJSONString());
                bulkRequestBuilder.add(update);
            }
        }
        if (bulkRequestBuilder.numberOfActions() > 0) {
            BulkResponse response = bulkRequestBuilder.execute().actionGet();
            if (!response.hasFailures()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * 按单个字段进行精确查询
     */
    public static SearchResponse termsSearch(String index, String type, String field, Object value) {
        QueryBuilder queryBuilder = QueryBuilders.termsQuery(field, value);
        SearchResponse searchResponse = client.prepareSearch(index).setTypes(type).setQuery(queryBuilder)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).execute().actionGet();
        return searchResponse;
    }

    /**
     * 按多字段进行精确查询
     */
    public static SearchResponse termSearch(String index, String type, HashMap<String, Object[]> contentMap) {
        QueryBuilder queryBuilder = createQueryBuilder(contentMap, SearchLogic.must);
        SearchResponse searchResponse = client.prepareSearch(index).setQuery(queryBuilder)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).execute().actionGet();
        return searchResponse;
    }

    /**
     * match 查询查询
     */
    public static SearchResponse matchSearch(String index, String type, String field, Object value, int size) {
        QueryBuilder qb = QueryBuilders.matchQuery(field, value);
        SearchResponse searchResponse = client.prepareSearch(index).setTypes(type).setSize(size).setQuery(qb).execute()
                .actionGet();
        return searchResponse;
    }

    /**
     * match 模糊查询，指定最低分数和返回最大个数
     *
     * @param index    索引名称
     * @param field    查询字段
     * @param value    查询的值
     * @param size     返回个数
     * @param minScore //最低分数
     */
    public static SearchResponse matchSearch(String index, String type, String field, String value, int size,
                                             float minScore) {
        QueryBuilder qb = QueryBuilders.matchQuery(field, value);
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index).setTypes(type) // 设置类型
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) // 设置搜索类型
                .setQuery(qb) // 设置query
                .setFrom(0).setSize(size) // 设置返回最大个数
                .setExplain(true);

        SearchResponse response = searchRequestBuilder.setMinScore(minScore) // 定义最低分
                .execute().actionGet();
        return response;
    }

    /**
     * 类似于批量索引的操作，一次执行多个查询请求
     */
    public static MultiSearchResponse multiSearch(String index, String type, Map<String, Object> requestMap) {
        List<QueryBuilder> queryBuilders = new ArrayList<QueryBuilder>(); // 声明查询处理列表

        for (Entry<String, Object> map : requestMap.entrySet()) {
            QueryBuilder qb = QueryBuilders.matchQuery( // 模糊查询
                    map.getKey().toString(), // 查询字段
                    map.getValue()); // 查询的值
            queryBuilders.add(qb);
        }
        MultiSearchRequestBuilder multiSearch = client.prepareMultiSearch(); // 声明批量查询请求
        for (QueryBuilder qbr : queryBuilders) {
            SearchRequestBuilder srb1 = client.prepareSearch(index).setTypes(type);
            srb1.setQuery(qbr); // 定义一个查询请求
            multiSearch.add(srb1); // 添加到批量查询请求里
        }
        MultiSearchResponse multiSearchResponse = multiSearch.execute().actionGet();
        return multiSearchResponse;
    }

    /**
     * scroll
     */
    public static void scrollSearch(String index, String type, QueryBuilder queryBuilder, int size) {
        SearchResponse searchResponse = client.prepareSearch(index).setTypes(type).setSearchType(SearchType.QUERY_THEN_FETCH)
                .setScroll(new TimeValue(10000)) // 10s 数据需要保存的时长
                .setQuery(queryBuilder).setSize(size).execute().actionGet();
        while (true) {
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                System.out.println(hit.getId() + ", " + hit.getSourceAsString());
            }
            searchResponse = client.prepareSearchScroll(searchResponse.getScrollId()).setScroll(new TimeValue(60000))
                    .execute().actionGet();
            if (searchResponse.getHits().getHits().length == 0) {
                break;
            }
        }
    }

    /**
     * 使用aggregation对type进行分组统计
     */
    public static Map<String, Long> aggregationCount(String index, String type, String field) {
        AggregationBuilder<?> aggregationBuilder = AggregationBuilders.terms("tests").field(field);
        SearchResponse response = client.prepareSearch(index).setTypes(type).addAggregation(aggregationBuilder)
                .execute().actionGet();
        Terms terms = response.getAggregations().get("tests");
        Map<String, Long> resultMap = new HashMap<String, Long>();
        for (Bucket b : terms.getBuckets()) {
            resultMap.put(String.valueOf(b.getKey()), b.getDocCount());
        }
        return resultMap;
    }

    /**
     * 使用aggregation对type进行分组统计
     */
    public static Map<String, Long> aggregationCount(String index, String type, String termFieldName, String field) {

        QueryBuilder qb = QueryBuilders.matchAllQuery();
        SearchRequestBuilder srb = client.prepareSearch(index).setSearchType(SearchType.QUERY_THEN_FETCH)
                .setTypes(type).setQuery(qb);

        TermsBuilder aggregation = AggregationBuilders.terms("TYPES").field(field);

        SearchResponse sr = srb.addAggregation(aggregation).execute().actionGet();

        Terms agg = sr.getAggregations().get("TYPES");
        long value = agg.getSumOfOtherDocCounts();
        System.out.println("count:" + value);

        // AggregationBuilder<?> aggregationBuilder = AggregationBuilders.terms(termFieldName).field(field);

        //SearchResponse response = srb.addAggregation(aggregationBuilder).execute().actionGet();

        //Map<String, Aggregation> aggMap = response.getAggregations().asMap();

        //Aggregation gradeTerms = aggMap.get(termFieldName);

//		Iterator<Bucket> gradeBucketIt = gradeTerms.toString().iterator();

        Map<String, Long> resultMap = new HashMap<String, Long>();
//		while (gradeBucketIt.hasNext()) {
//			Bucket gradeBucket = gradeBucketIt.next();
//			System.out.println(gradeBucket.getKey() + "有" + gradeBucket.getDocCount() + "个回答。");
//			resultMap.put(gradeBucket.getKey().toString(), gradeBucket.getDocCount());
//
//		}
        return resultMap;
    }

    /**
     * 使用facet对type进行分组统计
     */
//    public static TermsFacet facetCount(String index, String type, String field) {
//        @SuppressWarnings("deprecation")
//        TermsFacetBuilder facetBuilder = FacetBuilders.termsFacet("TermFactBuilder");
//        facetBuilder.field(field).size(Integer.MAX_VALUE);
//        facetBuilder.facetFilter(FilterBuilders.matchAllFilter());
//        @SuppressWarnings("deprecation")
//        SearchResponse response = client.prepareSearch(index).setTypes(type).addFacet(facetBuilder).execute()
//                .actionGet();
//        Facets f = response.getFacets();
//        TermsFacet facet = (TermsFacet) f.getFacets().get("TermFactBuilder");
//        return facet;
//    }

    /**
     * 对单个字段创建范围查询对象
     *
     * @param field  字段
     * @param values 范围值数组，必须是两个值的数组
     * @return 返回一个 RangeQueryBuilder 对象
     */
    private static RangeQueryBuilder createRangeQueryBuilder(String field, Object[] values) {
        if (values.length == 1 || values[1] == null || values[1].toString().trim().isEmpty()) {
            log.warn("[区间搜索]必须传递两个值，但是只传递了一个值，所以返回null");
            return null;
        }
        boolean timeType = false;
        if (TuringSearchOption.isDate(values[0])) {
            if (TuringSearchOption.isDate(values[1])) {
                timeType = true;
            }
        }
        String begin = "", end = "";
        if (timeType) {
            /*
             * 如果时间类型的区间搜索出现问题，有可能是数据类型导致的：
			 * （1）在监控页面（elasticsearch-head）中进行range搜索，看看什么结果，如果也搜索不出来，则：
			 * （2）请确定mapping中是date类型，格式化格式是yyyy-MM-dd HH:mm:ss
			 * （3）请确定索引里的值是类似2012-01-01 00:00:00的格式
			 * （4）如果是从数据库导出的数据，请确定数据库字段是char或者varchar类型，而不是date类型（此类型可能会有问题）
			 */
            begin = TuringSearchOption.formatDate(values[0]);
            end = TuringSearchOption.formatDate(values[1]);
        } else {
            begin = values[0].toString();
            end = values[1].toString();
        }
        return QueryBuilders.rangeQuery(field).from(begin).to(end);
    }

    /**
     * 创建过滤条件
     *
     * @param searchLogic      查询逻辑 must or should
     * @param queryBuilder     查询请求
     * @param searchContentMap 请求查询的数据
     * @param filterContentMap 过滤的数据
     */
//    private static QueryBuilder createFilterBuilder(SearchLogic searchLogic, QueryBuilder queryBuilder,
//                                                    HashMap<String, Object[]> searchContentMap, HashMap<String, Object[]> filterContentMap) throws Exception {
//        try {
//            Iterator<Entry<String, Object[]>> iterator = searchContentMap.entrySet().iterator();
//            AndFilterBuilder andFilterBuilder = null;
//            while (iterator.hasNext()) {
//                Entry<String, Object[]> entry = iterator.next();
//                Object[] values = entry.getValue();
//				/* 排除非法的搜索值 */
//                if (!checkValue(values)) {
//                    continue;
//                }
//                TuringSearchOption SearchOption = getSearchOption(values);
//                if (SearchOption.getDataFilter() == DataFilter.exists) {
//					/* 被搜索的条件必须有值 */
//                    ExistsFilterBuilder existsFilterBuilder = FilterBuilders.existsFilter(entry.getKey());
//                    if (andFilterBuilder == null) {
//                        andFilterBuilder = FilterBuilders.andFilter(existsFilterBuilder);
//                    } else {
//                        andFilterBuilder = andFilterBuilder.add(existsFilterBuilder);
//                    }
//                }
//            }
//            if (filterContentMap == null || filterContentMap.isEmpty()) {
//				/* 如果没有其它过滤条件，返回 */
//                return QueryBuilders.filteredQuery(queryBuilder, andFilterBuilder);
//            }
//			/* 构造过滤条件 */
//            QueryFilterBuilder queryFilterBuilder = FilterBuilders
//                    .queryFilter(createQueryBuilder(filterContentMap, searchLogic));
//			/* 构造not过滤条件，表示搜索结果不包含这些内容，而不是不过滤 */
//            NotFilterBuilder notFilterBuilder = FilterBuilders.notFilter(queryFilterBuilder);
//            return QueryBuilders.filteredQuery(queryBuilder,
//                    FilterBuilders.andFilter(andFilterBuilder, notFilterBuilder));
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
//        return null;
//    }

    /**
     * 构建单字段查询对象
     *
     * @param field        字段名
     * @param values       字段值
     * @param SearchOption 查询过滤条件
     * @return 返回一个构建的QueryBuilder查询对象
     */
    private static QueryBuilder createSingleFieldQueryBuilder(String field, Object[] values,
                                                              TuringSearchOption SearchOption) {
        try {
            if (SearchOption
                    .getSearchType() == TuringSearchOption.SearchType.range) {
                /* 区间搜索 */
                return createRangeQueryBuilder(field, values);
            }
            // String[] fieldArray =
            // field.split(",");/*暂时不处理多字段[field1,field2,......]搜索情况*/
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            for (Object valueItem : values) {
                if (valueItem instanceof TuringSearchOption) {
                    continue;
                }
                QueryBuilder queryBuilder = null;
                String formatValue = valueItem.toString().trim().replace("*", "");// 格式化搜索数据
                if (SearchOption
                        .getSearchType() == TuringSearchOption.SearchType.term) {
                    queryBuilder = QueryBuilders.termQuery(field, formatValue).boost(SearchOption.getBoost());
                } else if (SearchOption
                        .getSearchType() == TuringSearchOption.SearchType.querystring) {
                    if (formatValue.length() == 1) {
						/*
						 * 如果搜索长度为1的非数字的字符串，格式化为通配符搜索，暂时这样，以后有时间改成multifield搜索，
						 * 就不需要通配符了
						 */
                        if (!Pattern.matches("[0-9]", formatValue)) {
                            formatValue = "*" + formatValue + "*";
                        }
                    }
                    QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(formatValue)
                            .minimumShouldMatch(SearchOption.getQueryStringPrecision());
                    queryBuilder = queryStringQueryBuilder.field(field).boost(SearchOption.getBoost());
                }
                if (SearchOption.getSearchLogic() == SearchLogic.should) {
                    boolQueryBuilder = boolQueryBuilder.should(queryBuilder);
                } else {
                    boolQueryBuilder = boolQueryBuilder.must(queryBuilder);
                }
            }
            return boolQueryBuilder;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * 创建查询条件请求
     *
     * @param searchContentMap 由查询字段和查询的值构成的Map对象
     * @param searchLogic      查询逻辑 should or must
     * @return 返回一个构建的QueryBuilder查询对象
     */
    private static QueryBuilder createQueryBuilder(Map<String, Object[]> searchContentMap, SearchLogic searchLogic) {
        try {
            if (searchContentMap == null || searchContentMap.size() == 0) {
                return null;
            }
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            Iterator<Entry<String, Object[]>> iterator = searchContentMap.entrySet().iterator();
			/* 循环每一个需要搜索的字段和值 */
            while (iterator.hasNext()) {
                Entry<String, Object[]> entry = iterator.next();
                String field = entry.getKey();
                Object[] values = entry.getValue();
				/* 排除非法的搜索值 */
                if (!checkValue(values)) {
                    continue;
                }
				/* 获得搜索类型 */
                TuringSearchOption SearchOption = getSearchOption(values);
                QueryBuilder queryBuilder = createSingleFieldQueryBuilder(field, values, SearchOption);
                if (queryBuilder != null) {
                    if (searchLogic == SearchLogic.should) {
						/* should关系，也就是说，在A索引里有或者在B索引里有都可以 */
                        boolQueryBuilder = boolQueryBuilder.should(queryBuilder);
                    } else {
						/* must关系，也就是说，在A索引里有，在B索引里也必须有 */
                        boolQueryBuilder = boolQueryBuilder.must(queryBuilder);
                    }
                }
            }
            return boolQueryBuilder;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * 获得搜索选项
     *
     * @param values 查询的值数组
     */
    private static TuringSearchOption getSearchOption(Object[] values) {
        try {
            for (Object item : values) {
                if (item instanceof TuringSearchOption) {
                    return (TuringSearchOption) item;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return new TuringSearchOption();
    }

    /* 简单的值校验 */
    private static boolean checkValue(Object[] values) {
        if (values == null) {
            return false;
        } else if (values.length == 0) {
            return false;
        } else if (values[0] == null) {
            return false;
        } else if (values[0].toString().trim().isEmpty()) {
            return false;
        }
        return true;
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

}
