package ocr.inventorycenter.shelfwarning;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ocr.inventorycenter.stockonhand.StockOnHandQueryHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * TODO: 保质期预警
 * @date 2016年12月10日
 * @author wanghw
 */
public class ShelfWarningQueryHandler extends ActionHandlerImpl<JsonObject> {
	
	public ShelfWarningQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ShelfWarningConstant.QUERY_ADDRESS;
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
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		String onHandAddress = from_account + "." + this.appActivity.getService().getRealServiceName()
				+ ".stockonhand-mgr.query4shelfwarning";
		this.appActivity.getEventBus().send(onHandAddress, null, invRet -> {
			if (invRet.succeeded()) {
				msg.reply(invRet.result());
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
