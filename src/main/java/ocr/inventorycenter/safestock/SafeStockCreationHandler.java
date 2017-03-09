package ocr.inventorycenter.safestock;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseHandler;
import ocr.common.handler.SampleDocBaseHandler;
import ocr.common.handler.SampleDocQueryHandler;
import ocr.common.handler.SampleSingleDocBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 库存中心：安全库存-创建
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class SafeStockCreationHandler extends SampleSingleDocBaseHandler {

	public static final String ADDRESS = SafeStockConstant.CreateAddressConstant;

	public SafeStockCreationHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	@Override
	protected void beforeProess(OtoCloudBusMessage<JsonObject> msg, Future<JsonObject> future) {
		String err = safeStockNullVal(msg.body());
		if (err.isEmpty()) {
			future.complete(msg.body());
		} else {
			future.fail(err);
		}

	}

	private String safeStockNullVal(JsonObject bo) {
		StringBuffer errors = new StringBuffer();

		Object warehouses = bo.getValue(SafeStockConstant.warehouses);

		if (null == warehouses || warehouses.equals("")) {
			errors.append("仓库");
		}

		String sku = bo.getString(SafeStockConstant.sku);
		if (null == sku || sku.equals("")) {
			errors.append("sku");
		}

		Object goods = bo.getValue(SafeStockConstant.goods);

		if (null == goods || goods.equals("")) {
			errors.append("商品信息");
		}

		if (!bo.containsKey(SafeStockConstant.safenum)) {
			errors.append("安全库存");
		}
		return errors.toString();
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
