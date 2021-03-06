package com.feitai.admin.channel.userChannel.entity;

import com.feitai.utils.SnowFlakeIdGenerator;
import lombok.Data;
import lombok.ToString;

import javax.persistence.Id;
import javax.persistence.Table;

/**
 * detail:管理后台用户与渠道的中间表
 * author:longhaoteng
 * date:2018/12/20
 */
@Data
@ToString
@Table(name = "sys_user_channel")
public class UserChannel {

    @Id
    private Long id;

    private Long userId;

    private String primaryChannelCode;

    public UserChannel(Long userId,String primaryChannelCode){
        this.userId = userId;
        this.primaryChannelCode = primaryChannelCode;
        this.id = SnowFlakeIdGenerator.getDefaultNextId();
    }

}
