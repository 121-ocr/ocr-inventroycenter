package ocr.inventorycenter.stockout;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseHandler;
import ocr.inventorycenter.stockreserved.StockReservedConstant;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 拣货出库单--（贱货中-->拣货完成）
 * 
 * @author LCL
 *
 */
public class StockOutPickOutHandler extends SampleBillBaseHandler {

	public static final String ADDRESS = StockOutConstant.PickOutAddressConstant;

	public StockOutPickOutHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT,
				StockOutConstant.ONPickingStatus, StockOutConstant.PickOutedStatus, true, true);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

	// 拣货后，调用预留
	protected void afterProcess(OtoCloudBusMessage<JsonObject> msg) {
		processReserved(msg);
	}

	private void processReserved(OtoCloudBusMessage<JsonObject> msg) {

		String ReservedAddress = getReservedAdd();

		this.appActivity.getEventBus().send(ReservedAddress, getQueryParam(msg.body()), reservedResults -> {

			if (reservedResults.succeeded()) {
				msg.reply("预留成功");
			} else {
				Throwable err = reservedResults.cause();
				String errMsg = err.getMessage();
				componentImpl.getLogger().error(errMsg, err);
				msg.fail(100, errMsg);
			}
		});
	}

	private String getReservedAdd() {
		return this.appActivity.getAppInstContext().getAccount() + "."
				+ this.appActivity.getAppService().getRealServiceName() + "."
				+ StockReservedConstant.ComponentNameConstant + "."
				+ StockReservedConstant.ReservedAddressConstant;

	}

	private JsonObject getQueryParam(JsonObject so) {
		JsonObject queryMsg = new JsonObject();
		queryMsg.put("queryObj", getQueryConditon(so));
		queryMsg.put("resFields", getFieldsCols());
		return queryMsg;
	}

	private JsonObject getQueryConditon(JsonObject so) {
		JsonObject queryObj = new JsonObject();
		queryObj.put(StockReservedConstant.sku, so.getString(StockReservedConstant.sku));
		queryObj.put(StockReservedConstant.warehousecode, so.getString(StockReservedConstant.warehousecode));
		queryObj.put(StockReservedConstant.pickoutid, so.getString(StockReservedConstant.pickoutid));
		queryObj.put(StockReservedConstant.pickoutnum, so.getString(StockReservedConstant.pickoutnum));
		return queryObj;
	}

	private JsonObject getFieldsCols() {
		JsonObject fields = new JsonObject();
		fields.put("_id", false);
		fields.put(StockReservedConstant.pickoutnum, true);
		return fields;
	}

}
