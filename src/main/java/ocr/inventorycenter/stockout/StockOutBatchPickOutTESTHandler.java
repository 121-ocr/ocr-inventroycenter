package ocr.inventorycenter.stockout;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.inventorycenter.stockonhand.StockOnHandQueryByBatchCodeHandler;
import ocr.inventorycenter.stockonhand.StockOnHandQueryByLocationHandler;
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

		batchProcessMsg(headerMap, documents, actor, futures, resultObjects);

		CompositeFuture.join(futures).setHandler(ar -> {

			msg.reply(resultObjects);

		});

	}

	private void batchProcessMsg(MultiMap headerMap, JsonArray documents, JsonObject actor, List<Future> futures,
			JsonArray resultObjects) {

		documents.forEach(document -> {

			Future<JsonObject> returnFuture = Future.future();
			futures.add(returnFuture);

			JsonObject bo = (JsonObject) document;

			String boId = bo.getString("bo_id");
			
			String warehousecode = ((JsonObject)bo.getValue(StockOutConstant.warehouse)).getString("code");

			// TODO 如果没有boid，则调用单据号生成规则生成一个单据号
			// 交易单据一般要记录协作方
			String partnerAcct = bo.getJsonObject("channel").getString("account");

			JsonObject stockOutRet = new JsonObject();
			resultObjects.add(stockOutRet);

			// add by licli 自动匹配批次

			
			
			JsonArray details = bo.getJsonArray("detail");
			details.forEach(detail -> {

				JsonObject detailob = (JsonObject) detail;

				if (detailob.containsKey(StockOutConstant.batch_code)) {
					String batchCode = detailob.getString(StockOutConstant.batch_code);
					if (batchCode == null || batchCode.isEmpty()) {
						// 寻找自动匹配批次号,按照现存量批次号现进先出：根据批次号，入库日期先进先出
						    setBatchCodeByRule(detailob);
					}

				}

			});
			
			//add by licli 自动根据货位拆行。根据入库单维护的， 仓位--SKU--批次---量 补全货位。
			
			JsonArray newdetails = new  JsonArray();
			details.forEach(detail -> {
 				JsonObject detailobs = (JsonObject) detail;
 				
 				JsonObject detailob = (JsonObject)detailobs.getValue(StockOutConstant.goods);
 				
 				
				String product_sku_code = detailob.getString(StockOutConstant.product_sku_code);

				Double quantity_should = detailobs.getDouble(StockOutConstant.quantity_should);
				//先匹配未满仓，在出满仓的，按仓位现存量做由小到大排序后，再进行数量匹配
							
				JsonObject queryObj = new JsonObject();
				queryObj.put(StockReservedConstant.sku, product_sku_code);
				queryObj.put(StockReservedConstant.warehousecode, warehousecode);
				queryObj.put(StockReservedConstant.pickoutnum, quantity_should);
				

				JsonObject fields = new JsonObject();
				fields.put("_id", false);
				fields.put(StockReservedConstant.pickoutnum, true);

				JsonObject queryMsg = new JsonObject();
				queryMsg.put("queryObj", queryObj);
				queryMsg.put("resFields", fields);
				
				this.appActivity.getEventBus().send(getOnHandAddress(), queryMsg, onhandservice -> {
					
				
				//StockOnHandQueryByLocationHandler srmQueryHandler = new StockOnHandQueryByLocationHandler(this.appActivity);
				//srmQueryHandler.getLocationsByRule(queryMsg, onQueryServerdNum -> {
					if (onhandservice.succeeded()) {
//						JsonArray los = onhandservice.result();
//						if (los != null && los.size() > 0) {
//							for (int i = 0; i < los.size(); i++) {
//								JsonObject t =new JsonObject();
//								t= (JsonObject) detail;
//								t.put("location", los.getString(i));
//								newdetails.add(t);
//							}
//						}

					} else {
						newdetails.add(detail);
					}

				});
				


			});
			
			bo.put("detail", newdetails);

			// 记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
			// 自动查找数据源，自动进行分表处理
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

		});
	}

	private JsonArray getLocationsByRule(String sku, String batchCode, String warehousecode, String pickoutnum) {
	return null;
		
	}

	private void setBatchCodeByRule(JsonObject detailob) {
		JsonObject queryObj = new JsonObject();
		queryObj.put(StockReservedConstant.sku, detailob.getString(StockReservedConstant.sku));
		queryObj.put(StockReservedConstant.warehousecode, detailob.getString(StockReservedConstant.warehousecode));

		JsonObject fields = new JsonObject();
		fields.put("_id", false);
		fields.put(StockReservedConstant.pickoutnum, true);

		JsonObject queryMsg = new JsonObject();
		queryMsg.put("queryObj", queryObj);
		queryMsg.put("resFields", fields);

		// 1 根据仓库+ sku
		// 获取所有现目存量（其中现存量大于拣货数量，目前不支持混批），并且入库日期最早的那一条（先进先出）。
		// 2 从匹配后结果，得到对应批次号。
		StockOnHandQueryByBatchCodeHandler srmQueryHandler = new StockOnHandQueryByBatchCodeHandler(this.appActivity);
		srmQueryHandler.queryFristBatchNum(queryMsg, onQueryServerdNum -> {
			if (onQueryServerdNum.succeeded()) {

				JsonArray jArray = onQueryServerdNum.result();
				if (jArray != null && jArray.size() > 0) {
					String newbatch_code = jArray.getJsonObject(0).getString(StockReservedConstant.batch_code);
				}

			} else {

			}

		});
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
		String address =accountID + "."+ authSrvName + "." + ONHAND_REGISTER;
		return address;
	}

}
