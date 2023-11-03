package io.renren.common.utils;

import static io.renren.common.constant.Constant.UNDERLINE;

/**
 * 成都思致科技有限公司
 *
 * @Author intasect
 * @Date 2020/3/6 13:40
 * =========================================================================
 * 变更履历：
 * -------------------------------------------------------------------------
 * 变更编号     变更时间    变更人   变更原因    变更内容
 * <p>
 * -------------------------------------------------------------------------
 */
public class NameUtil {

    /**
     * 下划线命名转为驼峰命名
     *
     * @param para 待转换字符串
     * @return 转换后的字符串
     */
    public static String underLineToHump(String para) {
        StringBuilder result = new StringBuilder();
        String a[] = para.split(UNDERLINE);
        for (String s : a) {
            if (!para.contains(UNDERLINE)) {
                result.append(s);
                continue;
            }
            if (result.length() == 0) {
                result.append(s.toLowerCase());
            } else {
                result.append(s.substring(0, 1).toUpperCase());
                result.append(s.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }


    /***
     * 驼峰命名转为下划线命名
     *
     * @param para 待转换字符串
     * @return 转换后的字符串
     */
    public static String humpToUnderLine(String para) {
        StringBuilder sb = new StringBuilder(para);
        //定位
        int temp = 0;
        if (!para.contains(UNDERLINE)) {
            for (int i = 0; i < para.length(); i++) {
                if (i == 0) {
                    continue;
                }
                if (Character.isUpperCase(para.charAt(i))) {
                    sb.insert(i + temp, UNDERLINE);
                    temp += 1;
                }
            }
        }
        return sb.toString().toLowerCase();
    }

    public static void main(String[] args) {
        System.out.println(NameUtil.humpToUnderLine("C"));
        System.out.println(NameUtil.underLineToHump("C_c_c"));
    }
}