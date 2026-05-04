package com.finbrain.service;

import com.finbrain.dto.NavImportDTO;
import com.finbrain.dto.ProductImportDTO;
import com.finbrain.entity.DataImportLog;
import com.finbrain.vo.ImportResultVO;
import com.finbrain.vo.NavHistoryVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DataImportService {

    ImportResultVO importProducts(MultipartFile file, Long operatorId);

    ImportResultVO importNavData(MultipartFile file, Long operatorId);

    List<ProductImportDTO> parseProductExcel(MultipartFile file);

    List<NavImportDTO> parseNavExcel(MultipartFile file);

    void validateProductData(ProductImportDTO dto);

    void validateNavData(NavImportDTO dto);

    List<DataImportLog> getImportLogs(String importType, Integer pageNum, Integer pageSize);

    DataImportLog getImportLogById(Long id);

    List<NavHistoryVO> getNavHistory(Long productId, Integer pageNum, Integer pageSize);
}
