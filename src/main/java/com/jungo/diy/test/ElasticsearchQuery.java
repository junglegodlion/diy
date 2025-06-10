package com.jungo.diy.test;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ElasticsearchQuery {

    // 请求头常量
    private static final Map<String, String> DEFAULT_HEADERS = new HashMap<String, String>() {{
        put("accept", "application/json, text/plain, */*");
        put("accept-language", "zh-CN,zh;q=0.9");
        put("content-type", "application/json");
        put("kbn-version", "7.6.1");
        put("priority", "u=1, i");
        put("cookie","_login_uuid=193fb7ca5fa158-0c64e056341bca-26011851-2073600-193fb7ca5fb805; mysession-2=MTc0ODQ4NTAzNnxEdi1CQkFFQ180SUFBUkFCRUFBQVlmLUNBQUVHYzNSeWFXNW5EQTRBREhObGMzTnBiMjVzYjJkcGJnWnpkSEpwYm1jTVBRQTdhVzUwTFhObGNuWnBZMlV0Wld4ckxuUjFhSFY1ZFc0dVkyNHRiRzluYVc0dFdsSlJRVTlKV0ZkTVdscEZRa1pRV2t0TVYwcEhTMWhKVTAwPXyJpgKyHOK6rvJeCsZPcLU638ZE20ttpqVkGjq26bvpTw==; sensorsdata2015jssdkcross=%7B%22distinct_id%22%3A%22193fb7ca61815f-02e045669168b-26011851-2073600-193fb7ca619ca2%22%2C%22first_id%22%3A%22%22%2C%22props%22%3A%7B%22%24latest_traffic_source_type%22%3A%22%E7%9B%B4%E6%8E%A5%E6%B5%81%E9%87%8F%22%2C%22%24latest_search_keyword%22%3A%22%E6%9C%AA%E5%8F%96%E5%88%B0%E5%80%BC_%E7%9B%B4%E6%8E%A5%E6%89%93%E5%BC%80%22%2C%22%24latest_referrer%22%3A%22%22%7D%2C%22%24device_id%22%3A%22193fb7ca61815f-02e045669168b-26011851-2073600-193fb7ca619ca2%22%7D; Ops-Token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyX2lkIjoyNTYxLCJ1c2VyX25hbWUiOiJsaWNodWFuZzMiLCJuYW1lIjoiXHU2NzRlXHU5NWVmIiwiaXNfc3VwZXJ1c2VyIjowLCJzdWIiOjE3NDkxMjQxNjksImV4cCI6MTc0OTcyODk2OX0.CKOaZOFcGXtWHI1h_b_-KvpFotiG9cxBqKRqimuf5Jc\n");

    }};

    public static void main(String[] args) {
        String baseUrl = "https://int-service-elk.tuhuyun.cn/elasticsearch/logstash-int-service-server-side-log-*/_search";
        
        // 查询参数
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("rest_total_hits_as_int", true);
        queryParams.put("ignore_unavailable", true);
        queryParams.put("ignore_throttled", true);
        queryParams.put("preference", System.currentTimeMillis());
        queryParams.put("timeout", "30000ms");

        // 构建请求体
        JSONObject requestBody = buildRequestBody(
            "ext-website-cl-maint-api", 
            "/maintMainline/getBasicMaintainData",
            "2025-06-08T16:00:00.000Z", 
            "2025-06-09T15:59:59.999Z"
        );

        // 执行查询
        HttpResponse<JsonNode> response = executeQuery(baseUrl, queryParams, requestBody);

        // 输出结果
        System.out.println("Status: " + response.getStatus());
        System.out.println("Response: " + response.getBody().toPrettyString());
    }

    private static JSONObject buildRequestBody(String appId, String targetUrl,
                                               String startTime, String endTime) {
        // 时间范围条件
        JSONObject range = new JSONObject();
        range.put("gte", startTime);
        range.put("lte", endTime);
        range.put("format", "strict_date_optional_time");

        // bool查询结构 - 修正 must 应为数组
        JSONObject bool = new JSONObject();
        bool.put("must", new JSONObject[]{new JSONObject().put("match_all", new JSONObject())});
        bool.put("filter", new JSONObject[]{
                new JSONObject().put("match_phrase", new JSONObject().put("server_app_id", appId)),
                new JSONObject().put("match_phrase", new JSONObject().put("request_target_url", targetUrl)),
                new JSONObject().put("range", new JSONObject().put("@timestamp", range))
        });
        bool.put("must_not", new JSONObject[]{
                new JSONObject().put("match_phrase", new JSONObject().put("response_data", "Code=1"))
        });

        // 排序条件
        JSONObject timestampSort = new JSONObject();
        timestampSort.put("order", "desc");
        timestampSort.put("unmapped_type", "boolean");

        // 日期直方图聚合
        JSONObject dateHistogram = new JSONObject();
        dateHistogram.put("field", "@timestamp");
        dateHistogram.put("fixed_interval", "30m");
        dateHistogram.put("time_zone", "Asia/Shanghai");
        dateHistogram.put("min_doc_count", 1);

        // 文档值字段
        JSONObject[] docvalueFields = {
                new JSONObject().put("field", "@timestamp").put("format", "date_time"),
                new JSONObject().put("field", "request_begin_time").put("format", "date_time"),
                new JSONObject().put("field", "request_end_time").put("format", "date_time")
        };

        // 高亮设置
        JSONObject highlight = new JSONObject();
        highlight.put("pre_tags", new String[]{"@kibana-highlighted-field@"});
        highlight.put("post_tags", new String[]{"@/kibana-highlighted-field@"});
        highlight.put("fields", new JSONObject().put("*", new JSONObject()));
        highlight.put("fragment_size", 2147483647);

        // 构建完整请求体
        JSONObject body = new JSONObject();
        body.put("version", true);
        body.put("size", 500);
        body.put("sort", new JSONObject[]{new JSONObject().put("@timestamp", timestampSort)});
        body.put("aggs", new JSONObject().put("2", new JSONObject().put("date_histogram", dateHistogram)));
        body.put("stored_fields", new String[]{"*"});
        body.put("script_fields", new JSONObject());
        body.put("docvalue_fields", docvalueFields);
        body.put("_source", new JSONObject().put("excludes", new JSONArray()));
        body.put("query", new JSONObject().put("bool", bool));
        body.put("highlight", highlight);

        return body;
    }


    private static HttpResponse<JsonNode> executeQuery(String baseUrl, 
                                                     Map<String, Object> params, 
                                                     JSONObject body) {
        HttpResponse<JsonNode> httpResponse = Unirest.post(baseUrl)
                .headers(DEFAULT_HEADERS)
                .queryString(params)
                .body(body.toString())
                .asJson();
        return httpResponse;
    }
}
