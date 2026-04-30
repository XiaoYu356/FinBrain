package com.finbrain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.dto.ProductSearchDTO;
import com.finbrain.entity.FinancialProduct;
import com.finbrain.mapper.FinancialProductMapper;
import com.finbrain.service.ProductService;
import com.finbrain.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final FinancialProductMapper productMapper;

    @Override
    public Page<ProductVO> searchProducts(ProductSearchDTO dto) {
        Page<FinancialProduct> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        
        LambdaQueryWrapper<FinancialProduct> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(dto.getProductType())) {
            wrapper.eq(FinancialProduct::getProductType, dto.getProductType());
        }
        if (dto.getMinRate() != null) {
            wrapper.ge(FinancialProduct::getAnnualReturnRate, dto.getMinRate());
        }
        if (dto.getMaxRate() != null) {
            wrapper.le(FinancialProduct::getAnnualReturnRate, dto.getMaxRate());
        }
        if (dto.getMinTerm() != null) {
            wrapper.ge(FinancialProduct::getTermDays, dto.getMinTerm());
        }
        if (dto.getMaxTerm() != null) {
            wrapper.le(FinancialProduct::getTermDays, dto.getMaxTerm());
        }
        if (dto.getSaleStatus() != null) {
            wrapper.eq(FinancialProduct::getSaleStatus, dto.getSaleStatus());
        }
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w
                    .like(FinancialProduct::getProductName, dto.getKeyword())
                    .or()
                    .like(FinancialProduct::getProductCode, dto.getKeyword())
            );
        }
        
        wrapper.orderByDesc(FinancialProduct::getAnnualReturnRate);
        
        Page<FinancialProduct> productPage = productMapper.selectPage(page, wrapper);
        
        Page<ProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        voPage.setRecords(productPage.getRecords().stream().map(product -> {
            ProductVO vo = new ProductVO();
            BeanUtil.copyProperties(product, vo);
            return vo;
        }).collect(Collectors.toList()));
        
        return voPage;
    }

    @Override
    public ProductVO getProductById(Long id) {
        FinancialProduct product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        ProductVO vo = new ProductVO();
        BeanUtil.copyProperties(product, vo);
        return vo;
    }

    @Override
    public List<FinancialProduct> searchByParams(String productType, Double minRate, Double maxRate, Integer minTerm, Integer maxTerm) {
        LambdaQueryWrapper<FinancialProduct> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(productType)) {
            wrapper.eq(FinancialProduct::getProductType, productType);
        }
        if (minRate != null) {
            wrapper.ge(FinancialProduct::getAnnualReturnRate, BigDecimal.valueOf(minRate));
        }
        if (maxRate != null) {
            wrapper.le(FinancialProduct::getAnnualReturnRate, BigDecimal.valueOf(maxRate));
        }
        if (minTerm != null) {
            wrapper.ge(FinancialProduct::getTermDays, minTerm);
        }
        if (maxTerm != null) {
            wrapper.le(FinancialProduct::getTermDays, maxTerm);
        }
        
        wrapper.eq(FinancialProduct::getSaleStatus, 1);
        wrapper.orderByDesc(FinancialProduct::getAnnualReturnRate);
        
        return productMapper.selectList(wrapper);
    }
}
