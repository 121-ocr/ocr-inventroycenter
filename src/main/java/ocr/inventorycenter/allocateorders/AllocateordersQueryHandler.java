package ocr.inventorycenter.allocateorders;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseQueryHandler;
import ocr.inventorycenter.pharseorder.PharseOrderConstant;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudBusMessage;

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

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {

		JsonObject queryParams = msg.body();
		JsonObject fields = queryParams.getJsonObject("fields");
		JsonObject queryCond = queryParams.getJsonObject("query");
		JsonObject pagingInfo = queryParams.getJsonObject("paging");
		this.queryLatestFactDataList(appActivity.getBizObjectType(), getStatus2(queryParams), fields, pagingInfo,
				queryCond, null, findRet -> {
					if (findRet.succeeded()) {
						msg.reply(findRet.result());
					} else {
						Throwable errThrowable = findRet.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
					}

				});
	}

	public List<String> getStatus2(JsonObject msgBody) {
		List<String> ret = new ArrayList<>();
		ret.add(AllocateordersConstant.CREATE_STATUS);
		ret.add(AllocateordersConstant.CONFIRM_STATUS);
		return ret;
	}
}
