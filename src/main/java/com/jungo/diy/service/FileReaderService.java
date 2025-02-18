package com.jungo.diy.service;


import com.jungo.diy.entity.ApiDailyPerformanceEntity;
import com.jungo.diy.mapper.ApiDailyPerformanceMapper;
import com.jungo.diy.model.PerformanceFileModel;
import com.jungo.diy.model.PerformanceFolderModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileReaderService {
    // 指定本地路径（Windows格式）
    private static final String PREFIX = "C:\\Users\\Lichuang3\\Desktop\\备份\\c端网关接口性能统计\\数据收集\\";

    @Value("${file.storage.dir}")
    private String targetDir;

    // 正则表达式预编译
    // 必须包含至少一个 -：通过正向预查 (?=.*-) 实现
    // 仅允许英文字母和 -：通过字符集 [A-Za-z-] 控制（需注意 - 在正则中需放在末尾避免歧义）
    private static final Pattern PATTERN = Pattern.compile("^(?=.*-)[A-Za-z-]+$");

    @Autowired
    ApiDailyPerformanceMapper apiDailyPerformanceMapper;

    public String readTargetFiles() {
        // 校验路径是否为空或包含非法字符
        if (targetDir == null || targetDir.trim().isEmpty()) {
            throw new IllegalArgumentException("目标目录不能为空");
        }

        // 尝试解析路径
        Path dirPath;
        try {
            dirPath = Paths.get(PREFIX + targetDir);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("路径包含非法字符: " + e.getMessage());
        }

        // 路径校验
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("目录不存在或不是文件夹");
        }
        PerformanceFolderModel performanceFolderModel = new PerformanceFolderModel();
        performanceFolderModel.setFolderName(targetDir);
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
                            String str = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                            String[] lines = str.split("\\R");
                            List<List<String>> data = new ArrayList<>();
                            performanceFileModel.setData(data);
                            for (String line : lines) {
                                // 去除首尾空格后按逗号分割（支持逗号前后有空格）
                                String[] parts = line.trim().split("\\s*,\\s*");
                                List<String> collect = Arrays.stream(parts).map(String::trim).map(FileReaderService::cleanSpecialQuotes).collect(Collectors.toList());
                                boolean checkListForSlash = checkListForSlash(collect);
                                if (checkListForSlash) {
                                    data.add(collect);
                                }
                            }
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
        return "success";
    }

    // 同时处理转义双引号和常规双引号
    public static String cleanSpecialQuotes(String input) {
        return StringUtils.strip(input,  "\"");
    }


    /**
     * 检查字符串列表中是否最多只有一个字符串包含 "/"
     * 此方法用于确保列表中包含"/"的字符串数量不超过一个
     *
     * @param list 待检查的字符串列表
     * @return 如果列表中包含"/"的字符串不超过一个，则返回true；否则返回false
     */
    public static boolean checkListForSlash(List<String> list) {
        int count = 0;

        // 遍历list，统计包含 "/" 的字符串个数
        for (String str : list) {
            if (str.contains("/") || str.contains("=")) {
                count++;
            }
            if (count > 1) {
                return false; // 如果有2个及以上的字符串包含 "/"，返回false
            }
        }

        // 如果列表中包含"/"的字符串不超过一个，返回true
        return true;
    }

    public static boolean containsSlash(String str) {
        // indexOf返回字符首次出现的位置，若不存在则返回 -1
        return str.contains("/");
    }

    private void writeDataToDatabase(PerformanceFolderModel performanceFolderModel) {
        String folderName = performanceFolderModel.getFolderName();
        // folderName转化成Date
        // 创建SimpleDateFormat实例，并指定日期格式
        Date date = getDateFromString(folderName);
        Map<String, List<List<String>>> map = performanceFolderModel.getFiles().stream().collect(Collectors.toMap(PerformanceFileModel::getFileName, PerformanceFileModel::getData));
        // 慢查询文件
        List<List<String>> slowRequestSheetModel = map.get("慢查询");
        Map<String, Integer> slowRequestSheetModelMap = slowRequestSheetModel.stream().collect(Collectors.toMap(x -> x.get(0) + x.get(1), x -> Integer.parseInt(x.get(2)), (x, y) -> x));
        // 请求情况文件
        List<List<String>> requestSheetModel = map.get("请求情况");

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
        apiDailyPerformanceMapper.batchInsert(apiDailyPerformanceEntities);
    }

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
        return input != null && PATTERN.matcher(input).matches();
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
}