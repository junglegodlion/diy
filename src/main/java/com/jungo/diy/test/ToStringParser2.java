package com.jungo.diy.test;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToStringParser2 {

    public static void main(String[] args) throws Exception {
        String input = "BizResponse(code=10000, message=操作成功, data=ActivityPreviewResponse(unitPreviewInfos=[UnitPreviewInfo(activityId=3FLw07UdFK7ZO, versionInterval=null, unitId=2Vw1XqQ0YqdxO, unitType=MASTER_ORDER_AUDIT_UNIT, unitName=内容审核, unitDesc=, unitNameForC=途虎养车反馈有礼, unitBenefitPointForC=途虎反馈有礼, unitConditionDescForC=反馈体验 得途虎25元E卡, awardConfigs=[AwardConfig(unitAwardId=3QWmX9gKmh8dU, awardRuleGroup=3TXb2TMd3klKM, awardType=E_CARD, benefitId=3T3BiPdM9n0d8, awardValue=2500, grantMethod=AUTO, awardBenefitPointForC=途虎反馈有礼 得价值25元途虎E卡（支付直接抵扣）, awardBenefitPointUrlForC=null, awardBenefitPointUrlForPop=null, redeemTipsForC=null, awardContent={\"appId\": \"WXAPP\", \"channel\": \"activityUnit\", \"businessId\": \"activityUnit\", \"costCenter\": \"a\", \"financialAccount\": \"10296642\"})], hisRelatedEventIds=[950432637], activityPreviewInfo=ActivityPreviewInfo(activityId=3FLw07UdFK7ZO, activityType=5810, activityStartDate=Tue Apr 01 00:00:00 CST 2025, activityEndDate=Tue Jul 01 00:00:00 CST 2025, createdTime=Mon Mar 24 10:06:01 CST 2025, activityMaterialInfos=null, extraFieldMap={BIZ_SCENE=MARKETING, ACTIVITY_TEMPLATE=REPUTATION, ACTIVITY_RULE_URL=, ACTIVITY_NAME_FOR_C=途虎养车反馈有礼}))]))";
        Map<String, Object> jsonMap = parseToMap(input);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap);
        System.out.println(json);
    }

    public static Map<String, Object> parseToMap(String input) {
        int firstParen = input.indexOf('(');
        String rootName = input.substring(0, firstParen);
        String body = input.substring(firstParen + 1, input.lastIndexOf(')'));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(rootName, parseBody(body));
        return map;
    }

    private static Object parseBody(String body) {
        Map<String, Object> map = new LinkedHashMap<>();
        List<String> tokens = tokenize(body);

        for (String token : tokens) {
            int eqIdx = token.indexOf('=');
            if (eqIdx == -1) continue;

            String key = token.substring(0, eqIdx).trim();
            String value = token.substring(eqIdx + 1).trim();

            if (isObjectLike(value)) {
                map.put(key, parseToMap(value));
            } else if (value.startsWith("[") && value.endsWith("]")) {
                map.put(key, parseList(value));
            } else {
                map.put(key, value);
            }
        }

        return map;
    }

    private static List<Object> parseList(String listStr) {
        List<Object> list = new ArrayList<>();
        String content = listStr.substring(1, listStr.length() - 1);
        List<String> tokens = tokenize(content);

        for (String item : tokens) {
            item = item.trim();
            if (isObjectLike(item)) {
                list.add(parseToMap(item));
            } else {
                list.add(item);
            }
        }

        return list;
    }

    private static List<String> tokenize(String str) {
        List<String> tokens = new ArrayList<>();
        int depth = 0;
        StringBuilder sb = new StringBuilder();
        char[] chars = str.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == ',' && depth == 0) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                if (c == '(' || c == '[') depth++;
                else if (c == ')' || c == ']') depth--;
                sb.append(c);
            }
        }

        if (sb.length() > 0) {
            tokens.add(sb.toString().trim());
        }

        return tokens;
    }

    private static boolean isObjectLike(String value) {
        return value.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\(.*\\)$");
    }
}
