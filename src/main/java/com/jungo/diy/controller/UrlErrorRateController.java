package com.jungo.diy.controller;

import com.jungo.diy.model.ExcelModel;
import com.jungo.diy.model.SheetModel;
import com.jungo.diy.model.UrlStatusErrorModel;
import com.jungo.diy.util.CsvUtils;
import com.jungo.diy.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lichuang3
 * @date 2025-04-25 15:31
 */
@RestController
@Slf4j
@RequestMapping("/urlErrorRate")
public class UrlErrorRateController {

    @PostMapping("/upload/statusError")
    public void readFile(@RequestParam("file") MultipartFile file) throws IOException {
        List<List<String>> listList = CsvUtils.getDataFromInputStream(file.getInputStream());
        List<UrlStatusErrorModel> urlStatusErrorModels = new ArrayList<>();
        for (int i = 1; i < listList.size(); i++) {
            List<String> list = listList.get(i);
            UrlStatusErrorModel urlStatusErrorModel = new UrlStatusErrorModel();
            urlStatusErrorModel.setHost(list.get(0));
            urlStatusErrorModel.setUrl(list.get(1));
            urlStatusErrorModel.setStatus(Integer.parseInt(list.get(2)));
            urlStatusErrorModel.setCount(Integer.parseInt(list.get(3)));
            urlStatusErrorModels.add(urlStatusErrorModel);
        }

        // urlStatusErrorModels按照url进行分组
        Map<String, List<UrlStatusErrorModel>> urlStatusErrorModelMap = urlStatusErrorModels.stream()
                .collect(Collectors.groupingBy(UrlStatusErrorModel::getUrl));

        List<UrlStatusErrorModel> newUrlStatusErrorModels = new ArrayList<>();
        for (Map.Entry<String, List<UrlStatusErrorModel>> entry : urlStatusErrorModelMap.entrySet()) {
            List<UrlStatusErrorModel> value = entry.getValue();
            // 统计每个url的总请求数
            int totalCount = 0;
            int not200Count = 0;
            for (UrlStatusErrorModel urlStatusErrorModel : value) {
                Integer count = urlStatusErrorModel.getCount();
                totalCount = totalCount + count;
                if (urlStatusErrorModel.getStatus() != 200) {
                    not200Count = not200Count + count;
                }
            }
            int finalTotalCount = totalCount;
            int finalNot200Count = not200Count;
            value.forEach(urlStatusErrorModel -> {
                urlStatusErrorModel.setTotalCount(finalTotalCount);
                urlStatusErrorModel.setNot200Count(finalNot200Count);
                newUrlStatusErrorModels.add(urlStatusErrorModel);
            });

        }
        List<String> urlList = new ArrayList<>();
        urlList.add("/cl-tire-site/tireListModule/getTireList");
        urlList.add("/cl-maint-api/maintMainline/getBasicMaintainData");
        urlList.add("/cl-maint-mainline/mainline/getDynamicData");
        urlList.add("/cl-oto-front-api/batteryList/getBatteryList");
        urlList.add("/mlp-product-search-api/module/search/pageList");
        urlList.add("/mlp-product-search-api/main/search/api/mainProduct");
        urlList.add("/ext-website-cl-beauty-api/channelPage/v4/getBeautyHomeShopListAndRecommendLabel");
        urlList.add("/cl-product-components/GoodsDetail/detailModuleInfo");
        urlList.add("/cl-tire-site/tireModule/getTireDetailModuleData");
        urlList.add("/cl-maint-mainline/productMainline/getMaintProductDetailInfo");
        urlList.add("/cl-product-components/GoodsDetail/productDetailModularInfoForBff");
        urlList.add("/cl-ordering-aggregator/ordering/getOrderConfirmFloatLayerData");
        urlList.add("/cl-maint-order-create/order/getConfirmOrderData");

        // 将newUrlStatusErrorModels按照url排序，url按照urlList的顺序排序,如果url相同，再按照status排序
        List<UrlStatusErrorModel> sortedUrlStatusErrorModels = newUrlStatusErrorModels.stream()
                .sorted(Comparator.comparingInt(o -> urlList.indexOf(o.getUrl())))
                .sorted(Comparator.comparingInt(UrlStatusErrorModel::getStatus))
                .collect(Collectors.toList());

        System.out.println(JsonUtils.objectToJson(sortedUrlStatusErrorModels));
    }
}
