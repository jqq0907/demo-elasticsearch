package com.example.elasticsearch;

import com.alibaba.fastjson.JSON;
import com.example.pojo.Book;
import com.example.pojo.Item;
import net.minidev.json.JSONArray;
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
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.fetch.subphase.highlight.AbstractHighlighterBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.fetch.subphase.highlight.Highlighter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.TestExecutionListeners;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@EnableElasticsearchRepositories
class DemoElasticsearchApplicationTests {
    @Autowired
    private RestHighLevelClient client;

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
        Book book = new Book("心理科", "夏至", 112L, new Date(), "交通出版社");
        //2.创建请求
        IndexRequest request = new IndexRequest("book");
        request.id("1");
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
     * @throws IOException
     */
    @Test
    void searchMulti_match() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("book");
        //2.创建查询条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.multiMatchQuery("西游","name","desc"));
        request.source(sourceBuilder);
        //3.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.输出结果
        System.out.println(response);
    }
}
