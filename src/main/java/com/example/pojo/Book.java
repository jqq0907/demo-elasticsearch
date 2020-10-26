package com.example.pojo;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

/**
 * @Description:
 * @Author: jqq
 * @Date: 2020/10/26 15:45
 */
@Document(indexName = "book", shards = 1, replicas = 1)
public class Book {

    @Field(type = FieldType.Text, searchAnalyzer = "ik_max_word")
    private String name;
    @Field(type = FieldType.Keyword)
    private String author;
    @Field(type = FieldType.Long)
    private Long count;
    @Field(type = FieldType.Date)
    private Date onSale;
    @Field(type = FieldType.Text, searchAnalyzer = "ik_max_word")
    private String desc;

    public Book(String name, String author, Long count, Date onSale, String desc) {
        this.name = name;
        this.author = author;
        this.count = count;
        this.onSale = onSale;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Date getOnSale() {
        return onSale;
    }

    public void setOnSale(Date onSale) {
        this.onSale = onSale;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
