package ocr.inventorycenter.stockout;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseHandler;
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
	
	
	// 拣货后，调用预留出库
	@Override
	protected void afterProcess(JsonObject bo, Future<JsonObject> future){

		String ReservedAddress = getReservedAdd();		

		JsonObject params = new JsonObject().put("biz_data_type", this.appActivity.getBizObjectType())
				.put("bo_id", bo.getString("bo_id"))
				.put("from_status", "RES")
				.put("to_status", "OUT");

		this.appActivity.getEventBus().send(ReservedAddress, params, reservedResults -> {

			if (reservedResults.succeeded()) {
				future.complete((JsonObject)reservedResults.result().body());
			} else {
				Throwable err = reservedResults.cause();
				String errMsg = err.getMessage();
				componentImpl.getLogger().error(errMsg, err);
				future.fail(err);
			}
		});
	}


	private String getReservedAdd() {
		return this.appActivity.getAppInstContext().getAccount() + "."
				+ this.appActivity.getAppService().getRealServiceName() + ".stockonhand-mgr.update_status";
	}

}
