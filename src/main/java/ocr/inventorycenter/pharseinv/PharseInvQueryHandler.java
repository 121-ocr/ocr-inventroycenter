package ocr.inventorycenter.pharseinv;

import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;

/**
 * 库存中心：采购入库-查询
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class PharseInvQueryHandler extends SampleBillBaseQueryHandler {

	public static final String ADDRESS = PharseInvConstant.QueryAddressConstant;

	public PharseInvQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {

		return ADDRESS;
	}

	public String getStatus(JsonObject msgBody) {

		return PharseInvConstant.CreatedStatus;
	}

}
