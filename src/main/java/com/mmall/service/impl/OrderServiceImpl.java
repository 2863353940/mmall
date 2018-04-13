package com.mmall.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.dao.*;
import com.mmall.pojo.*;
import com.mmall.service.IOrderService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.FTPUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.OrderItemVo;
import com.mmall.vo.OrderProductVo;
import com.mmall.vo.OrderVo;
import com.mmall.vo.ShippingVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by Administrator on 2018/4/5.
 */
@Service("iOrderService")
public class OrderServiceImpl implements IOrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private PayInfoMapper payInfoMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ShippingMapper shippingMapper;

    /**
     * 创建未支付订单
     * @param userId
     * @param shippingId    收货地址id
     * @return
     */
    //todo 事务控制
    @Override
    public  ServerResponse createOrder(Integer userId,Integer shippingId){
        // 1.根据UserId从购物车中获取选中的数据
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);

        // 2.封装OrderItem(包括每种商品的总价)、校验库存、校验商品在售状态
        ServerResponse serverResponse = this.getCartOrderItem(userId,cartList);

        // 2.1判断商品校验是否成功
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }

        List<OrderItem> orderItemList = (List<OrderItem>)serverResponse.getData();  // 返回结果强转List<OrderItem>

        // 2.2.判断购物车中商品是否为空
        if(CollectionUtils.isEmpty(orderItemList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }

        // 3.计算所有商品的总价
        BigDecimal payment = this.getOrderTotalPrice(orderItemList);

        // 4.生成订单
        Order order = this.assembleOrder(userId,shippingId,payment);

        // 4.1.判断订单是否生成
        if(order == null){
            return ServerResponse.createByErrorMessage("生成订单错误");
        }

        // 5.将订单中的每个商品关联订单id
        for(OrderItem orderItem : orderItemList){
            orderItem.setOrderNo(order.getOrderNo());
        }
        // 6.mybatis 批量插入订单商品
        orderItemMapper.batchInsert(orderItemList);

        // 7.生成订单成功,我们要减少我们产品的库存
        this.reduceProductStock(orderItemList);
        // 8.将下单成功的商品从购物车中清除
        this.cleanCart(cartList);

        // 9.返回给前端数据(封装OrderVo对象)
        OrderVo orderVo = assembleOrderVo(order,orderItemList);
        return ServerResponse.createBySuccess(orderVo);
    }

    /**
     * 计算要购买商品的总价
     * @param userId    用户id
     * @param cartList  购物车选中的商品
     * @return
     */
    private ServerResponse getCartOrderItem(Integer userId,List<Cart> cartList){
        // 订单商品集合
        List<OrderItem> orderItemList = Lists.newArrayList();
        if(CollectionUtils.isEmpty(cartList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }
        // 校验购物车的数据,包括产品的状态和数量
        for(Cart cartItem : cartList){
            OrderItem orderItem = new OrderItem();
            // 1.根据购物车商品id查询商品信息
            Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
            // 2.校验要购买的商品是否在售
            if(Const.ProductStatusEnum.ON_SALE.getCode() != product.getStatus()){
                return ServerResponse.createByErrorMessage("产品"+product.getName()+"不是在线售卖状态");
            }
            // 3.校验库存
            if(cartItem.getQuantity() > product.getStock()){
                return ServerResponse.createByErrorMessage("产品"+product.getName()+"库存不足");
            }
            orderItem.setUserId(userId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            // 4.计算单种商品的总价(数量*单价)
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartItem.getQuantity()));
            orderItemList.add(orderItem);
        }
        // 5.返回商品列表
        return ServerResponse.createBySuccess(orderItemList);
    }

    /**
     * 计算所有商品的总价
     * @param orderItemList
     * @return
     */
    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList){
        BigDecimal payment = new BigDecimal("0");
        for(OrderItem orderItem : orderItemList){
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());
        }
        return payment;
    }


    /**
     * 生成订单
     * @param userId
     * @param shippingId    收货地址id
     * @param payment   总价
     * @return
     */
    private Order assembleOrder(Integer userId,Integer shippingId,BigDecimal payment){
        Order order = new Order();
        // 生成订单号码
        long orderNo = this.generateOrderNo();
        order.setOrderNo(orderNo);
        order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());    //默认订单状态（未支付）
        order.setPostage(0);                                        //邮费
        order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());   //交易类型
        order.setPayment(payment);      //总价

        order.setUserId(userId);
        order.setShippingId(shippingId);

        //todo 发货时间等等

        //todo 付款时间等等

        // 插入订单
        int rowCount = orderMapper.insert(order);
        if(rowCount > 0){
            return order;
        }
        return null;
    }


    /**
     * 生成订单号码
     * @return
     */
    private long generateOrderNo(){
        long currentTime =System.currentTimeMillis();
        return currentTime+new Random().nextInt(100);
    }


    /**
     * 减少商品库存
     * @param orderItemList
     */
    private void reduceProductStock(List<OrderItem> orderItemList){
        for(OrderItem orderItem : orderItemList){
            Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());   // 查询商品信息
            product.setStock(product.getStock()-orderItem.getQuantity());                   // 设置库存
            productMapper.updateByPrimaryKeySelective(product);                             // 更新商品
        }
    }


    /**
     * 将下单成功的商品从购物车中清除
     * @param cartList
     */
    private void cleanCart(List<Cart> cartList){
        for(Cart cart : cartList){
            cartMapper.deleteByPrimaryKey(cart.getId());
        }
    }

    /**
     * 封装OrderVo对象
     * @param order
     * @param orderItemList
     * @return
     */
    private OrderVo assembleOrderVo(Order order, List<OrderItem> orderItemList){
        OrderVo orderVo = new OrderVo();
        orderVo.setOrderNo(order.getOrderNo());
        orderVo.setPayment(order.getPayment());
        orderVo.setPaymentType(order.getPaymentType());
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());

        orderVo.setPostage(order.getPostage());
        orderVo.setStatus(order.getStatus());
        orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue());

        orderVo.setShippingId(order.getShippingId());
        Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());
        if(shipping != null){
            orderVo.setReceiverName(shipping.getReceiverName());
            // 封装ShippingVo对象
            orderVo.setShippingVo(assembleShippingVo(shipping));
        }
        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
        orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));
        orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        List<OrderItemVo> orderItemVoList = Lists.newArrayList();
        for(OrderItem orderItem : orderItemList){
            // 循环遍历封装OrderItemVo对象
            OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
            orderItemVoList.add(orderItemVo);
        }
        orderVo.setOrderItemVoList(orderItemVoList);
        return orderVo;
    }

    /**
     * 封装ShippingVo对象
     * @param shipping
     * @return
     */
    private ShippingVo assembleShippingVo(Shipping shipping){
        ShippingVo shippingVo = new ShippingVo();
        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverZip(shipping.getReceiverZip());
        shippingVo.setReceiverPhone(shippingVo.getReceiverPhone());
        return shippingVo;
    }

    /**
     * 循环遍历封装OrderItemVo对象
     * @param orderItem
     * @return
     */
    private OrderItemVo assembleOrderItemVo(OrderItem orderItem){
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setOrderNo(orderItem.getOrderNo());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        orderItemVo.setQuantity(orderItem.getQuantity());
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());
        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));
        return orderItemVo;
    }


    /**
     * 取消未支付订单
     * @param userId
     * @param orderNo
     * @return
     */
    @Override
    public ServerResponse<String> cancel(Integer userId,Long orderNo){
        Order order  = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("该用户此订单不存在");
        }
        if(order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode()){
            return ServerResponse.createByErrorMessage("已付款,无法取消订单");
        }
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(Const.OrderStatusEnum.CANCELED.getCode());

        int row = orderMapper.updateByPrimaryKeySelective(updateOrder);
        if(row > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }


    /**
     * 获取购物车中勾选的商品
     * @param userId
     * @return
     */
    @Override
    public ServerResponse getOrderCartProduct(Integer userId){
        OrderProductVo orderProductVo = new OrderProductVo();
        //从购物车中获取数据

        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);
        ServerResponse serverResponse =  this.getCartOrderItem(userId,cartList);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }
        List<OrderItem> orderItemList =( List<OrderItem> ) serverResponse.getData();

        List<OrderItemVo> orderItemVoList = Lists.newArrayList();

        BigDecimal payment = new BigDecimal("0");
        for(OrderItem orderItem : orderItemList){
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());
            orderItemVoList.add(assembleOrderItemVo(orderItem));
        }
        orderProductVo.setProductTotalPrice(payment);
        orderProductVo.setOrderItemVoList(orderItemVoList);
        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        return ServerResponse.createBySuccess(orderProductVo);
    }


    /**
     * 查询订单详细
     * @param userId
     * @param orderNo
     * @return
     */
    @Override
    public ServerResponse<OrderVo> getOrderDetail(Integer userId,Long orderNo){
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order != null){
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo,userId);
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return  ServerResponse.createByErrorMessage("没有找到该订单");
    }


    /**
     * 查询订单列表
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public ServerResponse<PageInfo> getOrderList(Integer userId, int pageNum, int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orderList = orderMapper.selectByUserId(userId);
        List<OrderVo> orderVoList = assembleOrderVoList(orderList,userId);
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccess(pageResult);
    }

    private List<OrderVo> assembleOrderVoList(List<Order> orderList,Integer userId){
        List<OrderVo> orderVoList = Lists.newArrayList();
        for(Order order : orderList){
            List<OrderItem>  orderItemList = Lists.newArrayList();
            if(userId == null){
                //todo 管理员查询的时候 不需要传userId
                orderItemList = orderItemMapper.getByOrderNo(order.getOrderNo());
            }else{
                orderItemList = orderItemMapper.getByOrderNoUserId(order.getOrderNo(),userId);
            }
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            orderVoList.add(orderVo);
        }
        return orderVoList;
    }







    /**********************************************************后台接口************************************************************************/



    public ServerResponse<PageInfo> manageList(int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orderList = orderMapper.selectAllOrder();
        List<OrderVo> orderVoList = this.assembleOrderVoList(orderList,null);
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccess(pageResult);
    }


    public ServerResponse<OrderVo> manageDetail(Long orderNo){
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }



    public ServerResponse<PageInfo> manageSearch(Long orderNo,int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItemList);

            PageInfo pageResult = new PageInfo(Lists.newArrayList(order));
            pageResult.setList(Lists.newArrayList(orderVo));
            return ServerResponse.createBySuccess(pageResult);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }


    public ServerResponse<String> manageSendGoods(Long orderNo){
        Order order= orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            if(order.getStatus() == Const.OrderStatusEnum.PAID.getCode()){
                order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
                order.setSendTime(new Date());
                orderMapper.updateByPrimaryKeySelective(order);
                return ServerResponse.createBySuccess("发货成功");
            }
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }














    /*************************************************************OVER************************************************************************/






    /**
     * 查询订单是否支付
     * @param userId
     * @param orderNo
     * @return
     */
    @Override
    public ServerResponse queryOrderPayStatus(Integer userId,Long orderNo){
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }





    /***********************************************************支付宝支付******************************************************************/


    /**
     * 支付宝预下单接口(返回支付二维码)
     * @param orderNo   订单号
     * @param userId    用户id
     * @param path      tomact下 webapp下upload文件夹    本地路径tomcat、webapps、ROOT、upload
     * @return
     */
    @Override
    public ServerResponse pay(Long orderNo, Integer userId, String path){

        Map<String, String> resultMap = Maps.newHashMap();

        // 1.查询用户该订单是否存在
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        resultMap.put("orderNo", String.valueOf(order.getOrderNo()));




        // 2.(必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = order.getOrderNo().toString();

        // 3.(必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = new StringBuilder().append("mmall扫码支付，订单号:").append(outTradeNo).toString();

        // 4.(必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();

        // 5.(可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 6.卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 7.订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder().append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();

        /****************************不动start***************************/
        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        /****************************不动over***************************/

        // 8.支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 9.商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();

        // 9.1获取该订单下的所有商品集合
        List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo, userId);
        for (OrderItem orderItem : orderItemList){
            // 9.2创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
            GoodsDetail goods = GoodsDetail.newInstance(orderItem.getProductId().toString(),
                    orderItem.getProductName(),
                    BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(), new Double(100).doubleValue()).longValue(),
                    orderItem.getQuantity());
            // 9.3创建好一个商品后添加至商品明细列表
            goodsDetailList.add(goods);
        }

        // 10.创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject)
                .setTotalAmount(totalAmount)
                .setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount)
                .setSellerId(sellerId)
                .setBody(body)
                .setOperatorId(operatorId)
                .setStoreId(storeId)
                .setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置(内网无法回调，需要使用内网穿透工具)
                .setGoodsDetailList(goodsDetailList);

        // 11.用于发送预下单请求
        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");
        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        AlipayTradeService tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);

        // 12.判断预下单返回状态
        switch (result.getTradeStatus()) {
            case SUCCESS:
                logger.info("支付宝预下单成功: )");
                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                // 需要修改为运行机器上的路径
                File folder = new File(path);   //path  tomcat下webapp/upload文件夹
                if(!folder.exists()){
                    folder.setWritable(true);
                    folder.mkdirs();
                }
                String filePath = String.format(path + "/qr-%s.png", response.getOutTradeNo());
                String fileName = String.format("qr-%s.png", response.getOutTradeNo());
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, filePath);  //生成二维码图片

                // 将图片上传至FTP
                File targetFile = new File(path, fileName);
                try {
                    FTPUtil.uploadFile(Lists.newArrayList(targetFile), PropertiesUtil.getProperty("ftp.server.http.prefix.image"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                logger.info("filePath:" + filePath);
                String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix")+targetFile.getName();
                resultMap.put("qrUrl", qrUrl);
                return ServerResponse.createBySuccess(resultMap);

            case FAILED:
                logger.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");

            case UNKNOWN:
                logger.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");

            default:
                logger.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
        }


    }


    // 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            logger.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                logger.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            logger.info("body:" + response.getBody());
        }
    }


    /**
     * 支付宝回调
     * @param params
     * @return
     */
    @Override
    public ServerResponse aliCallback(Map<String,String> params){
        Long orderNo = Long.parseLong(params.get("out_trade_no"));
        String tradeNo = params.get("trade_no");        //支付宝交易号
        String tradeStatus = params.get("trade_status");//支付宝支付状态
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("非快乐慕商城的订单,回调忽略");
        }
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess("支付宝重复调用");
        }
        if(Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)){
            // 修改订单信息
            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
            order.setStatus(Const.OrderStatusEnum.PAID.getCode());
            orderMapper.updateByPrimaryKeySelective(order);
        }

        PayInfo payInfo = new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(order.getOrderNo());
        payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
        payInfo.setPlatformNumber(tradeNo);
        payInfo.setPlatformStatus(tradeStatus);

        payInfoMapper.insert(payInfo);

        return ServerResponse.createBySuccess();
    }


    /****************************************************支付宝支付OVER*************************************************************/







    /**
     * hour个小时以内未付款的订单，进行关闭
     * @param hour
     */
    @Override
    public void closeOrder(int hour){

        // create_time <= new date - hour

        Date closeDateTime = DateUtils.addHours(new Date(), -hour);

        // 查询所有超时未支付的订单
        List<Order> orderList = orderMapper.selectOrderStatusByCreateTime(Const.OrderStatusEnum.NO_PAY.getCode(), DateTimeUtil.dateToStr(closeDateTime));

        for(Order order : orderList){

            // 根据订单号查询所有订单商品
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(order.getOrderNo());

            for(OrderItem orderItem : orderItemList){

                // 根据商品id查询商品库存，一定要用主键where条件，防止锁表。同时必须是支持MySQL的InnoDB。
                Integer stock = productMapper.selectStockByProductId(orderItem.getProductId());

                // 考虑到已生成的订单里的商品，被删除的情况
                if(stock == null){
                    continue;
                }

                Product product = new Product();
                product.setId(orderItem.getProductId());
                product.setStock(stock+orderItem.getQuantity());
                // 修改商品库存
                productMapper.updateByPrimaryKeySelective(product);

            }

            // 所有订单商品库存加完，修改订单状态为0
            orderMapper.closeOrderByOrderId(order.getId());
            logger.info("关闭订单OrderNo：{}",order.getOrderNo());
        }


    }








}
