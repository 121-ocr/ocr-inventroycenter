package ocr.inventorycenter.stockreserved;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.CommandMessage;

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
	public void handle(CommandMessage<JsonObject> event) {
		
		querySRNum(event.getContent(), ret->{
    	    if (ret.succeeded()) {
    	    	event.reply(ret.result()); 
    	    } else {
				Throwable errThrowable = ret.cause();
				event.fail(100, errThrowable.getMessage());
    	    }		
			
		});		

	}
	
	public void querySRNum(JsonObject params,  Handler<AsyncResult<JsonArray>> next){
		FindOptions findOptions = new FindOptions();
		findOptions.setFields(params.getJsonObject("resFields"));
		
		Future<JsonArray> future = Future.future();
		future.setHandler(next);
		
		appActivity.getAppDatasource().getMongoClient().findWithOptions(
				appActivity.getDBTableName(appActivity.getBizObjectType()), 
				params.getJsonObject("queryObj"), 
				findOptions,
				result -> {
    	    if (result.succeeded()) {
    	    	future.complete(new JsonArray(result.result()));
    	    	
    	    	//String resStr = result.result().toString(); 
    	    	//JsonArray reserverdnumArray = new JsonArray(resStr);
    	    	/*if(!reserverdnumArray.isEmpty()){
    	    		//event.reply(reserverdnumArray); 
    	    		future.complete(reserverdnumArray);
    	    	}else{
    	    		JsonObject obj = new JsonObject();
    	    		obj.put(StockReservedConstant.reserverdnum, 0.0);
    	    		JsonArray array = new JsonArray();
    	    		array.add(obj);
    	    		//event.reply(array);
    	    		future.complete(array);
    	    	}    	    	*/
    	    	   	    	
    	    } else {
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				//event.fail(100, errMsgString);	
				future.fail(errThrowable);
   
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