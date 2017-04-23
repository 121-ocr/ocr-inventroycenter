package ocr.inventorycenter.pharseorder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import ocr.inventorycenter.pharseinv.QueryPharseInvByResHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.CommandMessage;

/**
 * 采购入库按钮，查询所有未入库的采购订单表体数据
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class PharseOrderQueryNoPharseInvHandler extends CDOHandlerImpl<JsonObject> {

	public static final String ADDRESS = "queryNoInv";

	public PharseOrderQueryNoPharseInvHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {

		return ADDRESS;
	}

	// 处理器
	@Override
	public void handle(CommandMessage<JsonObject> msg) {

		JsonObject params = msg.body();
		queryByConditions(msg, params, ret -> {
			if (ret.succeeded()) {
				msg.reply(ret.result());
			} else {
				Throwable errThrowable = ret.cause();
				String errMsgString = errThrowable.getMessage();
				msg.fail(100, errMsgString);
			}
		});

	}

	public void queryByConditions(CommandMessage<JsonObject> msg, JsonObject params,
			Handler<AsyncResult<JsonObject>> next) {

		this.appActivity.getEventBus().send(getPharseOrderAddress(), params, facilityRes -> {
			if (facilityRes.succeeded()) {
				if (facilityRes.succeeded()) {
					msg.reply(facilityRes.result().body());
				} else {
					Throwable errThrowable = facilityRes.cause();
					String errMsgString = errThrowable.getMessage();
					appActivity.getLogger().error(errMsgString, errThrowable);
					msg.fail(100, errMsgString);
				}

			}

		});
	}

	private String getPharseOrderAddress() {
		String server = this.appActivity.getService().getRealServiceName();
		String address = this.appActivity.getAppInstContext().getAccount() + "." + server + "." + "pharseinv-mgr" + "."
				+ "queryByRes";
		return address;
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
