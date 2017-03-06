package ocr.inventorycenter.inventorycheck;

import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseQueryHandler;
import ocr.common.handler.SampleSingleDocQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;

/**
 * 库存中心：库区-查询
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class InventorycheckQueryHandler extends SampleBillBaseQueryHandler {


	public InventorycheckQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}
	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return InventorycheckConstant.QUERY_ADDRESS;
	}

	public String getStatus(JsonObject msgBody) {

		return InventorycheckConstant.CREATE_STATUS;
	}
}
