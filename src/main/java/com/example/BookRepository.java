package com.example;

import com.example.pojo.Book;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


/**
 * @Description: 类似通用mapper,具有增删改查
 * @Author: jqq
 * @Date: 2020/10/28 10:23
 */
public interface BookRepository extends ElasticsearchRepository<Book,Long> {
}
