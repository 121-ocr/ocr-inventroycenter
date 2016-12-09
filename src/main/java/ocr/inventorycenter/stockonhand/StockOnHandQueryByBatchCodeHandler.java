package ocr.inventorycenter.stockonhand;


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import ocr.inventorycenter.stockreserved.StockReservedConstant;
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
public class StockOnHandQueryByBatchCodeHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "query";

	public StockOnHandQueryByBatchCodeHandler(AppActivityImpl appActivity) {
		super(appActivity);
		
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> event) {

		queryAllBatchs(event.body(), ret -> {
			if (ret.succeeded()) {
				event.reply(ret.result());
			} else {
				Throwable errThrowable = ret.cause();
				event.fail(100, errThrowable.getMessage());
			}

		});

	}

	public void queryAllBatchs(JsonObject params, Handler<AsyncResult<JsonArray>> next) {
		FindOptions findOptions = new FindOptions();
		findOptions.setFields(params.getJsonObject("resFields"));
		findOptions.setLimit(1);
		// sort 1 asc -1 desc
		JsonObject sortFields = new JsonObject();
		sortFields.put(StockReservedConstant.batch_code, 1);
		findOptions.setSort(sortFields);

		Future<JsonArray> future = Future.future();
		future.setHandler(next);

		appActivity.getAppDatasource().getMongoClient().findWithOptions(
				appActivity.getDBTableName(appActivity.getBizObjectType()), params.getJsonObject("queryObj"),
				findOptions, result -> {
					if (result.succeeded()) {
						future.complete(new JsonArray(result.result()));

					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						// event.fail(100, errMsgString);
						future.fail(errThrowable);

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
				
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		
		return actionDescriptor;
	}
	
	
}
