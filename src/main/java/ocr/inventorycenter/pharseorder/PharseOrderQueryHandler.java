package ocr.inventorycenter.pharseorder;

import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;

/**
 * 采购订单
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class PharseOrderQueryHandler extends SampleBillBaseQueryHandler {

	public static final String ADDRESS = PharseOrderConstant.QueryAddressConstant;

	public PharseOrderQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {

		return ADDRESS;
	}

	public String getStatus(JsonObject msgBody) {

		return PharseOrderConstant.CreatedStatus;
	}

}
