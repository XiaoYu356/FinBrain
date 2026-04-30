package com.finbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.dto.ProductSearchDTO;
import com.finbrain.entity.FinancialProduct;
import com.finbrain.vo.ProductVO;

import java.util.List;

public interface ProductService {

    Page<ProductVO> searchProducts(ProductSearchDTO dto);

    ProductVO getProductById(Long id);

    List<FinancialProduct> searchByParams(String productType, Double minRate, Double maxRate, Integer minTerm, Integer maxTerm);
}
