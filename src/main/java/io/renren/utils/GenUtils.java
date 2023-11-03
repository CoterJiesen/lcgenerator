package io.renren.utils;

import io.renren.entity.ColumnEntity;
import io.renren.entity.TableEntity;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class GenUtils {

    public static final String POINT = ".";

    /**
     * zqj
     *
     * @return
     */
    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<>();
        templates.add("template/Application.java.vm");
        templates.add("template/Controller.java.vm");
        templates.add("template/DomainServiceImpl.java.vm");
        templates.add("template/DTO.java.vm");
        templates.add("template/Entity.java.vm");
        templates.add("template/EntityServiceImpl.java.vm");
        templates.add("template/IApi.java.vm");
        templates.add("template/Mapper.java.vm");
        templates.add("template/Repository.java.vm");
        templates.add("template/RepositoryImpl.java.vm");
        templates.add("template/VO.java.vm");
        return templates;
    }

    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> table,
                                     List<Map<String, String>> columns,
                                     ZipOutputStream zip,
                                     String moduleName,
                                     String author) {
        //配置信息
        Configuration config = getConfig();
        boolean hasBigDecimal = false;
        boolean hasInteger = false;
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));
        tableEntity.setComments(table.get("tableComment"));
        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), config.getString("tablePrefix"));
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));
        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        for (Map<String, String> column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName"));
            columnEntity.setDataType(column.get("dataType"));
            columnEntity.setComments(column.get("columnComment"));
            columnEntity.setExtra(column.get("extra"));
            columnEntity.setCharacterMaximumLength(null == column.get("character_maximum_length") ? String.valueOf(0) : String.valueOf(column.get("character_maximum_length")));
            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));
            columnEntity.setMaxValue(new BigDecimal(columnEntity.getCharacterMaximumLength()));
            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), "unknowType");
            columnEntity.setAttrType(attrType);
            if (!hasBigDecimal && attrType.equals("BigDecimal")) {
                hasBigDecimal = true;
            }

            if (attrType.equals("BigDecimal")) {
                columnEntity.setMaxValue(new BigDecimal(Math.pow(10, Double.valueOf(columnEntity.getCharacterMaximumLength())) - 1));
            }
            if (!hasInteger && attrType.equals("Integer")) {
                hasInteger = true;
            }
            if (attrType.equals("Integer")) {
                columnEntity.setMaxValue(new BigDecimal(Math.pow(10, Double.valueOf(columnEntity.getCharacterMaximumLength()) - 1) - 1));
            }

            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            columsList.add(columnEntity);
        }
        tableEntity.setColumns(columsList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        String mainPath = config.getString("mainPath");
        mainPath = StringUtils.isBlank(mainPath) ? "io.renren" : mainPath;
        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableEntity.getTableName());
        map.put("comments", tableEntity.getComments());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("hasInteger", hasInteger);
        map.put("mainPath", mainPath);
        map.put("package", config.getString("package"));
        map.put("moduleName", StringUtils.isEmpty(moduleName) ? "scf" : moduleName.trim());
        map.put("author", StringUtils.isEmpty(author) ? "cdyfsz" : author.trim());
        if (StringUtils.isNotEmpty(moduleName) && moduleName.contains(POINT)) {
            String[] firstmoduleName = moduleName.split("\\.");
            map.put("firstModuleName", firstmoduleName[0]);
        } else {
            map.put("firstModuleName", StringUtils.isEmpty(moduleName) ? "scf" : moduleName.trim());
        }
        map.put("email", config.getString("email"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);

            try {
                //添加到zip
                zip.putNextEntry(new ZipEntry(getFileName(template, tableEntity.getClassName(),
                        config.getString("package"), null != moduleName ? moduleName : config.getString("moduleName"))));
                IOUtils.write(sw.toString(), zip, "UTF-8");
                IOUtils.closeQuietly(sw);
                zip.closeEntry();
            } catch (IOException e) {
                throw new RRException("渲染模板失败，表名：" + template + tableEntity.getTableName(), e);
            }
        }
    }


    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replaceFirst(tablePrefix, "");
        }
        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new RRException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取相对路径文件名
     */
    public static String getFileName(String template, String className, String packageName, String moduleName) {

        String fileName = "";

        String packagePath = packageName + File.separator;
//        if (StringUtils.isNotBlank(packageName)) {
//            packagePath += packageName.replace(".", File.separator) + File.separator ;
//        }

        if (template.contains("Application.java.vm")) {
            fileName = packagePath + moduleName.replace(".", File.separator) + File.separator + "application" + File.separator + className + "Application.java";
        }

        if (template.contains("Controller.java.vm")) {
            fileName = packagePath + moduleName.replace(".", File.separator) + File.separator + "interfaces" + File.separator + "rest" + File.separator + className + "Controller.java";
        }

        if (template.contains("DomainServiceImpl.java.vm")) {
            fileName = packagePath + moduleName.replace(".", File.separator) + File.separator + "domain" + File.separator + className.toLowerCase() + File.separator + "service" + File.separator + className + "Service.java";
        }

        if (template.contains("DTO.java.vm")) {
            fileName = packagePath + "api" + File.separator + moduleName.replace(".", File.separator) + File.separator + "interfaces" + File.separator + "dto" + File.separator + className + "DTO.java";
        }

        if (template.contains("Entity.java.vm")) {
            fileName = packagePath + moduleName.replace(".", File.separator) + File.separator + "domain" + File.separator + className.toLowerCase() + File.separator + "entity" + File.separator + className + ".java";
        }

        if (template.contains("EntityServiceImpl.java.vm")) {
            fileName = packagePath + moduleName.replace(".", File.separator) + File.separator + "infrastructure" + File.separator + "persistent"+ File.separator + "repository"+ File.separator + "mappers" + File.separator + className + "Service.java";
        }

        if (template.contains("IApi.java.vm")) {
            fileName = packagePath + "api" + File.separator + moduleName.replace(".", File.separator) + File.separator + "interfaces" + File.separator + "api" + File.separator + className + "Api.java";
        }

        if (template.contains("Mapper.java.vm")) {
            fileName = packagePath + moduleName.replace(".", File.separator) + File.separator + "infrastructure" + File.separator + "persistent"+ File.separator + "repository"+ File.separator + "mappers" + File.separator + className + "Mapper.java";
        }

        if (template.contains("Repository.java.vm")) {
            fileName = packagePath + moduleName.replace(".", File.separator) + File.separator + "domain" + File.separator + className.toLowerCase() + File.separator + "repository" + File.separator + "I" + className + "Repository.java";
        }

        if (template.contains("RepositoryImpl.java.vm")) {
            fileName = packagePath + moduleName.replace(".", File.separator) + File.separator + "infrastructure" + File.separator + "persistent" + File.separator + "repository" + File.separator + className + "Repository.java";
        }

        if (template.contains("VO.java.vm")) {
            fileName = packagePath + "api" + File.separator + moduleName.replace(".", File.separator) + File.separator + "interfaces" + File.separator + "vo" + File.separator + className + "VO.java";
        }

        System.out.println("fileName = " + fileName);

        if ("".equals(fileName)) {
            throw new RuntimeException(template + "模板文件未找到");
        } else {
            return fileName;
        }

    }


}
