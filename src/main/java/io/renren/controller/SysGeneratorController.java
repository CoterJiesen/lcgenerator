/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package io.renren.controller;

import cn.hutool.core.util.StrUtil;
import io.renren.entity.GeneratorEntity;
import io.renren.service.SysGeneratorService;
import io.renren.utils.PageUtils;
import io.renren.utils.Query;
import io.renren.utils.R;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 代码生成器
 *
 * @author Mark sunlightcs@gmail.com
 */
@Controller
@RequestMapping("/sys/generator")
public class SysGeneratorController {
    @Autowired
    private SysGeneratorService sysGeneratorService;

    /**
     * 列表
     */
    @ResponseBody
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils pageUtil = sysGeneratorService.queryList(new Query(params));

        return R.ok().put("page", pageUtil);
    }

    /**
     * 生成代码
     */
    @RequestMapping("/code")
    public void code(String tables, String moduleName, String author, HttpServletResponse response) throws Exception {
        if(StrUtil.isBlank(moduleName) || moduleName.split(":").length > 1){
            throw new Exception("只支持单模块生成");
        }
        GeneratorEntity generatorEntity = new GeneratorEntity(tables.split(","), moduleName, author);
        byte[] data = sysGeneratorService.generatorCode(generatorEntity);

        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"scf.zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");

        IOUtils.write(data, response.getOutputStream());
    }

    /**
     * 生成代码
     */
    @RequestMapping("/app-code")
    public void appCode(String moduleName, String author, HttpServletResponse response) throws IOException {
        GeneratorEntity generatorEntity = new GeneratorEntity( moduleName, author);
        byte[] data = sysGeneratorService.generatorAppCode(generatorEntity);

        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"scfApp.zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");

        IOUtils.write(data, response.getOutputStream());
    }
}
