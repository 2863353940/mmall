package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.pojo.Category;

import java.util.List;

/**
 * Created by Administrator on 2018/4/2.
 */
public interface ICategoryService {

    /**
     * 增加分类
     * @param categoryName
     * @param parentId
     * @return
     */
    ServerResponse addCategory(String categoryName, Integer parentId);

    /**
     * 修改品类
     * @param categroyId
     * @param categoryName
     * @return
     */
    ServerResponse updateCategoryName(Integer categroyId, String categoryName);

    /**
     * 查询子节点的品类信息
     * @param categoryId
     * @return
     */
    ServerResponse<List<Category>> getChildrenParallelCategory(Integer categoryId);

    /**
     * 递归查询本节点的id及孩子节点的id
     * @param categoryId
     * @return
     */
    ServerResponse<List<Integer>> selectCategoryAndChildrenById(Integer categoryId);

}
