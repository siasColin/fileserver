package com.colin.springboot.fileserver.service;

import com.colin.springboot.fileserver.model.File;
import com.colin.springboot.fileserver.model.LayUI;
import com.colin.springboot.fileserver.repository.FileRepository;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.gridfs.GridFSDBFile;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * File 服务.
 * 
 * @since 1.0.0 2017年7月30日
 * @author <a href="https://waylau.com">Way Lau</a> 
 */
@Service
public class FileServiceImpl implements FileService {
	
	@Autowired
	public FileRepository fileRepository;

	// 获得SpringBoot提供的mongodb的GridFS对象,处理大文件（超过16M）
	@Autowired
	private GridFsTemplate gridFsTemplate;

	@Override
	public File saveFile(File file) {
		return fileRepository.save(file);
	}

	public ObjectId saveBigFile(InputStream in ,String fileName,String contentType){
		ObjectId objectId = gridFsTemplate.store(in, fileName, contentType);
		System.out.println("保存成功，objectId:"+objectId);
		return objectId;
	}

	@Override
	public void removeFile(String id) {
		fileRepository.deleteById(id);
	}

	@Override
	public Optional<File> getFileById(String id) {
		return fileRepository.findById(id);
	}

	@Override
	public List<File> listFilesByPage(int pageIndex, int pageSize) {
		Page<File> page = null;
		List<File> list = null;
		
		Sort sort = Sort.by(Sort.Direction.DESC, "uploadDate");
		Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);
		
		page = fileRepository.findAll(pageable);
		list = page.getContent();
		return list;
	}

	@Override
	public LayUI listFilesByNameWithPage(Map<String, Object> paramMap) {
		LayUI layUI = new LayUI();
		Page<File> page = null;
		List<File> list = null;
		Integer pageIndex = Integer.parseInt(paramMap.get("page").toString());
		Integer pageSize = Integer.parseInt(paramMap.get("limit").toString());
		Sort sort = Sort.by(Sort.Direction.DESC, "uploadDate");
		Pageable pageable = PageRequest.of(pageIndex -1, pageSize, sort);
		String name = paramMap.get("name") == null ? null : paramMap.get("name").toString();
		if(name != null && !name.trim().equals("")){
			page = fileRepository.findByNameLike(name,pageable);
		}else{
			page = fileRepository.findAll(pageable);
		}
		layUI.setCode(0);
		layUI.setMsg("成功");
		if(page != null){
			layUI.setData(page.getContent());
			layUI.setCount(page.getTotalElements());
		}
		return layUI;
	}

	@Override
	public GridFSFile bigFileDownload(String id) {
		Query query = Query.query(Criteria.where("_id").is(id));
		return gridFsTemplate.findOne(query);
	}

	@Override
	public void removeBigFile(String id) {
		Query query = Query.query(Criteria.where("_id").is(id));
		gridFsTemplate.delete(query);
	}
}
