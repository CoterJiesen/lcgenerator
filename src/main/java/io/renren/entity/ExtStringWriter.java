package io.renren.entity;

import java.io.StringWriter;

/**
 * 成都思致科技有限公司
 *
 * @author chengfaying
 * @version V1.0
 * @className
 * @date 2023/11/10
 */
public class ExtStringWriter extends StringWriter {
    public void clear(){
        getBuffer().delete(0, getBuffer().length());
    }
}
