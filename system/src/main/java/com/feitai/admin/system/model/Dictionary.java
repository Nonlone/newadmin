package com.feitai.admin.system.model;

// Generated 2014-8-26 14:05:09 by Hibernate Tools 3.2.2.GA

import lombok.Data;

import javax.persistence.Id;
import javax.persistence.Table;

/**
 * CommDictionary generated by hbm2java
 */
@Table(name = "sys_dictionary")
@Data
public class Dictionary {
	
	@Id
	protected Long id;

	private Long dictionaryTypeId;

	private String name;
	
	private String code;
	
	private String value;
	
	private Integer orderId;

	private String extend_1;

	private String extend_2;

	private String extend_3;

	private String memo;

}
