package com.example.elasticsearch;

import com.alibaba.fastjson.JSON;
import com.example.BookRepository;
import com.example.pojo.Book;
import com.example.pojo.Item;
import net.minidev.json.JSONArray;
import org.apache.lucene.util.ArrayUtil;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexAction;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.ArrayUtils;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.Cardinality;
import org.elasticsearch.search.aggregations.metrics.ExtendedStats;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.fetch.subphase.highlight.AbstractHighlighterBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.fetch.subphase.highlight.Highlighter;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@EnableElasticsearchRepositories(basePackages = "com.example")
class DemoElasticsearchApplicationTests {
    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private BookRepository bookRepository;

    /**
     * 通过模板查询
     */
    @Test
    void test() {
        Book book = (Book) this.bookRepository.findAll();
        System.out.println(book);
    }

    /**
     * 索引的创建
     */
    @Test
    void createIndex() throws IOException {
        //1.创建请求
        CreateIndexRequest request = new CreateIndexRequest("index_01");
        //2.索引setting,
        Settings.Builder setting = Settings.builder()
                .put("number_of_replicas", "1") //备份数
                .put("number_of_shards", "1"); //分片数
        //3.索引结构mapping
        /*
        "mappings": {
            "properties": {
              "name": {
                "type": "text",
                "analyzer": "ik_max_word",
                "index": true,
                "store": false
              },
              "author": {
                "type": "keyword"
              },
              "count": {
                "type": "long"
              },
              "onSale": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
              },
              "desc": {
                "type": "text",
                "analyzer": "ik_max_word"
              }
            }
          }
         */
        XContentBuilder mapping = JsonXContent.contentBuilder()
                .startObject()
                .startObject("properties")
                .startObject("name")
                .field("type", "text")
                .field("analyzer", "ik_max_word")
                .field("index", "true")
                .field("store", "false")
                .endObject()
                .startObject("author")
                .field("type", "keyword")
                .endObject()
                .startObject("count")
                .field("type", "long")
                .endObject()
                .startObject("onSale")
                .field("type", "date")
                .field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")
                .endObject()
                .startObject("desc")
                .field("type", "text")
                .field("analyzer", "ik_max_word")
                .endObject()
                .endObject()
                .endObject();

        //3.封装请求
        request.settings(setting).mapping(mapping);
        //通过client创建索引
        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);

        //4.获取结果
        System.out.println(response);
    }

    /**
     * 判断索引是否存在
     *
     * @throws IOException
     */
    @Test
    void existIndex() throws IOException {
        //1.获取索引请求
        GetIndexRequest request = new GetIndexRequest("index_01");
        //2.判断是否存在
        boolean response = client.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println(response);
    }

    /**
     * 删除索引
     *
     * @throws IOException
     */
    @Test
    void deleteIndex() throws IOException {
        DeleteIndexRequest request = new DeleteIndexRequest("index_01");
        AcknowledgedResponse delete = client.indices().delete(request, RequestOptions.DEFAULT);
        System.out.println(delete.isAcknowledged());
    }

    /**
     * 添加文档
     *
     * @throws IOException
     */
    @Test
    void addDocument() throws IOException {
        //1.数据
        Book book = new Book("java从入门到精通", "李四", 212L, new Date(), "带你进入java的世界");
        //2.创建请求
        IndexRequest request = new IndexRequest("book");
        request.id("3");
        request.timeout(TimeValue.timeValueSeconds(1));
        request.timeout("1s"); //二选一

        //将数据放入请求
        request.source(JSON.toJSONString(book), XContentType.JSON);

        //3.client请求
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
        System.out.println(response.toString());
        System.out.println(response.status()); //对于命运返回状态
    }

    /**
     * 获取文档
     *
     * @throws IOException
     */
    @Test
    void getDocument() throws IOException {
        //1.创建请求
        GetRequest request = new GetRequest("book", "1");
        //不获取返回的_source上下文
        request.fetchSourceContext(new FetchSourceContext(false));

        request.storedFields("_none_");

        //判断是否存在
        boolean exists = client.exists(request, RequestOptions.DEFAULT);
        System.out.println(exists);

        //2.获取文档信息
        GetResponse documentFields = client.get(request, RequestOptions.DEFAULT);

        //documentFields与命令式一样
        //3.输出数据
        System.out.println(documentFields.getSourceAsString());
    }

    /**
     * 更新文档
     *
     * @throws IOException
     */
    @Test
    void updateDocument() throws IOException {
        //1.数据,通过map或者json对象
//        Item item = new Item(1L, "小米手机m", "手机", "小米", 3699d, "/1");
        Book book = new Book("心理科", "夏至", 200L, new Date(), "交通出版社");
        Map<String, Object> map = new HashMap<>();
        map.put("name", "心理科大全");
        UpdateRequest updateRequest = new UpdateRequest("book", "1");
        updateRequest.timeout("1s");
//        updateRequest.doc(map);
        updateRequest.doc(JSON.toJSONString(book), XContentType.JSON);
        //2.client请求
        UpdateResponse response = client.update(updateRequest, RequestOptions.DEFAULT);
        //3.输出结果
        System.out.println(response.status());
    }

    /**
     * 删除文档
     *
     * @throws IOException
     */
    @Test
    void deleteDocument() throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest("index_01", "1");
        DeleteResponse delete = client.delete(deleteRequest, RequestOptions.DEFAULT);

        System.out.println(delete.status());
    }

    /**
     * 批量操作
     */
    @Test
    void bulkDocument() throws IOException {
        //批处理
        BulkRequest bulkRequest = new BulkRequest();
        ArrayList<Item> list = new ArrayList<>();
        list.add(new Item(1L, "小米手机m", "手机", "小米", 3699d, "/1"));
        list.add(new Item(2L, "小米手机10", "手机", "小米", 3999d, "/2"));

        for (int i = 0; i < list.size(); i++) {
            bulkRequest.add(new IndexRequest("index_01")
                    .id("" + (i + 1))
                    .source(JSON.toJSONString(list.get(i)), XContentType.JSON));
        }

        BulkResponse bulk = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        System.out.println(bulk.status());
    }

    /**
     * term查询：完全匹配，搜索之前不会对搜索的关键词进行分词
     */
    @Test
    void searchTerm() throws IOException {
        //1.创建请求
        SearchRequest request = new SearchRequest("book");

        //2.查询条件
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.from(0);
        builder.size(10); //分页从0-10，默认查10条数据

        builder.query(QueryBuilders.termQuery("name", "西游记"));
        request.source(builder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        for (SearchHit hit : response.getHits().getHits()) {
            System.out.println(hit.getSourceAsMap());
        }
    }

    /**
     * terms查询
     *
     * @throws IOException
     */
    @Test
    void searchTerms() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("book");
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        List<String> list = new ArrayList<>();
        list.add("西游记");
        list.add("心理科");
        sourceBuilder.query(QueryBuilders.termsQuery("name", list));
        request.source(sourceBuilder);
        //3.client执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * match_all查询
     */
    @Test
    void searchMatchAll() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("book"); //book是index名
        //2.创建查询条件
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.query(QueryBuilders.matchAllQuery());

        request.source(builder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //输出结果
        for (SearchHit hit : response.getHits().getHits()) {
            System.out.println(hit.getSourceAsMap());
        }
    }

    /**
     * match查询
     *
     * @throws IOException
     */
    @Test
    void searchMatch() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("book");
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("name", "西游"));
        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * match_boolean查询
     *
     * @throws IOException
     */
    @Test
    void searchMatch_boolean() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("book");
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("name", "西游 心理") // 用空格隔开
                .operator(Operator.OR)); //and or
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * multi_match查询，多个field对应一个text
     *
     * @throws IOException
     */
    @Test
    void searchMulti_match() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("book");
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.multiMatchQuery("西游", "name", "desc"));
        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * 根据ids查询
     *
     * @throws IOException
     */
    @Test
    void searchByIds() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("book");
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.idsQuery().addIds("1", "3"));
        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * 根据id查询，类似查询索引
     *
     * @throws IOException
     */
    @Test
    void searchById() throws IOException {
        //1.创建request
        GetRequest request = new GetRequest("book", "1");
        //2.执行查询
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        //3.输出结果
        System.out.println(response);
    }

    /**
     * prefix前缀匹配查询
     */
    @Test
    void searchPrefix() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("book");
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.prefixQuery("desc", "java"));
        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * fuzzy模糊查询
     */
    @Test
    void searchFuzzy() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("book");
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.fuzzyQuery("desc", "javq")
                .prefixLength(3)); //前面3个字不允许出错
        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * wildcard通配符查询
     */
    @Test
    void searchWildcard() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("book");
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.wildcardQuery("desc", "java*")); //*代表所有, ?代表一个
        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * regexp正则匹配查询
     */
    @Test
    void searchRegexp() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("book");
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.regexpQuery("desc", "[a-z]*")); //正则
        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * range范围查询
     */
    @Test
    void searchRange() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("book");
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.rangeQuery("count")
                .gt(10)
                .lt(150)); //gt、gte、lt、lte
        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * scroll分页查询
     */
    @Test
    void searchScroll() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest();
        //2.指定scroll
        request.scroll(TimeValue.timeValueMillis(1L)); //scroll_id存活时间
        //3.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(1); //每页大小
        sourceBuilder.sort("count", SortOrder.DESC); //排序
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        //4.查询第一页数据，获取scroll_id
        SearchResponse searchResponse = client.search(request, RequestOptions.DEFAULT);
        String scrollId = searchResponse.getScrollId();
        System.out.println(scrollId);
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            System.out.println(hit.getSourceAsMap());
        }
        System.out.println("------------------------");
        //5.循环下一页
        while (true) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueMinutes(1L));
            SearchResponse response = client.scroll(scrollRequest, RequestOptions.DEFAULT);
            SearchHit[] hits = response.getHits().getHits();
            if (hits.length > 0 && hits != null) {
                for (SearchHit hit : hits) {
                    System.out.println(hit.getSourceAsMap());
                }
            } else {
                break;
            }
        }
        System.out.println("------------------------");
        //6.清除scroll_id
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        System.out.println(clearScrollRequest);
    }

    /**
     * delete_by_query 删除查询的数据
     *
     * @throws IOException
     */
    @Test
    void delete_by_query() throws IOException {
        //1.创建deleteByQueryRequest
        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest("book");
        //2.设置查询条件
        deleteByQueryRequest.setQuery(QueryBuilders.rangeQuery("count").gt(10));
        //3.执行
        BulkByScrollResponse bulkByScrollResponse = client.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(bulkByScrollResponse.toString());
    }

    /**
     * 复合bool查询
     */
    void searchBool() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest();
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("name", "西游记")) //and
                .mustNot(QueryBuilders.termQuery("name", "心里")) //not
                .should(QueryBuilders.termQuery("desc", "西游记"))); //or

        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * boosting查询，操作查询后的_score,影响排序
     */
    @Test
    void searchBoosting() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest();
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.boostingQuery(
                QueryBuilders.termQuery("name", "java"),//满足positive后
                QueryBuilders.rangeQuery("count").gt(50)) //再满足negative
                .negativeBoost(0.1f)); //数据的_score乘以negative_boost的值

        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * .filter查询，基于bool，不计算score，不会排序，会做缓存
     */
    @Test
    void searchFilter() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest();
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders
                .boolQuery()
                .filter(QueryBuilders.termQuery("name", "西游记")));

        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * highlight高亮查询
     */
    @Test
    void searchHighLight() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest();
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termsQuery("name", "西游记"));
        //3.高亮条件
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder
                .field("name", 10) //高亮字段，可以多个
                .preTags("<font color='red'>") //高亮样式
                .postTags("</font>")
                .fragmentSize(2); //高亮数据展示多少个字符
        sourceBuilder.highlighter(highlightBuilder);
        request.source(sourceBuilder);
        //4.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //5.输出结果
        System.out.println(response);
    }

    /**
     * ardinality去重计数查询
     */
    @Test
    void searchCardinality() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest();
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.aggregation(AggregationBuilders
                .cardinality("agg")
                .field("author"));
        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        Cardinality agg = response.getAggregations().get("agg"); //agg为名称
        System.out.println(agg.getValue());
    }

    /**
     * .range范围统计，统计一定范围内出现的个数,时间范围、ip范围、数量范围
     */
    @Test
    void searchRangeAgg() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest();
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.aggregation(AggregationBuilders
                .range("agg")
                .field("count")
                .addUnboundedTo(0)
                .addRange(0, 10));
        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }

    /**
     * extended_stats统计聚合查询，查询指定field的最大值、最小值、平均值...
     */
    @Test
    void searchExtendedStats() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest();
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.aggregation(AggregationBuilders.extendedStats("agg").field("count"));
        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        ExtendedStats agge = response.getAggregations().get("agge");
        System.out.println(agge.getMax()); //最大值
    }

    /**
     * deo_polygon: 以多个点确认一个多边形
     */
    @Test
    void searchGeoPolygon() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest();
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(39.908404, 116.433733)); //lon经度
        sourceBuilder.query(QueryBuilders
                .geoPolygonQuery("location", points));
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        for (SearchHit hit : response.getHits().getHits()) {
            System.out.println(hit.getSourceAsMap());
        }
    }
}
