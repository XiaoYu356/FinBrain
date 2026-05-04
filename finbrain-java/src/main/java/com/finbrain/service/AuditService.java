package com.finbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.entity.OperationLog;
import com.finbrain.vo.OperationLogVO;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditService {

    void saveLog(OperationLog log);

    Page<OperationLogVO> getLogList(String module, String operation, Long userId, 
                                     LocalDateTime startTime, LocalDateTime endTime,
                                     Integer pageNum, Integer pageSize);

    OperationLogVO getLogById(Long id);

    List<String> getModules();

    List<String> getOperationsByModule(String module);

    long countByUserId(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    void cleanOldLogs(int days);
}
