package com.colin.springboot.fileserver.repository;

import com.colin.springboot.fileserver.model.File;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Pageable;



/**
 * @Package: com.colin.springboot.fileserver.repository
 * @Author: sxf
 * @Date: 2020-3-6
 * @Description: File 存储库
 */
public interface FileRepository extends MongoRepository<File, String> {
    public Page<File> findByNameLike(String name, Pageable pageable);
}
