package ocr.inventorycenter.allocateorders;

import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseQueryHandler;
import ocr.inventorycenter.pharseorder.PharseOrderConstant;
import otocloud.framework.app.function.AppActivityImpl;

/**
 * 盘点单
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class AllocateordersQueryHandler extends SampleBillBaseQueryHandler {

	public AllocateordersQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return AllocateordersConstant.QUERY_ADDRESS;
	}

	public String getStatus(JsonObject msgBody) {

		return AllocateordersConstant.CREATE_STATUS;
	}
}
