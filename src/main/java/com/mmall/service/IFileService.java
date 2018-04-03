package com.mmall.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Created by Administrator on 2018/4/3.
 */
public interface IFileService {

    /**
     * 文件上传
     * @param file
     * @param path  要上传的路径
     * @return
     */
    String upload(MultipartFile file, String path);
}
