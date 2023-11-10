/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package io.renren.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.renren.config.TemplateConfig;
import io.renren.dao.GeneratorDao;
import io.renren.entity.GeneratorEntity;
import io.renren.utils.*;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器
 *
 * @author Mark sunlightcs@gmail.com
 */
@Service
public class SysGeneratorService {
    @Autowired
    private GeneratorDao generatorDao;

    @Autowired
    TemplateConfig templateConfig;

    public PageUtils queryList(Query query) {
        Page<?> page = PageHelper.startPage(query.getPage(), query.getLimit());
        List<Map<String, Object>> list = generatorDao.queryList(query);

        return new PageUtils(list, (int) page.getTotal(), query.getLimit(), query.getPage());
    }

    public Map<String, String> queryTable(String tableName) {
        return generatorDao.queryTable(tableName);
    }

    public List<Map<String, String>> queryColumns(String tableName) {
        return generatorDao.queryColumns(tableName);
    }

    public byte[] generatorCode(GeneratorEntity generatorEntity) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);

        for (String tableName : generatorEntity.getTables()) {
            //查询表信息
            Map<String, String> table = queryTable(tableName);
            //查询列信息
            List<Map<String, String>> columns = queryColumns(tableName);
            //生成代码
//            GenUtils.generatorCode(table, columns, zip, generatorEntity.getModuleName(), generatorEntity.getAuthor());
            TemplateGen.generatorCode(table, columns, zip, templateConfig, generatorEntity.getModuleName(), generatorEntity.getAuthor());
        }
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

//    public byte[] generatorAppCode(GeneratorEntity generatorEntity) {
//        System.out.println(templateYamlConfig);
//
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        ZipOutputStream zip = new ZipOutputStream(outputStream);
//        //生成代码
//        AppGen.generatorAppCode(zip, generatorEntity.getModuleName(), generatorEntity.getAuthor());
//        IOUtils.closeQuietly(zip);
//        return outputStream.toByteArray();
//    }

    public byte[] generatorAppCode(GeneratorEntity generatorEntity) {
        System.out.println(templateConfig);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        //生成代码
        TemplateGen.generatorAppCode(zip, templateConfig, generatorEntity.getModuleName(), generatorEntity.getAuthor());
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }
}
