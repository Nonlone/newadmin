package com.feitai.admin.backend.customer.service;

import com.feitai.admin.core.service.ClassPrefixDynamicSupportService;
import com.feitai.jieya.server.dao.data.model.ContactData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.util.Sqls;

@Service
@Slf4j
public class ContactService extends ClassPrefixDynamicSupportService<ContactData> {

    public ContactData findByUserId(Long userId) {
        return getMapper().selectOneByExample(Example.builder(ContactData.class).andWhere(Sqls.custom().andEqualTo("userId",userId).andEqualTo("enable",true)).build());
    }

}
