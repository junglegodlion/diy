package com.jungo.diy.test;



import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

public class UnirestPostExample {
    public static void main(String[] args) {
        HttpResponse<JsonNode> response = Unirest.post("https://jsonplaceholder.typicode.com/posts")
                .header("Content-Type", "application/json")
                .body("{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}")
                .asJson();

        System.out.println("Status: " + response.getStatus());
        System.out.println("Response: " + response.getBody().toPrettyString());
    }
}
