package ocr.inventorycenter.invarea;


import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleSingleDocBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;
/**
 * 库存中心：库区-查询
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class InvAreaRemoveHandler  extends SampleSingleDocBaseHandler {
	

	public InvAreaRemoveHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		JsonObject query = msg.body();

		appActivity.getAppDatasource().getMongoClient().removeDocument(appActivity.getDBTableName(appActivity.getBizObjectType()),
				query,
				result -> {
					if (result.succeeded()) {
						msg.reply(result.result());
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
					}
				});
	}

	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return InvAreaConstant.REMOVE_ADDRESS;
	}
	
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, null, InvAreaConstant.REMOVE_STATUS);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}
	
}
