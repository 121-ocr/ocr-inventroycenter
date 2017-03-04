package ocr.inventorycenter.inventorycheck;


import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.common.DoubleUtil;
import ocr.common.handler.SampleBillBaseHandler;
import ocr.common.handler.SampleSingleDocBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;
/**
 * 库存盘点确认操作
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class InventorycheckConfirmHandler extends SampleBillBaseHandler{
	
	public InventorycheckConfirmHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override 
	public String getEventAddress() {
		return InventorycheckConstant.CONFIRM_ADDRESS;
	}

	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, InventorycheckConstant.CREATE_STATUS, InventorycheckConstant.CONFIRM_STATUS);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

	/**
	 * 更新现存量
	 * @param bo
	 * @param future
	 */
	protected void afterProcess(JsonObject bos, Future<JsonObject> future) {
		//根据盘点出来的盈亏，更新现存量
		JsonArray invOnhand = getInvOnhandObject(bos);
		createInvOnhand(invOnhand, future);
	}
	
	/**
	 * 获取现存量VO
	 * 
	 * @param replenishment
	 * @return
	 */
	private JsonArray getInvOnhandObject(JsonObject invCheck) {
		JsonArray paramList = new JsonArray();
		for (Object detail : invCheck.getJsonArray("detail")) {
			JsonObject param = new JsonObject();
			JsonObject detailO = (JsonObject) detail;
			param.put("warehouses", invCheck.getJsonObject("warehouses"));
			param.put("goods", detailO.getJsonObject("goods"));
			param.put("sku", detailO.getJsonObject("goods").getString("product_sku_code"));
			param.put("invbatchcode", detailO.getString("invbatchcode"));
			param.put("warehousecode", invCheck.getJsonObject("warehouses").getString("code"));
			//计算盈亏
			Double booknum = detailO.getDouble("booknum");//账面
			Double realnum = detailO.getDouble("realnum");//实际
			Double loss = DoubleUtil.sub(realnum, booknum);
			if(loss.compareTo(0.0) > 0){
				param.put("status", "in");
			}else if(loss.compareTo(0.0) < 0){
				param.put("status", "out");
			}else{
				continue;
			}			
			param.put("biz_data_type", this.appActivity.getBizObjectType());
			param.put("bo_id", invCheck.getString("bo_id"));
			param.put("goodaccount", detailO.getJsonObject("goods").getString("account"));
		
			param.put("onhandnum", Math.abs(loss));
			
			paramList.add(param);
		}
		return paramList;
	}
	
	/**
	 * 保存现存量
	 * 
	 * @param prices
	 * @param invOnhandFuture
	 */
	private void createInvOnhand(JsonArray invOnhand, Future<JsonObject> invOnhandFuture) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		// 按照分页条件查询收货通知
		String onHandAddress = from_account + "." + this.appActivity.getService().getRealServiceName()
				+ ".stockonhand-mgr.batchcreate";
		this.appActivity.getEventBus().send(onHandAddress, invOnhand, invRet -> {
			if (invRet.succeeded()) {
				invOnhandFuture.complete();
			} else {
				Throwable errThrowable = invRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				invOnhandFuture.fail(invRet.cause());
			}
		});
	}
	
}
