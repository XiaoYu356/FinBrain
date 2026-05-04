package com.finbrain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.entity.OperationLog;
import com.finbrain.mapper.OperationLogMapper;
import com.finbrain.service.AuditService;
import com.finbrain.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void saveLog(OperationLog log) {
        operationLogMapper.insert(log);
    }

    @Override
    public Page<OperationLogVO> getLogList(String module, String operation, Long userId,
                                            LocalDateTime startTime, LocalDateTime endTime,
                                            Integer pageNum, Integer pageSize) {
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (module != null && !module.isEmpty()) {
            wrapper.eq(OperationLog::getModule, module);
        }
        
        if (operation != null && !operation.isEmpty()) {
            wrapper.eq(OperationLog::getOperation, operation);
        }
        
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        
        if (startTime != null) {
            wrapper.ge(OperationLog::getCreateTime, startTime);
        }
        
        if (endTime != null) {
            wrapper.le(OperationLog::getCreateTime, endTime);
        }
        
        wrapper.orderByDesc(OperationLog::getCreateTime);
        
        Page<OperationLog> logPage = operationLogMapper.selectPage(page, wrapper);
        
        Page<OperationLogVO> voPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        voPage.setRecords(logPage.getRecords().stream().map(log -> {
            OperationLogVO vo = new OperationLogVO();
            BeanUtil.copyProperties(log, vo);
            return vo;
        }).collect(Collectors.toList()));
        
        return voPage;
    }

    @Override
    public OperationLogVO getLogById(Long id) {
        OperationLog log = operationLogMapper.selectById(id);
        if (log == null) {
            return null;
        }
        OperationLogVO vo = new OperationLogVO();
        BeanUtil.copyProperties(log, vo);
        return vo;
    }

    @Override
    public List<String> getModules() {
        return operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>()
                        .select(OperationLog::getModule)
                        .groupBy(OperationLog::getModule)
        ).stream().map(OperationLog::getModule).collect(Collectors.toList());
    }

    @Override
    public List<String> getOperationsByModule(String module) {
        return operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>()
                        .select(OperationLog::getOperation)
                        .eq(OperationLog::getModule, module)
                        .groupBy(OperationLog::getOperation)
        ).stream().map(OperationLog::getOperation).collect(Collectors.toList());
    }

    @Override
    public long countByUserId(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getUserId, userId);
        
        if (startTime != null) {
            wrapper.ge(OperationLog::getCreateTime, startTime);
        }
        
        if (endTime != null) {
            wrapper.le(OperationLog::getCreateTime, endTime);
        }
        
        return operationLogMapper.selectCount(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanOldLogs(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        
        int deleted = operationLogMapper.delete(
                new LambdaQueryWrapper<OperationLog>()
                        .lt(OperationLog::getCreateTime, threshold)
        );
        
        log.info("清理 {} 天前的操作日志，共删除 {} 条", days, deleted);
    }
}
