package ocr.inventorycenter.pharseinv;

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
public class PharseInvCreationHandler extends SampleBillBaseHandler {

	public PharseInvCreationHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return PharseInvConstant.ADDRESS;
	}

	/**
	 * 单据保存前，
	 * 
	 * @param bo
	 * @param future
	 */
	protected void beforeProess(OtoCloudBusMessage<JsonObject> msg, Future<JsonObject> future) {
		JsonObject bo = msg.body();
		String inv_date = bo.getString(PharseInvConstant.INV_DATE);
		JsonArray replacedDetails = new JsonArray();
		JsonArray details = bo.getJsonArray("detail");

		for (Object detailObj : details) {
			JsonObject detail = (JsonObject) detailObj;
			String batchCode = detail.getString(PharseInvConstant.BATCH_CODE);
			if (null == batchCode || batchCode.isEmpty()) {
				JsonObject newDetail = new JsonObject();
				newDetail = detail.copy();
				newDetail.put(PharseInvConstant.BATCH_CODE, generatorBatchCode(detail, inv_date));
				replacedDetails.add(newDetail);
				continue;
			}
			replacedDetails.add(detail);
		}

		if (replacedDetails.size() > 0) {
			details.clear();
			details.addAll(replacedDetails);
			// bo.put("detail", details);
		}

		future.complete(bo);
	}

	/**
	 * 实效日期 +入库日期
	 * 
	 * @param detailO
	 * 
	 * @return 批次号
	 */
	private String generatorBatchCode(JsonObject detailO, String inv_date) {
		String code = detailO.getString(PharseInvConstant.EXPDATE) + "-" + inv_date;
		return code.replace("-", "/");
	}

	/**
	 * 单据保存后处理--新增现存量
	 * 
	 * @param bo
	 * @param future
	 */
	protected void afterProcess(JsonObject bo, Future<JsonObject> future) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		JsonArray paramList = new JsonArray();
		for (Object detail : bo.getJsonArray("detail")) {
			JsonObject param = new JsonObject();
			JsonObject detailO = (JsonObject) detail;
			param.put(PharseInvConstant.WAREHOUSES, bo.getJsonObject(PharseInvConstant.WAREHOUSE));
			param.put(PharseInvConstant.GOODS, detailO.getJsonObject(PharseInvConstant.GOODS));
			param.put(PharseInvConstant.SKU,
					detailO.getJsonObject(PharseInvConstant.GOODS).getString(PharseInvConstant.PRODUCT_SKU_CODE));
			param.put(PharseInvConstant.INVBATCHCODE, detailO.getString(PharseInvConstant.BATCH_CODE));
			param.put(PharseInvConstant.WAREHOUSECODE, bo.getJsonObject(PharseInvConstant.WAREHOUSE).getString("code"));
			param.put(PharseInvConstant.ONHANDNUM, detailO.getValue(PharseInvConstant.NSNUM));
			param.put(PharseInvConstant.GOODACCOUNT,
					detailO.getJsonObject(PharseInvConstant.GOODS).getString("account"));

			paramList.add(param);
		}

		// 增加现存量，调用现存量的接口
		this.appActivity.getEventBus().send(getOnhandAddr(), paramList, invRet -> {
			if (invRet.succeeded()) {
				future.complete(bo);
			} else {
				future.fail(invRet.cause());
			}
		});

	}

	private String getOnhandAddr() {
		return this.appActivity.getAppInstContext().getAccount() + "."
				+ this.appActivity.getAppService().getRealServiceName() + ".stockonhand-mgr.batchcreate";

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(PharseInvConstant.ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, null,
				StockOutConstant.CreatedStatus);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

}
