package ocr.inventorycenter.invorg;


import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleDocQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudBusMessage;
/**
 * 库存中心：仓库
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class InvWarehouseRemoveHandler  extends SampleDocQueryHandler {
	

	public InvWarehouseRemoveHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		JsonObject query = msg.body();

		appActivity.getAppDatasource().getMongoClient().find(appActivity.getDBTableName(appActivity.getBizObjectType()),
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
		return InvWarehouseConstant.QUERY_ADDRESS;
	}
	
}
