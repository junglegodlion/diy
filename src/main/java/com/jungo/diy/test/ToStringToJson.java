package com.jungo.diy.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.*;

public class ToStringToJson {

    public static void main(String[] args) throws Exception {
        String input = "CreateOrderBaseResponse(code=10000, message=操作成功, data=GetMaintenanceOrderPageInfoResponse(contact=ContactInfoModel(addressId={55EC0EFA-A68C-4F59-B9FE-ACFD9F8CDB82}, consignees=大好人, cellphone=155\\*\\*\\*\\*\\*880,provinceId=1, province=上海市, cityId=1, city=上海市, districtId=35, district=黄浦区, townId=310101013, townName=外滩街道, addressDetail=还好吧不过估计你呢, isDefaultAddress=true, completedAddress=null, completeAddressTip=null), installTypes=\\[InstallTypeModel(code=1, installType=配送到店)], payMotheds=\\[PayMothedModel(code=1, payMothed=在线支付)], shop=ShopInfoModel(shopId=30460, carparName=测试新增3, province=陕西省, city=西安市, address=上海市闵行区顾戴路2688号, distance=0.79, shopTip=null, payType=null, satisfactionInfoModel=null), recommendShop=RecommendShopModel(shopTip=null, shops=\\[RecommendShopInfoModel(shopId=30460, carparName=测试新增3, province=陕西省, city=西安市, address=上海市闵行区顾戴路2688号, distance=0.79, shopImage=[https://img2.tuhu.org/Images/Marketing/Shops/FmCTIXtFviwyC0BwEyQ4JO3BwLbx\\_w398\\_h398.jpg](https://img2.tuhu.org/Images/Marketing/Shops/FmCTIXtFviwyC0BwEyQ4JO3BwLbx_w398_h398.jpg), payTypes=\\[在线支付, 到店支付], tags=\\[距离最近], checkStatus=true, arrivalTime=null, labelTypeName=null, labelType=null, deliveryTip=null, satisfactionInfoModel=null), RecommendShopInfoModel(shopId=11727, carparName=途虎养车工场店（莲花路店）, province=上海市, city=闵行区, address=合川路麦当劳, distance=1.19, shopImage=[https://img4.tuhu.org/Images/Marketing/Shops/FkaS\\_\\_kJ8P8NryVSlaB-fUebV3LW\\_w564\\_h564.jpg](https://img4.tuhu.org/Images/Marketing/Shops/FkaS__kJ8P8NryVSlaB-fUebV3LW_w564_h564.jpg), payTypes=\\[在线支付, 到店支付], tags=\\[口碑好店], checkStatus=false, arrivalTime=预计2025-05-17发货, labelTypeName=null, labelType=null, deliveryTip=交通管制，配送可能不及时，请耐心等待并安抚客户，给您造成的不便敬请谅解！, satisfactionInfoModel=SatisfactionInfoModel(labelName=放心装, satisfactionFieldModels=\\[SatisfactionFieldModel(areaType=satisfactionRate, title=车主好评率95.1%, content=null)])), RecommendShopInfoModel(shopId=38, carparName=途虎养车工场店（仙霞小周）, province=上海市, city=黄浦区, address=莲花路111测试, distance=1.25, shopImage=[https://img4.tuhu.org/Images/Marketing/Shops/e8vRoGa\\_JXD9I347\\_1USeA\\_w132\\_h132.jpeg](https://img4.tuhu.org/Images/Marketing/Shops/e8vRoGa_JXD9I347_1USeA_w132_h132.jpeg), payTypes=\\[在线支付, 到店支付], tags=\\[], checkStatus=false, arrivalTime=门店现货，下单后可立即预约安装, labelTypeName=马上装, labelType=0, deliveryTip=null, satisfactionInfoModel=SatisfactionInfoModel(labelName=放心装, satisfactionFieldModels=\\[SatisfactionFieldModel(areaType=satisfactionRate, title=车主好评率95%, content=null)]))], recommendId=ed98bcdc035f4de2a44aae83bd4f1049, refreshShops=true, shopMemoryPath=true), orderPackages=\\[OrderPackageInfoModel(pid=CP-SHELL-ULTRAGXDL-XBY|1, activityId=b3a88e06-b8f6-4911-845c-dc4c4f2e4941, name=壳牌/Shell 超凡喜力 天然气全合成机油 高效动力版 小保养套餐, imageUrl=[https://img4.tuhu.org/image/Exw5r5lJFs74or92Uus5QQ\\_w278\\_h388.png@100Q.png](https://img4.tuhu.org/image/Exw5r5lJFs74or92Uus5QQ_w278_h388.png@100Q.png), price=30.69, packagePriceInfo=PackagePriceModel(packagePayAmount=30.69, packageActivityPreferentialMoney=null, packageProductMoney=null, preferential=null, proofId=null), packages=\\[MaintenancePackageModel(packageType=xby, packageName=小保养, packageOriginPrice=573.56, packageInstallServiceOriginPrice=50.0, items=\\[MaintenancePackageItemModel(maintenanceType=jiyou, zhName=发动机油, partCategory=MaterialPart, products=\\[MaintenanceProductModel(pid=OL-MO-S2000-5W30|1, activityId=null, productName=【正品授权】美孚/Mobil 新速霸2000全合成机油 5W-30 SN级 4L, shortTitle=杀人, price=28.66, marketingPrice=500.0, originalPrice=500.0, takePrice=28.38, count=1, imageUrl=[https://img1.tuhu.org/image/1QbYXbcEAfCBvnKH14bQ5w\\_w371\\_h367.png@250w\\_250h\\_100Q.png](https://img1.tuhu.org/image/1QbYXbcEAfCBvnKH14bQ5w_w371_h367.png@250w_250h_100Q.png), currentInstallType=null, productType=product, activityInfo=ProductActivityModel(activityId=b3a88e06-b8f6-4911-845c-dc4c4f2e4941, activityType=AllNetActivity), isHazardousChemical=false, childProducts=null, limitExchangeTip=null)], partServiceType=), MaintenancePackageItemModel(maintenanceType=jv, zhName=机油滤清器, partCategory=MaterialPart, products=\\[MaintenanceProductModel(pid=HP-FI-MAN-OIL|3, activityId=null, productName=加雷思/Gareth 机油滤清器 HX-218, shortTitle=null, price=1.34, marketingPrice=45.0, originalPrice=23.56, takePrice=1.32, count=1, imageUrl=[https://img4.tuhu.org/Images/Products/FvXQGJOyMyujAoU8HN\\_cBy1rG1Mo\\_w800\\_h800.png@250w\\_250h\\_100Q.png](https://img4.tuhu.org/Images/Products/FvXQGJOyMyujAoU8HN_cBy1rG1Mo_w800_h800.png@250w_250h_100Q.png), currentInstallType=null, productType=product, activityInfo=ProductActivityModel(activityId=b3a88e06-b8f6-4911-845c-dc4c4f2e4941, activityType=AllNetActivity), isHazardousChemical=false, childProducts=null, limitExchangeTip=null)], partServiceType=)], installServices=\\[MaintenanceInstallServiceModel(pid=FU-BY-XBY|, productName=【人工费】机油+机滤安装费, price=1.0, takePrice=0.99, marketingPrice=50.0, originalPrice=50.0, count=1, imageUrl=[https://img4.tuhu.org/qIZnukkT1HK7V9dQ9WYS9w\\_w1053\\_h636.png@250w\\_250h\\_100Q.png@100Q.png](https://img4.tuhu.org/qIZnukkT1HK7V9dQ9WYS9w_w1053_h636.png@250w_250h_100Q.png@100Q.png), description=相关操作：保养完成需对保养提示灯进行归零复位，特殊车型保养灯复位请与门店协商, productType=installService)], gifts=\\[MaintenanceProductModel(pid=XU-556161744727179271|1, activityId=null, productName=赠品送E卡zhh专用10, shortTitle=null, price=0.0, marketingPrice=10.0, originalPrice=0.0, takePrice=0.0, count=2, imageUrl=[https://img4.tuhu.org/image/vqZFbWcCg5UtgbylLgYjtw\\_w800\\_h800.png@250w\\_250h\\_100Q.png](https://img4.tuhu.org/image/vqZFbWcCg5UtgbylLgYjtw_w800_h800.png@250w_250h_100Q.png), currentInstallType=null, productType=gift, activityInfo=null, isHazardousChemical=null, childProducts=null, limitExchangeTip=null)], virtualPackage=false, quotationPackage=false, services=null, packageInstallServiceReferencePrice=null, packageInstallServiceFacePrice=50.0, showPackageInstallServiceReductionTag=false, packageInstallServiceReductionMsg=null, packageInstallServiceReductionPreferentialPrice=null)], packageGifts=null, newPriceTag=null, extendedInfo=null, maintProductType=4)], commonInstallServices=\\[CommonInstallServiceModel(pid=FU-TUHU-MFQCJC|1, packageName=免费常规检测, productName=免费常规检测服务, price=0.0, marketingPrice=0.0, count=1, imageUrl=[https://img2.tuhu.org/Images/Products/06f6/b8af/b7781166b9ad7bccb00f7777\\_w800\\_h800.png@100Q.png](https://img2.tuhu.org/Images/Products/06f6/b8af/b7781166b9ad7bccb00f7777_w800_h800.png@100Q.png), description=免费常规检测服务, type=1)], optionals=null, optionalExtendInfo=null, deliveryFeeInfo=DeliveryFeeInfoModel(totalFee=40.0, remark=null), coupon=null, arrivalTimeInfo=ArrivalTimeInfoModel(arrivalTime=null, labelType=null, labelTypeName=null, content=, title=null), insurances=\\[null], integral=IntegralInfoModel(useIntegral=false, amount=0.0, integralNum=10, integralValidity=false, minAvailIntegral=100, description=1、提交订单后，对应积分即转换为E卡，用于订单支付时抵扣。\\n2、使用积分抵扣的商品，当产生退款等退回返还时，将以积分形式返回（而不是E卡）。同时，有效期仍延续提交订单时的积分有效期。\\n3、E卡可用于订单支付时，1元E卡抵扣1元现金。途虎有权调整换算比例，具体以E卡准则为准。, title=null), serviceFeeInfo=ServiceFeeDerateInfoModel(feeDerateDetailList=\\[], totalDerateMoney=null), priceInfo=PriceInfoModel(preferential=PreferentialModel(couponPreferentialMoney=null, activityPreferentialMoney=-542.56, packagePreferentialMoney=null, maintenancePackagePreferentialMoney=0.0, maintenancePackagePreferentialShow=套餐立减, cardPreferentialMoney=0.0, redemptionCardPreferentialMoney=null, blackCardPreferentialMoney=0.0, superMemberPreferentialMoney=null, singleDiscountPreferentialMoney=0.0), serviceMoney=50.0, serviceTakeTotalPrice=0.99, preferentialMoney=-542.56, productMoney=523.56, deliveryFee=40.0, integralMoney=0.0, accountCardMoney=0.0, payAmount=71.0, actualPayMoney=71.0, totalPrice=573.56, priceDetailList=\\[PriceDetailModel(name=商品总价, value=523.56, type=1), PriceDetailModel(name=工时费, value=50.0, type=2), PriceDetailModel(name=运费, value=40.0, type=4), PriceDetailModel(name=活动优惠, value=-542.56, type=16)], scenePriceInfos=null), tip=null, disclaimerMsg=null, orderCreatable=true, activityReward=ActivityRewardModel(taskRewardName=利益点2-对C, reward=第二组20分, activityId=3FAmIeA5eYXrW, unitId=3TLyv5KTt0Sr0), activityRewardInfo=ActivityRewardInfoModel(title=活动, activityRewards=\\[ActivityRewardModel(taskRewardName=1, reward=430第一组, activityId=371t1gbGZQPfC, unitId=3QhkHXovrDCyC), ActivityRewardModel(taskRewardName=利益点2-对C, reward=第二组20分, activityId=3FAmIeA5eYXrW, unitId=3TLyv5KTt0Sr0), ActivityRewardModel(taskRewardName=1, reward=自动化-1, activityId=3HTrqnnWKRzud, unitId=3aoV7a4TAvve2), ActivityRewardModel(taskRewardName=2, reward=自动化-口碑, activityId=3IBo7DJBQw7Iy, unitId=3aqYZrObqKJjX)]), retrieved=RetrievedModel(awardHint=本单安装后即享第二组20分！, popup=PopupModel(reportInfo=ReportInfoModel(sceneCode=maintConfirm\\_retrievedPopup, events=\\[firstNotPopup]), popupInfo=null)), invoiceInfo=InvoiceInfoModel(showInvoiceModule=false), hideModules=\\[OrderConfirmHideModuleModel(moduleType=genuineProduct)], quotationInfo=QuotationInfoModel(iqrNo=null), mlpCouponModel=null, popupTip=null, toastTip=null, showDeliveredHome=false, satisfactionFlag=true, preferentialBannerInfo=null, paymentInfo=PaymentInfoModel(paymentSubCategoryInfos=\\[PaymentSubInfoModel(paymentCategoryCode=WX\\_APP, paymentCategoryName=微信支付, icon=[https://img1.tuhu.org/tech/pic/U-BbzNdL6ThD4GamnurkPw\\_w72\\_h72.jpeg](https://img1.tuhu.org/tech/pic/U-BbzNdL6ThD4GamnurkPw_w72_h72.jpeg), discountPrice=null, paymentCategoryExtraDesc=null)], changePayWayButton=ChangePayWayButtonModel(colorType=3, description=更换), payProductInfos=\\[com.tuhu.maint.order.create.facade.model.PayProductInfoModel\\@629933dd], orderCategory=CAR\\_MAINTENANCE, payMoney=7100, scene=V2, paymentEventType=0), fulfillmentProcessModel=FulfillmentProcessModel(fulfillmentProcessUrl=[https://v.tuhu.org/image/l-jIJc-0obS4wSfCAogSbw\\_w1053\\_h42.png](https://v.tuhu.org/image/l-jIJc-0obS4wSfCAogSbw_w1053_h42.png)), abTestCodes=\\[by\\_paid\\_second\\_recommend], postOrderJumpLink=null, extendInfo=OrderPageExtendModel(showTakeStyle=false, redemptionType=null), orderGiftAggModel=OrderGiftAggModel(giftTitle=E卡、E卡, gifts=\\[OrderGiftViewModel(pid=XU-556161744727179271|1, imageUrl=[https://img1.tuhu.org/image/vqZFbWcCg5UtgbylLgYjtw\\_w800\\_h800.png@100Q.png@100Q.png](https://img1.tuhu.org/image/vqZFbWcCg5UtgbylLgYjtw_w800_h800.png@100Q.png@100Q.png), floatingLayerRewardCustomName=E卡（价值299.97元）, description=【赠E卡】), OrderGiftViewModel(pid=XU-556161744727179271|1, imageUrl=[https://img1.tuhu.org/image/vqZFbWcCg5UtgbylLgYjtw\\_w800\\_h800.png@100Q.png@100Q.png](https://img1.tuhu.org/image/vqZFbWcCg5UtgbylLgYjtw_w800_h800.png@100Q.png@100Q.png), floatingLayerRewardCustomName=E卡（价值299.97元）, description=【赠E卡】)]))) ";

        Map<String, Object> result = parseToMap(input);
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String json = mapper.writeValueAsString(result);
        System.out.println(json);
    }

    public static Map<String, Object> parseToMap(String str) {
        return parseObject(str);
    }

    private static Map<String, Object> parseObject(String input) {
        Map<String, Object> map = new LinkedHashMap<>();
        input = input.trim();

        int start = input.indexOf('(');
        String objectName = (start > 0) ? input.substring(0, start).trim() : "Object";
        String content = (start > 0 && input.endsWith(")")) ? input.substring(start + 1, input.length() - 1) : input;

        List<String> tokens = splitKeyValuePairs(content);

        for (String token : tokens) {
            int eqIndex = token.indexOf('=');
            if (eqIndex != -1) {
                String key = token.substring(0, eqIndex).trim();
                String value = token.substring(eqIndex + 1).trim();
                if (value.matches("^[A-Za-z_][A-Za-z_0-9]*\\(.*\\)$")) {
                    map.put(key, parseObject(value));
                } else if (value.startsWith("[") && value.endsWith("]")) {
                    map.put(key, parseList(value));
                } else {
                    map.put(key, stripQuotes(value));
                }
            }
        }

        return Collections.singletonMap(objectName, map);
    }

    private static List<String> splitKeyValuePairs(String input) {
        List<String> tokens = new ArrayList<>();
        int depth = 0;
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == '(' || c == '[') depth++;
            else if (c == ')' || c == ']') depth--;
            if (c == ',' && depth == 0) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) tokens.add(sb.toString().trim());
        return tokens;
    }

    private static List<Object> parseList(String input) {
        String content = input.substring(1, input.length() - 1).trim();
        List<Object> result = new ArrayList<>();
        List<String> elements = splitKeyValuePairs(content);
        for (String el : elements) {
            if (el.matches("^[A-Za-z_][A-Za-z_0-9]*\\(.*\\)$")) {
                result.add(parseObject(el));
            } else {
                result.add(stripQuotes(el));
            }
        }
        return result;
    }

    private static String stripQuotes(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) return value.substring(1, value.length() - 1);
        return value;
    }
}
