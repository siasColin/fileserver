package com.colin.springboot.fileserver.service;

import com.colin.springboot.fileserver.model.File;
import com.colin.springboot.fileserver.model.LayUI;
import com.colin.springboot.fileserver.repository.FileRepository;
import com.mongodb.client.gridfs.GridFSFindIterable;
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
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
	@Autowired
	private GridFsOperations gridFsOperations;

	@Override
	public File saveFile(File file) {
		return fileRepository.save(file);
	}

	public ObjectId saveBigFile(InputStream in ,String fileName,String contentType){
		ObjectId objectId = gridFsTemplate.store(in, fileName, contentType);
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
	public LayUI bigFilesByNameWithPage(Map<String, Object> paramMap) {
		LayUI layUI = new LayUI();
		Integer pageIndex = Integer.parseInt(paramMap.get("page").toString());
		Integer pageSize = Integer.parseInt(paramMap.get("limit").toString());
		Sort sort = Sort.by(Sort.Direction.DESC, "uploadDate");
		String name = paramMap.get("name") == null ? null : paramMap.get("name").toString();
		Query query = new Query();
		if(name != null && !name.trim().equals("")){
			query.addCriteria(Criteria.where("filename").regex("^.*"+name+".*$"));
		}
		query.with(sort);
		GridFSFindIterable gridFSFiles = gridFsTemplate.find(query);
		Iterator<GridFSFile> gridFSFileIterator = gridFSFiles.iterator();
		layUI.setCode(0);
		layUI.setMsg("成功");
		List<Map<String,Object>> resultList = new ArrayList<Map<String,Object>>();
		long total = 0l;
		long startIndex = (pageIndex - 1) * pageSize;
		long endIndex = (pageIndex - 1) * pageSize + pageSize;
		while (gridFSFileIterator.hasNext()){
			GridFSFile gridFSFile = gridFSFileIterator.next();
			if(total >= startIndex && total < endIndex){
				HashMap<String,Object> map = new HashMap<>(6);
				map.put("fileId",getFileId(gridFSFile.getId().toString()));
				map.put("fileName",gridFSFile.getFilename());
				map.put("fileSize",gridFSFile.getLength()/1024);
				map.put("uploadTime",gridFSFile.getUploadDate());
				resultList.add(map);
			}
			total++;
		}
		layUI.setData(resultList);
		layUI.setCount(total);
		return layUI;
	}

	//匹配文件ID的正则
	private static Pattern NUMBER_PATTERN = Pattern.compile("(?<==).*(?=})");
	/**
	 * 因为从mongo中获取的文件Id是BsonObjectId {value=5d7068bbcfaf962be4c7273f}的样子
	 * 需要字符串截取
	 * @param bsonObjectId 数据库文件的BsonObjectId
	 */
	private String getFileId(String bsonObjectId) {
		Matcher m = NUMBER_PATTERN.matcher(bsonObjectId);
		if(!m.find()){
			return bsonObjectId;
		}
		return m.group();
	}

	@Override
	public void removeBigFile(String id) {
		Query query = Query.query(Criteria.where("_id").is(id));
		gridFsTemplate.delete(query);
	}
}
