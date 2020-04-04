package com.colin.springboot.fileserver.controller;

import com.colin.springboot.fileserver.model.File;
import com.colin.springboot.fileserver.model.LayUI;
import com.colin.springboot.fileserver.service.FileService;
import com.colin.springboot.fileserver.util.MD5Util;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
		LayUI result = new LayUI();
		Integer pageNum = Integer.parseInt(request.getParameter("page"));
		Integer pageSize = Integer.parseInt(request.getParameter("limit"));
		result = fileService.listFilesByNameWithPage(paramMap);
		return result;
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
			File f = new File(file.getOriginalFilename(), file.getContentType(), file.getSize(),
					new Binary(file.getBytes()));
			f.setMd5(MD5Util.getMD5(file.getInputStream()));
			File returnFile = fileService.saveFile(f);
			resultMap.put("returnCode",0);
			resultMap.put("returnMessage","上传成功！");
			resultMap.put("id",returnFile.getId());
			return resultMap;

		} catch (IOException | NoSuchAlgorithmException ex) {
			ex.printStackTrace();
			resultMap.put("returnCode",-1);
			resultMap.put("returnMessage","上传失败！");
			return resultMap;
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
			List<String> idList = new ArrayList<String>();
			for (MultipartFile file : files) {
				File f = new File(file.getOriginalFilename(), file.getContentType(), file.getSize(),
						new Binary(file.getBytes()));
				f.setMd5(MD5Util.getMD5(file.getInputStream()));
				File returnFile = fileService.saveFile(f);
				resultMap.put("returnCode",0);
				resultMap.put("returnMessage","上传成功！");
				idList.add(returnFile.getId());
			}
			resultMap.put("returnCode",0);
			resultMap.put("returnMessage","上传成功！");
			resultMap.put("id",idList);
			return resultMap;

		} catch (IOException | NoSuchAlgorithmException ex) {
			ex.printStackTrace();
			resultMap.put("returnCode",-1);
			resultMap.put("returnMessage","上传失败！");
			return resultMap;
		}

	}

	/**
	 * 删除文件
	 * 
	 * @param id
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
