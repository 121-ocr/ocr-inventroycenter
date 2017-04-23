package ocr.inventorycenter.stockreserved;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.CommandMessage;

public class StockReservedSaveHandler extends ActionHandlerImpl<JsonObject>{

	public static final String ADDRESS = "saveStockReserved";
	
	public StockReservedSaveHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	@Override
	public String getEventAddress() {
		
		return ADDRESS;
	}

	@Override
	public void handle(CommandMessage<JsonObject> event) {			
		JsonObject bo = event.getContent();
		saveStockReserved(bo, ret->{
			if (ret.succeeded()) {
				event.reply(event.body());
			} else {
				Throwable errThrowable = ret.cause();
				String errMsgString = errThrowable.getMessage();
				event.fail(100,errMsgString);
			}	
			
		});

	}
	
	public void saveStockReserved(JsonObject bo,  Handler<AsyncResult<JsonObject>> next){
		Future<JsonObject> future = Future.future();
		future.setHandler(next);
		appActivity.getAppDatasource().getMongoClient().save(appActivity.getDBTableName(appActivity.getBizObjectType()),
				bo, result -> {
					if (result.succeeded()) {
						//event.reply(event.body());
						future.complete(bo);
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						//event.fail(100,errMsgString);
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