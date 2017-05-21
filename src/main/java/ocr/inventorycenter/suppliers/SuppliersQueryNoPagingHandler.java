package ocr.inventorycenter.suppliers;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleSingleDocQueryHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 库存中心：库区-查询
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class SuppliersQueryNoPagingHandler extends SampleSingleDocQueryHandler {

//	public static final String ADDRESS = "querylist";

	public SuppliersQueryNoPagingHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return "query-nopaging";
	}

	// 处理器
	@Override
	public void handle(CommandMessage<JsonObject> msg) {

		this.queryBizDataList(null, appActivity.getBizObjectType(), null, findRet -> {
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

	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.GET);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}
}
