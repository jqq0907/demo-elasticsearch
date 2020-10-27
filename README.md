#elasticsearch

##form和size的ES查询原理
    1.将用户指定的关键词进行分词
    2.去分词库中检索关键词，得到多个id
    3.根据id去分片中拉去指定的数据
    4.将数据根据score(匹配度)进行排序
    5.根据from的值将查询结果舍弃一部分
    6.返回结果  
##scroll查询原理
    1.将用户指定的关键词进行分词
    2.去分词库中检索关键词，得到多个id
    3.将文档id存放在es上下文中
    4.根据指定的size大小去es中检索指定个数的数据，拿完数据的文档id，会从上下文中移除
    5.如果需要下一页数据，直接去es的上下文中查找
    
##es的Restful语法

###1.创建一个索引
```
PUT /index1 #索引名
{
"settings": {
   "number_of_shards": 1, #分片数
   "number_of_replicas": 1 #备份数
}
}
```
###2.查看索引映射关系
```
GET /book/_mapping #book为索引名
```
###3.删除索引
```
DELETE index1
```
###4.创建索引并指定结构
```
PUT /book
{
  "settings": {
    "number_of_replicas": 1,
    " ": 1
  },
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
}
```
###5.添加文档随机id
```
POST /book/_doc
{
  "name": "西游记",
  "author": "吴道子",
  "count": 12,
  "onSale": "2020-10-25",
  "desc": "西游记测试"
}
```
###6.添加文档指定id或者根据id修改文档
```
POST /book/_doc/1 #1是id
{
  "name": "java基础",
  "author": "刘安",
  "count": 100,
  "onSale": "2020-10-25",
  "desc": "java"
}
```
###7.根据id删除
```
DELETE /book/_doc/1
```
###8.查询所有match_all
```
GET /book/_search
{
  "query": {
    "match_all": {
      
    }
  },
    "_source": {
    "includes": ["name","author"], #包括的field
    "excludes": "onSale"
  }
}
```
###9.match根据field筛选查询
```
GET /book/_search
{
  "query": {
    "match": {
      "name": "西游记" #name字段名
    }
  }
}
```
###10.match布尔查询
```
POST /book/_search
{
  "query": {
    "match": {
      "name": {
        "query": "西游 心里",
        "operator": "or" #or and
      }
    }
  }
}
```
###11.multi_match多个field对应一个text
```
POST /book/_search
{
  "query": {
    "multi_match": {
      "query": "西游",
      "fields": ["name","desc"] #多个field
    }
  }
}
```
###12.term查询，代表完全匹配，搜索前不会对搜索的关键词进行分词，直接匹配
```
POST /book/_search
{
  "query": {
    "term": {
      "name": {
        "value": "西游"
      }
    }
  }
}
```
###13.trems查询，搜索前不会对搜索的关键词进行分词，针对一个field对应多个text
```
POST /book/_search
{
  "query": {
    "terms": {
      "name": [
        "西游",
        "西游记"
      ]
    }
  }
}
```
###14.根据id查询
```
GET /book/_doc/1 #book索引名，1为id
```
##15.根据ids查询
```
POST /book/_search
{
  "query": {
    "ids": {
      "values": ["1","3"]
    }
  }
}
```
###16.prefix前缀匹配查询
```
POST /book/_search
{
  "query": {
    "prefix": {
      "name": {
        "value": "心"
      }
    }
  }
}
```
###17.fuzzy模糊查询
```
POST /book/_search
{
  "query": {
    "fuzzy": {
      "name": {
        "value": "java", #允许有错字
        "prefix_length": 4 #指定前面四个字必须匹配
      }
    }
  }
}
```
###18.wildcard通配符查询
```
POST /book/_search
{
  "query": {
    "wildcard": {
      "name": {
        "value": "java*"  #*代表所有,?代表一个
      }
    }
  }
}
```
###19.regexp自定义正则匹配查询
```
POST /book/_search
{
  "query": {
    "regexp": {
      "name": "[a-z]*" #正则表达式
    }
  }
}
```
###20.rang范围查询
```
POST /book/_search
{
  "query": {
    "range": {
      "count": {
        "gte": 10, #gte大于等于,gt大于
        "lte": 150
      }
    }
  }
}
```
###21.scroll分页查询
```
#获取第一页数据
POST /book/_search/?scroll=1m #scroll上下文存活时间1分钟
{
  "query": {
    "match_all": {
    
    }
  },
  "size": 2, #每页大小
  "sort": [ #排序
    {
      "count": {
        "order": "desc"
      }
    }
  ]
}
#下一页数据
POST /_search/scroll
{
  "scroll_id": "FGluY2x1ZGVfY29udGV4dF91dWlkDXF1ZXJ5QW5kRmV0Y2gBFEhyREdaWFVCRTdhRmpFWXo4dFBSAAAAAAAAEEwWOGRPUUJna05UVnk2Q1dwWXI1VGJWQQ==", #scroll_id
  "scroll": "1m"
}
#删除scroll上下文
DELETE/_search/scroll/FGluY2x1ZGVfY29udGV4dF91dWlkDXF1ZXJ5QW5kRmV0Y2gBFEk3REhaWFVCRTdhRmpFWXpfOU44AAAAAAAAEFEWOGRPUUJna05UVnk2Q1dwWXI1VGJWQQ==
```
###22.delete_by_query,删除term,match查询出来的数据
```
POST /book/_delete_by_query
{
  "query": {
    "range": {
        "count": {
          "lt": 1
       }
    }
  }
}
```
###23.复合bool查询
```
POST /book/_search
{
  "query": {
    "bool": {
      "should": [  #should是or
        {
          "term": {
            "name": {
              "value": "西游记"
            }
          }
        }
      ],
      "must": [ #must代表and
        {
          "match": {
            "desc": "java"
          }
        }
      ],
      "must_not": [ #must_not代表not
        {
          "match": {
            "name": "心里"
          }
        }
      ]
    }
  }
}
```
###24.boosting查询，操作查询后的_score
```
POST /book/_search
{
  "query": {
    "boosting": {
      "positive": {
        "match": {
          "name": "java"
        }
      },
      "negative": { 
        "match": {
          "desc": "java"
        }
      },
      "negative_boost": 0.2 #查出来的数据满足positive后满足negative,就把数据的_score乘以negative_boost的值
    }
  }
}
```
###25.filter查询，基于bool，不计算score，不会排序，会做缓存
```
POST /book/_search
{
  "query": {
    "bool": {
      "filter": [
        {
          "term": {
            "name": "西游记"
          }
        }
      ]
    }
  }
}
```
###26.highlight高亮查询
```
POST /book/_search
{
  "query": {
    "match": {
      "name": "java"
    }
  },
  "highlight": {
    "fields": {
      "name": {} #高亮field，多个
    },
    "pre_tags": "<font color='red'>", 
    "post_tags": "</font>",
    "fragment_size": 3 #  一段 fragment 包含多少个字符    
  }
}
```
###27. cardinality去重计数查询
```
POST /book/_search
{
  "aggs": {
    "agg": { #名称
      "cardinality": { #去重计数
        "field": "author"
      }
    }
  }
}
```
###28.range范围统计，统计一定范围内出现的个数
```
#数值范围
POST /book/_search
{
  "aggs": {
    "agg": {
      "range": {
        "field": "count", #字段
        "ranges": [
          {
            "from": 0,  #大于等于50
            "to": 100 #小于100
          },
          {
            "from": 100,
            "to": 150
          }
        ]
      }
    }
  }
}
#时间范围
POST /book/_search
{
  "aggs": {
    "agg": {
      "date_range": {
        "field": "onSale",
        "format": "yyyy", #时间格式
        "ranges": [
          {
            "from": "2020",
            "to": "2020"
          }
        ]
      }
    }
  }
}
#ip范围
POST /book/_search
{
  "aggs": {
    "agg": {
      "ip_range": {
        "field": "ip",
        "ranges": [
          {
            "from": "10.0.0.5",
            "to": "10.0.0.10"
          }
        ]
      }
    }
  }
}
```
###29.extended_stats统计聚合查询，查询指定field的最大值、最小值、平均值...
```
POST /book/_search
{
  "aggs": {
    "agg": {
      "extended_stats": {
        "field": "count"
      }
    }
  }
}
```
###30.地图
```
#添加地图所以
PUT /map
{
  "mappings": {
    "properties": {
      "name": {
        "type": "text"
      },
      "location": {
        "type": "geo_point"
      }
    }
  }
}
#添加数据
POST /map/_doc
{
  "name": "天安门",
  "location": {
    "lon": 116.302509,
    "lat": 39.991152
  }
}
```
###31.ES地图检索
```
geo_distance: 直线距离检索方式
geo_bounding_box: 以两个点确定一个矩形，获取矩形内数据
deo_polygon: 以多个点确认一个多边形

POST /map/_search
{
  "query": {
    "bool": {
      "filter": {
        "geo_distance": {
          "distance": 4000, #半径
          "distance_type": "arc", #圆形
           "location": {
              "lon": 116.433733,
              "lat": 39.908404
            }
        }
      }
    }
  }
}

POST /map/_search
{
  "query": {
    "bool": {
      "filter": {
        "geo_bounding_box": {
          "location": {
            "top_left": { #左上角
              "lat": 40.73,
              "lon": -74.1
            },
            "bottom_right": { #右下角
              "lat": 40.717,
              "lon": -73.99
            }
          }
        }
      }
    }
  }
}

POST /map/_search
{
  "query": {
    "bool": {
      "filter": {
        "geo_polygon": {
          "location": {
            "points": [ #多个点
              {
                "lat": 40.73,
                "lon": -74.1
              },
              {
                "lat": 40.83,
                "lon": -75.1
              }
            ]
          }
        }
      }
    }
  }
}
```