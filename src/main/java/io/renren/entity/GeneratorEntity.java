package io.renren.entity;

/**
 * 成都思致科技有限公司
 * 生成工具
 *
 * @Author intasect
 * @Date 2019/12/10 12:18
 * =========================================================================
 * 变更履历：
 * -------------------------------------------------------------------------
 * 变更编号     变更时间    变更人   变更原因    变更内容
 * <p>
 * -------------------------------------------------------------------------
 */
public class GeneratorEntity {

    /**
     * 表名
     */
    private String[] tables;

    /**
     * 模块名
     */
    private String moduleName;

    /**
     * 作者
     */
    private String author;

    public GeneratorEntity() {
    }

    public GeneratorEntity(String[] tables, String moduleName, String author) {
        this.tables = tables;
        this.moduleName = moduleName;
        this.author = author;
    }

    public String[] getTables() {
        return tables;
    }

    public void setTables(String[] tables) {
        this.tables = tables;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}