package com.jungo.diy.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 昨日接口峰值QPS分析工具
 * <p>
 * 通过查询 Elasticsearch 聚合数据，迭代缩小时间窗口，精确定位峰值QPS时间段。
 * 依赖：OkHttp3、Jackson Databind
 */
public class PeakQpsAnalyzer {

    private static final String ES_URL = "https://int-service-elk.tuhuyun.cn"
            + "/elasticsearch/logstash-int-service-server-side-log-*/_search"
            + "?rest_total_hits_as_int=true"
            + "&ignore_unavailable=true"
            + "&ignore_throttled=true"
            + "&preference=1774940490516"
            + "&timeout=30000ms";

    // 初始聚合时间窗口（首次查询用10分钟桶）
    private static final String INITIAL_FIXED_INTERVAL = "10m";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Scanner STDIN = new Scanner(System.in);

    // -------------------------------------------------------------------------
    // 入口
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        // 收集必要参数
        String serverAppId = promptRequired("请输入 server_app_id（如 int-service-mkt-rule-engine-service）：");
        String requestTargetUrl = promptRequired("请输入 request_target_url（如 /rule/matchRule）：");
        System.out.print("请输入 request_params 关键词（可选，直接回车跳过）：");
        String requestParams = STDIN.nextLine().trim();
        if (requestParams.isEmpty()) {
            requestParams = null;
        }

        // 初始时间范围：昨天北京时间 00:00:00 ~ 23:59:59（对应UTC）
        OffsetDateTime[] range = buildYesterdayRangeUtc();
        String timeFrom = formatUtc(range[0]);
        String timeTo = formatUtc(range[1]);

        System.out.println("\n📅 分析时间范围（UTC）：" + timeFrom + " ~ " + timeTo);

        // 第一步：获取 Cookie 并请求，直到拿到有效数据
        String cookie = promptRequired("请粘贴 Cookie（从浏览器开发者工具复制）：");
        String fixedInterval = INITIAL_FIXED_INTERVAL;

        BucketResult peakBucket = null;
        Duration interval = null;

        while (true) {
            String responseBody = requestWithRetry(cookie, serverAppId, requestTargetUrl,
                    requestParams, timeFrom, timeTo, fixedInterval);

            // 第二步：解析峰值 Bucket
            BucketAnalysis analysis = analyzeBuckets(responseBody);

            if (analysis == null) {
                // buckets 为空
                System.out.println("⚠️  该时间范围内没有日志数据，请确认参数是否正确。");
                return;
            }

            peakBucket = analysis.peakBucket;
            interval = analysis.interval;

            printPeakInfo(peakBucket, interval);

            // 间隔已经是1秒，精度最小，直接结束
            if (interval.getSeconds() <= 1) {
                break;
            }

            // 第三步：询问用户是否继续细化
            System.out.printf(
                    "%n是否继续细化时间范围（当前窗口 %s，doc_count=%d）？[y/N] ",
                    formatDuration(interval), peakBucket.docCount
            );
            String answer = STDIN.nextLine().trim();
            if (!answer.equalsIgnoreCase("y")) {
                break;
            }

            // 构建更小的时间范围
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(peakBucket.keyAsString)
                    .withOffsetSameInstant(ZoneOffset.UTC);
            OffsetDateTime bucketStart = getBucketStart(interval, offsetDateTime);
            OffsetDateTime bucketEnd = getBucketEnd(interval, offsetDateTime);

            timeFrom = formatUtc(bucketStart);
            timeTo = formatUtc(bucketEnd);

            // 将下一次聚合桶大小缩小为当前 interval 的 1/6（至少1秒）
            fixedInterval = computeNextInterval(interval);

            System.out.println("\n🔍 缩小时间范围：" + timeFrom + " ~ " + timeTo
                    + "，聚合粒度：" + fixedInterval);
            System.out.println("    (北京时间：" + toBeijingDisplay(timeFrom) + " ~ " + toBeijingDisplay(timeTo) + ")");
        }

        // 最终结果输出
        printFinalResult(serverAppId, requestTargetUrl, peakBucket, interval);
    }

    private static OffsetDateTime getBucketEnd(Duration interval, OffsetDateTime offsetDateTime) {
        if (interval.compareTo(Duration.ofMinutes(1)) > 0) {
            // duration > 1分钟
            return offsetDateTime.plus(interval);
        }

        return offsetDateTime.plus(Duration.ofMinutes(1));
    }

    private static OffsetDateTime getBucketStart(Duration interval, OffsetDateTime offsetDateTime) {
        if (interval.compareTo(Duration.ofMinutes(1)) > 0) {
            // duration > 1分钟
            return offsetDateTime.minus(interval);
        }

        return offsetDateTime.minus(Duration.ofMinutes(1));
    }

    // -------------------------------------------------------------------------
    // Step 1：带 Cookie 重试的请求
    // -------------------------------------------------------------------------

    private static String requestWithRetry(
            String cookie,
            String serverAppId,
            String requestTargetUrl,
            String requestParams,
            String timeFrom,
            String timeTo,
            String fixedInterval
    ) throws IOException {
        while (true) {
            String body = doRequest(cookie, serverAppId, requestTargetUrl,
                    requestParams, timeFrom, timeTo, fixedInterval);

            if (isValidEsResponse(body)) {
                return body;
            }

            // 响应异常（登录页、401/403 等）
            System.out.println("\n⚠️  Cookie 已过期或无效，请重新从浏览器复制最新的 Cookie：");
            cookie = promptRequired("新 Cookie：");
        }
    }

    private static String doRequest(
            String cookie,
            String serverAppId,
            String requestTargetUrl,
            String requestParams,
            String timeFrom,
            String timeTo,
            String fixedInterval
    ) throws IOException {
        OkHttpClient client = new OkHttpClient();

        String jsonBody = buildRequestBody(serverAppId, requestTargetUrl,
                requestParams, timeFrom, timeTo, fixedInterval);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), jsonBody);

        Request request = new Request.Builder()
                .url(ES_URL)
                .post(body)
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                .addHeader("kbn-version", "7.6.1")
                .addHeader("Content-Type", "application/json")
                .addHeader("Cookie", cookie)
                .addHeader("Origin", "https://int-service-elk.tuhuyun.cn")
                .addHeader("Referer", "https://int-service-elk.tuhuyun.cn/app/kibana")
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String text = responseBody != null ? responseBody.string() : "";

            if (!response.isSuccessful()) {
                System.out.printf("⚠️  HTTP %d，响应：%s%n", response.code(),
                        text.substring(0, Math.min(200, text.length())));
                return ""; // 触发重试
            }
            return text;
        }
    }

    // -------------------------------------------------------------------------
    // Step 2：解析 Buckets，找峰值
    // -------------------------------------------------------------------------

    private static boolean isValidEsResponse(String body) {
        if (body == null || body.isEmpty()) {
            return false;
        }
        try {
            JsonNode root = JSON.readTree(body);
            JsonNode buckets = root.path("aggregations").path("2").path("buckets");
            return buckets.isArray() && !buckets.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static BucketAnalysis analyzeBuckets(String body) throws Exception {
        JsonNode root = JSON.readTree(body);
        JsonNode buckets = root.path("aggregations").path("2").path("buckets");

        if (!buckets.isArray() || buckets.isEmpty()) {
            return null;
        }

        // 计算相邻桶之间的时间间隔
        Duration interval;
        if (buckets.size() >= 2) {
            OffsetDateTime t0 = OffsetDateTime.parse(buckets.get(0).path("key_as_string").asText());
            OffsetDateTime t1 = OffsetDateTime.parse(buckets.get(1).path("key_as_string").asText());
            interval = Duration.between(t0, t1).abs();
        } else {
            // 只有一个桶，无法计算间隔，视为已经足够精确
            interval = Duration.ofSeconds(1);
        }

        // 找 doc_count 最大的桶
        JsonNode peakNode = null;
        long maxDocCount = -1;
        for (JsonNode bucket : buckets) {
            long docCount = bucket.path("doc_count").asLong();
            if (docCount > maxDocCount) {
                maxDocCount = docCount;
                peakNode = bucket;
            }
        }

        BucketResult peak = new BucketResult(
                peakNode.path("key_as_string").asText(),
                maxDocCount
        );

        return new BucketAnalysis(peak, interval);
    }

    // -------------------------------------------------------------------------
    // 请求 Body 构建
    // -------------------------------------------------------------------------

    private static String buildRequestBody(
            String serverAppId,
            String requestTargetUrl,
            String requestParams,
            String timeFrom,
            String timeTo,
            String fixedInterval
    ) {
        // 按需拼接 request_params 过滤条件
        String paramsFilter = (requestParams != null && !requestParams.isEmpty())
                ? "{\"match_phrase\":{\"request_params\":\"" + requestParams + "\"}},"
                : "";

        return "{"
                + "\"version\":true,"
                + "\"size\":0,"  // 只需要聚合数据，size=0 避免返回无用的 hits
                + "\"sort\":[{\"@timestamp\":{\"order\":\"desc\",\"unmapped_type\":\"boolean\"}}],"
                + "\"aggs\":{"
                + "\"2\":{"
                + "\"date_histogram\":{"
                + "\"field\":\"@timestamp\","
                + "\"fixed_interval\":\"" + fixedInterval + "\","
                + "\"time_zone\":\"Asia/Shanghai\","
                + "\"min_doc_count\":1"
                + "}"
                + "}"
                + "},"
                + "\"stored_fields\":[\"*\"],"
                + "\"script_fields\":{},"
                + "\"docvalue_fields\":["
                + "{\"field\":\"@timestamp\",\"format\":\"date_time\"}"
                + "],"
                + "\"_source\":{\"excludes\":[]},"
                + "\"query\":{"
                + "\"bool\":{"
                + "\"must\":[{\"match_all\":{}}],"
                + "\"filter\":["
                + "{\"match_phrase\":{\"server_app_id\":\"" + serverAppId + "\"}},"
                + "{\"match_phrase\":{\"request_target_url\":\"" + requestTargetUrl + "\"}},"
                + paramsFilter
                + "{\"range\":{\"@timestamp\":{"
                + "\"gte\":\"" + timeFrom + "\","
                + "\"lte\":\"" + timeTo + "\","
                + "\"format\":\"strict_date_optional_time\""
                + "}}}"
                + "],"
                + "\"should\":[],"
                + "\"must_not\":[]"
                + "}"
                + "}"
                + "}";
    }

    // -------------------------------------------------------------------------
    // 时间工具
    // -------------------------------------------------------------------------

    /**
     * 返回昨天北京时间 [00:00:00, 23:59:59.999] 对应的 UTC OffsetDateTime 数组
     */
    private static OffsetDateTime[] buildYesterdayRangeUtc() {
        // 必须基于北京时间（UTC+8）的"昨天"来计算，不能用 UTC 日期 minusDays(1)
        // 北京时间昨天 00:00:00 CST = 北京时间昨天日期 - 8小时 (UTC)
        ZoneOffset cst = ZoneOffset.ofHours(8);
        LocalDate yesterdayCst = OffsetDateTime.now(cst).toLocalDate().minusDays(1);
        // 昨天北京时间 00:00:00 转 UTC
        OffsetDateTime from = yesterdayCst.atTime(0, 0, 0, 0)
                .atOffset(cst)
                .withOffsetSameInstant(ZoneOffset.UTC);
        // 昨天北京时间 23:59:59.999 转 UTC
        OffsetDateTime to = yesterdayCst.atTime(23, 59, 59, 999_000_000)
                .atOffset(cst)
                .withOffsetSameInstant(ZoneOffset.UTC);
        return new OffsetDateTime[]{from, to};
    }

    private static String formatUtc(OffsetDateTime dt) {
        return dt.withOffsetSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }

    /**
     * 将 UTC 时间字符串（yyyy-MM-dd'T'HH:mm:ss.SSS'Z'）转为北京时间可读格式
     */
    private static String toBeijingDisplay(String utcStr) {
        try {
            // formatUtc() 输出的末尾是字面量 'Z'，用 ISO_OFFSET_DATE_TIME 无法直接解析
            // 先把尾部的 Z 替换为 +00:00，再用标准格式解析
            String normalized = utcStr.endsWith("Z")
                    ? utcStr.substring(0, utcStr.length() - 1) + "+00:00"
                    : utcStr;
            OffsetDateTime utc = OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            OffsetDateTime cst = utc.withOffsetSameInstant(ZoneOffset.ofHours(8));
            return cst.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return utcStr;
        }
    }


    /**
     * 将 interval 缩小为约 1/6，向下取整到合理单位：
     * 1h -> 10m, 10m -> 1m, 1m -> 10s, 10s -> 1s
     */
    private static String computeNextInterval(Duration interval) {
        long seconds = interval.getSeconds();
        long next = Math.max(1, seconds / 6);

        if (next >= 3600) {
            return (next / 3600) + "h";
        }
        if (next >= 60) {
            return (next / 60) + "m";
        }
        return next + "s";
    }

    private static String formatDuration(Duration d) {
        long s = d.getSeconds();
        if (s >= 3600) return (s / 3600) + "h";
        if (s >= 60) return (s / 60) + "m";
        return s + "s";
    }

    // -------------------------------------------------------------------------
    // 控制台输出
    // -------------------------------------------------------------------------

    private static void printPeakInfo(BucketResult peak, Duration interval) {
        long qps = interval.getSeconds() > 0 ? peak.docCount / interval.getSeconds() : peak.docCount;
        System.out.println("\n📊 当前峰值 Bucket 信息：");
        System.out.println("  时间段开始：" + peak.keyAsString);
        System.out.println("  文档数（请求量）：" + peak.docCount);
        System.out.println("  时间窗口大小：" + formatDuration(interval));
        System.out.println("  估算峰值QPS：" + qps + " req/s");
    }

    private static void printFinalResult(
            String serverAppId,
            String requestTargetUrl,
            BucketResult peak,
            Duration interval
    ) {
        long qps = interval.getSeconds() > 0 ? peak.docCount / interval.getSeconds() : peak.docCount;
        System.out.println("\n✅ 接口峰值QPS分析完成");
        System.out.println("服务：" + serverAppId);
        System.out.println("接口：" + requestTargetUrl);
        System.out.println("🏆 峰值时间段：" + peak.keyAsString);
        System.out.println("📦 该时间窗口请求量：" + peak.docCount + " 次");
        System.out.println("⏱  时间窗口大小：" + formatDuration(interval));
        System.out.println("🚀 估算峰值QPS：" + qps + " req/s");
    }

    private static String promptRequired(String prompt) {
        String value;
        do {
            System.out.print(prompt);
            value = STDIN.nextLine().trim();
            if (value.isEmpty()) System.out.println("此项不能为空，请重新输入。");
        } while (value.isEmpty());
        return value;
    }

    // -------------------------------------------------------------------------
    // 内部数据类
    // -------------------------------------------------------------------------

    private static class BucketResult {
        final String keyAsString;
        final long docCount;

        BucketResult(String keyAsString, long docCount) {
            this.keyAsString = keyAsString;
            this.docCount = docCount;
        }
    }

    private static class BucketAnalysis {
        final BucketResult peakBucket;
        final Duration interval;

        BucketAnalysis(BucketResult peakBucket, Duration interval) {
            this.peakBucket = peakBucket;
            this.interval = interval;
        }
    }
}