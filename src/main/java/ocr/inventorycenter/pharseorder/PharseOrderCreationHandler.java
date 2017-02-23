package ocr.inventorycenter.pharseorder;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseHandler;
import ocr.inventorycenter.stockout.StockOutConstant;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 库存中心：采购入库-创建
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class PharseOrderCreationHandler extends SampleBillBaseHandler {

	private static final String DETAIL_CODE = "detail_code";

	public PharseOrderCreationHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return PharseOrderConstant.ADDRESS;
	}

	/**
	 * 单据保存前，设置表体行号，逻辑是找出当天最大的行号。在此基础上增加行号
	 * 
	 * @param bo
	 * @param future
	 */
	protected void beforeProess(OtoCloudBusMessage<JsonObject> msg, Future<JsonObject> future) {
		JsonObject bo = msg.body();

		JsonArray replacedDetails = new JsonArray();
		JsonArray details = bo.getJsonArray("detail");

		Integer maxCode = getMaxCode(details);

		int i = 1;
		for (Object detailObj : details) {
			JsonObject detail = (JsonObject) detailObj;
			Integer detailcode = detail.getInteger(DETAIL_CODE);
			if (detailcode.compareTo(-1)==0) {
				JsonObject newDetail = new JsonObject();
				newDetail = detail.copy();
				newDetail.put(DETAIL_CODE, maxCode + i);
				replacedDetails.add(newDetail);
				i++;
				continue;
			}
			replacedDetails.add(detail);

		}

		if (replacedDetails.size() > 0) {
			details.clear();
			details.addAll(replacedDetails);
		}

		future.complete(bo);
	}

	private Integer getMaxCode(JsonArray details) {
		
		Integer maxCode = 0; // 找出最大的序号
		if (details == null || details.size() == 0) {
			return maxCode;
		}

		for (Object detailObj : details) {
			JsonObject detail = (JsonObject) detailObj;
			Integer idetailcode = detail.getInteger(DETAIL_CODE);
			if (idetailcode != null) {
				
				if (idetailcode.compareTo(maxCode) > 0) {
					maxCode = idetailcode;
				}
			}

		}
		return maxCode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(PharseOrderConstant.ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, null,
				StockOutConstant.CreatedStatus);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

}
