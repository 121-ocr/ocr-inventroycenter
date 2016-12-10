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
			resultObjects.add(allStockOutRet);
			
			Future<Void> nextFuture = Future.future();
			nextFuture.setHandler(nextHandler->{
				// 2 创建拣货单并且预留
				createPickOutAndReseved(headerMap, actor, returnFuture, bo, allStockOutRet);

			});

			// 1 自动匹配批次+仓位
			marchLocationsAndBatch(bo, allStockOutRet, nextFuture);

		});

		CompositeFuture.join(futures).setHandler(ar -> {

			msg.reply(resultObjects);

		});

	}
	
	private void createPickOutAndReseved(MultiMap headerMap, JsonObject actor, Future<JsonObject> returnFuture,
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
	}

	private void marchLocationsAndBatch(JsonObject bo, JsonObject stockOutRet, Future<Void> next) {		
		JsonArray details = bo.getJsonArray("detail");
		int size = details.size();		
		if(size == 0){
			next.complete();
		}else{
			String warehousecode = ((JsonObject) bo.getValue(StockOutConstant.warehouse)).getString("code");	
			Map<Integer, JsonArray> replacedDetails = new HashMap<Integer, JsonArray>();

			List<Future> futures = new ArrayList<>();
			
			for(Integer i=0; i<details.size(); i++){
				final Integer finalPos = i;
				
				Object detail = details.getValue(i);
				JsonObject detailob = (JsonObject) detail;
				
				Future<JsonObject> returnBatchFuture = Future.future();
				futures.add(returnBatchFuture);
				
				if (!detailob.containsKey(StockOutConstant.batch_code)) {
					returnBatchFuture.complete();
					continue;
				}

				String batchCode = detailob.getString(StockOutConstant.batch_code);
				
				Future<Void> nextFuture = Future.future();
				nextFuture.setHandler(nextHandler->{
					
					if (batchCode != null && !batchCode.isEmpty()) {
						returnBatchFuture.complete();
					}else{

						// 寻找自动匹配批次号,按照现存量批次号现进先出：根据批次号，入库日期先进先出
						// 1 根据仓库+ sku 获取所有批次信息。并且排序，并且入库日期最早的那一条（先进先出）。
						// 2 从匹配后结果，得到对应批次号。
						StockOnHandQueryByBatchCodeHandler srmQueryHandler = new StockOnHandQueryByBatchCodeHandler(this.appActivity);
						srmQueryHandler.queryAllBatchs(getParam4QueryBatch(detailob), batchcodeinfos -> {
							if (batchcodeinfos.succeeded()) {
								JsonArray batchcodes = batchcodeinfos.result();
								if (batchcodes == null || batchcodes.size() == 0) {
									returnBatchFuture.complete();
								}else{
									if(batchcodes.size() <= 0){
										returnBatchFuture.complete();
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
														next.fail("没有货位");
														return;
													}else{
														JsonArray newdetails = new JsonArray();
														los.forEach(lo -> {
															JsonObject lo2 = (JsonObject) lo;
															JsonObject t = new JsonObject();
															t = (JsonObject) detail;
															t.put("location", lo2.getString("locationcode"));
															newdetails.add(t);
														});
														replacedDetails.put(finalPos, newdetails);
													}
													innerReturnFuture.complete();
												} else {
													Throwable err = onhandservice.cause();
													String errMsg = err.getMessage();
													componentImpl.getLogger().error(errMsg, err);
													returnBatchFuture.fail(err);
				
												}
				
											});
				
										});
										CompositeFuture.join(innerfutures).setHandler(ar -> {
											returnBatchFuture.complete();
										});
									}
								}
		
							} else {
								Throwable err = batchcodeinfos.cause();
								String errMsg = err.getMessage();
								componentImpl.getLogger().error(errMsg, err);
		
								returnBatchFuture.complete();
							}
		
						});		
					}
					
				});
				
	
				if (batchCode != null && !batchCode.isEmpty()) {
					// 根据批次和sku+仓库找货位（可能）
					this.appActivity.getEventBus().send(getOnHandAddress(),
							getParamByLocations(warehousecode, detail, batchCode), onhandservice -> {
						if (onhandservice.succeeded()) {
							JsonArray los = (JsonArray) onhandservice.result().body();
							if (los == null || los.isEmpty()) {
								//returnBatchFuture.complete();
							}else{
								JsonArray newdetails = new JsonArray();
								los.forEach(lo -> {
									JsonObject lo2 = (JsonObject) lo;
									JsonObject t = new JsonObject();
									t = (JsonObject) detail;
									t.put("location", lo2.getString("locationcode"));
									newdetails.add(t);
								});
								replacedDetails.put(finalPos, newdetails);
							}						
	
						} else {
							Throwable err = onhandservice.cause();
							String errMsg = err.getMessage();
							componentImpl.getLogger().error(errMsg, err);
							//returnBatchFuture.fail(err);
						}					
						nextFuture.complete();
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
