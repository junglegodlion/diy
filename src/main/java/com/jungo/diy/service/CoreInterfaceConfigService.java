package com.jungo.diy.service;

import com.jungo.diy.entity.CoreInterfaceConfigEntity;
import com.jungo.diy.mapper.CoreInterfaceConfigMapper;
import com.jungo.diy.request.CoreInterfaceConfigRequest;
import com.jungo.diy.util.TableUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lichuang3
 * @date 2025-02-25 19:21
 */
@Service
public class CoreInterfaceConfigService {

    @Autowired
    CoreInterfaceConfigMapper coreInterfaceConfigMapper;

    public void write2DB(List<List<String>> data) {

        // 创建实体对象列表
        List<CoreInterfaceConfigEntity> coreInterfaceConfigEntities = createCoreInterfaceConfigEntities(data);

        // 插入数据库
        if (coreInterfaceConfigEntities != null && !coreInterfaceConfigEntities.isEmpty()) {
            coreInterfaceConfigMapper.batchInsert(coreInterfaceConfigEntities);
        }

    }

    private List<CoreInterfaceConfigEntity> createCoreInterfaceConfigEntities(List<List<String>> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        List<CoreInterfaceConfigEntity> coreInterfaceConfigEntities = new ArrayList<>();
        for (int i = 1; i < data.size(); i++) {
            List<String> list = data.get(i);
            CoreInterfaceConfigEntity coreInterfaceConfigEntity = new CoreInterfaceConfigEntity();
            coreInterfaceConfigEntity.setPageName(list.get(0));
            coreInterfaceConfigEntity.setInterfaceUrl(list.get(1));

            if (StringUtils.isNotBlank(list.get(2))) {
                coreInterfaceConfigEntity.setP99Target(TableUtils.convertStringToInteger(list.get(2)));
            }
            coreInterfaceConfigEntity.setInterfaceType(TableUtils.convertStringToInteger(list.get(4)));
            coreInterfaceConfigEntity.setSortOrder(TableUtils.convertStringToInteger(list.get(5)));
            coreInterfaceConfigEntity.setOwner(list.get(6));
            coreInterfaceConfigEntities.add(coreInterfaceConfigEntity);
        }
        return coreInterfaceConfigEntities;
    }

    public int insert(CoreInterfaceConfigRequest request) {
        CoreInterfaceConfigEntity coreInterfaceConfigEntity = new CoreInterfaceConfigEntity();
        coreInterfaceConfigEntity.setPageName(request.getPageName());
        coreInterfaceConfigEntity.setInterfaceUrl(request.getInterfaceUrl());
        coreInterfaceConfigEntity.setHost(request.getHost());
        coreInterfaceConfigEntity.setP99Target(request.getP99Target());
        coreInterfaceConfigEntity.setSlowRequestRateTarget(request.getSlowRequestRateTarget());
        coreInterfaceConfigEntity.setInterfaceType(request.getInterfaceType());
        coreInterfaceConfigEntity.setSortOrder(request.getSortOrder());
        coreInterfaceConfigEntity.setOwner(request.getOwner());

        int result = coreInterfaceConfigMapper.insert(coreInterfaceConfigEntity);
        if (result > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}
