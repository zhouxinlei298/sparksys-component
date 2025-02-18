package com.github.sparkzxl.mongodb.dynamic;

import com.google.common.collect.Maps;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.MongoDatabaseFactory;

/**
 * description: 抽象mongodb多数据源加载
 *
 * @author zhouxinlei
 */
@Slf4j
public abstract class AbstractMongoDatabaseFactoryProvider implements DynamicMongoDatabaseFactoryProvider {

    private final DefaultMongoDatabaseFactoryCreator mongoDatabaseFactoryCreator = new DefaultMongoDatabaseFactoryCreator();

    protected Map<String, MongoDatabaseFactory> createMongoDatabaseFactoryMap(
            Map<String, DynamicMongoProperties.MongoDatabaseProperty> mongoDatabasePropertyMap) {
        log.info("MongoDatabase创建连接工厂====");
        Map<String, MongoDatabaseFactory> databaseFactoryMap = Maps.newHashMapWithExpectedSize(mongoDatabasePropertyMap.size() * 2);
        for (Map.Entry<String, DynamicMongoProperties.MongoDatabaseProperty> databasePropertyEntry : mongoDatabasePropertyMap.entrySet()) {
            String key = databasePropertyEntry.getKey();
            DynamicMongoProperties.MongoDatabaseProperty databaseProperty = databasePropertyEntry.getValue();
            databaseFactoryMap.put(key, mongoDatabaseFactoryCreator.createMongoDatabaseFactory(databaseProperty));
        }
        return databaseFactoryMap;
    }

}
