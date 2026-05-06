package com.example.repair.modules.workorder.dto;

import com.example.repair.modules.workorder.entity.WorkOrder;
import com.example.repair.modules.workorder.entity.WorkOrderImage;
import com.example.repair.modules.workorder.entity.WorkOrderProgress;
import com.example.repair.modules.workorder.entity.WorkOrderRating;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkOrderDetailResponse {
  private WorkOrder workOrder;
  private List<WorkOrderImage> images;
  private List<WorkOrderProgress> progress;
  private WorkOrderRating rating;

  private String requesterPhone;
  private String workerPhone;
}

