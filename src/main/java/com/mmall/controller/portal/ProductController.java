package com.mmall.controller.portal;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.service.IProductService;
import com.mmall.vo.ProductDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Created by Administrator on 2018/4/3.
 */
@Controller
@RequestMapping("/product")
public class ProductController {


    @Autowired
    private IProductService iProductService;

    /**
     * 根据id查询产品信息（RESTful风格接口）
     * @param productId
     * @return
     */
    @RequestMapping(value = "/{productId}", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<ProductDetailVo> detailRESTful(@PathVariable Integer productId){
        return iProductService.getProductDetail(productId);
    }

    /**
     * 根据id查询产品信息
     * @param productId
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse<ProductDetailVo> detail(Integer productId){
        return iProductService.getProductDetail(productId);
    }


    /**
     * 用户端产品搜索
     * @param keyword       关键字
     * @param categoryId    品类id
     * @param pageNum
     * @param pageSize
     * @param orderBy       排序(price_desc)
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse<PageInfo> list(@RequestParam(value = "keyword",required = false)String keyword,
                                         @RequestParam(value = "categoryId",required = false)Integer categoryId,
                                         @RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                         @RequestParam(value = "pageSize",defaultValue = "10") int pageSize,
                                         @RequestParam(value = "orderBy",defaultValue = "") String orderBy){
        return iProductService.getProductByKeywordCategory(keyword,categoryId,pageNum,pageSize,orderBy);
    }










}
