package ocr.inventorycenter.stockreserved;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

public class StockReservedNumQueryHandler extends ActionHandlerImpl<JsonObject>{
	
	public static final String ADDRESS = "querySRNum";

	public StockReservedNumQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		
	}
	
	@Override
	public String getEventAddress() {
		
		return ADDRESS;
	}
	
	
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> event) {
		FindOptions findOptions = new FindOptions();
		findOptions.setFields(event.body().getJsonObject("resFields"));
		
		appActivity.getAppDatasource().getMongoClient().findWithOptions(
				appActivity.getDBTableName(appActivity.getBizObjectType()), 
				event.body().getJsonObject("queryObj"), 
				findOptions,
				result -> {
    	    if (result.succeeded()) {
    	    	String resStr = result.result().toString(); 
    	    	JsonArray reserverdnumArray = new JsonArray(resStr);
    	    	if(!reserverdnumArray.isEmpty()){
    	    		event.reply(reserverdnumArray); 
    	    	}else{
    	    		JsonObject obj = new JsonObject();
    	    		obj.put(StockReservedConstant.pickoutnum, 0.0);
    	    		JsonArray array = new JsonArray();
    	    		array.add(obj);
    	    		event.reply(array);
    	    	}    	    	
    	    	   	    	
    	    } else {
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				event.fail(100, errMsgString);		
   
    	    }	
		});
		
	}

	
	@Override
	public ActionDescriptor getActionDesc() {		
		
		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.GET);
		handlerDescriptor.setRestApiURI(uri);
		
		
		return actionDescriptor;
	}
	

}