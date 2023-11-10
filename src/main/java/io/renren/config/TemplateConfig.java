package io.renren.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 成都思致科技有限公司
 *
 * @author chengfaying
 * @version V1.0
 * @className
 * @date 2023/11/9
 */

@Configuration
@ConfigurationProperties(prefix = "template")
public class TemplateConfig {

    /**
     * 模板变量
     */
    private Map<String, String> ctx;

    private String rootPath;
    private String svcPath;
    private String apiPath;
    private String packagePath;
    private String tablePrefix;

    /**
     * 单个空目录
     */
    private TemplateItemsConfig app;
    /**
     * 模板文件初始化
     */
    private TemplateItemsConfig tables;

    /**
     * 表字段和java字段类型映射
     */
    private Map<String, String> attrType;


    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getSvcPath() {
        return svcPath;
    }

    public void setSvcPath(String svcPath) {
        this.svcPath = svcPath;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public TemplateItemsConfig getApp() {
        return app;
    }

    public void setApp(TemplateItemsConfig app) {
        this.app = app;
    }

    public TemplateItemsConfig getTables() {
        return tables;
    }

    public void setTables(TemplateItemsConfig tables) {
        this.tables = tables;
    }

    public Map<String, String> getAttrType() {
        return attrType;
    }

    public void setAttrType(Map<String, String> attrType) {
        this.attrType = attrType;
    }

    public Map<String, String> getCtx() {
        return ctx;
    }

    public void setCtx(Map<String, String> ctx) {
        this.ctx = ctx;
    }
}
