package com.finbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finbrain.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
}
