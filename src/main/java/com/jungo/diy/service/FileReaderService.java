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

    private static final String CSV_EXTENSION = ".csv";

    private static final String SLOW_QUERY = "慢查询";
    private static final String REQUEST_INFO = "请求情况";
    private static final String DOMAIN_SLOW_QUERY = "域名慢查询";
    private static final String DOMAIN_REQUEST_INFO = "域名请求情况";

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
        // 验证目录路径并转换为Path对象
        Path dirPath = validateAndGetPath(directoryName);
        // 创建文件夹模型对象
        PerformanceFolderModel folderModel = createFolderModel(directoryName);
        try (Stream<Path> paths = Files.list(dirPath)) {
            // 处理目录中的文件:
            // 1. 过滤出常规文件：排除目录；排除符号链接；排除设备文件等其他特殊文件类型
            // 2. 限制前5个文件
            // 3. 处理每个文件
            // 4. 过滤掉null结果
            // 5. 收集到列表中
            folderModel.setFiles(
                    paths.filter(Files::isRegularFile)
                            .limit(5)
                            .map(this::processFile)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );
        } catch (IOException e) {
            throw new RuntimeException("目录访问失败", e);
        }

        // 将性能数据写入数据库
        writeDataToDatabase(folderModel);
        log.info("FileReaderService#readTargetFiles,成功处理目录:{}", directoryName);
        return "success";
    }

    private PerformanceFileModel processFile(Path path) {
        try {
            String fileName = path.getFileName().toString();
            if (!fileName.endsWith(CSV_EXTENSION)) {
                log.warn("跳过非CSV文件: {}", fileName);
                return null;
            }

            PerformanceFileModel fileModel = new PerformanceFileModel();
            fileModel.setFileName(removeExtension(fileName));
            fileModel.setData(getLists(path.getParent().toString(), path));
            return fileModel;
        } catch (IOException e) {
            log.error("文件处理失败: {}", path, e);
            return null;
        }
    }

    private String removeExtension(String fileName) {
        return fileName.substring(0, fileName.length() - CSV_EXTENSION.length());
    }

    private PerformanceFolderModel createFolderModel(String directoryName) {
        PerformanceFolderModel model = new PerformanceFolderModel();
        model.setFolderName(directoryName);
        model.setFiles(new ArrayList<>());
        return model;
    }

    private Path validateAndGetPath(String directoryName) {
        if (directoryName == null || directoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("目标目录不能为空");
        }

        Path path;
        try {
            path = Paths.get(PREFIX + directoryName);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("路径包含非法字符", e);
        }

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new IllegalArgumentException("目录不存在或不是文件夹");
        }
        return path;
    }

    /**
     * 从指定目录读取CSV数据并返回二维列表
     *
     * @param directoryName 目录名称
     * @param path 文件路径
     * @return 包含CSV数据的二维列表，每个子列表代表一行数据
     * @throws IOException 当发生I/O错误时抛出
     */
    private static List<List<String>> getLists(String directoryName, Path path) throws IOException {
        // 调用CsvUtils工具类读取CSV文件数据
        return CsvUtils.readCsvData(directoryName, path);
    }

    /**
     * 将性能文件夹数据写入数据库
     *
     * @param performanceFolderModel 包含要写入数据库的文件夹数据对象，不可为null
     *
     * 方法逻辑：
     * 1. 首先检查输入参数是否为null，若是则记录错误日志并返回
     * 2. 获取文件夹名称并校验是否为空或空字符串
     * 3. 调用write2DBNew方法将数据写入数据库
     * 4. 捕获并处理可能出现的异常
     *
     * 注意：
     * - 使用StringUtils.isEmpty()方法判断字符串是否为空
     * - 所有错误情况都会记录详细的错误日志
     * - 方法会在遇到错误时提前返回，避免继续执行
     */
    public void writeDataToDatabase(PerformanceFolderModel performanceFolderModel) {
        if (performanceFolderModel == null) {
            log.error("FileReaderService#writeDataToDatabase,Attempted to write null performance folder model！");
            return;
        }
        try {
            // >= 2025-01-17 后的数据按照下面的方式进行写入数据库
            String folderName = performanceFolderModel.getFolderName();
            if (StringUtils.isEmpty(folderName)) {
                log.error("FileReaderService#writeDataToDatabase,Empty folder name in performance folder model！");
                return;
            }
            // folderName转化成LocalDate
            write2DBNew(performanceFolderModel, folderName);
        } catch (Exception e) {
            log.error("FileReaderService#writeDataToDatabase,Failed to write performance data to DB for folder: {}", performanceFolderModel.getFolderName(), e);
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
        if (performanceFolderModel == null || performanceFolderModel.getFiles() == null) {
            throw new IllegalArgumentException("Performance files data is null");
        }
        // folderName转化成Date
        // 创建SimpleDateFormat实例，并指定日期格式
        Date date = getDateFromString(folderName);
        Map<String, List<List<String>>> map = performanceFolderModel.getFiles().stream().collect(Collectors.toMap(PerformanceFileModel::getFileName, PerformanceFileModel::getData));
        // 慢查询文件
        List<List<String>> slowRequestSheetModel = map.get(SLOW_QUERY);
        Map<String, Integer> slowRequestSheetModelMap = slowRequestSheetModel.stream().collect(Collectors.toMap(x -> x.get(0) + x.get(1), x -> Integer.parseInt(x.get(2)), (x, y) -> x));
        // 请求情况文件
        List<List<String>> requestSheetModel = deduplicate(map.get(REQUEST_INFO));
        // 域名慢查询文件
        List<List<String>> domainSlowRequestSheetModel = map.get(DOMAIN_SLOW_QUERY);
        if (domainSlowRequestSheetModel == null) {
            domainSlowRequestSheetModel = new ArrayList<>();
        }
        Map<String, Integer> domainSlowRequestSheetModelMap = domainSlowRequestSheetModel.stream().collect(Collectors.toMap(x -> x.get(0), x -> Integer.parseInt(x.get(1)), (x, y) -> x));
        // 域名请求情况文件
        List<List<String>> domainRequestSheetModel = map.get(DOMAIN_REQUEST_INFO);
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

        // 判断字符是否大于255个字符
        int byteLength = path.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength > 255) {
            log.error("FileReaderService#checkUrl,出现异常！字符数大于255，{}", path);
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

    /**
     * 获取指定日期范围内的多个文件
     *
     * @param startDirectoryName 起始日期字符串（格式：yyyy-MM-dd）
     * @param endDirectoryName 结束日期字符串（格式：yyyy-MM-dd）
     * @return 操作结果，"success"表示执行成功
     */
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