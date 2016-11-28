package ocr.inventorycenter.stockonhand;


import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;
/**
 * 库存中心：现存量-更新
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class StockOnHandUpdateHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "update";

	public StockOnHandUpdateHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	//处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {

		JsonObject settingInfo = msg.body();
		JsonObject so = msg.body();
		settingInfo.put(StockOnHandConstant.bo_id, "");
		settingInfo.put(StockOnHandConstant.account, this.appActivity.getAppInstContext().getAccount());
		settingInfo.put(StockOnHandConstant.locations, so.getString("locations"));
		settingInfo.put(StockOnHandConstant.warehouses, so.getString("warehouses"));
		settingInfo.put(StockOnHandConstant.goods, so.getString("goods"));
		settingInfo.put(StockOnHandConstant.sku, so.getString("sku"));
		settingInfo.put(StockOnHandConstant.invbatchcode, so.getString("invbatchcode"));
		settingInfo.put(StockOnHandConstant.locationcode, so.getString("locationcode"));
		settingInfo.put(StockOnHandConstant.warehousecode, so.getString("warehousecode"));
		settingInfo.put(StockOnHandConstant.onhandnum, so.getString("num"));
		
		MongoClient mongoClient = appActivity.getAppDatasource().getMongoClient();
		mongoClient.save(
				appActivity.getDBTableName(appActivity.getName()), settingInfo,
				result -> {
					if (result.succeeded()) {
						settingInfo.put("_id", result.result());
						msg.reply(settingInfo);						
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);		
					}
	
			});


	}
	

	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {		
		
		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
		//handlerDescriptor.setMessageFormat("command");
		
		//参数
/*		List<ApiParameterDescriptor> paramsDesc = new ArrayList<ApiParameterDescriptor>();
		paramsDesc.add(new ApiParameterDescriptor("targetacc",""));		
		paramsDesc.add(new ApiParameterDescriptor("soid",""));		
		
		actionDescriptor.getHandlerDescriptor().setParamsDesc(paramsDesc);	*/
				
		ActionURI uri = new ActionURI("create", HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		return actionDescriptor;
	}
	
	
}
