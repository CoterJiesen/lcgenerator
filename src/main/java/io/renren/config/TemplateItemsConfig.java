package io.renren.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 成都思致科技有限公司
 *
 * @author chengfaying
 * @version V1.0
 * @className
 * @date 2023/11/9
 */

public class TemplateItemsConfig {

    /**
     * app初始化：单个空目录
     */
    private List<String> emptyFiles;
    /**
     * app初始化：根据模板文件初始化
     */
    private Map<String, String> templateFiles;

    public List<String> getEmptyFiles() {
        return emptyFiles;
    }

    public void setEmptyFiles(List<String> emptyFiles) {
        this.emptyFiles = emptyFiles;
    }

    public Map<String, String> getTemplateFiles() {
        return templateFiles;
    }

    public void setTemplateFiles(Map<String, String> templateFiles) {
        this.templateFiles = templateFiles;
    }
}
