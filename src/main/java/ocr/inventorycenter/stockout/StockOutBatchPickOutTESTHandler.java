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
import ocr.common.BaseContants;
import ocr.inventorycenter.stockonhand.StockOnHandConstant;
import ocr.inventorycenter.stockonhand.StockOnHandQueryByBatchCodeHandler;
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
public class StockOutBatchPickOutTESTHandler extends ActionHandlerImpl<JsonArray> {

	public static final String ADDRESS = StockOutConstant.BatchPickOutAddressTestConstant;

	public static final String ONHAND_REGISTER = "stockonhand-mgr.querylocations";

	public StockOutBatchPickOutTESTHandler(AppActivityImpl appActivity) {
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
				StockOutConstant.CreatedStatus, StockOutConstant.PickOutedStatus);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

	@SuppressWarnings("rawtypes")
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
		String reservedAddress = getReservedAdd();
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
			Map<Integer, JsonArray> replacedDetails = new HashMap<Integer, JsonArray>();

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
<<<<<<< HEAD

						// 寻找自动匹配批次号,按照现存量批次号现进先出：根据批次号，入库日期先进先出
						// 1 根据仓库+ sku 获取所有批次信息。并且排序，并且入库日期最早的那一条（先进先出）。
						// 2 从匹配后结果，得到对应批次号。
						StockOnHandQueryByBatchCodeHandler srmQueryHandler = new StockOnHandQueryByBatchCodeHandler(this.appActivity);
						srmQueryHandler.queryAllBatchs(getParam4QueryBatch(detailob), batchcodeinfos -> {
							if (batchcodeinfos.succeeded()) {
								JsonArray batchcodes = batchcodeinfos.result();
								if (batchcodes == null || batchcodes.size() == 0) {
=======
					
						if (batchCode != null && !batchCode.isEmpty()) {
							
							//执行商品行预留
							execcuteReseved(headerMap, bo, detailobj, reservedErrors, resevedRet->{
								if (resevedRet.succeeded()) {
									
>>>>>>> branch 'develop' of https://github.com/121-ocr/ocr-inventroycenter.git
									returnBatchFuture.complete();
								}else{
									Throwable err = resevedRet.cause();
									String errMsg = err.getMessage();
									componentImpl.getLogger().error(errMsg, err);
									
									//预留失败，设置实际拣货量为零
									detailobj.put("quantity_fact", 0.00);
			
									returnBatchFuture.fail(errMsg);
								}
							});							
							
						}else{
							
							// 寻找自动匹配批次号,按照现存量批次号现进先出：根据批次号，入库日期先进先出
							// 1 根据仓库+ sku 获取所有批次信息。并且排序，并且入库日期最早的那一条（先进先出）。
							// 2 从匹配后结果，得到对应批次号。
							StockOnHandQueryByBatchCodeHandler srmQueryHandler = new StockOnHandQueryByBatchCodeHandler(this.appActivity);
							srmQueryHandler.queryAllBatchs(getParam4QueryBatch(detailobj), batchcodeinfos -> {
								if (batchcodeinfos.succeeded()) {
									JsonArray batchcodes = batchcodeinfos.result();
									if (batchcodes == null || batchcodes.size() == 0) {
										//错误记录
										JsonObject reservedError = new JsonObject();
										reservedError.put(StockReservedConstant.sku, detailobj.getJsonObject("goods").getString("product_sku_code"));
										reservedError.put(StockReservedConstant.batch_code, detailobj.getString("batch_code"));
										String errMsg = "无匹配SKU批次";
										reservedError.put("error", errMsg);
										reservedErrors.add(reservedError);		
										
										//无库存量，设置实际拣货量为零
										detailobj.put("quantity_fact", 0.00);
	
										returnBatchFuture.fail(errMsg);
									}else{
										List<Future> innerfutures = new ArrayList<>();
										// 如果不等于空，根据此寻找仓位是否满足，如果满足返回批次+货位，如果不满足寻找下一个批次。
										batchcodes.forEach(batchcodeObj -> {
											Future<JsonObject> innerReturnFuture = Future.future();
											innerfutures.add(innerReturnFuture);
											
											String bathcode = ((JsonObject) batchcodeObj).getString("batchcode");
											this.appActivity.getEventBus().send(getOnHandAddress(),
													getParamByLocations(warehousecode, detail, bathcode), onhandservice -> {
												if (onhandservice.succeeded()) {
													JsonArray los = (JsonArray) onhandservice.result().body();
													if (los == null || los.isEmpty()) {
<<<<<<< HEAD
														next.fail("没有货位");
														return;
=======
														//错误记录
														JsonObject reservedError = new JsonObject();
														reservedError.put(StockReservedConstant.sku, detailobj.getJsonObject("goods").getString("product_sku_code"));
														reservedError.put(StockReservedConstant.batch_code, detailobj.getString("batch_code"));
														String errMsg = "无匹配SKU货位";
														reservedError.put("error", errMsg);
														reservedErrors.add(reservedError);		
														
														//无库存量，设置实际拣货量为零
														detailobj.put("quantity_fact", 0.00);
	
														innerReturnFuture.fail(errMsg);												
														
														
>>>>>>> branch 'develop' of https://github.com/121-ocr/ocr-inventroycenter.git
													}else{
														List<Future> newDetailFutures = new ArrayList<>();
														JsonArray newdetails = new JsonArray();
														Integer newDetailCodeBase = 1; //新拆分后的行号基
														for(Object lo : los){
														//los.forEach(lo -> {
															JsonObject lo2 = (JsonObject) lo;
															JsonObject t = (JsonObject) detail;
															
															//设置拆分后新行号=老行号字符串+自增字符串
															String newDetailCode = detailCode.toString() + newDetailCodeBase.toString();															
															t.put(StockOutConstant.detail_code, newDetailCode);
															newDetailCodeBase = newDetailCodeBase + 1;
															
															//设置货位
															t.put("location", lo2.getString("locationcode"));
															
															Future<JsonObject> newDetailFuture = Future.future();
															newDetailFutures.add(newDetailFuture);														
															//执行商品行预留
															execcuteReseved(headerMap, bo, t, reservedErrors, resevedRet->{
																if (resevedRet.succeeded()) {
																	//预留有效，则加入拣货单表体新增记录集合
																	newdetails.add(t);
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
															replacedDetails.put(finalPos, newdetails);
															innerReturnFuture.complete();
														});
													
													}
													
												} else {
													Throwable err = onhandservice.cause();
													String errMsg = "匹配SKU货位出错：" + err.getMessage();
													componentImpl.getLogger().error(errMsg, err);
													
													//错误记录
													JsonObject reservedError = new JsonObject();
													reservedError.put(StockReservedConstant.sku, detailobj.getJsonObject("goods").getString("product_sku_code"));
													reservedError.put(StockReservedConstant.batch_code, detailobj.getString("batch_code"));
													reservedError.put("error", errMsg);
													reservedErrors.add(reservedError);		
													
													//无库存量，设置实际拣货量为零
													detailobj.put("quantity_fact", 0.00);												
													
													innerReturnFuture.fail(err);			
												}
				
											});
				
										});
										CompositeFuture.join(innerfutures).setHandler(ar -> {
											returnBatchFuture.complete();
										});
										
									}
			
								} else {
									Throwable err = batchcodeinfos.cause();
									String errMsg = "匹配SKU批次出错：" + err.getMessage();
									componentImpl.getLogger().error(errMsg, err);
									
									//错误记录
									JsonObject reservedError = new JsonObject();
									reservedError.put(StockReservedConstant.sku, detailobj.getJsonObject("goods").getString("product_sku_code"));
									reservedError.put(StockReservedConstant.batch_code, detailobj.getString("batch_code"));
									reservedError.put("error", errMsg);
									reservedErrors.add(reservedError);		
									
									//无库存量，设置实际拣货量为零
									detailobj.put("quantity_fact", 0.00);
	
									returnBatchFuture.fail(errMsg);
	
								}
			
							});		
						}
					}
					
				});
				
	
				if (batchCode != null && !batchCode.isEmpty()) {
					// 根据批次和sku+仓库找货位（可能）
					this.appActivity.getEventBus().send(getOnHandAddress(),
							getParamByLocations(warehousecode, detail, batchCode), onhandservice -> {
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
								
							}else{
								List<Future> newDetailFutures = new ArrayList<>();
								JsonArray newdetails = new JsonArray();
								Integer newDetailCodeBase = 1; //新拆分后的行号基
								for(Object lo : los){
								//los.forEach(lo -> {
									JsonObject lo2 = (JsonObject) lo;
									JsonObject t = (JsonObject) detail;
									
									//设置拆分后新行号=老行号字符串+自增字符串
									String newDetailCode = detailCode.toString() + newDetailCodeBase.toString();															
									t.put(StockOutConstant.detail_code, newDetailCode);
									newDetailCodeBase = newDetailCodeBase + 1;
									
									t.put("location", lo2.getString("locationcode"));
									
									Future<JsonObject> newDetailFuture = Future.future();
									newDetailFutures.add(newDetailFuture);
									
									//执行商品行预留
									execcuteReseved(headerMap, bo, t, reservedErrors, resevedRet->{
										if (resevedRet.succeeded()) {
											//预留有效，则加入拣货单表体新增记录集合
											newdetails.add(t);
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
										replacedDetails.put(finalPos, newdetails);
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
							
							nextFuture.fail(err);
						}					
						
					});
				}else{
					nextFuture.complete();
				}
	
			}
			CompositeFuture.join(futures).setHandler(ar -> {			
				if(replacedDetails.size() > 0){
					replacedDetails.forEach((key,newDetails) -> {
						
						details.remove(key);
						details.addAll(newDetails);
						
						next.complete();
					});
				}
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
	private JsonObject getParam4QueryBatch(JsonObject bt) {
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
	}

	private JsonObject getParamByLocations(String warehousecode, Object detail, String batchcode) {
		JsonObject detailobs = (JsonObject) detail;
		JsonObject detailob = (JsonObject) detailobs.getValue(StockOutConstant.goods);
		String product_sku_code = detailob.getString(StockOutConstant.product_sku_code);
		Double quantity_should = detailobs.getDouble(StockOutConstant.quantity_should);

		JsonObject queryObj = new JsonObject();
		queryObj.put(StockOnHandConstant.sku, product_sku_code);
		queryObj.put(StockOnHandConstant.warehousecode, warehousecode);
		queryObj.put(StockOnHandConstant.invbatchcode, batchcode);

		JsonObject fields = new JsonObject();
		fields.put(StockOutConstant.quantity_should, quantity_should);
		fields.put("res_bid", detailobs.getString("detail_code"));

		JsonObject queryMsg = new JsonObject();
		queryMsg.put(BaseContants.QUERY_OBJ, queryObj);
		queryMsg.put("params", fields);
		return queryMsg;
	}

	private List<Future> batchReserved(MultiMap headerMap, JsonObject bo, JsonObject stockOutRet) {
		JsonArray reservedErrors = new JsonArray();
		stockOutRet.put("details", reservedErrors);

		List<Future> reservedFutures = new ArrayList<>();

		JsonArray boDetailArray = bo.getJsonArray("detail");
		boDetailArray.forEach(item -> {
			Future<Void> reservedFuture = Future.future();
			reservedFutures.add(reservedFuture);

			JsonObject boDetail = (JsonObject) item;

			// 进行预留
			String reservedAddress = getReservedAdd();
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
	}

	private String getReservedAdd() {
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

	private String getOnHandAddress() {
		String accountID = this.appActivity.getAppInstContext().getAccount();
		String authSrvName = this.appActivity.getDependencies().getJsonObject("stockonhand_service")
				.getString("service_name", "");
		String address = accountID + "." + authSrvName + "." + ONHAND_REGISTER;
		return address;
	}

}
