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
		
		List<Future> futures = new ArrayList<>();
		
		JsonArray resultObjects = new JsonArray();
		
		documents.forEach(document->{
			
			Future<JsonObject> returnFuture = Future.future();
			futures.add(returnFuture);
			
			JsonObject bo = (JsonObject)document;
			
			String boId = bo.getString("bo_id");
			
			//TODO 如果没有boid，则调用单据号生成规则生成一个单据号		
			//交易单据一般要记录协作方
	    	String partnerAcct = bo.getJsonObject("channel").getString("account");
	    	
	    	JsonObject stockOutRet = new JsonObject();
	    	resultObjects.add(stockOutRet);

			// 记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
			// 自动查找数据源，自动进行分表处理
			this.recordFactData(appActivity.getBizObjectType(), bo, boId, actor, partnerAcct, null, result -> {		
				stockOutRet.put("bo_id", bo.getString("bo_id"));
				stockOutRet.put("warehouse", bo.getJsonObject("warehouse"));				
				if (result.succeeded()) {	
					
					JsonArray reservedErrors = new JsonArray();
					stockOutRet.put("details", reservedErrors);
					
					List<Future> reservedFutures = new ArrayList<>();

					JsonArray boDetailArray = bo.getJsonArray("detail");
					boDetailArray.forEach(item->{
						Future<Void> reservedFuture = Future.future();
						reservedFutures.add(reservedFuture);
						
						JsonObject boDetail = (JsonObject)item;						

						//进行预留
						String ReservedAddress = getReservedAdd();
						JsonObject stockReservedObject = convertToStockReservedObj(bo, boDetail);
						DeliveryOptions options = new DeliveryOptions();
						options.setHeaders(headerMap);
						this.appActivity.getEventBus().send(ReservedAddress, stockReservedObject, options, 
								next -> {
							if (next.succeeded()) {
								reservedFuture.complete();								
							} else {
								Throwable err = next.cause();
								String errMsg = err.getMessage();
								componentImpl.getLogger().error(errMsg, err);								
								reservedFuture.fail(err);	
								
								//错误记录
								JsonObject reservedError = new JsonObject();
								reservedError.put(StockReservedConstant.sku, boDetail.getJsonObject("goods").getString("product_sku_code"));
								reservedError.put(StockReservedConstant.batch_code, boDetail.getString("batch_code"));
								reservedError.put("error", errMsg);
								reservedErrors.add(reservedError);							
								
							}
						});
					});
					
					CompositeFuture.join(reservedFutures).setHandler(ar -> {						
						returnFuture.complete();
					});	
					
				} else {
					stockOutRet.put("error", result.cause().getMessage());	
					returnFuture.fail(result.cause());
				}
			});

		});	

		CompositeFuture.join(futures).setHandler(ar -> {
			msg.reply(resultObjects);
		});	

	}


	private String getReservedAdd() {
		return this.appActivity.getAppInstContext().getAccount() + "."
				+ this.appActivity.getAppService().getRealServiceName() + "." 
				+ StockReservedConstant.ComponentNameConstant + "." 
				+ StockReservedConstant.ReservedAddressConstant;

	}


	private JsonObject convertToStockReservedObj(JsonObject so, JsonObject detail) {
		JsonObject retObj = new JsonObject();
		JsonObject goodsJsonObject = detail.getJsonObject("goods");
		JsonObject warehouseJsonObject = so.getJsonObject("warehouse");
		retObj.put(StockReservedConstant.sku, goodsJsonObject.getString("product_sku_code"));
		retObj.put(StockReservedConstant.warehousecode, warehouseJsonObject.getString("code"));
		if(detail.containsKey("batch_code")){
			String batchCode = detail.getString("batch_code");
			if(batchCode != null && !batchCode.isEmpty())
				retObj.put("batch_code", batchCode);	
		}
		retObj.put(StockReservedConstant.pickoutid, so.getString("bo_id"));
		retObj.put(StockReservedConstant.goodaccount, so.getString("goodaccount"));
		retObj.put(StockReservedConstant.pickoutnum, detail.getDouble("quantity_should"));
		retObj.put(StockReservedConstant.warehouses, warehouseJsonObject);
		retObj.put(StockReservedConstant.goods, goodsJsonObject);
		return retObj;
	}


}
