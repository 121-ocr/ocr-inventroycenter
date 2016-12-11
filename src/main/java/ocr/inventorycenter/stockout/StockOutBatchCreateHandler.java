package ocr.inventorycenter.stockout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.inventorycenter.stockonhand.StockOnHandConstant;
import ocr.inventorycenter.stockreserved.StockReservedConstant;
import otocloud.common.ActionContextTransfomer;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 拣货出库单--（创建态-->拣货状）
 * 
 * @author LCL
 *
 */
public class StockOutBatchCreateHandler extends ActionHandlerImpl<JsonArray> {

	public static final String ADDRESS = StockOutConstant.BatchCreateAddressConstant;
	public static final String ONHAND_FOR_BATCH = "stockonhand-mgr.querylocations";  //批次查货位服务地址
	public static final String ONHAND_FOR_FIFO = "stockonhand-mgr.query_fifo";  //先进先出服务地址

	public StockOutBatchCreateHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT,
				null, StockOutConstant.CreatedStatus);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}
	
	@Override
	public void handle(OtoCloudBusMessage<JsonArray> msg) {

		MultiMap headerMap = msg.headers();

		JsonArray documents = msg.body();
		// 当前操作人信息
		JsonObject actor = ActionContextTransfomer.fromMessageHeaderToActor(headerMap);

		List<Future> futures = new ArrayList<Future>();

		JsonArray resultObjects = new JsonArray();
		
		documents.forEach(document -> {
			JsonObject bo = (JsonObject) document;

			Future<JsonObject> returnFuture = Future.future();
			futures.add(returnFuture);
			
			JsonObject allStockOutRet = new JsonObject();
			allStockOutRet.put("bo_id", bo.getString("bo_id"));
			allStockOutRet.put("warehouse", bo.getJsonObject("warehouse"));
			resultObjects.add(allStockOutRet);
			
			//allStockOutRet.put("sku", bo.getJsonObject("goods").getString("product_sku_code"));				

			
			Future<Void> nextFuture = Future.future();
			nextFuture.setHandler(nextHandler->{
				// 2 创建拣货单
				createPickOut(actor, returnFuture, bo, allStockOutRet);

			});

			// 1 自动匹配批次+仓位，并且进行预留
			marchLocationsAndBatch(headerMap, bo, allStockOutRet, nextFuture);

		});

		CompositeFuture.join(futures).setHandler(ar -> {

			msg.reply(resultObjects);

		});

	}
	
	private void createPickOut(JsonObject actor, Future<JsonObject> returnFuture,
			JsonObject bo, JsonObject allStockOutRet) {
		// TODO 如果没有boid，则调用单据号生成规则生成一个单据号
		// 交易单据一般要记录协作方
		String boId = bo.getString("bo_id");
		String partnerAcct = bo.getJsonObject("channel").getString("account");
		this.recordFactData(appActivity.getBizObjectType(), bo, boId, actor, partnerAcct, null, result -> {
			if (result.succeeded()) {
				returnFuture.complete();
			} else {
				allStockOutRet.put("error", result.cause().getMessage());
				returnFuture.fail(result.cause());
			}
		});
	}
	
/*	private void createPickOutAndReseved(MultiMap headerMap, JsonObject actor, Future<JsonObject> returnFuture,
			JsonObject bo, JsonObject stockOutRet) {
		// TODO 如果没有boid，则调用单据号生成规则生成一个单据号
		// 交易单据一般要记录协作方
		String boId = bo.getString("bo_id");
		String partnerAcct = bo.getJsonObject("channel").getString("account");
		this.recordFactData(appActivity.getBizObjectType(), bo, boId, actor, partnerAcct, null, result -> {
			stockOutRet.put("bo_id", bo.getString("bo_id"));
			stockOutRet.put("warehouse", bo.getJsonObject("warehouse"));
			if (result.succeeded()) {
				List<Future> reservedFutures = batchReserved(headerMap, bo, stockOutRet);
				CompositeFuture.join(reservedFutures).setHandler(ar -> {
					returnFuture.complete();
				});

			} else {
				stockOutRet.put("error", result.cause().getMessage());
				returnFuture.fail(result.cause());
			}
		});
	}*/
	
	//单项预留
	private void execcuteReseved(MultiMap headerMap, JsonObject bo, JsonObject boDetail, 
			JsonArray stockOutRet, Handler<AsyncResult<Void>> next) {
		
		Future<Void> retFuture = Future.future();
		retFuture.setHandler(next);
		
		// 进行预留
		String reservedAddress = getReservedAddr();
		JsonObject stockReservedObject = convertToStockReservedObj(bo, boDetail);
		DeliveryOptions options = new DeliveryOptions();
		options.setHeaders(headerMap);
		this.appActivity.getEventBus().send(reservedAddress, stockReservedObject, options, reservedRet -> {
			if (reservedRet.succeeded()) {				
				retFuture.complete();
			} else {
				Throwable err = reservedRet.cause();
				String errMsg = err.getMessage();
				componentImpl.getLogger().error(errMsg, err);
				retFuture.fail(err);

				// 错误记录
				JsonObject reservedError = new JsonObject();
				reservedError.put(StockReservedConstant.sku,
						boDetail.getJsonObject("goods").getString("product_sku_code"));
				if(boDetail.containsKey("batch_code")){
					reservedError.put(StockReservedConstant.batch_code, boDetail.getString("batch_code"));
				}
				reservedError.put("error", errMsg);
				stockOutRet.add(reservedError);

			}
		});

	}

	private void marchLocationsAndBatch(MultiMap headerMap, JsonObject bo, JsonObject stockOutRet, Future<Void> next) {		
		JsonArray details = bo.getJsonArray("detail");
		int size = details.size();		
		if(size == 0){
			next.complete();
		}else{
			JsonArray reservedErrors = new JsonArray();
			stockOutRet.put("details", reservedErrors);
			
			String warehousecode = ((JsonObject) bo.getValue(StockOutConstant.warehouse)).getString("code");	
			//Map<Integer, JsonArray> replacedDetails = new HashMap<Integer, JsonArray>();
			JsonArray replacedDetails = new JsonArray();
			JsonArray errDetails = new JsonArray();

			List<Future> futures = new ArrayList<>();
			
			for(Integer i=0; i<details.size(); i++){
				final Integer finalPos = i;
				
				Object detail = details.getValue(i);
				JsonObject detailobj = (JsonObject) detail;
				
				//生成行号
				Integer detailCode = i+1;
				detailobj.put(StockOutConstant.detail_code, detailCode.toString());
				
				Future<JsonObject> returnBatchFuture = Future.future();
				futures.add(returnBatchFuture);
				
				String tmpBatchCode = "";
				if (detailobj.containsKey(StockOutConstant.batch_code)) {
					tmpBatchCode = detailobj.getString(StockOutConstant.batch_code);
				}

				final String batchCode = tmpBatchCode;
				
				Future<Void> nextFuture = Future.future();
				nextFuture.setHandler(nextHandler->{
					if (!nextHandler.succeeded()) {
						returnBatchFuture.complete();
					}else{
					
						if (batchCode != null && !batchCode.isEmpty()) {					
							returnBatchFuture.complete();							
						}else{
							
							// 先进先出分组规则：仓库+SKU+批次+货位
							// 先进先出排序规则：先批次，后货位存量
							// 条件：仓库、SKU
							this.appActivity.getEventBus().send(getOnHandAddressForFIFO(),
									getParamByFIFO(warehousecode, detailobj), onhandservice -> {
								if (onhandservice.succeeded()) {
										JsonArray los = (JsonArray) onhandservice.result().body();
										if (los == null || los.isEmpty()) {
											//错误记录
											JsonObject reservedError = new JsonObject();
											reservedError.put(StockReservedConstant.sku, detailobj.getJsonObject("goods").getString("product_sku_code"));
											reservedError.put(StockReservedConstant.batch_code, detailobj.getString("batch_code"));
											String errMsg = "无匹配SKU批次和货位";
											reservedError.put("error", errMsg);
											reservedErrors.add(reservedError);		
											
											//无库存量，设置实际拣货量为零
											detailobj.put("quantity_fact", 0.00);
											errDetails.add(detailobj);

											returnBatchFuture.fail(errMsg);												
											
											
										}else{
											List<Future> newDetailFutures = new ArrayList<>();
											//JsonArray newdetails = new JsonArray();
											Integer newDetailCodeBase = 1; //新拆分后的行号基
											for(Object lo : los){
											//los.forEach(lo -> {
												JsonObject lo2 = (JsonObject) lo;
												JsonObject t = detailobj.copy();
												
												//设置拆分后新行号=老行号字符串+自增字符串
												String newDetailCode = detailCode.toString() + newDetailCodeBase.toString();															
												t.put(StockOutConstant.detail_code, newDetailCode);
												newDetailCodeBase = newDetailCodeBase + 1;
												
												//设置货位和拣货量
												t.put("invbatchcode", lo2.getString("invbatchcode"));
												t.put("location_code", lo2.getString("locationcode"));
												t.put("quantity_should", lo2.getDouble("surplus_onhand"));
												
												Future<JsonObject> newDetailFuture = Future.future();
												newDetailFutures.add(newDetailFuture);														
												//执行商品行预留
												execcuteReseved(headerMap, bo, t, reservedErrors, resevedRet->{
													if (resevedRet.succeeded()) {
														//预留有效，则加入拣货单表体新增记录集合
														
														t.put("quantity_fact", t.getDouble("quantity_should"));

														//newdetails.add(t);
														replacedDetails.add(t);
														newDetailFuture.complete();																
													}else{
														Throwable err = resevedRet.cause();
														String errMsg = err.getMessage();
														componentImpl.getLogger().error(errMsg, err);
														
														//预留失败，设置实际拣货量为零
														t.put("quantity_fact", 0.00);
								
														newDetailFuture.fail(errMsg);
													}
												});	
												
											}								
											CompositeFuture.join(newDetailFutures).setHandler(ar -> {
												//replacedDetails.put(finalPos, newdetails);												
												returnBatchFuture.complete();
											});
										
										}
												
									} else {
										Throwable err = onhandservice.cause();
										String errMsg = "匹配SKU批次货位出错：" + err.getMessage();
										componentImpl.getLogger().error(errMsg, err);
										
										//错误记录
										JsonObject reservedError = new JsonObject();
										reservedError.put(StockReservedConstant.sku, detailobj.getJsonObject("goods").getString("product_sku_code"));
										reservedError.put(StockReservedConstant.batch_code, detailobj.getString("batch_code"));
										reservedError.put("error", errMsg);
										reservedErrors.add(reservedError);		
										
										//无库存量，设置实际拣货量为零
										detailobj.put("quantity_fact", 0.00);		
										errDetails.add(detailobj);
										
										returnBatchFuture.fail(err);			
									}
	
								});				

	
						}
					}
					
				});
				
	
				if (batchCode != null && !batchCode.isEmpty()) {
					// 分组规则：仓库+SKU+批次+货位
					// 排序规则：货位存量
					// 条件：仓库、SKU、批次号
					this.appActivity.getEventBus().send(getOnHandAddressForBatchNo(),
							getParamByLocations(warehousecode, detailobj, batchCode), onhandservice -> {
						if (onhandservice.succeeded()) {
							JsonArray los = (JsonArray) onhandservice.result().body();
							if (los == null || los.isEmpty()) {
								//错误记录
								JsonObject reservedError = new JsonObject();
								reservedError.put(StockReservedConstant.sku, detailobj.getJsonObject("goods").getString("product_sku_code"));
								reservedError.put(StockReservedConstant.batch_code, detailobj.getString("batch_code"));
								String errMsg = "无匹配SKU货位";
								reservedError.put("error", errMsg);
								reservedErrors.add(reservedError);		
								
								//无库存量，设置实际拣货量为零
								detailobj.put("quantity_fact", 0.00);
								errDetails.add(detailobj);
								
							}else{
								List<Future> newDetailFutures = new ArrayList<>();
								//JsonArray newdetails = new JsonArray();
								Integer newDetailCodeBase = 1; //新拆分后的行号基
								for(Object lo : los){
								//los.forEach(lo -> {
									JsonObject lo2 = (JsonObject) lo;
									JsonObject t = detailobj.copy();
									
									
									//设置拆分后新行号=老行号字符串+自增字符串
									String newDetailCode = detailCode.toString() + newDetailCodeBase.toString();															
									t.put(StockOutConstant.detail_code, newDetailCode);
									newDetailCodeBase = newDetailCodeBase + 1;
									
									//设置货位和拣货量
									t.put("location_code", lo2.getString("locationcode"));
									t.put("quantity_should", lo2.getDouble("surplus_onhand"));
									
									Future<JsonObject> newDetailFuture = Future.future();
									newDetailFutures.add(newDetailFuture);
									
									//执行商品行预留
									execcuteReseved(headerMap, bo, t, reservedErrors, resevedRet->{
										if (resevedRet.succeeded()) {
											//预留有效，则加入拣货单表体新增记录集合
											t.put("quantity_fact", t.getDouble("quantity_should"));
											//newdetails.add(t);
											replacedDetails.add(t);
											newDetailFuture.complete();																
										}else{
											Throwable err = resevedRet.cause();
											String errMsg = err.getMessage();
											componentImpl.getLogger().error(errMsg, err);
											
											//预留失败，设置实际拣货量为零
											t.put("quantity_fact", 0.00);
					
											newDetailFuture.fail(errMsg);
										}
									});	
									
									CompositeFuture.join(newDetailFutures).setHandler(ar -> {
										//replacedDetails.put(finalPos, newdetails);
										nextFuture.complete();
									});									
									
								}								
							}						
	
						} else {
							Throwable err = onhandservice.cause();
							String errMsg = "匹配SKU货位出错：" + err.getMessage();
							componentImpl.getLogger().error(errMsg, err);
							//returnBatchFuture.fail(err);						
							
							//错误记录
							JsonObject reservedError = new JsonObject();
							reservedError.put(StockReservedConstant.sku, detailobj.getJsonObject("goods").getString("product_sku_code"));
							reservedError.put(StockReservedConstant.batch_code, detailobj.getString("batch_code"));
							reservedError.put("error", errMsg);
							reservedErrors.add(reservedError);		
							
							//无库存量，设置实际拣货量为零
							detailobj.put("quantity_fact", 0.00);	
							errDetails.add(detailobj);
							
							nextFuture.fail(err);
						}					
						
					});
				}else{
					nextFuture.complete();
				}
	
			}
			CompositeFuture.join(futures).setHandler(ar -> {	
				details.clear();

				if(replacedDetails.size() > 0){
					details.addAll(replacedDetails);
				}
				
				if(errDetails.size() > 0){
					details.addAll(errDetails);
				}
				
				next.complete();
			});
			
		}
	}

	/**
	 * 根据仓库+sku查询现场量中，批次集合，并按照批次排序
	 * 
	 * @param bt
	 *            组装参数
	 * @return 需要参数
	 */
/*	private JsonObject getParam4QueryBatch(JsonObject bt) {
		JsonObject queryObj = new JsonObject();
		queryObj.put(StockOnHandConstant.sku, bt.getString(StockReservedConstant.sku));
		queryObj.put(StockOnHandConstant.warehousecode, bt.getString(StockOnHandConstant.warehousecode));

		JsonObject fields = new JsonObject();
		fields.put("_id", false);
		fields.put(StockOnHandConstant.invbatchcode, true);

		JsonObject queryMsg = new JsonObject();
		queryMsg.put(BaseContants.QUERY_OBJ, queryObj);
		queryMsg.put(BaseContants.RESFIELDS, fields);
		return queryMsg;
	}*/

	/**
	 * 输入参数：{
	 * 		query: { warehousecode: "WH001", sku: "WINE001SP0001SP0001", invbatchcode: "1"},
	 * 		groupKeys： ["warehousecode","sku", "invbatchcode", "locationcode"],
	 * 		params: {"res_bid": "xxxx", "quantity_should": 200}, 
	 * }
	 */
	private JsonObject getParamByLocations(String warehousecode, JsonObject detailobs, String batchcode) {
		
		JsonObject detailob = (JsonObject) detailobs.getValue(StockOutConstant.goods);
		String product_sku_code = detailob.getString(StockOutConstant.product_sku_code);
		Double quantity_should = detailobs.getDouble(StockOutConstant.quantity_should);

		JsonObject queryObj = new JsonObject();
		queryObj.put(StockOnHandConstant.warehousecode, warehousecode);
		queryObj.put(StockOnHandConstant.sku, product_sku_code);
		queryObj.put(StockOnHandConstant.invbatchcode, batchcode);

		JsonObject fields = new JsonObject();
		fields.put(StockOutConstant.quantity_should, quantity_should);
		fields.put("res_bid", detailobs.getString("detail_code"));

		JsonObject params = new JsonObject();
		params.put("query", queryObj);
		params.put("params", fields);
		
		JsonArray groupKeys = new JsonArray();
		groupKeys.add("warehousecode").add("sku").add("invbatchcode").add("locationcode");		
		params.put("groupKeys", groupKeys);
		
		return params;
	}
	
	/**
	 * 输入参数：{
	 * 		query: { warehousecode: "WH001", sku: "WINE001SP0001SP0001"},
	 * 		groupKeys： ["warehousecode","sku", "invbatchcode", "locationcode"],
	 * 		params: {"res_bid": "xxxx", "quantity_should": 200}, 
	 * }
	 */
	private JsonObject getParamByFIFO(String warehousecode, JsonObject detailobs) {		
		JsonObject detailob = (JsonObject) detailobs.getValue(StockOutConstant.goods);
		String product_sku_code = detailob.getString(StockOutConstant.product_sku_code);
		Double quantity_should = detailobs.getDouble(StockOutConstant.quantity_should);

		JsonObject queryObj = new JsonObject();
		queryObj.put(StockOnHandConstant.warehousecode, warehousecode);
		queryObj.put(StockOnHandConstant.sku, product_sku_code);

		JsonObject fields = new JsonObject();
		fields.put(StockOutConstant.quantity_should, quantity_should);
		fields.put("res_bid", detailobs.getString("detail_code"));

		JsonObject params = new JsonObject();
		params.put("query", queryObj);
		params.put("params", fields);
		
		JsonArray groupKeys = new JsonArray();
		groupKeys.add("warehousecode").add("sku").add("invbatchcode").add("locationcode");		
		params.put("groupKeys", groupKeys);
		
		return params;
	}

/*	private List<Future> batchReserved(MultiMap headerMap, JsonObject bo, JsonObject stockOutRet) {
		JsonArray reservedErrors = new JsonArray();
		stockOutRet.put("details", reservedErrors);

		List<Future> reservedFutures = new ArrayList<>();

		JsonArray boDetailArray = bo.getJsonArray("detail");
		boDetailArray.forEach(item -> {
			Future<Void> reservedFuture = Future.future();
			reservedFutures.add(reservedFuture);

			JsonObject boDetail = (JsonObject) item;

			// 进行预留
			String reservedAddress = getReservedAddr();
			JsonObject stockReservedObject = convertToStockReservedObj(bo, boDetail);
			DeliveryOptions options = new DeliveryOptions();
			options.setHeaders(headerMap);
			this.appActivity.getEventBus().send(reservedAddress, stockReservedObject, options, next -> {
				if (next.succeeded()) {
					reservedFuture.complete();
				} else {
					Throwable err = next.cause();
					String errMsg = err.getMessage();
					componentImpl.getLogger().error(errMsg, err);
					reservedFuture.fail(err);

					// 错误记录
					JsonObject reservedError = new JsonObject();
					reservedError.put(StockReservedConstant.sku,
							boDetail.getJsonObject("goods").getString("product_sku_code"));
					reservedError.put(StockReservedConstant.batch_code, boDetail.getString("batch_code"));
					reservedError.put("error", errMsg);
					reservedErrors.add(reservedError);

				}
			});
		});
		return reservedFutures;
	}*/

	private String getReservedAddr() {
		return this.appActivity.getAppInstContext().getAccount() + "."
				+ this.appActivity.getAppService().getRealServiceName() + "."
				+ StockReservedConstant.ComponentNameConstant + "." + StockReservedConstant.ReservedAddressConstant;

	}

	private JsonObject convertToStockReservedObj(JsonObject so, JsonObject detail) {
		JsonObject retObj = new JsonObject();
		JsonObject goodsJsonObject = detail.getJsonObject("goods");
		JsonObject warehouseJsonObject = so.getJsonObject("warehouse");
		retObj.put(StockReservedConstant.sku, goodsJsonObject.getString("product_sku_code"));
		retObj.put(StockReservedConstant.warehousecode, warehouseJsonObject.getString("code"));
		if (detail.containsKey("batch_code")) {
			String batchCode = detail.getString("batch_code");
			if (batchCode != null && !batchCode.isEmpty())
				retObj.put("batch_code", batchCode);
		}
		retObj.put(StockReservedConstant.pickoutid, so.getString("bo_id"));
		retObj.put(StockReservedConstant.goodaccount, so.getString("goodaccount"));
		retObj.put(StockReservedConstant.pickoutnum, detail.getDouble("quantity_should"));
		retObj.put(StockReservedConstant.warehouses, warehouseJsonObject);
		retObj.put(StockReservedConstant.goods, goodsJsonObject);
		return retObj;
	}

	private String getOnHandAddressForBatchNo() {
		String accountID = this.appActivity.getAppInstContext().getAccount();
		String authSrvName = this.appActivity.getDependencies().getJsonObject("stockonhand_service")
				.getString("service_name", "");
		String address = accountID + "." + authSrvName + "." + ONHAND_FOR_BATCH;
		return address;
	}

	private String getOnHandAddressForFIFO() {
		String accountID = this.appActivity.getAppInstContext().getAccount();
		String authSrvName = this.appActivity.getDependencies().getJsonObject("stockonhand_service")
				.getString("service_name", "");
		String address = accountID + "." + authSrvName + "." + ONHAND_FOR_FIFO;
		return address;
	}

}
