package ocr.inventorycenter.allocateorders;


import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
/**
 * 库存调拨确认操作
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class AllocateordersConfirmHandler extends SampleBillBaseHandler{
	
	public AllocateordersConfirmHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override 
	public String getEventAddress() {
		return AllocateordersConstant.CONFIRM_ADDRESS;
	}

	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, AllocateordersConstant.CREATE_STATUS, AllocateordersConstant.CONFIRM_STATUS);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

	/**
	 * 生成出入存量记录
	 * @param bo
	 * @param future
	 */
	@Override
	protected void afterProcess(JsonObject bos, Future<JsonObject> future) {
		//调拨出库单做减库存，调拨入库单做加库存
		JsonArray invOnhand = getInvOnhandObject(bos);
		createInvOnhand(bos, invOnhand, future);
	}
	
	/**
	 * 获取现存量VO
	 * 
	 * @param replenishment
	 * @return
	 */
	private JsonArray getInvOnhandObject(JsonObject allocateorder) {
		JsonArray paramList = new JsonArray();
		for (int i = 0; i < 2; i++) {
			JsonObject warehouses = null;
			String warehouses_code = null;
			if( i == 0){
				//调拨出
				warehouses = allocateorder.getJsonObject("outwarehouses");
				warehouses_code = allocateorder.getJsonObject("outwarehouses").getString("code");
			}else if(i == 1){
				//调拨入
				warehouses = allocateorder.getJsonObject("inwarehouses");
				warehouses_code = allocateorder.getJsonObject("inwarehouses").getString("code");
			}			
			for (Object detail : allocateorder.getJsonArray("detail")) {
				JsonObject param = new JsonObject();
				JsonObject detailO = (JsonObject) detail;
				param.put("warehouses", warehouses);
				param.put("goods", detailO.getJsonObject("goods"));
				param.put("sku", detailO.getJsonObject("goods").getString("product_sku_code"));
				param.put("invbatchcode", detailO.getString("invbatchcode"));
				param.put("warehousecode", warehouses_code);

				if(i == 0){
					param.put("status", "out");
				}else if(i == 1){
					param.put("status", "in");
				}
				
				param.put("biz_data_type", this.appActivity.getBizObjectType());
				param.put("bo_id", allocateorder.getString("bo_id"));
				param.put("goodaccount", detailO.getJsonObject("goods").getString("account"));
			
				param.put("onhandnum", detailO.getDouble("num"));
				
				paramList.add(param);
			}
		}		
		return paramList;
	}
	
	/**
	 * 保存现存量
	 * @param bos 
	 * 
	 * @param prices
	 * @param invOnhandFuture
	 */
	private void createInvOnhand(JsonObject bos, JsonArray invOnhand, Future<JsonObject> invOnhandFuture) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		// 按照分页条件查询收货通知
		String onHandAddress = from_account + "." + this.appActivity.getService().getRealServiceName()
				+ ".istockonhand-mgr.batchcreate";
		this.appActivity.getEventBus().send(onHandAddress, invOnhand, invRet -> {
			if (invRet.succeeded()) {
				invOnhandFuture.complete(bos);
			} else {
				Throwable errThrowable = invRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				invOnhandFuture.fail(invRet.cause());
			}
		});
	}
	
}
