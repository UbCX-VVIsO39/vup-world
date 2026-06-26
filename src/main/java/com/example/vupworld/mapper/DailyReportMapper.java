package com.example.vupworld.mapper;

import com.example.vupworld.model.DailyReport;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 日报 Mapper —— SQL 已迁移至 resources/mapper/DailyReportMapper.xml
 * 答辩时可展示：注解方式见其他 Mapper，XML 方式见本文件对应的 XML 映射。
 */
@Mapper
public interface DailyReportMapper {

    void insert(DailyReport dailyReport);

    DailyReport findByVupIdAndDay(Long vupId, int day);

    List<DailyReport> findAllByVupId(Long vupId);

    void updateStrategyPanelJson(Long id, String strategyPanelJson);
}
