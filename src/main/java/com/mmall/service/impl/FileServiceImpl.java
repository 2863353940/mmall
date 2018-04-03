package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.mmall.service.IFileService;
import com.mmall.util.FTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Administrator on 2018/4/3.
 */
@Service("iFileService")
public class FileServiceImpl implements IFileService{

    private static Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    /**
     * 文件上传
     * 1.文件上传至tomcat
     * 2.文件上传至ftp   （FTPClient）
     * 3.删除tomcat上的文件
     * 可修改跳过上传tomcat步骤直接上传FTP
     *
     *
     * 文件上传ftp时，如果上传到指定文件夹下(非根目录)
     * 三种解决方案:
     * 上传结束之后，返回带文件夹的路径（img/1111.png）
     * 或者在nginx代理处配置ftp转发路径时带上文件夹名称(F:\ftpfile\img;)
     * 或者在properties文件下直接配置带文件夹的路径(http://image.imooc.com/img/)
     *
     */
    public String upload(MultipartFile file, String path){
        //获取上传文件的文件名
        String fileName = file.getOriginalFilename();
        //获取上传文件的后缀
        String fileExtensionName = fileName.substring(fileName.lastIndexOf(".")+1);
        //设置新的文件名
        String uploadFileName = UUID.randomUUID().toString()+"."+fileExtensionName;
        logger.info("开始上传文件,上传文件的文件名:{},上传的路径:{},新文件名:{}", fileName, path, uploadFileName);

        File fileDir = new File(path);
        //判断要上传的文件路径是否存在
        if(!fileDir.exists()){
            //赋予写的权限
            fileDir.setWritable(true);
            fileDir.mkdirs();
        }
        File targetFile = new File(path, uploadFileName);
        try {
            //文件上传
            file.transferTo(targetFile);

            //将targetFile上传到文件服务器
            FTPUtil.uploadFile(Lists.newArrayList(targetFile), "img");

            //上传结束之后，删除tomcat中的文件
            targetFile.delete();

        } catch (IOException e) {
            logger.error("文件上传异常",e);
            return null;
        }
        return targetFile.getName();
    }









}
