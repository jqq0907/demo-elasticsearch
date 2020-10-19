package com.example.elasticsearch;

import com.alibaba.fastjson.JSON;
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
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
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
        //1.创建索引请求
        CreateIndexRequest request = new CreateIndexRequest("index_01");
        //2.客户端执行请求 IndicesClient，请求后获得响应
        CreateIndexResponse indexResponse =
                client.indices().create(request, RequestOptions.DEFAULT);
        System.out.println(indexResponse.index());
    }

    /**
     * 判断索引是否存在
     *
     * @throws IOException
     */
    @Test
    void existIndex() throws IOException {
        //获取索引请求
        GetIndexRequest request = new GetIndexRequest("index_01");
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
        //创建对象
        Item item = new Item(1L, "小米手机", "手机", "小米", 3799d, "/1");
        //创建请求
        IndexRequest index_01 = new IndexRequest("index_01");
        //规则 put /index_01/_doc/1
        index_01.id("1");
        index_01.timeout(TimeValue.timeValueSeconds(1));
        index_01.timeout("1s"); //二选一

        //将数据放入请求
        index_01.source(JSON.toJSONString(item), XContentType.JSON);

        //发送请求
        IndexResponse response = client.index(index_01, RequestOptions.DEFAULT);
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
        //创建请求
        GetRequest request = new GetRequest("index_01", "1");
        //不获取返回的_source上下文
        request.fetchSourceContext(new FetchSourceContext(false));

        request.storedFields("_none_");

        //判断是否存在
        boolean exists = client.exists(request, RequestOptions.DEFAULT);
        System.out.println(exists);

        //获取文档信息
        GetResponse documentFields = client.get(request, RequestOptions.DEFAULT);
        //documentFields与命令式一样
        System.out.println(documentFields.getSourceAsString());
    }

    /**
     * 更新文档
     *
     * @throws IOException
     */
    @Test
    void updateDocument() throws IOException {
        //创建对象
        Item item = new Item(1L, "小米手机m", "手机", "小米", 3699d, "/1");
        UpdateRequest updateRequest = new UpdateRequest("index_01", "1");
        updateRequest.timeout("1s");
        updateRequest.doc(JSON.toJSONString(item), XContentType.JSON);

        UpdateResponse response = client.update(updateRequest, RequestOptions.DEFAULT);
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
     * 查询
     */
    @Test
    void searchDocument() throws IOException {
        //搜索请求
        SearchRequest searchRequest = new SearchRequest("index_01");

        //构建搜索条件构造
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //分页
        searchSourceBuilder.from();
        searchSourceBuilder.size();

        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);

        //查询条件
        //termQuery()精确匹配
        //matchAllQuery()匹配所有
        QueryBuilders.termQuery("title", "小米手机m"); //精确匹配
        QueryBuilders.matchAllQuery(); //匹配所有

        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        searchRequest.source(searchSourceBuilder);
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        for (SearchHit hit : response.getHits().getHits()) {
            //数据
            System.out.println(hit.getSourceAsMap());
            //获取高亮字段
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField title = highlightFields.get("title");
        }
    }
}
