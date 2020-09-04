package com.alishangtian.mtimer.common.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
/**
 * @Desc JSONUtils
 * @Time 2020/08/30
 * @Author alishangtian
 */
@Slf4j
public class JSONUtils {

    /**
     * 默认的objectMapper  线程安全的,多线程调用效率低
     */
    private static ObjectMapper objectMapper = createMapper();

    public final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    private JSONUtils() {
    }

    public static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        //非 null 字段进行序列化
        mapper.setSerializationInclusion(Include.NON_NULL);
        //取消默认转换timestamps形式
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        //所有的日期格式都统一为以下的样式，即yyyy-MM-dd HH:mm:ss  SimpleDateFormat是拷贝使用的线程安全
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        //忽略空Bean转json的错误
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        //忽略 在json字符串中存在，但是在java对象中不存在对应属性的情况。防止错误
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * 转换json(对效率要求比较高的服务请自己 new 个 mapper)
     *
     * @param obj
     * @return
     */
    public static String toJSONString(Object obj) {
        return toJSONString(obj, objectMapper);
    }

    public static String toJSONString(Object obj, ObjectMapper objectMapper) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSONUtils toJSONString failed ! obj :" + obj.toString(), e);
            throw new RuntimeException("json parse error");
        }
    }

    /**
     * 有格式的
     *
     * @param obj
     * @return
     */
    public static String toJSONStringPretty(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSONUtils toJSONStringPretty failed ! obj :" + obj.toString(), e);
        }
        return null;
    }

    /**
     * bytes转对象 (对效率要求比较高的服务请使用自己的mapper)
     *
     * @param array
     * @param clazz
     * @return
     */
    public static <T> T parseObject(final byte[] array, Class<T> clazz) {
        return parseObject(array, clazz, objectMapper);
    }

    public static <T> T parseObject(final byte[] array, Class<T> clazz, ObjectMapper objectMapper) {
        if (null == array || array.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(array, clazz);
        } catch (Exception e) {
            log.error("JSONUtils parseObject failed", e);
        }
        return null;
    }

    /**
     * 字符串转对象 (对效率要求比较高的服务请使用自己的mapper)
     *
     * @param json
     * @param clazz
     * @return
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        return parseObject(json, clazz, objectMapper);
    }

    public static <T> T parseObject(String json, Class<T> clazz, ObjectMapper objectMapper) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("JSONUtils parseObject failed ! json :" + json, e);
        }
        return null;
    }

    /**
     * 字段符转List之类的集合 (对效率要求比较高的服务请自己 new 个 mapper)
     *
     * @param json
     * @param typeReference
     * @return
     */
    public static <T> T parseObject(String json, TypeReference<T> typeReference) {
        return parseObject(json, typeReference, objectMapper);
    }

    public static <T> T parseObject(String json, TypeReference<T> typeReference, ObjectMapper objectMapper) {
        if (StringUtils.isEmpty(json) || typeReference == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            log.error("JSONUtils parseObject failed ! json :" + json, e);
        }
        return null;
    }

    /**
     * @Description parseObject
     * @Date 2020/7/10 上午10:37
     * @Author maoxiaobing
     **/
    public static <T> T parseObject(byte[] bytes, TypeReference<T> typeReference) {
        return parseObject(bytes, typeReference, objectMapper);
    }

    public static <T> T parseObject(byte[] bytes, TypeReference<T> typeReference, ObjectMapper objectMapper) {
        if (null == bytes || null == typeReference || null == objectMapper) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, typeReference);
        } catch (Exception e) {
            log.error("JSONUtils parseObject failed ! bytes :" + bytes, e);
        }
        return null;
    }
}