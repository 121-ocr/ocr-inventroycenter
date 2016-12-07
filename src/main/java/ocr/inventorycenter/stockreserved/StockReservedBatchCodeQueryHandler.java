package ocr.inventorycenter.stockreserved;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import ocr.inventorycenter.stockonhand.StockOnHandConstant;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

public class StockReservedBatchCodeQueryHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "getFirstBatch";

	public StockReservedBatchCodeQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	@Override
	public String getEventAddress() {

		return ADDRESS;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> event) {

		queryFristBatchNum(event.body(), ret -> {
			if (ret.succeeded()) {
				event.reply(ret.result());
			} else {
				Throwable errThrowable = ret.cause();
				event.fail(100, errThrowable.getMessage());
			}

		});

	}

	public void queryFristBatchNum(JsonObject params, Handler<AsyncResult<JsonArray>> next) {
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

	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.GET);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

}