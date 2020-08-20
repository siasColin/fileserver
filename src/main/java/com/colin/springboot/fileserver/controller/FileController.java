package com.colin.springboot.fileserver.controller;

import com.colin.springboot.fileserver.model.File;
import com.colin.springboot.fileserver.model.LayUI;
import com.colin.springboot.fileserver.service.FileService;
import com.colin.springboot.fileserver.util.MD5Util;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.gridfs.GridFSDBFile;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @Package: com.colin.springboot.fileserver.controller
 * @Author: sxf
 * @Date: 2020-3-6
 * @Description: 文件上传
 */
@CrossOrigin(origins = "*", maxAge = 3600) // 允许所有域名访问
@Controller
public class FileController {

	@Autowired
	private FileService fileService;

	@Value("${server.address}")
	private String serverAddress;

	@Value("${server.port}")
	private String serverPort;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private GridFSBucket gridFSBucket;


	@RequestMapping(value = "/")
	public String index(Model model) {
		// 展示最新二十条数据
		model.addAttribute("files", fileService.listFilesByPage(0, 20));
		return "index";
	}

	@RequestMapping(value = "/listpage")
	public String listpage(Model model) {
		return "fileList";
	}



	/**
	 * 分页查询文件
	 * 
	 * @param pageIndex
	 * @param pageSize
	 * @return
	 */
	@GetMapping("files/{pageIndex}/{pageSize}")
	@ResponseBody
	public List<File> listFilesByPage(@PathVariable int pageIndex, @PathVariable int pageSize) {
		return fileService.listFilesByPage(pageIndex, pageSize);
	}


	/**
	 * 带条件分页查询文件
	 *
	 * @param paramMap
	 * @return
	 */
	@GetMapping("listFiles")
	@ResponseBody
	public LayUI listFilesByNameWithPage(HttpServletRequest request, @RequestParam Map<String,Object> paramMap) {
		LayUI result = fileService.listFilesByNameWithPage(paramMap);
		return result;
	}

	/**
	 * 带条件分页查询大文件
	 *
	 * @param paramMap
	 * @return
	 */
	@GetMapping("listBigFiles")
	@ResponseBody
	public LayUI listBigFilesByNameWithPage(HttpServletRequest request, @RequestParam Map<String,Object> paramMap) {
		LayUI result = fileService.bigFilesByNameWithPage(paramMap);
		return result;
	}

	/**
	 * 检查文件是否存在
	 * @return
	 */
	@GetMapping("checkExist/{id}")
	@ResponseBody
	public Object checkExist(@PathVariable String id){
		Map<String,Object> resultMap = new HashMap<String,Object>();
		try{
			Optional<File> file = fileService.getFileById(id);
			if (file.isPresent()) { //检索到文件
				resultMap.put("returnCode",0);
				resultMap.put("returnMessage","检索完成");
				resultMap.put("exist",1);//0 不存在，1存在
				resultMap.put("fileType",0);//0 小文件（小于16M的），1大文件（大于16M的）
			}else{//文件不存在，再检索是否是大文件（超过16M）
				GridFSFile gfsfile = fileService.bigFileDownload(id);
				if(gfsfile != null && gfsfile.getLength() > 0){//文件存在，大文件
					resultMap.put("returnCode",0);
					resultMap.put("returnMessage","检索完成");
					resultMap.put("exist",1);//0 不存在，1存在
					resultMap.put("fileType",1);//0 小文件（小于16M的），1大文件（大于16M的）
				}else{//文件不存在
					resultMap.put("returnCode",0);
					resultMap.put("returnMessage","检索完成");
					resultMap.put("exist",0);//0 不存在，1存在
				}
			}
		}catch (Exception e){
			resultMap.put("returnCode",-1);
			resultMap.put("returnMessage","检索失败");
			e.printStackTrace();
		}
		return resultMap;
	}

	/**
	 * 获取文件片信息
	 * 
	 * @param id
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	@GetMapping("files/{id}")
	@ResponseBody
	public ResponseEntity<Object> serveFile(@PathVariable String id) throws UnsupportedEncodingException {

		Optional<File> file = fileService.getFileById(id);

		if (file.isPresent()) {
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; fileName=" + new String(file.get().getName().getBytes("utf-8"),"ISO-8859-1"))
					.header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
					.header(HttpHeaders.CONTENT_LENGTH, file.get().getSize() + "").header("Connection", "close")
					.body(file.get().getContent().getData());
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File was not fount");
		}

	}

	/**
	 * 在线显示文件
	 * 
	 * @param id
	 * @return
	 */
	@GetMapping("/view/{id}")
	@ResponseBody
	public ResponseEntity<Object> serveFileOnline(@PathVariable String id) {

		Optional<File> file = fileService.getFileById(id);

		if (file.isPresent()) {
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "fileName=\"" + file.get().getName() + "\"")
					.header(HttpHeaders.CONTENT_TYPE, file.get().getContentType())
					.header(HttpHeaders.CONTENT_LENGTH, file.get().getSize() + "").header("Connection", "close")
					.body(file.get().getContent().getData());
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File was not fount");
		}

	}

	/**
	 * 上传
	 * 
	 * @param file
	 * @param redirectAttributes
	 * @return
	 */
	@PostMapping("/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {

		try {
			File f = new File(file.getOriginalFilename(), file.getContentType(), file.getSize(),
					new Binary(file.getBytes()));
			f.setMd5(MD5Util.getMD5(file.getInputStream()));
			fileService.saveFile(f);
		} catch (IOException | NoSuchAlgorithmException ex) {
			ex.printStackTrace();
			redirectAttributes.addFlashAttribute("message", "Your " + file.getOriginalFilename() + " is wrong!");
			return "redirect:/";
		}

		redirectAttributes.addFlashAttribute("message",
				"You successfully uploaded " + file.getOriginalFilename() + "!");

		return "redirect:/";
	}

	/**
	 * 上传接口
	 * 
	 * @param file
	 * @return
	 */
	@PostMapping("/upload")
	@ResponseBody
	public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file) {
		File returnFile = null;
		try {
			File f = new File(file.getOriginalFilename(), file.getContentType(), file.getSize(),
					new Binary(file.getBytes()));
			f.setMd5(MD5Util.getMD5(file.getInputStream()));
			returnFile = fileService.saveFile(f);
			String path = "//" + serverAddress + ":" + serverPort + "/view/" + returnFile.getId();
			return ResponseEntity.status(HttpStatus.OK).body(path);

		} catch (IOException | NoSuchAlgorithmException ex) {
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
		}

	}

	/**
	 * 单文件上传
	 *
	 * @param file
	 * @return
	 */
	@PostMapping("/singleUpload")
	@ResponseBody
	public Object singleUpload(@RequestParam("file") MultipartFile file) {
		Map<String,Object> resultMap = new HashMap<>();
		try {
			if(file.getSize() > 16777216){
				ObjectId objectId = fileService.saveBigFile(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
//				String downLoadUrl = "http://" + serverAddress + ":" + serverPort + "/bigFileDownload/" + objectId.toString();
				resultMap.put("returnCode",0);
				resultMap.put("returnMessage","上传成功！");
//				resultMap.put("downLoadUrl",downLoadUrl);
				resultMap.put("fileType",1);//0 小文件（小于16M的），1大文件（大于16M的）
				resultMap.put("id",objectId.toString());
			}else{
				File f = new File(file.getOriginalFilename(), file.getContentType(), file.getSize(),
						new Binary(file.getBytes()));
				f.setMd5(MD5Util.getMD5(file.getInputStream()));
				File returnFile = fileService.saveFile(f);
//				String downLoadUrl = "http://" + serverAddress + ":" + serverPort + "/files/" + returnFile.getId();
				resultMap.put("returnCode",0);
				resultMap.put("returnMessage","上传成功！");
//				resultMap.put("downLoadUrl",downLoadUrl);
				resultMap.put("fileType",0);//0 小文件（小于16M的），1大文件（大于16M的）
				resultMap.put("id",returnFile.getId());
			}
			return resultMap;

		} catch (IOException | NoSuchAlgorithmException ex) {
			ex.printStackTrace();
			resultMap.put("returnCode",-1);
			resultMap.put("returnMessage","上传失败！");
			return resultMap;
		}

	}

	/**
	 * 下载大文件 大于 16M的文件
	 *
	 * @param id
	 * @return
	 */
	@GetMapping("bigFileDownload/{id}")
	@ResponseBody
	public void bigFileDownload(@PathVariable String id, HttpServletResponse response) {
		InputStream inputStream = null;
		OutputStream out = null;
		try{
			GridFSFile gfsfile = fileService.bigFileDownload(id);
			if(gfsfile != null && gfsfile.getLength() > 0){
				//打开下载流对象
				GridFSDownloadStream gridFSDownloadStream =
						gridFSBucket.openDownloadStream(gfsfile.getObjectId());
				//创建gridFsResource，用于获取流对象
				GridFsResource gridFsResource = new GridFsResource(gfsfile, gridFSDownloadStream);
				//获取流中的数据
				inputStream = gridFsResource.getInputStream();
				String filename = gfsfile.getFilename();
				//解决乱码
				filename = URLEncoder.encode(filename, "UTF-8");
				response.reset();
				// 设置文件名称
				response.setHeader("content-disposition", "attachment;fileName=\""+filename+"\"");
				response.setContentType("application/octet-stream");
				response.setHeader("content-type", "application/octet-stream");
				response.setHeader("content-length", gfsfile.getLength()+"");
				int len = 0;
				byte buffer[]=new byte[1024];
				out = response.getOutputStream();
				while((len = inputStream.read(buffer))>0){
					out.write(buffer, 0, len);
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}finally {
			if (inputStream != null){
				try {
					inputStream.close();
					inputStream = null;
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if(out != null){
				try {
					out.close();
					out = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 多文件上传
	 *
	 * @param files
	 * @return
	 */
	@PostMapping("/multipleUpload")
	@ResponseBody
	public Object multipleUpload(@RequestParam("files") MultipartFile [] files) {
		Map<String,Object> resultMap = new HashMap<>();
		try {
			List<Map<String,Object>> resultList = new ArrayList<>();
			for (MultipartFile file : files) {
				Map<String,Object> map = new HashMap<>();
				try{
					if(file.getSize() > 16777216){
						ObjectId objectId = fileService.saveBigFile(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
						map.put("returnCode",0);
						map.put("returnMessage","上传成功！");
						map.put("fileType",1);//0 小文件（小于16M的），1大文件（大于16M的）
						map.put("id",objectId.toString());
					}else{
						File f = new File(file.getOriginalFilename(), file.getContentType(), file.getSize(),
								new Binary(file.getBytes()));
						f.setMd5(MD5Util.getMD5(file.getInputStream()));
						File returnFile = fileService.saveFile(f);
						map.put("returnCode",0);
						map.put("returnMessage","上传成功！");
						map.put("fileType",0);//0 小文件（小于16M的），1大文件（大于16M的）
						map.put("id",returnFile.getId());
					}
				}catch (Exception e){
					map.put("returnCode",-1);
					map.put("returnMessage","上传失败！");
					e.printStackTrace();
				}
				resultList.add(map);
			}
			resultMap.put("returnCode",0);
			resultMap.put("returnMessage","上传成功！");
			resultMap.put("reusltList",resultList);
		} catch (Exception e) {
			e.printStackTrace();
			resultMap.put("returnCode",-1);
			resultMap.put("returnMessage","上传失败！");
		}
		return resultMap;
	}

	/**
	 * 删除文件
	 * 
	 * @return
	 */
	@DeleteMapping("/file/{id}")
	@ResponseBody
	public Object deleteFile(@PathVariable String id) {
		Map<String,Object> resultMap = new HashMap<>();
		try {

			fileService.removeFile(id);
			resultMap.put("returnCode",0);
			resultMap.put("returnMessage","删除成功！");
			return resultMap;
		} catch (Exception e) {
			resultMap.put("returnCode",-1);
			resultMap.put("returnMessage","删除失败！");
			return resultMap;
		}
	}

	/**
	 * 删除文件
	 *
	 * @param id
	 * @return
	 */
	@DeleteMapping("/bigFile/{id}")
	@ResponseBody
	public Object deleteBigFile(@PathVariable String id) {
		Map<String,Object> resultMap = new HashMap<>();
		try {

			fileService.removeBigFile(id);
			resultMap.put("returnCode",0);
			resultMap.put("returnMessage","删除成功！");
			return resultMap;
		} catch (Exception e) {
			resultMap.put("returnCode",-1);
			resultMap.put("returnMessage","删除失败！");
			return resultMap;
		}
	}

	/**
	 * 批量删除文件
	 *
	 * @param ids
	 * @return
	 */
	@DeleteMapping("/files/{ids}")
	@ResponseBody
	public Object deleteFiles(@PathVariable String [] ids) {
		Map<String,Object> resultMap = new HashMap<>();
		try {
			for (String id : ids) {
				fileService.removeFile(id);
			}
			resultMap.put("returnCode",0);
			resultMap.put("returnMessage","删除成功！");
			return resultMap;
		} catch (Exception e) {
			resultMap.put("returnCode",-1);
			resultMap.put("returnMessage","删除失败！");
			return resultMap;
		}
	}
}
