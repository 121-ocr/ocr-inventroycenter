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

	private static final String IS_CREATED_PHARSE_INV = "is_createdPharseInv";

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

		Future<Void> nextFuture = Future.future(); // 规定步骤1、2之间的顺序

		JsonObject results = new JsonObject();
		// 步骤一、查询是否已经生成过采购入库单
		isCreatedInv(msg, results, nextFuture);

		// 步骤二，与步骤一串行的 ，第一步根据结果处理,补全采购订单数据
		nextFuture.setHandler(nextHandler -> {
			queryAllPhrase(msg, results);
		});

	}

	private void queryAllPhrase(OtoCloudBusMessage<JsonObject> msg, JsonObject results) {

		JsonObject result = new JsonObject();

		if (!results.getBoolean(IS_CREATED_PHARSE_INV)) {
			JsonObject params = msg.body();
			queryByConditions(params, ret -> {
				if (ret.succeeded()) {
					result.put("result", ret.result());
					msg.reply(result);
				} else {
					Throwable errThrowable = ret.cause();
					String errMsgString = errThrowable.getMessage();
					msg.fail(100, errMsgString);
				}
			});
		} else {
			msg.reply(null);
		}

	}

	private void isCreatedInv(OtoCloudBusMessage<JsonObject> msg, JsonObject results, Future<Void> nextFuture) {

		this.appActivity.getEventBus().send(getPharseOrderAddress(), msg.body(), facilityRes -> {
			if (facilityRes.succeeded()) {
				JsonObject body = (JsonObject) facilityRes.result().body();
				boolean rs = body.getBoolean("result");
				if (rs) {
					results.put(IS_CREATED_PHARSE_INV, true);
				} else {
					results.put(IS_CREATED_PHARSE_INV, false);
				}
				nextFuture.complete();
			} else {
				Throwable errThrowable = facilityRes.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				nextFuture.failed();
			}

		});

	}

	private String getPharseOrderAddress() {
		String server = this.appActivity.getService().getRealServiceName();
		String address = this.appActivity.getAppInstContext().getAccount() + "." + server + "." + "pharseinv-mgr" + "."
				+ "queryByRes";
		return address;
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
