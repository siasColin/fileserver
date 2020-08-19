package com.colin.springboot.fileserver.config;

/**
 * @Package: com.colin.springboot.fileserver.config
 * @Author: sxf
 * @Date: 2020-8-19
 * @Description: mongodb的配置类
 *  解决新版本不支持获取GGridFSDBFile
 */

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;

@Configuration
public class MongoConf {
    @Autowired
    private MongoDbFactory mongoDbFactory;

    /**
     * GridFSBucket用于打开下载流
     * @return
     */
    @Bean
    public GridFSBucket gridFSBucket() {
        MongoDatabase db = mongoDbFactory.getDb();
        return GridFSBuckets.create(db);
    }

}
