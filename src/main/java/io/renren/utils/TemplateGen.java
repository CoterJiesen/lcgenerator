package io.renren.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import io.renren.config.TemplateConfig;
import io.renren.config.TemplateItemsConfig;
import io.renren.entity.ColumnEntity;
import io.renren.entity.ExtStringWriter;
import io.renren.entity.TableEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TemplateGen {

    private static List<String> getAllFilePaths(String filePath, List<String> AllFilePaths) {
        File dirfile = FileUtil.file(filePath);
        //listFiles():以相对路径返回该目录下所有的文件名的一个File对象数组
        File[] files = dirfile.listFiles();
        if (files == null) {
            return AllFilePaths;
        }
        //遍历目录
        for (File file : files) {
            if (file.isDirectory()) {
                // getAbsolutePath(): 返回的是定义时的路径对应的相对路径
                getAllFilePaths(file.getAbsolutePath(), AllFilePaths);
            } else {
                AllFilePaths.add(file.getPath());
            }
        }
        return AllFilePaths;
    }

    /**
     * 生成代码
     */
    public static void generatorAppCode(ZipOutputStream zip, TemplateConfig templateConfig, String moduleName, String author) {
        //封装模板数据
        VelocityContext ctx = new VelocityContext(templateConfig.getCtx());
        ctx.put("author",  StringUtils.isNotBlank(author) ? author.trim() : templateConfig.getCtx().getOrDefault("author","cdsz"));
        ctx.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));

        //生成应用
        genProjectFiles(zip, moduleName, templateConfig.getApp(), ctx);
    }


    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> table,
                                     List<Map<String, String>> columns,
                                     ZipOutputStream zip,
                                     TemplateConfig templateConfig,
                                     String moduleName,
                                     String author) {
        //配置信息
        boolean hasBigDecimal = false;
        boolean hasInteger = false;
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));
        tableEntity.setComments(table.get("tableComment"));
        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), templateConfig.getTablePrefix());
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
            String attrType = templateConfig.getAttrType().getOrDefault(columnEntity.getDataType() ,"unknowType");
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

        //封装模板数据
        VelocityContext ctx = new VelocityContext(templateConfig.getCtx());
        ctx.put("tableName", tableEntity.getTableName());
        ctx.put("comments", tableEntity.getComments());
        ctx.put("pk", tableEntity.getPk());
        ctx.put("className", tableEntity.getClassName());
        ctx.put("classname", tableEntity.getClassname());
        ctx.put("pathName", tableEntity.getClassname().toLowerCase());
        ctx.put("columns", tableEntity.getColumns());
        ctx.put("hasBigDecimal", hasBigDecimal);
        ctx.put("hasInteger", hasInteger);
        ctx.put("moduleName", moduleName.trim());
        ctx.put("author", StringUtils.isNotBlank(author) ? author.trim() : templateConfig.getCtx().getOrDefault("author","cdsz"));
        ctx.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        //生成表代码
        genProjectFiles(zip, moduleName, templateConfig.getTables(), ctx);
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
     * 将文件写入zip
     *
     * @param zip     zip
     * @param fileMap 文件名，文件字符串内容
     */
    public static void addZipfileList(ZipOutputStream zip, Map<String, String> fileMap) {
        try {
            //添加到zip
            for (Map.Entry<String, String> entry : fileMap.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                if (StringUtils.isNotBlank(entry.getValue())) {
                    IOUtils.write(entry.getValue(), zip, "UTF-8");
                }
            }
            zip.closeEntry();
        } catch (IOException e) {
            throw new RRException("添加到zip失败：" + fileMap, e);
        }
    }

    /**
     * 获取相对路径文件名
     */
    public static void genProjectFiles(ZipOutputStream zip, String moduleNames, TemplateItemsConfig config, VelocityContext ctx) {
        String[] modules = moduleNames.split(":");
        Map<String, String> fileMap = new HashMap<>();
        VelocityEngine engine = new VelocityEngine();
        engine.init();
        ExtStringWriter sw = new ExtStringWriter();
        //1、添加空文件目录初始化
        config.getEmptyFiles().forEach(m -> {
            //1、不包含moduleName，即单个文件
            if (!m.contains("${moduleName}")) {
                fileMap.put(m, "");
                return;
            }
            //2、包含moduleName，循环按照模块名添加单个文件
            for (String module : modules) {
                ctx.put("moduleName", module);
                //渲染路径
                sw.clear();
                engine.evaluate(ctx, sw, "", m);
                fileMap.put(sw.toString(), "");
            }
        });

        //2、模板文件:存放路径
        if (modules.length == 1) {
            ctx.put("moduleName", modules[0]);
        }
        Map<String, String> fileTemplatePathMap = getAllTemplateAndPath(config.getTemplateFiles());
        //2.1 获取所有模板文件的包名
        fileTemplatePathMap.forEach((k, v) -> {
            String[] pathKey = v.split("@");
            if(pathKey.length ==2){
                //渲染路径
                sw.clear();;
                engine.evaluate(ctx, sw, "", getPackageName(pathKey[0]));
                ctx.put(pathKey[1], sw.toString());
                fileTemplatePathMap.put(k, pathKey[0]);
            }
        });
        //2.2 填充模板和路径
        fileTemplatePathMap.forEach((k, v) -> {
            //渲染路径
            sw.clear();;
            engine.evaluate(ctx, sw, "", v);
            String path = sw.toString();

            //渲染模板
            sw.clear();;
            Template tpl = Velocity.getTemplate(k, "UTF-8");
            tpl.merge(ctx, sw);
            fileMap.put(path, sw.toString());
        });
        IOUtils.closeQuietly(sw);
        addZipfileList(zip, fileMap);
    }

    public static String getPackageName(String fileUrl){
        if(StringUtils.isBlank(fileUrl)|| !fileUrl.contains("/src/main/java/")){
            return "";
        }
        Path path = Paths.get(fileUrl);
        String filePath = path.getParent().toString();  //获取文件路径
        filePath = filePath.substring(filePath.substring(0, filePath.indexOf("java")).length()+5);
        return filePath.replace("/",".").replace("\\",".");
    }



    /**
     * //1、获取文件
     * //2、判断是文件还是文件夹
     * //2、1若为文件： 进行模板变量替换，生成文件为v(全路径)
     * //2、2文件夹文件夹： 进行模板变量替换，生成文件为v(全路径)
     *
     * @param tplPath
     * @return 模板文件:存放路径
     */
    public static Map<String, String> getAllTemplateAndPath(Map<String, String> tplPath) {
        if (MapUtil.isEmpty(tplPath)) {
            return tplPath;
        }
        Map<String, String> tplAll = new HashMap<>();
        tplPath.forEach((k, v) -> {
            File dirfile = FileUtil.file(k);
            if (dirfile.isFile()) {
                tplAll.put(k, v);
                return;
            }
            List<String> allFilePaths = new ArrayList<>();
            String rootPath = dirfile.getPath();
            allFilePaths = getAllFilePaths(k, allFilePaths);
            allFilePaths.forEach(m -> {
                String relativePathName = m.replace(rootPath, "");
                //添加上相对路径文件名
                tplAll.put(k + relativePathName, v + relativePathName.replace(".vm", ""));
            });
        });
        System.out.println(tplAll);
        return tplAll;
    }

    public static void main(String[] args) {
        List<String> AllFilePaths = new ArrayList<>();
        File dirfile = FileUtil.file("apptemplate/svcinfrastructure");
        String rootPath = dirfile.getPath();
        System.out.println(rootPath);
        AllFilePaths = getAllFilePaths("apptemplate/svcinfrastructure", AllFilePaths);
        for (String path : AllFilePaths) {
            System.out.println(path);
        }

//        getFolderFileNames("apptemplate/svc-infrastructure");
//        try {
//            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream("C:\\Users\\jc\\Desktop\\genapp.zip"));
//            generatorAppCode(zip, "a:b:c", "c");
//            IOUtils.closeQuietly(zip);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//    }
    }
}
