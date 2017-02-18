package ocr.inventorycenter.pharseorder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 采购订单
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class PharseOrderQueryByConditonHandler extends CDOHandlerImpl<JsonObject> {

	public static final String ADDRESS = "querybyconditon";

	public PharseOrderQueryByConditonHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {

		return ADDRESS;
	}

	// 处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {

		JsonObject params = msg.body();
		queryByConditions(params, ret -> {
			if (ret.succeeded()) {
				msg.reply(ret.result());
			} else {
				Throwable errThrowable = ret.cause();
				String errMsgString = errThrowable.getMessage();
				msg.fail(100, errMsgString);
			}
		});

	}

	public void queryByConditions(JsonObject params, Handler<AsyncResult<JsonObject>> next) {

		Future<JsonObject> future = Future.future();
		future.setHandler(next);

		JsonObject queryParams = params;

		String boId = queryParams.getString("bo_id");

		this.queryLatestFactData(appActivity.getBizObjectType(), boId, null, null, findRet -> {
			if (findRet.succeeded()) {			
				future.complete(findRet.result());
			} else {
				Throwable errThrowable = findRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
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
