package com.feitai.admin.system.model;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * detail:补件提交大数次数表
 * author:longhaoteng
 * date:2018/11/30
 */
@Data
@ToString
@Table(name = "sys_supply_count_info")
public class SupplyCountInfo{

    @Id
    private Long id;

    private Integer count;

    private Date createdTime;

    private Date updateTime;

}
