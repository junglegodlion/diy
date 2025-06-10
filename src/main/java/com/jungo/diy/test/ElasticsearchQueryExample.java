package com.jungo.diy.test;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

public class ElasticsearchQueryExample {
    public static void main(String[] args) {
        String url = "https://int-service-elk.tuhuyun.cn/elasticsearch/logstash-int-service-server-side-log-*/_search" +
                "?rest_total_hits_as_int=true&ignore_unavailable=true&ignore_throttled=true" +
                "&preference=1749524984520&timeout=30000ms";

        String bodyJson = "{\"version\":true,\"size\":500,\"sort\":[{\"@timestamp\":{\"order\":\"desc\",\"unmapped_type\":\"boolean\"}}],\"aggs\":{\"2\":{\"date_histogram\":{\"field\":\"@timestamp\",\"fixed_interval\":\"30m\",\"time_zone\":\"Asia/Shanghai\",\"min_doc_count\":1}}},\"stored_fields\":[\"*\"],\"script_fields\":{},\"docvalue_fields\":[{\"field\":\"@timestamp\",\"format\":\"date_time\"},{\"field\":\"request_begin_time\",\"format\":\"date_time\"},{\"field\":\"request_end_time\",\"format\":\"date_time\"}],\"_source\":{\"excludes\":[]},\"query\":{\"bool\":{\"must\":[{\"match_all\":{}}],\"filter\":[{\"match_phrase\":{\"server_app_id\":\"ext-website-cl-maint-api\"}},{\"match_phrase\":{\"request_target_url\":\"/maintMainline/getBasicMaintainData\"}},{\"range\":{\"@timestamp\":{\"gte\":\"2025-06-08T16:00:00.000Z\",\"lte\":\"2025-06-09T15:59:59.999Z\",\"format\":\"strict_date_optional_time\"}}}],\"should\":[],\"must_not\":[{\"match_phrase\":{\"response_data\":\"Code=1\"}}]}},\"highlight\":{\"pre_tags\":[\"@kibana-highlighted-field@\"],\"post_tags\":[\"@/kibana-highlighted-field@\"],\"fields\":{\"*\":{}},\"fragment_size\":2147483647}}";
        HttpResponse<JsonNode> response = Unirest.post(url)
                .header("accept", "application/json, text/plain, */*")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("content-type", "application/json")
                .header("kbn-version", "7.6.1")
                .header("priority", "u=1, i")
                .header("sec-ch-ua", "\"Google Chrome\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-origin")
                .header("cookie","_login_uuid=193fb7ca5fa158-0c64e056341bca-26011851-2073600-193fb7ca5fb805; mysession-2=MTc0ODQ4NTAzNnxEdi1CQkFFQ180SUFBUkFCRUFBQVlmLUNBQUVHYzNSeWFXNW5EQTRBREhObGMzTnBiMjVzYjJkcGJnWnpkSEpwYm1jTVBRQTdhVzUwTFhObGNuWnBZMlV0Wld4ckxuUjFhSFY1ZFc0dVkyNHRiRzluYVc0dFdsSlJRVTlKV0ZkTVdscEZRa1pRV2t0TVYwcEhTMWhKVTAwPXyJpgKyHOK6rvJeCsZPcLU638ZE20ttpqVkGjq26bvpTw==; sensorsdata2015jssdkcross=%7B%22distinct_id%22%3A%22193fb7ca61815f-02e045669168b-26011851-2073600-193fb7ca619ca2%22%2C%22first_id%22%3A%22%22%2C%22props%22%3A%7B%22%24latest_traffic_source_type%22%3A%22%E7%9B%B4%E6%8E%A5%E6%B5%81%E9%87%8F%22%2C%22%24latest_search_keyword%22%3A%22%E6%9C%AA%E5%8F%96%E5%88%B0%E5%80%BC_%E7%9B%B4%E6%8E%A5%E6%89%93%E5%BC%80%22%2C%22%24latest_referrer%22%3A%22%22%7D%2C%22%24device_id%22%3A%22193fb7ca61815f-02e045669168b-26011851-2073600-193fb7ca619ca2%22%7D; Ops-Token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyX2lkIjoyNTYxLCJ1c2VyX25hbWUiOiJsaWNodWFuZzMiLCJuYW1lIjoiXHU2NzRlXHU5NWVmIiwiaXNfc3VwZXJ1c2VyIjowLCJzdWIiOjE3NDkxMjQxNjksImV4cCI6MTc0OTcyODk2OX0.CKOaZOFcGXtWHI1h_b_-KvpFotiG9cxBqKRqimuf5Jc\n")
                .body(bodyJson)
                .asJson();

        System.out.println("Status: " + response.getStatus());
        System.out.println("Response: " + response.getBody().toPrettyString());
    }
}
