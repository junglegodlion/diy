package com.jungo.diy.service;


import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.entity.GateWayDailyPerformanceEntity;
import com.jungo.diy.model.PerformanceFileModel;
import com.jungo.diy.model.PerformanceFolderModel;
import com.jungo.diy.repository.PerformanceRepository;
import com.jungo.diy.util.CsvUtils;
import com.jungo.diy.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author lichuang3
 */
@Service
@Slf4j
public class FileReaderService {
    // 指定本地路径（Windows格式）
    private static final String PREFIX = "C:\\Users\\Lichuang3\\Desktop\\备份\\c端网关接口性能统计\\数据收集\\";

    // 正则表达式预编译
    // 必须包含至少一个 -：通过正向预查 (?=.*-) 实现
    // 仅允许英文字母和 -：通过字符集 [A-Za-z-] 控制（需注意 - 在正则中需放在末尾避免歧义）
    private static final Pattern PATTERN = Pattern.compile("^(?=.*-)[A-Za-z-]+$");


    @Autowired
    PerformanceRepository performanceRepository;

    /**
     * 读取指定目录下的文件，并将文件内容写入数据库
     *
     * 主要流程：
     * 1. 参数校验与路径解析
     * 2. 创建文件夹数据模型
     * 3. 读取目录下前4个普通文件
     * 4. 解析文件内容并进行数据清洗
     * 5. 将结构化数据持久化到数据库
     *
     * @param directoryName 目标文件夹名称（相对路径，不需要包含系统前缀）
     * @return 固定返回"success"字符串表示操作成功，异常时会抛出运行时异常
     * @throws IllegalArgumentException 当参数不合法或路径无效时抛出
     * @throws RuntimeException 当目录访问失败时抛出
     */
    public String readTargetFiles(String directoryName) {
        // 校验路径是否为空或包含非法字符
        if (directoryName == null || directoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("目标目录不能为空");
        }

        // 尝试解析路径
        Path dirPath;
        try {
            dirPath = Paths.get(PREFIX + directoryName);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("路径包含非法字符: " + e.getMessage());
        }

        // 路径校验
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("目录不存在或不是文件夹");
        }
        PerformanceFolderModel performanceFolderModel = new PerformanceFolderModel();
        performanceFolderModel.setFolderName(directoryName);
        List<PerformanceFileModel> files = new ArrayList<>();
        performanceFolderModel.setFiles(files);

        try (Stream<Path> paths = Files.list(dirPath)) {
            paths.filter(Files::isRegularFile)
                    .limit(4)
                    .forEach(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            // fileName去除.csv后缀
                            fileName = fileName.substring(0, fileName.length() - 4);
                            PerformanceFileModel performanceFileModel = new PerformanceFileModel();
                            performanceFileModel.setFileName(fileName);
                            List<List<String>> data = getLists(directoryName, path);
                            performanceFileModel.setData(data);
                            files.add(performanceFileModel);
                        } catch (IOException e) {
                            log.error("FileReaderService#readTargetFiles,出现异常！", e);
                        }
                    });

        } catch (IOException e) {
            throw new RuntimeException("目录访问失败", e);
        }

        // 将性能数据写入数据库
        writeDataToDatabase(performanceFolderModel);
        log.info("FileReaderService#readTargetFiles,成功将性能数据写入数据库！文件名为：{}", directoryName);
        return "success";
    }

    private static List<List<String>> getLists(String directoryName, Path path) throws IOException {

        return CsvUtils.getData(directoryName, path);
    }

    public void writeDataToDatabase(PerformanceFolderModel performanceFolderModel) {
        try {
            // >= 2025-01-17 后的数据按照下面的方式进行写入数据库
            String folderName = performanceFolderModel.getFolderName();
            // folderName转化成LocalDate
            write2DBNew(performanceFolderModel, folderName);
        } catch (Exception e) {
            log.error("FileReaderService#writeDataToDatabase,出现异常！", e);
        }

    }


    /**
     * 对请求表数据进行去重处理，保留每个token中总请求数最大的记录
     *
     * @param requestSheetModel 原始请求表数据，每个元素为一个记录行，包含token和请求数等信息
     * @return 去重后的请求表数据列表，每个token仅保留请求数最大的记录
     */
    private List<List<String>> deduplicate(List<List<String>> requestSheetModel) {
        Map<String, List<String>> tokenMap = new HashMap<>();
        for (List<String> record : requestSheetModel) {
            String token = (record.get(0) + record.get(1).trim()).toLowerCase();
            tokenMap.merge(token, record, (existingRecord, newRecord) -> {
                int existingCount = Integer.parseInt(existingRecord.get(2));
                int newCount = Integer.parseInt(newRecord.get(2));
                return newCount > existingCount ? newRecord : existingRecord;
            });
        }
        return new ArrayList<>(tokenMap.values());
    }

    private void write2DBNew(PerformanceFolderModel performanceFolderModel, String folderName) {
        // folderName转化成Date
        // 创建SimpleDateFormat实例，并指定日期格式
        Date date = getDateFromString(folderName);
        Map<String, List<List<String>>> map = performanceFolderModel.getFiles().stream().collect(Collectors.toMap(PerformanceFileModel::getFileName, PerformanceFileModel::getData));
        // 慢查询文件
        List<List<String>> slowRequestSheetModel = map.get("慢查询");
        Map<String, Integer> slowRequestSheetModelMap = slowRequestSheetModel.stream().collect(Collectors.toMap(x -> x.get(0) + x.get(1), x -> Integer.parseInt(x.get(2)), (x, y) -> x));
        // 请求情况文件
        List<List<String>> requestSheetModel = deduplicate(map.get("请求情况"));
        // 域名慢查询文件
        List<List<String>> domainSlowRequestSheetModel = map.get("域名慢查询");
        if (domainSlowRequestSheetModel == null) {
            domainSlowRequestSheetModel = new ArrayList<>();
        }
        Map<String, Integer> domainSlowRequestSheetModelMap = domainSlowRequestSheetModel.stream().collect(Collectors.toMap(x -> x.get(0), x -> Integer.parseInt(x.get(1)), (x, y) -> x));
        // 域名请求情况文件
        List<List<String>> domainRequestSheetModel = map.get("域名请求情况");
        if (domainRequestSheetModel == null) {
            domainRequestSheetModel = new ArrayList<>();
        }
        List<GateWayDailyPerformanceEntity> gateWayDailyPerformanceEntities = getGateWayDailyPerformanceEntities(domainRequestSheetModel, date, domainSlowRequestSheetModelMap);
        List<ApiDailyPerformanceEntity> apiDailyPerformanceEntities = getApiDailyPerformanceEntities(requestSheetModel, date, slowRequestSheetModelMap);

        performanceRepository.writePerformanceData2DB(gateWayDailyPerformanceEntities, apiDailyPerformanceEntities);
    }

    private List<GateWayDailyPerformanceEntity> getGateWayDailyPerformanceEntities(List<List<String>> domainRequestSheetModel,
                                                                                   Date date,
                                                                                   Map<String, Integer> domainSlowRequestSheetModelMap) {
        if (domainRequestSheetModel == null) {
            return Collections.emptyList();
        }
        List<GateWayDailyPerformanceEntity> gateWayDailyPerformanceEntities = new ArrayList<>();
        for (List<String> list : domainRequestSheetModel) {
            GateWayDailyPerformanceEntity gateWayDailyPerformanceEntity = new GateWayDailyPerformanceEntity();
            gateWayDailyPerformanceEntity.setDate(date);
            gateWayDailyPerformanceEntity.setHost(list.get(0));
            gateWayDailyPerformanceEntity.setP999(Integer.parseInt(list.get(2)));
            gateWayDailyPerformanceEntity.setP99(Integer.parseInt(list.get(3)));
            gateWayDailyPerformanceEntity.setP90(Integer.parseInt(list.get(4)));
            gateWayDailyPerformanceEntity.setP75(Integer.parseInt(list.get(5)));
            gateWayDailyPerformanceEntity.setP50(Integer.parseInt(list.get(6)));
            gateWayDailyPerformanceEntity.setTotalRequestCount(Integer.parseInt(list.get(1)));
            gateWayDailyPerformanceEntity.setSlowRequestCount(domainSlowRequestSheetModelMap.getOrDefault(list.get(0), 0));
            gateWayDailyPerformanceEntities.add(gateWayDailyPerformanceEntity);
        }

        return gateWayDailyPerformanceEntities;
    }

    private List<ApiDailyPerformanceEntity> getApiDailyPerformanceEntities(List<List<String>> requestSheetModel, Date date, Map<String, Integer> slowRequestSheetModelMap) {
        List<ApiDailyPerformanceEntity> apiDailyPerformanceEntities = new ArrayList<>();
        for (List<String> list : requestSheetModel) {
            String url = list.get(1);
            if (checkUrl(url)) {
                ApiDailyPerformanceEntity apiDailyPerformanceEntity = new ApiDailyPerformanceEntity();
                apiDailyPerformanceEntity.setDate(date);
                apiDailyPerformanceEntity.setHost(list.get(0));
                apiDailyPerformanceEntity.setUrl(url);
                apiDailyPerformanceEntity.setTotalRequestCount(Integer.parseInt(list.get(2)));
                apiDailyPerformanceEntity.setP999(Integer.parseInt(list.get(3)));
                apiDailyPerformanceEntity.setP99(Integer.parseInt(list.get(4)));
                apiDailyPerformanceEntity.setP90(Integer.parseInt(list.get(5)));
                apiDailyPerformanceEntity.setP75(Integer.parseInt(list.get(6)));
                apiDailyPerformanceEntity.setP50(Integer.parseInt(list.get(7)));
                apiDailyPerformanceEntity.setSlowRequestCount(slowRequestSheetModelMap.getOrDefault(list.get(0) + list.get(1), 0));
                apiDailyPerformanceEntities.add(apiDailyPerformanceEntity);
            }
        }
        return apiDailyPerformanceEntities;
    }

    // 匹配以字母开头，包含字母、数字和下划线，并以字母或数字结尾的正则表达式
    private boolean checkUrl(String path) {
        if (StringUtils.isBlank(path)) {
            return false;
        }

        // 1. 去除首部斜杠（适配不同路径格式）
        String trimmedPath = path.startsWith("/")  ? path.substring(1)  : path;

        // 2. 按斜杠分割路径
        String[] segments = trimmedPath.split("/");

        if (segments.length < 1) {
            return false;
        }
        String input = segments[0];
        return input != null && PATTERN.matcher(input).matches() && !path.contains("\\");
    }

    private static Date getDateFromString(String folderName) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            // 使用parse()方法将String转换为Date
            return dateFormat.parse(folderName);
        } catch (ParseException e) {
            log.error("FileReaderService#writeDataToDatabase,出现异常！", e);
        }

        return null;
    }

    public Object getMultiFile(String startDirectoryName, String endDirectoryName) {
        LocalDate startDate = DateUtils.getLocalDate(startDirectoryName, DateUtils.YYYY_MM_DD);
        LocalDate endDate = DateUtils.getLocalDate(endDirectoryName, DateUtils.YYYY_MM_DD);
        // 遍历日期范围
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String formattedDate = DateUtils.getDateString(date, DateUtils.YYYY_MM_DD);
            readTargetFiles(formattedDate);
        }
        return "success";
    }
}