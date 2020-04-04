package com.colin.springboot.fileserver.service;


import com.colin.springboot.fileserver.model.File;
import com.colin.springboot.fileserver.model.LayUI;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @Package: com.colin.springboot.fileserver.service
 * @Author: sxf
 * @Date: 2020-3-6
 * @Description: File 服务接口
 */
public interface FileService {
	/**
	 * 保存文件
	 * @param file
	 * @return
	 */
	File saveFile(File file);
	
	/**
	 * 删除文件
	 * @param id
	 * @return
	 */
	void removeFile(String id);
	
	/**
	 * 根据id获取文件
	 * @param id
	 * @return
	 */
	Optional<File> getFileById(String id);

	/**
	 * 分页查询，按上传时间降序
	 * @param pageIndex
	 * @param pageSize
	 * @return
	 */
	List<File> listFilesByPage(int pageIndex, int pageSize);

	/**
	 * 带条件分页查询文件
	 * @param paramMap
	 * @return
	 */
    LayUI listFilesByNameWithPage(Map<String, Object> paramMap);
}
