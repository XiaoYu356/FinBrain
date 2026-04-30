package com.finbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.dto.AdminProductDTO;
import com.finbrain.vo.AdminAssetVO;
import com.finbrain.vo.AdminOrderVO;
import com.finbrain.vo.AdminUserVO;
import com.finbrain.vo.ProductVO;
import com.finbrain.vo.StatisticsVO;

public interface AdminService {

    Page<AdminUserVO> getUserList(Integer pageNum, Integer pageSize, String keyword);

    AdminUserVO getUserDetail(Long id);

    void updateUserStatus(Long currentUserId, Long id, Integer status);

    Page<ProductVO> getProductList(Integer pageNum, Integer pageSize);

    void addProduct(AdminProductDTO dto);

    void updateProduct(Long id, AdminProductDTO dto);

    void updateProductSaleStatus(Long id, Integer saleStatus);

    void deleteProduct(Long id);

    Page<AdminOrderVO> getOrderList(Integer pageNum, Integer pageSize, Long userId, Integer status);

    AdminOrderVO getOrderDetail(Long id);

    Page<AdminAssetVO> getAssetList(Integer pageNum, Integer pageSize, String keyword);

    StatisticsVO getStatistics();
}
