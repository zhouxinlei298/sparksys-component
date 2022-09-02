package com.github.sparkzxl.data.sync.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

/**
 * description:  租户元数据实体对象
 *
 * @author zhouxinlei
 * @since 2022-08-24 10:40:34
 */
@Data
@ToString
@EqualsAndHashCode(callSuper = false)
public class MetaData implements Serializable {

    private static final long serialVersionUID = -1647129778629174524L;

    /**
     * MetaData id
     */
    private String id;

    /**
     * 租户id
     */
    private String tenantId;


    /**
     * 地区code
     */
    private String areaCode;

    /**
     * 属性类型
     */
    private String attributeType;

    /**
     * 属性类型描述
     */
    private String attributeTypeDesc;

    /**
     * 属性code
     */
    private String attributeCode;

    /**
     * 属性名
     */
    private String attributeName;

    /**
     * 属性描述
     */
    private String attributeDescription;

    /**
     * 属性值
     */
    private String attributeValue;

    /**
     * 属性值扩展
     */
    private String attributeValueExtension;

    /**
     * 是否启用
     */
    private boolean enabled;
}
