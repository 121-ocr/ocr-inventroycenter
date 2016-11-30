package ocr.inventorycenter.stockonhand;


import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.WriteOption;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;
/**
 * 库存中心：现存量-删除
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class StockOnHandRemoveHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "remove";
	
	public StockOnHandRemoveHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 此action的入口地址
	 */
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	//处理器
	/**
	 * {"data_type": "VENDOR/PURCHASECONTRACT/PURCHASEINVOICE", "required": 是否必选, "desc":"描述"}
	 */
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		String acctId = this.appActivity.getAppInstContext().getAccount();
		JsonObject settingInfo = msg.body();
		settingInfo.put("account", acctId);
		appActivity.getAppDatasource().getMongoClient().removeDocumentsWithOptions(
				appActivity.getDBTableName(appActivity.getName()),
				getQueryConditon4Del(settingInfo), 
				WriteOption.ACKNOWLEDGED, 
				result -> {
					if (result.succeeded()) {
						JsonObject jo = new JsonObject();
						jo.put("RemovedCount : ", result.result().getRemovedCount());
						msg.reply(jo);						
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);		
					}
	
			});

	}
	
	private JsonObject getQueryConditon4Del(JsonObject so) {
		JsonObject query = new JsonObject();		
		query.put(StockOnHandConstant.sku, so.getString("sku"));
		query.put(StockOnHandConstant.goodaccount, so.getString("goodaccount"));
		query.put(StockOnHandConstant.invbatchcode, so.getString("invbatchcode"));
		query.put(StockOnHandConstant.locationcode, so.getString("locationcode"));
		query.put(StockOnHandConstant.warehousecode, so.getString("warehousecode"));
		return query;
	}

	/**
	 * {@inheritDoc} 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {		
		
		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		return actionDescriptor;
	}
	
}
