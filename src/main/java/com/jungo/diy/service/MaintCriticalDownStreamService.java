package com.jungo.diy.service;

import com.jungo.diy.entity.MaintCriticalDownStreamEntity;
import com.jungo.diy.mapper.MaintCriticalDownStreamMapper;
import com.jungo.diy.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author lichuang3
 * @date 2025-08-03 13:27
 */
@Service
public class MaintCriticalDownStreamService {

    @Autowired
    private MaintCriticalDownStreamMapper maintCriticalDownStreamMapper;

    public int batchSave(List<List<String>> csvData, LocalDate previousDate) {
        List<MaintCriticalDownStreamEntity> list = new ArrayList<>();
        Date date = DateUtils.getDate(previousDate);

        for (List<String> csvDatum : csvData) {
            MaintCriticalDownStreamEntity entity = new MaintCriticalDownStreamEntity();
            entity.setClientAppId(csvDatum.get(1));
            entity.setServerAppId(csvDatum.get(0));
            entity.setInterfaceUrl(csvDatum.get(2));
            entity.setP99(Integer.valueOf(csvDatum.get(4)));
            entity.setDate(date);
            list.add(entity);
        }

        return maintCriticalDownStreamMapper.batchInsert(list);
    }
}
