package ocr.inventorycenter.safestockwarning;

import java.time.LocalDate;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * TODO: 安全库存预警
 * 
 * @date 2016年12月10日
 * @author wanghw
 */
public class SafeStockWarningQueryHandler extends ActionHandlerImpl<JsonObject> {

	public SafeStockWarningQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return SafeStockWarningConstant.QUERY_ADDRESS;
	}

	/**
	 * 要查询的单据状态
	 * 
	 * @return
	 */
	public String getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 查询现存量
	 */
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg){
		String from_account = this.appActivity.getAppInstContext().getAccount();
		String onHandAddress = from_account + "." + this.appActivity.getService().getRealServiceName()
				+ ".stockonhand-mgr.query4shelfwarning";
		this.appActivity.getEventBus().send(onHandAddress, null, invRet -> {
			if (invRet.succeeded()) {
				JsonArray ret = (JsonArray) invRet.result().body();
				for (Object onhand : ret) {
					JsonObject _id = ((JsonObject) onhand).getJsonObject("_id");
					String shelf_life = _id.getString("shelf_life");			
					LocalDate shelf_date = LocalDate.parse(shelf_life);
					LocalDate now_date = LocalDate.now();
					long remain_days = shelf_date.toEpochDay() - now_date.toEpochDay();
					_id.put("remain_day", remain_days);//剩余天数
					//设置是否预警---如果已经过期，则报警
					if(remain_days < 0){
						_id.put("isWarning", "过期");
					}else{
						_id.put("isWarning", "未过期");
					}
				}
				msg.reply(ret);
			} else {
				Throwable errThrowable = invRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.reply(invRet.cause());
			}
		});
	}

	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

}
