package com.finbrain.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.dto.NavImportDTO;
import com.finbrain.dto.ProductImportDTO;
import com.finbrain.entity.DataImportLog;
import com.finbrain.entity.FinancialProduct;
import com.finbrain.entity.NavHistory;
import com.finbrain.entity.User;
import com.finbrain.enums.ProductStatus;
import com.finbrain.mapper.DataImportLogMapper;
import com.finbrain.mapper.FinancialProductMapper;
import com.finbrain.mapper.NavHistoryMapper;
import com.finbrain.mapper.UserMapper;
import com.finbrain.service.DataImportService;
import com.finbrain.vo.ImportResultVO;
import com.finbrain.vo.NavHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportServiceImpl implements DataImportService {

    private final FinancialProductMapper productMapper;
    private final NavHistoryMapper navHistoryMapper;
    private final DataImportLogMapper importLogMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importProducts(MultipartFile file, Long operatorId) {
        String importNo = "IMP" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 6).toUpperCase();
        
        DataImportLog importLog = new DataImportLog();
        importLog.setImportNo(importNo);
        importLog.setImportType("product");
        importLog.setFileName(file.getOriginalFilename());
        importLog.setOriginalName(file.getOriginalFilename());
        importLog.setFileSize(file.getSize());
        importLog.setOperatorId(operatorId);
        importLog.setStatus("processing");
        importLog.setStartTime(LocalDateTime.now());
        
        if (operatorId != null) {
            User operator = userMapper.selectById(operatorId);
            if (operator != null) {
                importLog.setOperatorName(operator.getRealName() != null ? operator.getRealName() : operator.getUsername());
            }
        }
        
        importLogMapper.insert(importLog);
        
        ImportResultVO result = new ImportResultVO();
        result.setImportNo(importNo);
        result.setImportType("product");
        result.setFileName(file.getOriginalFilename());
        result.setStartTime(importLog.getStartTime());
        
        try {
            List<ProductImportDTO> products = parseProductExcel(file);
            result.setTotalCount(products.size());
            
            int successCount = 0;
            int failCount = 0;
            List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();
            
            for (ProductImportDTO dto : products) {
                try {
                    validateProductData(dto);
                    
                    if (!dto.isValid()) {
                        failCount++;
                        ImportResultVO.ImportErrorVO error = new ImportResultVO.ImportErrorVO();
                        error.setRowNum(dto.getRowNum());
                        error.setErrorMessage(dto.getErrorMessage());
                        errors.add(error);
                        continue;
                    }
                    
                    FinancialProduct product = new FinancialProduct();
                    product.setProductCode(dto.getProductCode());
                    product.setProductName(dto.getProductName());
                    product.setProductType(dto.getProductType());
                    product.setAnnualReturnRate(dto.getAnnualReturnRate());
                    product.setMinAmount(dto.getMinAmount());
                    product.setMaxAmount(dto.getMaxAmount());
                    product.setTermDays(dto.getTermDays());
                    product.setRiskLevel(dto.getRiskLevel());
                    product.setIssuer(dto.getIssuer());
                    product.setStartDate(dto.getStartDate());
                    product.setEndDate(dto.getEndDate());
                    product.setMaturityDate(dto.getMaturityDate());
                    product.setDescription(dto.getDescription());
                    product.setStatus(ProductStatus.DRAFT.getCode());
                    product.setSaleStatus(0);
                    
                    productMapper.insert(product);
                    successCount++;
                    
                } catch (Exception e) {
                    failCount++;
                    ImportResultVO.ImportErrorVO error = new ImportResultVO.ImportErrorVO();
                    error.setRowNum(dto.getRowNum());
                    error.setErrorMessage(e.getMessage());
                    errors.add(error);
                    log.error("导入产品失败: rowNum={}, error={}", dto.getRowNum(), e.getMessage());
                }
            }
            
            result.setSuccessCount(successCount);
            result.setFailCount(failCount);
            result.setErrors(errors);
            result.setStatus(failCount == 0 ? "success" : (successCount > 0 ? "partial" : "failed"));
            
            importLog.setTotalCount(products.size());
            importLog.setSuccessCount(successCount);
            importLog.setFailCount(failCount);
            importLog.setStatus(result.getStatus());
            importLog.setEndTime(LocalDateTime.now());
            importLogMapper.updateById(importLog);
            
            result.setEndTime(importLog.getEndTime());
            result.setDuration(java.time.Duration.between(importLog.getStartTime(), importLog.getEndTime()).toMillis());
            
            log.info("产品导入完成: importNo={}, total={}, success={}, fail={}", importNo, products.size(), successCount, failCount);
            
        } catch (Exception e) {
            result.setStatus("failed");
            result.setErrorMessage(e.getMessage());
            
            importLog.setStatus("failed");
            importLog.setErrorMessage(e.getMessage());
            importLog.setEndTime(LocalDateTime.now());
            importLogMapper.updateById(importLog);
            
            log.error("产品导入失败: importNo={}", importNo, e);
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importNavData(MultipartFile file, Long operatorId) {
        String importNo = "IMP" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 6).toUpperCase();
        
        DataImportLog importLog = new DataImportLog();
        importLog.setImportNo(importNo);
        importLog.setImportType("nav");
        importLog.setFileName(file.getOriginalFilename());
        importLog.setOriginalName(file.getOriginalFilename());
        importLog.setFileSize(file.getSize());
        importLog.setOperatorId(operatorId);
        importLog.setStatus("processing");
        importLog.setStartTime(LocalDateTime.now());
        
        if (operatorId != null) {
            User operator = userMapper.selectById(operatorId);
            if (operator != null) {
                importLog.setOperatorName(operator.getRealName() != null ? operator.getRealName() : operator.getUsername());
            }
        }
        
        importLogMapper.insert(importLog);
        
        ImportResultVO result = new ImportResultVO();
        result.setImportNo(importNo);
        result.setImportType("nav");
        result.setFileName(file.getOriginalFilename());
        result.setStartTime(importLog.getStartTime());
        
        try {
            List<NavImportDTO> navList = parseNavExcel(file);
            result.setTotalCount(navList.size());
            
            int successCount = 0;
            int failCount = 0;
            List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();
            
            for (NavImportDTO dto : navList) {
                try {
                    validateNavData(dto);
                    
                    if (!dto.isValid()) {
                        failCount++;
                        ImportResultVO.ImportErrorVO error = new ImportResultVO.ImportErrorVO();
                        error.setRowNum(dto.getRowNum());
                        error.setErrorMessage(dto.getErrorMessage());
                        errors.add(error);
                        continue;
                    }
                    
                    FinancialProduct product = productMapper.selectOne(
                            new LambdaQueryWrapper<FinancialProduct>()
                                    .eq(FinancialProduct::getProductCode, dto.getProductCode())
                    );
                    
                    if (product == null) {
                        failCount++;
                        ImportResultVO.ImportErrorVO error = new ImportResultVO.ImportErrorVO();
                        error.setRowNum(dto.getRowNum());
                        error.setErrorMessage("产品代码不存在: " + dto.getProductCode());
                        errors.add(error);
                        continue;
                    }
                    
                    NavHistory existing = navHistoryMapper.selectOne(
                            new LambdaQueryWrapper<NavHistory>()
                                    .eq(NavHistory::getProductId, product.getId())
                                    .eq(NavHistory::getNavDate, dto.getNavDate())
                    );
                    
                    if (existing != null) {
                        existing.setNav(dto.getNav());
                        existing.setAccumulatedNav(dto.getAccumulatedNav());
                        existing.setDailyReturnRate(dto.getDailyReturnRate());
                        existing.setDataSource("IMPORT");
                        navHistoryMapper.updateById(existing);
                    } else {
                        NavHistory navHistory = new NavHistory();
                        navHistory.setProductId(product.getId());
                        navHistory.setProductCode(dto.getProductCode());
                        navHistory.setProductName(dto.getProductName());
                        navHistory.setNav(dto.getNav());
                        navHistory.setAccumulatedNav(dto.getAccumulatedNav());
                        navHistory.setNavDate(dto.getNavDate());
                        navHistory.setDailyReturnRate(dto.getDailyReturnRate());
                        navHistory.setDataSource("IMPORT");
                        navHistoryMapper.insert(navHistory);
                    }
                    
                    successCount++;
                    
                } catch (Exception e) {
                    failCount++;
                    ImportResultVO.ImportErrorVO error = new ImportResultVO.ImportErrorVO();
                    error.setRowNum(dto.getRowNum());
                    error.setErrorMessage(e.getMessage());
                    errors.add(error);
                    log.error("导入净值失败: rowNum={}, error={}", dto.getRowNum(), e.getMessage());
                }
            }
            
            result.setSuccessCount(successCount);
            result.setFailCount(failCount);
            result.setErrors(errors);
            result.setStatus(failCount == 0 ? "success" : (successCount > 0 ? "partial" : "failed"));
            
            importLog.setTotalCount(navList.size());
            importLog.setSuccessCount(successCount);
            importLog.setFailCount(failCount);
            importLog.setStatus(result.getStatus());
            importLog.setEndTime(LocalDateTime.now());
            importLogMapper.updateById(importLog);
            
            result.setEndTime(importLog.getEndTime());
            result.setDuration(java.time.Duration.between(importLog.getStartTime(), importLog.getEndTime()).toMillis());
            
            log.info("净值导入完成: importNo={}, total={}, success={}, fail={}", importNo, navList.size(), successCount, failCount);
            
        } catch (Exception e) {
            result.setStatus("failed");
            result.setErrorMessage(e.getMessage());
            
            importLog.setStatus("failed");
            importLog.setErrorMessage(e.getMessage());
            importLog.setEndTime(LocalDateTime.now());
            importLogMapper.updateById(importLog);
            
            log.error("净值导入失败: importNo={}", importNo, e);
        }
        
        return result;
    }

    @Override
    public List<ProductImportDTO> parseProductExcel(MultipartFile file) {
        List<ProductImportDTO> products = new ArrayList<>();
        
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            int rowNum = 0;
            
            for (Row row : sheet) {
                if (rowNum == 0) {
                    rowNum++;
                    continue;
                }
                
                ProductImportDTO dto = new ProductImportDTO();
                dto.setRowNum(rowNum + 1);
                
                try {
                    dto.setProductCode(getCellValueAsString(row.getCell(0)));
                    dto.setProductName(getCellValueAsString(row.getCell(1)));
                    dto.setProductType(getCellValueAsString(row.getCell(2)));
                    dto.setAnnualReturnRate(getCellValueAsBigDecimal(row.getCell(3)));
                    dto.setMinAmount(getCellValueAsBigDecimal(row.getCell(4)));
                    dto.setMaxAmount(getCellValueAsBigDecimal(row.getCell(5)));
                    dto.setTermDays(getCellValueAsInteger(row.getCell(6)));
                    dto.setRiskLevel(getCellValueAsInteger(row.getCell(7)));
                    dto.setIssuer(getCellValueAsString(row.getCell(8)));
                    dto.setStartDate(getCellValueAsDate(row.getCell(9)));
                    dto.setEndDate(getCellValueAsDate(row.getCell(10)));
                    dto.setMaturityDate(getCellValueAsDate(row.getCell(11)));
                    dto.setDescription(getCellValueAsString(row.getCell(12)));
                    
                    products.add(dto);
                } catch (Exception e) {
                    dto.setValid(false);
                    dto.setErrorMessage("解析失败: " + e.getMessage());
                    products.add(dto);
                }
                
                rowNum++;
            }
            
        } catch (IOException e) {
            log.error("解析产品Excel失败", e);
            throw new RuntimeException("解析Excel文件失败: " + e.getMessage());
        }
        
        return products;
    }

    @Override
    public List<NavImportDTO> parseNavExcel(MultipartFile file) {
        List<NavImportDTO> navList = new ArrayList<>();
        
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            int rowNum = 0;
            
            for (Row row : sheet) {
                if (rowNum == 0) {
                    rowNum++;
                    continue;
                }
                
                NavImportDTO dto = new NavImportDTO();
                dto.setRowNum(rowNum + 1);
                
                try {
                    dto.setProductCode(getCellValueAsString(row.getCell(0)));
                    dto.setProductName(getCellValueAsString(row.getCell(1)));
                    dto.setNav(getCellValueAsBigDecimal(row.getCell(2)));
                    dto.setAccumulatedNav(getCellValueAsBigDecimal(row.getCell(3)));
                    dto.setNavDate(getCellValueAsDate(row.getCell(4)));
                    dto.setDailyReturnRate(getCellValueAsBigDecimal(row.getCell(5)));
                    
                    navList.add(dto);
                } catch (Exception e) {
                    dto.setValid(false);
                    dto.setErrorMessage("解析失败: " + e.getMessage());
                    navList.add(dto);
                }
                
                rowNum++;
            }
            
        } catch (IOException e) {
            log.error("解析净值Excel失败", e);
            throw new RuntimeException("解析Excel文件失败: " + e.getMessage());
        }
        
        return navList;
    }

    @Override
    public void validateProductData(ProductImportDTO dto) {
        if (!StringUtils.hasText(dto.getProductCode())) {
            dto.setValid(false);
            dto.setErrorMessage("产品代码不能为空");
            return;
        }
        
        if (!StringUtils.hasText(dto.getProductName())) {
            dto.setValid(false);
            dto.setErrorMessage("产品名称不能为空");
            return;
        }
        
        if (!StringUtils.hasText(dto.getProductType())) {
            dto.setValid(false);
            dto.setErrorMessage("产品类型不能为空");
            return;
        }
        
        if (dto.getAnnualReturnRate() == null) {
            dto.setValid(false);
            dto.setErrorMessage("年化收益率不能为空");
            return;
        }
        
        if (dto.getMinAmount() == null) {
            dto.setValid(false);
            dto.setErrorMessage("起购金额不能为空");
            return;
        }
        
        if (dto.getTermDays() == null) {
            dto.setValid(false);
            dto.setErrorMessage("期限不能为空");
            return;
        }
        
        FinancialProduct existing = productMapper.selectOne(
                new LambdaQueryWrapper<FinancialProduct>()
                        .eq(FinancialProduct::getProductCode, dto.getProductCode())
        );
        
        if (existing != null) {
            dto.setValid(false);
            dto.setErrorMessage("产品代码已存在: " + dto.getProductCode());
            return;
        }
    }

    @Override
    public void validateNavData(NavImportDTO dto) {
        if (!StringUtils.hasText(dto.getProductCode())) {
            dto.setValid(false);
            dto.setErrorMessage("产品代码不能为空");
            return;
        }
        
        if (dto.getNav() == null) {
            dto.setValid(false);
            dto.setErrorMessage("净值不能为空");
            return;
        }
        
        if (dto.getNavDate() == null) {
            dto.setValid(false);
            dto.setErrorMessage("净值日期不能为空");
            return;
        }
    }

    @Override
    public List<DataImportLog> getImportLogs(String importType, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<DataImportLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(importType)) {
            wrapper.eq(DataImportLog::getImportType, importType);
        }
        wrapper.orderByDesc(DataImportLog::getCreateTime);
        
        Page<DataImportLog> page = new Page<>(pageNum, pageSize);
        return importLogMapper.selectPage(page, wrapper).getRecords();
    }

    @Override
    public DataImportLog getImportLogById(Long id) {
        return importLogMapper.selectById(id);
    }

    @Override
    public List<NavHistoryVO> getNavHistory(Long productId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<NavHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NavHistory::getProductId, productId);
        wrapper.orderByDesc(NavHistory::getNavDate);
        
        Page<NavHistory> page = new Page<>(pageNum, pageSize);
        List<NavHistory> records = navHistoryMapper.selectPage(page, wrapper).getRecords();
        
        return records.stream().map(nav -> {
            NavHistoryVO vo = new NavHistoryVO();
            vo.setId(nav.getId());
            vo.setProductId(nav.getProductId());
            vo.setProductCode(nav.getProductCode());
            vo.setProductName(nav.getProductName());
            vo.setNav(nav.getNav());
            vo.setAccumulatedNav(nav.getAccumulatedNav());
            vo.setNavDate(nav.getNavDate());
            vo.setDailyReturnRate(nav.getDailyReturnRate());
            vo.setDataSource(nav.getDataSource());
            vo.setCreateTime(nav.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.STRING) {
            String value = cell.getStringCellValue().trim();
            if (StringUtils.hasText(value)) {
                return new BigDecimal(value);
            }
        }
        return null;
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.STRING) {
            String value = cell.getStringCellValue().trim();
            if (StringUtils.hasText(value)) {
                return Integer.parseInt(value);
            }
        }
        return null;
    }

    private LocalDate getCellValueAsDate(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (cell.getCellType() == CellType.STRING) {
            String value = cell.getStringCellValue().trim();
            if (StringUtils.hasText(value)) {
                return LocalDate.parse(value);
            }
        }
        return null;
    }
}
