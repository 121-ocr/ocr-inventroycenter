package ocr.inventorycenter.stockreserved;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

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
	public void handle(OtoCloudBusMessage<JsonObject> event) {			
		appActivity.getAppDatasource().getMongoClient().save(appActivity.getDBTableName(appActivity.getBizObjectType()),
				event.body(), result -> {
					if (result.succeeded()) {
						event.reply(event.body());
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						event.fail(100,errMsgString);
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