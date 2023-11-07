package io.renren.utils;

import cn.hutool.core.io.file.PathUtil;
import com.sun.xml.internal.rngom.util.Uri;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AppGen {

    public static final String POINT = ".";

    public static List<String> getFolderFileNames(String path) {
        List<String> templates = new ArrayList<>();
        ClassLoader classLoader = AppGen.class.getClassLoader();
        try {
            URL resourceUrl = classLoader.getResource(path);
            if (resourceUrl != null) {
                URI uri = resourceUrl.toURI();
                List<File> files = PathUtil.loopFiles(Paths.get(uri), null);
                files.forEach(m -> {
                    String fullPath = m.getPath();
                    templates.add(fullPath.substring(fullPath.substring(0, fullPath.indexOf("classes")).length() + 8));
                });
                System.out.println(files);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return templates;
    }


    public static List<String> getAppTemplates() {
        List<String> templates = new ArrayList<>();
        templates.add("apptemplate/api-pom.xml.vm");
        templates.add("apptemplate/app.properties.vm");
        templates.add("apptemplate/Application.java.vm");
        templates.add("apptemplate/application-dev.yml.vm");
        templates.add("apptemplate/application-exc.yml.vm");
        templates.add("apptemplate/bootstrap.yml.vm");
        templates.add("apptemplate/logback.xml.vm");
        templates.add("apptemplate/parent-pom.xml.vm");
        templates.add("apptemplate/svc-pom.xml.vm");
        templates.addAll(getFolderFileNames("apptemplate/api-constants"));
        templates.addAll(getFolderFileNames("apptemplate/svc-infrastructure"));
        return templates;

    }

    /**
     * 生成代码
     */
    public static void generatorAppCode(ZipOutputStream zip, String moduleName, String author) {
        //配置信息
        Configuration config = getConfig();

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        String mainPath = config.getString("mainPath");
        mainPath = StringUtils.isBlank(mainPath) ? "io.renren" : mainPath;
        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("mainPath", mainPath);
        map.put("package", config.getString("package"));
        map.put("author", null != author ? author : config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("appName", config.getString("appName"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        VelocityContext context = new VelocityContext(map);

        //1、生成模块
        String appName = config.getString("appName");
        String packageName = config.getString("package");
        genModuleFile(zip, moduleName, packageName, appName);

        //2、根据模板生成文件
        //获取模板列表
        List<String> templates = getAppTemplates();
        genTemplateFile(zip, templates, packageName, appName, context);
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
     * 获取模板文件相对路径文件名
     */
    public static String getAppTemplateFileName(String template, String packageName, String appName) {

        String fileName = "";
        String svcPath = appName + File.separator;
        String apiPath = appName + "-api" + File.separator;
        //设置根路径
        Function<String, String> pkgPath = (String root) ->
                svcPath + root + File.separator + "src\\main\\java\\" + packageName.replace(".", File.separator) + File.separator;

        if (template.contains("parent-pom.xml.vm")) {
            fileName = svcPath + "pom.xml";
        }

        if (template.contains("svc-pom.xml.vm")) {
            fileName = svcPath + svcPath + "pom.xml";
        }

        if (template.contains("api-pom.xml.vm")) {
            fileName = svcPath + apiPath + "pom.xml";
        }

        if (template.contains("Application.java.vm")) {
            fileName = pkgPath.apply(appName) + "Application.java";
        }

        if (template.contains("svc-infrastructure")) {
            fileName = pkgPath.apply(appName) + "infrastructure/"+template.replace(".vm", "").replace("apptemplate\\svc-infrastructure\\","");
        }

        if (template.contains("api-constants")) {
            fileName = pkgPath.apply(appName + "-api") + "constants/"+template.replace(".vm", "").replace("apptemplate\\api-constants\\","");
        }
        //resources
        if (template.contains("app.properties.vm") || template.contains("application-dev.yml.vm") || template.contains("application-exc.yml.vm") || template.contains("bootstrap.yml.vm") || template.contains("logback.xml.vm")) {
            fileName = svcPath + svcPath + "src/main/resources/" + template.replace(".vm", "").replace("apptemplate/", "");
        }

        System.out.println("fileName = " + fileName);

        if ("".equals(fileName)) {
            throw new RuntimeException(template + "模板文件未找到");
        } else {
            return fileName;
        }

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
     * 添加替换后的模板文件到zip
     *
     * @param zip
     * @param templates
     * @param packageName
     * @param appName
     * @param context
     */
    public static void genTemplateFile(ZipOutputStream zip, List<String> templates, String packageName, String appName, VelocityContext context) {
        Map<String, String> fileMap = new HashMap<>();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);
            fileMap.put(getAppTemplateFileName(template, packageName, appName), sw.toString());
            IOUtils.closeQuietly(sw);
        }
        addZipfileList(zip, fileMap);
    }

    /**
     * 获取相对路径文件名
     */
    public static void genModuleFile(ZipOutputStream zip, String moduleNames, String packageName, String appName) {
        //设置根路径
        Function<String, String> pkgPath = (String root) ->
                appName + File.separator + root + File.separator + "src\\main\\java\\" + packageName.replace(".", File.separator) + File.separator;

        String[] modules = moduleNames.split(":");
        Map<String, String> fileMap = new HashMap<>();
        for (String module : modules) {
            module += "/.gitkeep";
            fileMap.put(pkgPath.apply(appName) + "application/" + module, "");
            fileMap.put(pkgPath.apply(appName) + "interfaces/rest/" + module, "");
            fileMap.put(pkgPath.apply(appName) + "domain/" + module, "");
            fileMap.put(pkgPath.apply(appName) + "infrastructure/persistent/" + module, "");
            fileMap.put(pkgPath.apply(appName) + "infrastructure/repository/" + module, "");
            fileMap.put(appName + File.separator + appName + File.separator + "src/main/resources/mapper/" + module, "");
            fileMap.put(pkgPath.apply(appName + "-api") + "vo/" + module, "");
            fileMap.put(pkgPath.apply(appName + "-api") + "dto/" + module, "");
            fileMap.put(pkgPath.apply(appName + "-api") + "api/" + module, "");
        }
        fileMap.put(pkgPath.apply(appName) + "infrastructure/config/" + ".gitkeep", "");
        fileMap.put(pkgPath.apply(appName) + "infrastructure/external/" + ".gitkeep", "");
        fileMap.put(pkgPath.apply(appName) + "infrastructure/mq/" + ".gitkeep", "");
        addZipfileList(zip, fileMap);
    }


    public static void main(String[] args) {
//        getFolderFileNames("apptemplate/svc-infrastructure");
        try {
            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream("C:\\Users\\jc\\Desktop\\genapp.zip"));
            generatorAppCode(zip, "a:b:c", "c");
            IOUtils.closeQuietly(zip);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
