package ocr.inventorycenter.stockout;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseHandler;
import ocr.inventorycenter.stockonhand.StockOnHandConstant;
import ocr.inventorycenter.stockreserved.StockReservedConstant;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 拣货出库单--（拣货态-->发货状）
 * 
 * @author LCL
 *
 */
public class StockOutOnPickingHandler extends SampleBillBaseHandler {

	public static final String ADDRESS = StockOutConstant.ONShippingOutAddressConstant;

	public StockOutOnPickingHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
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
				StockOutConstant.CreatedStatus, StockOutConstant.ONPickingStatus);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

	// 出库后： 解除预留（把预留值改成负数插入数据），删除现存量
	protected void afterProcess(OtoCloudBusMessage<JsonObject> msg) {

		removeReserved(msg);

		removeOnhand(msg);
	}

	private void removeOnhand(OtoCloudBusMessage<JsonObject> msg) {
		String onhandAddress = getOnhandRemoveAddress();
		this.appActivity.getEventBus().send(onhandAddress, getOnhandParam(msg.body()), reservedResults -> {
			if (reservedResults.succeeded()) {
				msg.reply("扣减现存量成功");
			} else {
				Throwable err = reservedResults.cause();
				String errMsg = err.getMessage();
				componentImpl.getLogger().error(errMsg, err);
				msg.fail(100, errMsg);
			}
		});

	}

	private void removeReserved(OtoCloudBusMessage<JsonObject> msg) {
		String reservedAddress = getReservedModifyAddress();
		this.appActivity.getEventBus().send(reservedAddress, getReservedParam(msg.body()), reservedResults -> {
			if (reservedResults.succeeded()) {
				msg.reply("解除预留成功");
			} else {
				Throwable err = reservedResults.cause();
				String errMsg = err.getMessage();
				componentImpl.getLogger().error(errMsg, err);
				msg.fail(100, errMsg);
			}
		});
	}

	private JsonObject getReservedParam(JsonObject so) {
		Double pickoutnum = so.getDouble(StockReservedConstant.pickoutnum);
		so.put(StockReservedConstant.pickoutnum, -pickoutnum);
		return so;
	}

	private JsonObject getOnhandParam(JsonObject so) {
		// JsonObject params = new JsonObject();
		//
		// params.put(StockOnHandConstant.sku, so.getString("sku"));
		// params.put(StockOnHandConstant.goodaccount,
		// so.getString("goodaccount"));
		// params.put(StockOnHandConstant.invbatchcode,
		// so.getString("invbatchcode"));
		// params.put(StockOnHandConstant.locationcode,
		// so.getString("locationcode"));
		// params.put(StockOnHandConstant.warehousecode,
		// so.getString("warehousecode"));

		return so;
	}

	private String getReservedModifyAddress() {
		return this.appActivity.getAppInstContext().getAccount() + "."
				+ this.appActivity.getAppService().getRealServiceName() + StockReservedConstant.ComponentNameConstant
				+ StockReservedConstant.ModifyAddressConstant;

	}

	private String getOnhandRemoveAddress() {
		return this.appActivity.getAppInstContext().getAccount() + "."
				+ this.appActivity.getAppService().getRealServiceName() + StockOnHandConstant.ComponentNameConstant
				+ StockOnHandConstant.CreateAddressConstant;

	}

}
