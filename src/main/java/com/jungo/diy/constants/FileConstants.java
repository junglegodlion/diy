package com.jungo.diy.constants;

/**
 * @author lichuang3
 * @date 2025-07-18 16:54
 */
public class FileConstants {

    public static final String OUTPUT_DIRECTORY = System.getProperty("user.home") +
            "/Desktop/备份/c端网关接口性能统计/数据统计/输出/";


    public static final String[] STATUS_CODE_URLS = {
            "/cl-list-aggregator/channel/getChannelModuleInfo",
            "/mlp-product-search-api/module/search/pageListAndFilter",
            "/cl-tire-site/tireListModule/getTireList",
            "/cl-maint-api/maintMainline/getBasicMaintainData",
            "/cl-maint-mainline/mainline/getDynamicData",
            "/cl-maint-api/mainline/maintenance/basic",
            "/cl-oto-front-api/batteryList/getBatteryList",
            "/mlp-product-search-api/module/search/pageList",
            "/mlp-product-search-api/main/search/api/mainProduct",
            "/ext-website-cl-beauty-api/channelPage/v4/getBeautyHomeShopListAndRecommendLabel",
            "/cl-product-components/GoodsDetail/detailModuleInfo",
            "/cl-tire-site/tireModule/getTireDetailModuleData",
            "/cl-maint-mainline/productMainline/getMaintProductDetailInfo",
            "/cl-product-components/GoodsDetail/productDetailModularInfoForBff",
            "/cl-maint-order-create/order/getConfirmOrderData"
    };

    public static final String[] BUSINESS_ERROR_URLS = {
            "/channel/getChannelModuleInfo",
            "/module/search/pageListAndFilter",
            "/tireListModule/getTireList",
            "/maintMainline/getBasicMaintainData",
            "/mainline/getDynamicData",
            "/mainline/maintenance/basic",
            "/batteryList/getBatteryList",
            "/module/search/pageList",
            "/main/search/api/mainProduct",
            "/channelPage/v4/getBeautyHomeShopListAndRecommendLabel",
            "/GoodsDetail/detailModuleInfo",
            "/tireModule/getTireDetailModuleData",
            "/productMainline/getMaintProductDetailInfo",
            "/GoodsDetail/productDetailModularInfoForBff",
            "/order/getConfirmOrderData"
    };


    public static final String[] STATUS_COLUMN_TITLES = {
            "host", "url", "status", "请求次数", "请求总数", "占比", "非200占比"
    };
    public static final String[] BUSINESS_COLUMN_TITLES = {
            "服务名称", "接口路径", "总请求量", "非10000请求量", "非10000占比", "10000占比"
    };

    public static final String[] SUCCESS_COLUMN_TITLES = {
            "服务名称", "接口路径", "总请求量", "非10000请求量", "非10000占比", "10000占比", "非200占比", "成功率"
    };
}
