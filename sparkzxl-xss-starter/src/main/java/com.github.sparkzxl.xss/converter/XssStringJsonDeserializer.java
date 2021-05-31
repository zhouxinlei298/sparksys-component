package com.github.sparkzxl.xss.converter;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.github.sparkzxl.xss.utils.XssUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * description: 过滤跨站脚本的 反序列化工具
 *
 * @author zhouxinlei
 * @date 2021-05-31 14:02:02
 */
public class XssStringJsonDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext dc) throws IOException {
        if (!p.hasToken(JsonToken.VALUE_STRING)) {
            return null;
        }
        String value = p.getValueAsString();
        if (StrUtil.isEmpty(value)) {
            return value;
        }

        List<String> list = new ArrayList<>();
        list.add("<script>");
        list.add("</script>");
        list.add("<iframe>");
        list.add("</iframe>");
        list.add("<noscript>");
        list.add("</noscript>");
        list.add("<frameset>");
        list.add("</frameset>");
        list.add("<frame>");
        list.add("</frame>");
        list.add("<noframes>");
        list.add("</noframes>");
        if (list.stream().anyMatch(value::contains)) {
            return XssUtils.xssClean(value, null);
        }
        return value;
    }

}
