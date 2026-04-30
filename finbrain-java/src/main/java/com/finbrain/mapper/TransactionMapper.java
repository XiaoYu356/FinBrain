package com.finbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finbrain.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionMapper extends BaseMapper<Transaction> {
}
