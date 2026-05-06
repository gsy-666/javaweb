package com.example.repair.modules.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.repair.modules.workorder.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {}

