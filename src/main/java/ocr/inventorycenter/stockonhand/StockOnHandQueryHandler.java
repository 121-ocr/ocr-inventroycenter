package ocr.inventorycenter.stockonhand;


import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;
/**
 * 库存中心：现存量-查询
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class StockOnHandQueryHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "query";

	public StockOnHandQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	//处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {

		
//		String menusFilePath = OtoCloudDirectoryHelper.getConfigDirectory() + "stockonhand.json";	
		
		FindOptions findOptions = new FindOptions();
		//limit
		findOptions.setLimit(2);
		//fields
		JsonObject fields = new JsonObject();
		fields.put("_id", false);
		fields.put(StockOnHandConstant.sku, true);
		fields.put(StockOnHandConstant.goodaccount, true);
		fields.put(StockOnHandConstant.invbatchcode, true);
		fields.put(StockOnHandConstant.warehousecode, true);
		
		findOptions.setFields(fields);
		//sort 1 asc -1 desc
		JsonObject sortFields = new JsonObject();
		sortFields.put(StockOnHandConstant.sku, 1);
		
		findOptions.setSort(sortFields);
		
		appActivity.getAppDatasource().getMongoClient().find(
				appActivity.getDBTableName(appActivity.getBizObjectType()), 
				getQueryConditon(msg.body()), 
				//null,
				result -> {

    	    if (result.succeeded()) {
    	  
    	        msg.reply(result.result());     	        
    	        
    	    } else {
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);		
   
    	    }	
		});


	}
	
	private JsonObject getQueryConditon(JsonObject so) {
		JsonObject query = new JsonObject();
		if(so.containsKey(StockOnHandConstant.sku)
				&& !so.getString(StockOnHandConstant.sku).isEmpty()){
			query.put(StockOnHandConstant.sku, so.getString(StockOnHandConstant.sku));
		}
		if(so.containsKey(StockOnHandConstant.goodaccount)){
			String goodaccount = so.getString(StockOnHandConstant.goodaccount);		
			if(goodaccount != null && !goodaccount.isEmpty()){
				query.put(StockOnHandConstant.goodaccount, goodaccount);
			}
		}
		
		if(so.containsKey("invbatchcode")){
			String batchCode = so.getString("invbatchcode");
			if(batchCode != null && !batchCode.isEmpty())
				query.put("invbatchcode", batchCode);	
		}
		
//		query.put(StockOnHandConstant.sku, so.getString("sku"));
//		query.put(StockOnHandConstant.invbatchcode, so.getString("invbatchcode"));
//		query.put(StockOnHandConstant.locationcode, so.getString("locationcode"));
		query.put(StockOnHandConstant.warehousecode, so.getString("warehousecode"));
		
		return query;
	}
	

	/**
	 * 此action的自描述元数据
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
