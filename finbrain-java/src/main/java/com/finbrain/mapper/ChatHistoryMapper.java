package com.finbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finbrain.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    @Select("SELECT session_id FROM chat_history WHERE user_id = #{userId} GROUP BY session_id ORDER BY MAX(create_time) DESC")
    List<String> selectSessionIdsByUserId(Long userId);
}
